"""
AcademiAI Backend - Ollama Service
Chat IA con streaming + análisis de sentimiento + herramientas Python rápidas.
Incluye filtros de contenido y completitud de respuestas garantizada.
"""
import httpx
import json
import os
import re
from datetime import datetime, timedelta

OLLAMA_HOST = os.getenv("OLLAMA_HOST", "http://localhost:11434")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "llama3.2:3b")

SYSTEM_PROMPT = """Eres AcademiAI, un tutor académico virtual empático y cercano de una institución de educación superior. Tu rol es:

1. Acompañar al estudiante en su carrera universitaria.
2. Ayudarlo a organizar su tiempo de estudio y preparar evaluaciones.
3. Resolver dudas académicas de CUALQUIER materia: matemáticas, física, programación, historia, literatura, biología, química, derecho, psicología, ingeniería, administración, contabilidad, estadística, etc.
4. Cuando expliques un tema, sé claro, usa ejemplos concretos y si es apropiado, usa fórmulas o pasos numerados.
5. NUNCA dar diagnósticos de salud mental — deriva al servicio de bienestar estudiantil.
6. TIENES ACCESO A LOS DATOS DEL ESTUDIANTE. Si el estudiante te pregunta por sus fechas, tareas, proyectos o evaluaciones, MIRA EL CONTEXTO. 
7. NO DIGAS QUE NO TIENES ACCESO, sí lo tienes (se adjunta al final de tus instrucciones). Usa esa información para responderle con fechas y detalles específicos de lo que debe estudiar.
8. Si te preguntan algo que NO es académico (política, religión, contenido sexual, violencia, hackeo, drogas, armas, etc.), responde amablemente: "Eso está fuera de mi área 😅 Soy tu tutor académico, ¿te puedo ayudar con alguna materia o evaluación?"
9. SIEMPRE termina tus respuestas de forma completa. No dejes oraciones a medias. Si estás haciendo una lista, completa todos los puntos. Cierra siempre con un mensaje motivador breve.

Usa un tono cálido, cercano y motivador. Tutea al estudiante. Usa emojis con moderación.
Responde de forma concisa pero COMPLETA (máximo 4 párrafos cortos). NUNCA dejes una respuesta incompleta."""

# ─── Filtro de contenido ────────────────────────────────────

BLOCKED_TOPICS = [
    # Violencia y armas
    "cómo hacer una bomba", "como hacer una bomba", "fabricar armas", "hacer explosivo",
    "cómo matar", "como matar", "cómo hackear", "como hackear", "hackear wifi",
    "hackear cuenta", "hackear facebook", "hackear instagram", "hackear whatsapp",
    # Drogas
    "cómo hacer droga", "como hacer droga", "fabricar droga", "cocaína", "metanfetamina",
    "cultivar marihuana", "hacer crack",
    # Contenido sexual
    "contenido sexual", "pornografía", "porno", "desnudos", "sexo explícito",
    # Trampas académicas
    "hazme el examen", "responde mi prueba", "haz mi tarea completa",
    "dame las respuestas del examen", "copia de examen",
    # Otros
    "suicidio", "cómo suicidarme", "como suicidarme", "autolesión", "cortarme",
]

SOFT_BLOCKED_TOPICS = [
    # Temas no académicos que redirigimos amablemente
    "política", "religión", "fútbol", "partido de", "farándula", "chisme",
    "horóscopo", "signo zodiacal", "receta de cocina", "novia", "novio",
    "cuéntame un chiste", "cuentame un chiste",
]

def check_content_filter(message: str) -> dict | None:
    """
    Revisa si el mensaje contiene temas bloqueados.
    Retorna None si pasa el filtro, o un dict con la respuesta de bloqueo.
    """
    lower = message.lower().strip()
    
    # Temas gravemente bloqueados
    for topic in BLOCKED_TOPICS:
        if topic in lower:
            # Caso especial: salud mental → derivar con empatía
            if any(w in topic for w in ["suicid", "autolesión", "cortarme"]):
                return {
                    "blocked": True,
                    "response": (
                        "💙 Entiendo que estás pasando por un momento muy difícil, y quiero que sepas que no estás solo/a. "
                        "Yo soy un tutor académico y esto me queda grande, pero hay personas capacitadas que pueden ayudarte.\n\n"
                        "🆘 **Contacta a tu servicio de bienestar estudiantil** de la universidad o llama a una línea de ayuda:\n"
                        "• Chile: 600 360 7777 (Salud Responde)\n"
                        "• General: Acude al departamento de orientación de tu institución.\n\n"
                        "Tu bienestar es lo más importante. ❤️"
                    )
                }
            return {
                "blocked": True,
                "response": "Eso está fuera de mi área 😅 Soy tu tutor académico, ¿te puedo ayudar con alguna materia o evaluación?"
            }
    
    # Temas redirigidos amablemente
    for topic in SOFT_BLOCKED_TOPICS:
        if topic in lower:
            return {
                "blocked": True,
                "response": (
                    f"¡Ja! Me encantaría charlar de eso, pero soy un tutor académico 😄 "
                    f"¿En qué materia puedo echarte una mano? Estoy listo para ayudarte con tus estudios 📚"
                )
            }
    
    return None  # Pasa el filtro

