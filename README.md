# AcademiAI

Prototipo de apoyo académico para estudiantes de primer año. La app Android permite conversar con un tutor de IA, consultar evaluaciones y registrar el estado de ánimo. Un panel docente muestra estudiantes según un nivel de riesgo calculado a partir de palabras clave en el chat.

## Cómo funciona

1. La app Android, hecha con Kotlin y Jetpack Compose, envía solicitudes al backend FastAPI.
2. El backend guarda perfiles, evaluaciones, conversaciones y alertas en Firebase Authentication y Firestore.
3. Para preguntas académicas, el backend envía a Ollama el mensaje, parte del historial y las evaluaciones pendientes. La respuesta llega a la app por *Server-Sent Events* (SSE).
4. Las consultas de agenda y planes de estudio simples se responden con reglas en Python. La detección de estrés también usa palabras clave y actualiza el nivel de riesgo que aparece en el panel docente.

La carpeta `backend/static` contiene una página para crear cuentas de demostración con datos académicos ficticios.

## Tecnologías

- Android: Kotlin, Jetpack Compose, Material 3, Navigation Compose y OkHttp.
- Backend: Python, FastAPI y Pydantic.
- Datos: Firebase Authentication y Cloud Firestore.
- IA local: Ollama con `llama3.2:3b` por defecto.

## Configuración local

1. Crea un proyecto propio en Firebase. Descarga `google-services.json` en `app/` y una clave de cuenta de servicio en `backend/firebase-credentials.json`. Estos archivos están excluidos de Git.
2. Instala Python 3.12 y las dependencias del backend:

   ```powershell
   cd backend
   python -m pip install -r requirements.txt
   Copy-Item .env.example .env
   ```

3. Instala Ollama, descarga el modelo con `ollama pull llama3.2:3b` y ejecuta `ollama serve`.
4. Desde `backend/`, inicia el servidor con `python main.py`. La API estará en `http://127.0.0.1:8000/docs`.
5. Abre la carpeta raíz con Android Studio y un JDK 17 o posterior, y ejecuta `app`. El emulador usa `10.0.2.2:8000` para llegar al backend local. Para probar desde un teléfono en la misma red, cambia `SERVER_HOST` a `0.0.0.0` en `backend/.env`, reinicia el backend y configura en la app la IP local del computador.

El proyecto incluye la configuración de Gradle 8.5, pero no los ejecutables del *Gradle Wrapper*. Android Studio puede sincronizarlo con una instalación local de Gradle.

## Estado del prototipo

El código implementa chat, evaluaciones, registro de ánimo, historial y vista docente. No hay resultados medidos de aprendizaje o retención estudiantil en este repositorio. La detección de riesgo es una heurística por palabras clave, y algunas cifras de la interfaz son de demostración.

**Uso de demostración:** el backend actual no verifica la contraseña en el inicio de sesión y sus rutas de datos no exigen un token de autenticación. No lo expongas a Internet ni lo uses con datos reales sin completar la autenticación, el control de roles y la protección de datos.
