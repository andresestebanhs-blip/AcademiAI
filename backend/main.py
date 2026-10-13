"""
AcademiAI Backend - Servidor Principal FastAPI
Conecta la app Android con Firebase + Ollama.
"""
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse, HTMLResponse, FileResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel
from dotenv import load_dotenv
import json, os, random
from datetime import datetime, timedelta

load_dotenv()

import firebase_service as fb
import ollama_service as ai

app = FastAPI(title="AcademiAI Backend", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# Servir archivos estáticos (landing page)
STATIC_DIR = os.path.join(os.path.dirname(__file__), "static")
if os.path.exists(STATIC_DIR):
    app.mount("/static", StaticFiles(directory=STATIC_DIR), name="static")

@app.on_event("startup")
def startup():
    fb.init_firebase()
    print("🚀 AcademiAI Backend listo")
    print(f"🤖 Modelo: {os.getenv('OLLAMA_MODEL', 'llama3.2:3b')}")

# ─── Modelos de Request ─────────────────────────────────────

class RegisterRequest(BaseModel):
    email: str
    password: str
    name: str
    role: str  # "estudiante" o "docente"

class ChatRequest(BaseModel):
    uid: str
    message: str

class EvaluationRequest(BaseModel):
    uid: str
    subject: str
    eval_type: str
    date: str

class MoodRequest(BaseModel):
    uid: str
    mood: str

# ─── Endpoints: Auth ────────────────────────────────────────

@app.post("/api/register")
def register(req: RegisterRequest):
    try:
        profile = fb.create_user(req.email, req.password, req.name, req.role)
        return {"status": "ok", "user": profile}
    except Exception as e:
        # If it's an email already exists error, we can try to log them in for the prototype
        if "EMAIL_EXISTS" in str(e) or "already exists" in str(e).lower():
            try:
                print(f"Intento de login para {req.email}...")
                profile = fb.login_user(req.email, req.password)
                print(f"Login exitoso: {profile}")
                return {"status": "ok", "user": profile}
            except Exception as login_err:
                print(f"Error en login_user: {login_err}")
                raise HTTPException(400, str(login_err))
        print(f"Error en register (no es EMAIL_EXISTS): {e}")
        raise HTTPException(400, str(e))

@app.post("/api/login")
def login(req: RegisterRequest):
    try:
        profile = fb.login_user(req.email, req.password)
        return {"status": "ok", "user": profile}
    except Exception as e:
        raise HTTPException(400, str(e))

@app.get("/api/student/{uid}")
def get_student_dashboard(uid: str):
    try:
        profile = fb.get_user_profile(uid)
        evals = fb.get_evaluations(uid)
        return {"status": "ok", "user": profile, "evaluations": evals}
    except Exception as e:
        raise HTTPException(500, str(e))

@app.get("/api/user/{uid}")
def get_user(uid: str):
    profile = fb.get_user_profile(uid)
    if not profile:
        raise HTTPException(404, "Usuario no encontrado")
    return profile

@app.put("/api/mood")
def update_mood(req: MoodRequest):
    try:
        fb.get_db().collection("users").document(req.uid).update({"mood": req.mood})
        return {"status": "ok"}
    except Exception as e:
        raise HTTPException(500, str(e))

@app.get("/api/student/{uid}/history")
def get_student_history(uid: str):
    try:
        history = fb.get_chat_history(uid, limit=20)
        return {"status": "ok", "history": history}
    except Exception as e:
        raise HTTPException(500, str(e))

# ─── Endpoints: Chat IA ────────────────────────────────────

@app.post("/api/chat")
async def chat(req: ChatRequest):
    """Chat inteligente: usa Python para respuestas rápidas, Ollama para el resto."""
    # Guardar mensaje del estudiante
    fb.save_message(req.uid, req.message, "user")

    # Detectar estrés
    risk = ai.detect_stress(req.message)
    if risk in ("alto", "medio"):
        profile = fb.get_user_profile(req.uid)
        name = profile.get("name", "Estudiante") if profile else "Estudiante"
        fb.create_alert(req.uid, name, risk, f'Detectado en chat: "{req.message[:80]}"')
        # Actualizar nivel de riesgo y ánimo del estudiante
        new_mood = "estresado" if risk == "medio" else "mal"
        fb.get_db().collection("users").document(req.uid).update({
            "risk_level": risk,
            "mood": new_mood
        })

    # ── Filtro de contenido ──
    filter_result = ai.check_content_filter(req.message)
    if filter_result and filter_result.get("blocked"):
        response = filter_result["response"]
        fb.save_message(req.uid, response, "assistant")
        return {"type": "instant", "response": response}

    # Clasificar intención
    intent = ai.classify_intent(req.message)
    evaluations = fb.get_evaluations(req.uid)

    # Respuestas rápidas (Python, sin IA)
    if intent == "schedule":
        response = ai.quick_schedule_response(evaluations)
        fb.save_message(req.uid, response, "assistant")
        return {"type": "instant", "response": response}

    if intent == "study_plan":
        response = ai.quick_study_plan(evaluations)
        if response:
            fb.save_message(req.uid, response, "assistant")
            return {"type": "instant", "response": response}

    # Respuesta con IA (streaming)
    history = fb.get_chat_history(req.uid)

    user_profile = fb.get_user_profile(req.uid)
    
    # Construir contexto con datos reales del estudiante
    context_parts = []
    if user_profile:
        context_parts.append(f"Estudiante de: {user_profile.get('career', 'Carrera no especificada')}")
        context_parts.append(f"Semestre: {user_profile.get('semester', '?')}")
        context_parts.append(f"Contexto Académico: {user_profile.get('academic_context', 'Sin contexto')}\n")
        
    if evaluations:
        context_parts.append("Evaluaciones pendientes (fechas y descripciones):")
        for e in evaluations:
            ctx = e.get('context', 'Sin detalles adicionales.')
            context_parts.append(f"- {e.get('subject')} ({e.get('type')} el {e.get('date')}): {ctx}")
    context = "\n".join(context_parts)

    async def generate():
        full_response = ""
        async for token in ai.stream_chat(req.message, history, context):
            full_response += token
            yield f"data: {json.dumps({'token': token})}\n\n"
        # Guardar respuesta completa
        fb.save_message(req.uid, full_response, "assistant")
        yield f"data: {json.dumps({'done': True})}\n\n"

    return StreamingResponse(generate(), media_type="text/event-stream")

# ─── Endpoints: Evaluaciones ───────────────────────────────

@app.post("/api/evaluations")
def add_evaluation(req: EvaluationRequest):
    fb.add_evaluation(req.uid, req.subject, req.eval_type, req.date)
    return {"status": "ok"}

@app.get("/api/evaluations/{uid}")
def get_evaluations(uid: str):
    return fb.get_evaluations(uid)

# ─── Endpoints: Panel Docente ──────────────────────────────

@app.get("/api/alerts")
def get_alerts():
    return fb.get_alerts()

@app.get("/api/students")
def get_students():
    return fb.get_students_for_teacher()

# ─── Health Check ──────────────────────────────────────────

@app.get("/api/health")
def health():
    return {
        "status": "ok",
        "service": "AcademiAI Backend",
        "ollama_model": os.getenv("OLLAMA_MODEL", "llama3.2:3b")
    }

# ─── Demo: Registro con datos aleatorios ───────────────────

CAREERS = [
    {
        "name": "Ingeniería en Informática",
        "context": "Tiene dificultades con la programación orientada a objetos pero es excelente en análisis de sistemas.",
        "subjects": [
            {"subject": "Programación Avanzada", "types": ["Proyecto Final", "Examen Parcial"], "contexts": [
                "Desarrollar una aplicación de gestión de inventario en Java con interfaz gráfica.",
                "Examen teórico-práctico sobre herencia, polimorfismo y patrones de diseño."
            ]},
            {"subject": "Bases de Datos", "types": ["Examen Parcial", "Tarea"], "contexts": [
                "Examen teórico sobre normalización (hasta 3FN) y consultas SQL complejas con JOIN.",
                "Diseñar el modelo entidad-relación para un sistema de biblioteca."
            ]},
            {"subject": "Matemáticas Discretas", "types": ["Tarea Semanal", "Examen"], "contexts": [
                "Resolver guía de 20 ejercicios sobre teoría de grafos y árboles.",
                "Examen sobre lógica proposicional y teoría de conjuntos."
            ]},
            {"subject": "Redes de Computadores", "types": ["Laboratorio", "Examen"], "contexts": [
                "Configurar una red LAN simulada con subnetting en Packet Tracer.",
                "Examen sobre modelo OSI, TCP/IP y protocolos de enrutamiento."
            ]},
        ]
    },
    {
        "name": "Ingeniería Civil",
        "context": "Buen rendimiento en cálculo pero presenta dificultades con la resistencia de materiales.",
        "subjects": [
            {"subject": "Resistencia de Materiales", "types": ["Examen Parcial", "Laboratorio"], "contexts": [
                "Examen sobre esfuerzo, deformación y diagrama de Mohr.",
                "Ensayo de tracción en probetas de acero y análisis de resultados."
            ]},
            {"subject": "Cálculo III", "types": ["Examen", "Tarea"], "contexts": [
                "Examen sobre integrales de superficie y teorema de Stokes.",
                "Resolver 15 ejercicios de ecuaciones diferenciales ordinarias."
            ]},
            {"subject": "Topografía", "types": ["Proyecto", "Examen"], "contexts": [
                "Levantamiento topográfico del campus con estación total.",
                "Examen sobre curvas de nivel y sistemas de coordenadas."
            ]},
            {"subject": "Mecánica de Fluidos", "types": ["Examen", "Laboratorio"], "contexts": [
                "Examen sobre ecuación de Bernoulli y pérdidas de carga.",
                "Laboratorio de medición de caudal con vertederos."
            ]},
        ]
    },
    {
        "name": "Administración de Empresas",
        "context": "Excelente en trabajo en equipo y liderazgo, pero le cuesta el análisis financiero cuantitativo.",
        "subjects": [
            {"subject": "Contabilidad Financiera", "types": ["Examen Parcial", "Tarea"], "contexts": [
                "Examen sobre estados financieros, balance general y estado de resultados.",
                "Preparar el balance de una empresa ficticia con 20 transacciones."
            ]},
            {"subject": "Marketing Digital", "types": ["Proyecto", "Presentación"], "contexts": [
                "Diseñar una estrategia de marketing digital para una PYME local.",
                "Presentar el análisis de métricas de una campaña en redes sociales."
            ]},
            {"subject": "Estadística Empresarial", "types": ["Examen", "Tarea"], "contexts": [
                "Examen sobre distribuciones de probabilidad y pruebas de hipótesis.",
                "Análisis estadístico de datos de ventas usando Excel."
            ]},
            {"subject": "Gestión de Recursos Humanos", "types": ["Ensayo", "Examen"], "contexts": [
                "Ensayo sobre modelos de motivación laboral y su aplicación moderna.",
                "Examen sobre legislación laboral y contratos de trabajo."
            ]},
        ]
    },
    {
        "name": "Psicología",
        "context": "Muy empática y buena en análisis cualitativo, pero tiene dificultades con la estadística aplicada.",
        "subjects": [
            {"subject": "Psicología del Desarrollo", "types": ["Ensayo", "Examen"], "contexts": [
                "Ensayo comparativo entre las teorías de Piaget y Vygotsky.",
                "Examen sobre etapas del desarrollo humano y crisis normativas."
            ]},
            {"subject": "Estadística para Psicología", "types": ["Examen", "Tarea"], "contexts": [
                "Examen sobre ANOVA, correlación de Pearson y chi-cuadrado.",
                "Analizar un dataset de encuestas con SPSS y reportar hallazgos."
            ]},
            {"subject": "Psicopatología", "types": ["Examen Parcial", "Caso Clínico"], "contexts": [
                "Examen sobre trastornos del estado de ánimo según DSM-5.",
                "Análisis de caso clínico: diagnóstico diferencial y plan de intervención."
            ]},
            {"subject": "Metodología de la Investigación", "types": ["Proyecto", "Examen"], "contexts": [
                "Elaborar el marco teórico y diseño metodológico de una investigación.",
                "Examen sobre diseños experimentales y validez interna/externa."
            ]},
        ]
    },
]

class DemoRegisterRequest(BaseModel):
    name: str
    email: str
    password: str

@app.post("/api/demo-register")
def demo_register(req: DemoRegisterRequest):
    """Registra un usuario de demo con carrera y evaluaciones aleatorias."""
    try:
        # 1. Elegir carrera al azar
        career = random.choice(CAREERS)
        semester = random.randint(2, 6)

        # 2. Crear usuario en Firebase Auth
        from firebase_admin import auth
        try:
            user = auth.create_user(email=req.email, password=req.password, display_name=req.name)
        except Exception as e:
            if "EMAIL_EXISTS" in str(e) or "already exists" in str(e).lower():
                raise HTTPException(400, "⚠️ Este correo ya está registrado. Usa otro o inicia sesión directamente en la app.")
            raise HTTPException(400, str(e))

        # 3. Guardar perfil con carrera asignada
        profile = {
            "uid": user.uid,
            "name": req.name,
            "email": req.email,
            "role": "estudiante",
            "career": career["name"],
            "semester": semester,
            "academic_context": career["context"],
            "mood": "bien",
            "risk_level": "bajo",
            "created_at": datetime.now().isoformat()
        }
        fb.get_db().collection("users").document(user.uid).set(profile)

        # 4. Generar 3-4 evaluaciones aleatorias con fechas futuras
        selected_subjects = random.sample(career["subjects"], k=min(random.randint(3, 4), len(career["subjects"])))
        evaluations = []
        for subj in selected_subjects:
            type_idx = random.randint(0, len(subj["types"]) - 1)
            eval_type = subj["types"][type_idx]
            context = subj["contexts"][type_idx] if type_idx < len(subj["contexts"]) else ""
            # Fecha aleatoria entre 3 y 20 días en el futuro
            days_ahead = random.randint(3, 20)
            eval_date = (datetime.now() + timedelta(days=days_ahead)).strftime("%Y-%m-%d")

            eval_data = {
                "subject": subj["subject"],
                "type": eval_type,
                "date": eval_date,
                "context": context,
                "completed": False,
                "created_at": datetime.now().isoformat()
            }
            fb.get_db().collection("users").document(user.uid).collection("evaluations").add(eval_data)
            evaluations.append(eval_data)

        return {"status": "ok", "user": profile, "evaluations": evaluations}

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(500, f"Error interno: {str(e)}")

# ─── Landing Page ──────────────────────────────────────────

@app.get("/", response_class=HTMLResponse)
def landing_page():
    """Sirve la página de registro de demo."""
    html_path = os.path.join(STATIC_DIR, "demo.html")
    if os.path.exists(html_path):
        with open(html_path, "r", encoding="utf-8") as f:
            return HTMLResponse(content=f.read())
    return HTMLResponse(content="<h1>AcademiAI Backend está corriendo</h1>")

# ─── Run ───────────────────────────────────────────────────

if __name__ == "__main__":
    import uvicorn
    host = os.getenv("SERVER_HOST", "0.0.0.0")
    port = int(os.getenv("SERVER_PORT", "8000"))
    print(f"\n🎓 AcademiAI Backend iniciando en http://{host}:{port}")
    print(f"📱 Desde tu celular usa: http://<IP-DE-TU-PC>:{port}")
    print(f"📋 Documentación: http://localhost:{port}/docs\n")
    uvicorn.run(app, host=host, port=port)