# ─── Herramientas Python Rápidas (sin IA) ───────────────────

def quick_schedule_response(evaluations: list) -> str | None:
    """Respuesta rápida sobre evaluaciones sin llamar a Ollama."""
    if not evaluations:
        return "📅 No tienes evaluaciones registradas por ahora. ¿Quieres agregar alguna?"
    lines = ["📅 Tus próximas evaluaciones:\n"]
    for e in evaluations:
        lines.append(f"• **{e.get('subject', '?')}** ({e.get('type', '?')}) — {e.get('date', '?')}")
    lines.append("\n¿Quieres que te ayude a armar un plan de estudio? 😊")
    return "\n".join(lines)

def quick_study_plan(evaluations: list) -> str | None:
    """Genera un plan de estudio algorítmico sin usar IA."""
    if not evaluations:
        return None
    lines = ["📚 **Plan de estudio sugerido:**\n"]
    for i, e in enumerate(evaluations):
        subject = e.get("subject", "Materia")
        lines.append(f"**Día {i+1}-{i+2}:** Dedica 2 horas a {subject}")
        lines.append(f"  → 45 min estudio + 10 min descanso + 45 min repaso + 10 min pausa\n")
    lines.append("⏰ Recuerda: es mejor estudiar un poco cada día que todo la noche anterior.")
    lines.append("🏃 Incluye al menos 15 minutos de actividad física para reducir el estrés.")
    return "\n".join(lines)

# ─── Detección de Estrés ────────────────────────────────────

STRESS_KEYWORDS = [
    "no puedo más", "quiero dejar", "desertar", "abandonar la carrera",
    "estresado", "ansiedad", "agotado", "no aguanto", "no sirvo",
    "odio la carrera", "me quiero ir", "fracasé", "no doy más",
    "quiero rendirme", "todo me sale mal", "no tengo ganas"
]

def detect_stress(message: str) -> str:
    """Detecta nivel de riesgo basado en palabras clave."""
    lower = message.lower()
    matches = sum(1 for kw in STRESS_KEYWORDS if kw in lower)
    if matches >= 2:
        return "alto"
    elif matches >= 1:
        return "medio"
    return "bajo"

# ─── Clasificador de intención (Python rápido) ──────────────

def classify_intent(message: str) -> str:
    """Clasifica la intención del mensaje para decidir si usar IA o Python.
    
    IMPORTANTE: Solo devuelve 'schedule' o 'study_plan' cuando el usuario
    EXPLÍCITAMENTE pide ver su lista de fechas o armar un plan.
    Si el usuario pregunta algo SOBRE una evaluación (ej: "explícame la prueba de BD"),
    eso va a la IA para que responda con contexto.
    """
    lower = message.lower().strip()
    
    # ── Preguntas que SÍ van a la IA (tienen prioridad) ──
    # Si el usuario pregunta, pide detalles, o quiere explicación → IA
    ai_indicators = [
        "qué", "que ", "cómo", "como ", "por qué", "porque", "explica", "explícame",
        "ayúdame", "ayudame", "detalle", "dime sobre", "cuéntame", "cuentame",
        "qué entra", "que entra", "qué incluye", "que incluye", "qué temas",
        "que temas", "cómo estudio", "como estudio para", "tips", "consejo",
        "resumen", "prepararme", "preparar", "difícil", "dificil", "entiendo",
        "no entiendo", "me cuesta", "necesito ayuda", "puedes ayudar",
        "qué es", "que es", "define", "ejemplo", "diferencia entre",
    ]
    if any(w in lower for w in ai_indicators):
        return "chat"
    
    # ── Respuesta rápida: solo si pide VER la lista de evaluaciones ──
    schedule_phrases = [
        "mis evaluaciones", "mis pruebas", "mis exámenes", "mis examenes",
        "mis tareas", "mi calendario", "mis fechas", "qué tengo pendiente",
        "que tengo pendiente", "mostrar evaluaciones", "ver evaluaciones",
        "listar evaluaciones", "próximas evaluaciones", "proximas evaluaciones",
        "cuándo es", "cuando es", "qué fecha", "que fecha",
    ]
    if any(phrase in lower for phrase in schedule_phrases):
        return "schedule"
    
    # ── Respuesta rápida: solo si pide ARMAR un plan de estudio ──
    study_phrases = [
        "plan de estudio", "arma un plan", "hazme un plan", "organizar mi estudio",
        "planificar mi estudio", "organizar mis tiempos",
    ]
    if any(phrase in lower for phrase in study_phrases):
        return "study_plan"
        
    return "chat"  # Todo lo demás va a la IA

