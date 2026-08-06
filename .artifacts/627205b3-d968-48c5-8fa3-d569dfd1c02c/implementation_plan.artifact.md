# Plan de Implementación: Respaldo y Sincronización en Google Drive

Este plan detalla la implementación de un sistema de copia de seguridad en la nube integrado en la pantalla de Perfil, permitiendo a los usuarios asegurar sus datos financieros en su cuenta de Google.

## User Review Required

> [!IMPORTANT]
> **Permisos de Google Drive:** Se utilizará el scope `drive.appdata` para almacenar el archivo en una carpeta oculta de la aplicación. Esto previene que el usuario borre el archivo manualmente desde su Google Drive personal.
> **Sobreescritura de Datos:** La función "Restaurar" sobreescribirá la base de datos local actual. Se mostrará un diálogo de confirmación obligatorio antes de proceder.
> **WorkManager:** El respaldo automático requiere conexión a internet y se ejecutará en segundo plano según la frecuencia seleccionada (Diario, Semanal, Mensual).

## Proposed Changes

### [Componente] Configuración y Dependencias

#### [MODIFY] [libs.versions.toml](file:///C:/proyectos/MyLittleFinanControl/gradle/libs.versions.toml)
- Añadir versiones y librerías para Google API Client, Google Drive API y WorkManager.

#### [MODIFY] [build.gradle.kts](file:///C:/proyectos/MyLittleFinanControl/app/build.gradle.kts)
- Implementar las nuevas dependencias.

### [Componente] Interfaz de Usuario (UI)

#### [MODIFY] [fragment_perfil.xml](file:///C:/proyectos/MyLittleFinanControl/app/src/main/res/layout/fragment_perfil.xml)
- Añadir una nueva `MaterialCardView` con:
    - Estado de vinculación de Google.
    - Botones "Respaldar Ahora" y "Restaurar".
    - Selector de frecuencia (ExposedDropdownMenu).
    - Indicador de progreso y fecha de última copia.

### [Componente] Lógica de Respaldo y Sincronización

#### [NEW] `GoogleDriveService.kt`
- Clase encargada de la comunicación con la API de Google Drive: autenticación, subida y descarga de archivos.

#### [NEW] `BackupManager.kt`
- Lógica de copia física de la base de datos SQLite (`/databases/finanzas.db`) hacia una ubicación temporal para su subida.
- Manejo del cierre y reapertura de `NotasDatabaseHelper` durante la restauración.

#### [NEW] `BackupWorker.kt`
- Implementación de `Worker` para ejecutar respaldos automáticos en segundo plano mediante WorkManager.

### [Componente] Integración en Perfil

#### [MODIFY] [FragmentPerfil.kt](file:///C:/proyectos/MyLittleFinanControl/app/src/main/java/com/example/mylittlefinancontrol/Fragmentos/FragmentPerfil.kt)
- Integrar la autenticación de Google con Credential Manager.
- Manejar los clics de los botones de respaldo y restauración.
- Persistir la configuración de frecuencia en SharedPreferences y agendar tareas en WorkManager.

## Verification Plan

### Automated Tests
- Se realizarán pruebas unitarias para la copia de archivos de base de datos.

### Manual Verification
1. **Autenticación:** Vincular una cuenta de Google y verificar que el correo se muestre correctamente.
2. **Respaldo Manual:** Presionar "Respaldar Ahora" y verificar la notificación de éxito.
3. **Restauración:** Modificar algunos datos locales, presionar "Restaurar" y verificar que los datos vuelvan al estado del respaldo.
4. **Respaldo Automático:** Cambiar la frecuencia a "Diario" y verificar mediante `adb shell dumpsys jobscheduler` que la tarea de WorkManager esté agendada.
