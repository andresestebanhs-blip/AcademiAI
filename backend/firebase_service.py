"""
AcademiAI Backend - Firebase Service
Maneja autenticación, Firestore y datos de estudiantes/docentes.
"""
import firebase_admin
from firebase_admin import credentials, auth, firestore
from datetime import datetime
import os

_db = None

def init_firebase():
    """Inicializa Firebase con las credenciales del service account."""
    global _db
    cred_path = os.getenv("FIREBASE_CREDENTIALS", "firebase-credentials.json")
    if not firebase_admin._apps:
        cred = credentials.Certificate(cred_path)
        firebase_admin.initialize_app(cred)
    _db = firestore.client()
    print("✅ Firebase conectado")

def get_db():
    return _db

# ─── Usuarios ───────────────────────────────────────────────

def create_user(email: str, password: str, name: str, role: str) -> dict:
    """Crea usuario en Firebase Auth y guarda perfil en Firestore."""
    user = auth.create_user(email=email, password=password, display_name=name)
    profile = {
        "uid": user.uid,
        "name": name,
        "email": email,
        "role": role,  # "estudiante" o "docente"
        "created_at": datetime.now().isoformat(),
        "mood": "bien",
        "risk_level": "bajo"
    }
    _db.collection("users").document(user.uid).set(profile)
    return profile

def get_user_profile(uid: str) -> dict:
    """Obtiene el perfil del usuario desde Firestore."""
    doc = _db.collection("users").document(uid).get()
    return doc.to_dict() if doc.exists else None

def login_user(email: str, password: str) -> dict:
    """Simula el login para el prototipo o usa auth para verificar si existe."""
    try:
        user = auth.get_user_by_email(email)
        # En un entorno real se verifica la contraseña mediante el SDK cliente
        # o la REST API de Firebase. Para el backend admin, retornamos el perfil.
        return get_user_profile(user.uid)
    except Exception as e:
        raise Exception("Credenciales inválidas o usuario no encontrado.")

def get_students_for_teacher() -> list:
    """Obtiene todos los estudiantes con su nivel de riesgo."""
    docs = _db.collection("users").where("role", "==", "estudiante").stream()
    return [doc.to_dict() for doc in docs]

# ─── Evaluaciones ───────────────────────────────────────────

def add_evaluation(uid: str, subject: str, eval_type: str, date: str, context: str = ""):
    """Agrega una evaluación al calendario del estudiante."""
    _db.collection("users").document(uid).collection("evaluations").add({
        "subject": subject,
        "type": eval_type,
        "date": date,
        "context": context,
        "completed": False,
        "created_at": datetime.now().isoformat()
    })

def get_evaluations(uid: str) -> list:
    """Obtiene las evaluaciones pendientes del estudiante."""
    docs = (_db.collection("users").document(uid)
            .collection("evaluations")
            .where("completed", "==", False)
            .stream())
    return [{"id": doc.id, **doc.to_dict()} for doc in docs]

# ─── Alertas ────────────────────────────────────────────────

def create_alert(student_uid: str, student_name: str, level: str, detail: str):
    """Crea una alerta para el panel docente."""
    _db.collection("alerts").add({
        "student_uid": student_uid,
        "student_name": student_name,
        "level": level,  # "alto", "medio", "bajo"
        "detail": detail,
        "read": False,
        "timestamp": datetime.now().isoformat()
    })

def get_alerts() -> list:
    """Obtiene todas las alertas ordenadas por fecha."""
    docs = (_db.collection("alerts")
            .order_by("timestamp", direction=firestore.Query.DESCENDING)
            .limit(50)
            .stream())
    return [{"id": doc.id, **doc.to_dict()} for doc in docs]

# ─── Chat History ───────────────────────────────────────────

def save_message(uid: str, content: str, role: str):
    """Guarda un mensaje en el historial (role: 'user' o 'assistant')."""
    _db.collection("users").document(uid).collection("messages").add({
        "content": content,
        "role": role,
        "timestamp": datetime.now().isoformat()
    })

def get_chat_history(uid: str, limit: int = 10) -> list:
    """Obtiene los últimos mensajes del estudiante."""
    docs = (_db.collection("users").document(uid)
            .collection("messages")
            .order_by("timestamp", direction=firestore.Query.DESCENDING)
            .limit(limit)
            .stream())
    messages = [doc.to_dict() for doc in docs]
    messages.reverse()  # Orden cronológico
    return messages