# ─── Chat con Ollama (streaming) ────────────────────────────

async def stream_chat(prompt: str, history: list, context: str = ""):
    """Genera respuesta streaming desde Ollama con reintentos de completitud."""
    # Construir historial conversacional
    history_text = ""
    for msg in history[-8:]:  # Últimos 8 mensajes
        role = "Estudiante" if msg.get("role") == "user" else "AcademiAI"
        history_text += f"{role}: {msg.get('content', '')}\n"

    full_prompt = f"{history_text}Estudiante: {prompt}\nAcademiAI:"

    system = SYSTEM_PROMPT
    if context:
        system += f"\n\nCONTEXTO DEL ESTUDIANTE:\n{context}"

    body = {
        "model": OLLAMA_MODEL,
        "prompt": full_prompt,
        "system": system,
        "stream": True,
        "options": {
            "temperature": 0.7,
            "num_predict": 800,        # Mucho más espacio para completar
            "top_p": 0.9,
            "repeat_penalty": 1.1,     # Evita loops repetitivos
            "stop": ["\nEstudiante:", "\n\nEstudiante:"]  # Para que no simule al usuario
        }
    }

    accumulated = ""
    
    async with httpx.AsyncClient(timeout=180.0) as client:
        async with client.stream("POST", f"{OLLAMA_HOST}/api/generate", json=body) as response:
            async for line in response.aiter_lines():
                if line:
                    try:
                        chunk = json.loads(line)
                        token = chunk.get("response", "")
                        if token:
                            accumulated += token
                            yield token
                        if chunk.get("done", False):
                            break
                    except json.JSONDecodeError:
                        continue
    
    # ─── Verificar completitud ──────────────────────────────
    # Si la respuesta termina cortada (sin punto, sin emoji, sin signo de cierre),
    # generar un cierre automático
    stripped = accumulated.strip()
    if stripped and not _response_looks_complete(stripped):
        # Generar un cierre corto sin streaming (append directo)
        closure = await _generate_closure(client=None, accumulated=stripped, system=system)
        if closure:
            yield closure


def _response_looks_complete(text: str) -> bool:
    """Verifica si una respuesta parece estar completa."""
    text = text.strip()
    if not text:
        return False
    
    last_char = text[-1]
    # Termina en puntuación de cierre, emoji, o signo de exclamación/interrogación
    good_endings = ".!?😊🎯💪📚✅🚀❤️😄👍🏆✨🙌😉💡🤓📖📝"
    
    if last_char in good_endings:
        return True
    
    # Termina en una palabra completa (no cortada a medias)
    # Verificar si hay paréntesis o comillas sin cerrar
    if text.count("(") > text.count(")"):
        return False
    if text.count("**") % 2 != 0:
        return False
    
    # Si termina en una letra o dígito, probablemente está cortado
    if last_char.isalpha() or last_char.isdigit():
        return False
    
    return True


async def _generate_closure(client, accumulated: str, system: str) -> str:
    """Genera un cierre para una respuesta incompleta."""
    try:
        closure_body = {
            "model": OLLAMA_MODEL,
            "prompt": f"Completa esta respuesta cortada del tutor AcademiAI (agrega SOLO la frase final de cierre, máximo 1 oración motivadora):\n\n{accumulated[-200:]}",
            "system": "Eres un asistente que completa frases cortadas. Solo agrega las palabras faltantes para cerrar la idea. Sé muy breve.",
            "stream": False,
            "options": {
                "temperature": 0.5,
                "num_predict": 60
            }
        }
        async with httpx.AsyncClient(timeout=30.0) as c:
            resp = await c.post(f"{OLLAMA_HOST}/api/generate", json=closure_body)
            if resp.status_code == 200:
                data = resp.json()
                closure = data.get("response", "").strip()
                if closure and len(closure) < 200:
                    return " " + closure
    except Exception:
        # Si falla el cierre, agregar un cierre genérico
        return "\n\n¡Éxito en tus estudios! 💪"
    return ""
