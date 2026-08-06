# Walkthrough: Respaldo y Sincronización en Google Drive

Se ha implementado un sistema robusto de copias de seguridad en la nube, integrado directamente en la sección de Perfil de la aplicación.

## Características Implementadas

### 1. Gestión de Cuenta de Google
- **Vinculación Segura:** El usuario puede vincular su cuenta de Google directamente desde el perfil. Se utiliza el scope `drive.appdata` para asegurar que los archivos se guarden en un espacio privado y seguro, invisible para el usuario en su Drive general.
- **Estado en Tiempo Real:** La interfaz indica si la cuenta está vinculada y el correo electrónico asociado.

### 2. Control de Copias de Seguridad
- **Respaldo Manual:** Un botón para subir inmediatamente la base de datos actual a la nube. Ideal antes de cambiar de dispositivo o formatear.
- **Restauración Inteligente:** Permite bajar el último respaldo guardado. Incluye una advertencia de seguridad, ya que sobreescribirá los datos locales actuales para asegurar la consistencia.

### 3. Automatización (WorkManager)
- **Frecuencia Seleccionable:** El usuario puede elegir entre "Desactivado", "Diario", "Semanal" o "Mensual".
- **Inteligencia en Segundo Plano:** Las tareas se ejecutan automáticamente utilizando WorkManager, respetando condiciones como la conexión a internet.

## Detalles Técnicos
- **Google Drive API v3:** Utilizada para la transferencia eficiente de archivos `.db`.
- **SQLite Checkpoint:** Se maneja el cierre de la base de datos durante la restauración para evitar corrupción de datos.
- **Material 3 UI:** El nuevo diseño utiliza tarjetas y menús desplegables modernos que se adaptan al tema claro/oscuro de la app.

> [!CAUTION]
> Al restaurar una copia de seguridad, los datos actuales del dispositivo se perderán y serán reemplazados por los de la nube. La aplicación se reiniciará automáticamente para cargar la nueva base de datos.

> [!TIP]
> Te recomendamos activar el respaldo "Diario" para que tus finanzas estén siempre protegidas sin que tengas que hacer nada.
