# 🏦 DABAFINN (MyLittleFinanControl)
### *Sistema Inteligente de Gestión de Finanzas Personales para Android*

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Android SDK](https://img.shields.io/badge/Android_SDK-API_24--35-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![SQLite](https://img.shields.io/badge/SQLite-WAL_Mode-003B57?style=for-the-badge&logo=sqlite&logoColor=white)](https://www.sqlite.org/)
[![Firebase](https://img.shields.io/badge/Firebase-Auth_%26_RealtimeDB-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)](https://firebase.google.com/)
[![Google Drive](https://img.shields.io/badge/Google_Drive_API-v3-4285F4?style=for-the-badge&logo=googledrive&logoColor=white)](https://developers.google.com/drive)
[![Google ML Kit](https://img.shields.io/badge/Google_ML_Kit-OCR_Offline-FF6F00?style=for-the-badge&logo=google&logoColor=white)](https://developers.google.com/ml-kit)
[![Gemini AI](https://img.shields.io/badge/Google_Gemini-Generative_AI-8E75B2?style=for-the-badge&logo=googlegemini&logoColor=white)](https://ai.google.dev/)

---

## 📖 Descripción General

**DABAFINN (MyLittleFinanControl)** es una aplicación móvil nativa para Android diseñada bajo la filosofía **Offline-First**, garantizando rendimiento extremo, privacidad de datos y un control contable riguroso. Su arquitectura está optimizada para ejecutar animaciones a 60 FPS estables y responder a consultas en menos de 10 ms, incluso en dispositivos de gama de entrada con almacenamiento eMMC 5.1.

El sistema permite gestionar ingresos, gastos, compromisos fijos quincenales/mensuales, deudas amortizables y cajas de ahorro intocables. Integra un **lector inteligente offline de facturas y SMS bancarios por OCR**, analítica financiera local basada en reglas deterministas (exportable a PDF) y un asistente conversacional con **IA adaptativa mediante Google Gemini**.

---

## ✨ Características Principales (Módulos)

### 📊 1. Dashboard Inteligente (Home)
- **Separación Contable Estricta:** Distingue el **Balance Disponible** (dinero líquido real para gastar hoy: `Ingresos - Gastos - Ahorros`) de **Mis Ahorros (Intocable)** (patrimonio congelado en metas/fondos).
- **Modo Incógnito (`****`):** Oculta instantáneamente todos los montos sensibles de la pantalla con un solo toque.
- **Resúmenes Temporales:** Tarjetas de rendimiento en tiempo real para periodos Semanal, Mensual, Semestral y Anual.

### 📝 2. Gestión de Movimientos e Inserción Continua
- **Flujo de Inserción Continua (*"¿Quieres agregar otro?"*):** Al crear un registro, el sistema permite encadenar múltiples transacciones vaciando las entradas y manteniendo el foco en la pantalla para una captura ágil de datos.
- **Categorías Dinámicas:** Creación de categorías al vuelo mediante la opción `"Otra..."`, guardándolas por módulo (`MOVIMIENTO`, `GASTO_FIJO`, `DEUDA`) en SQLite.

### 📷 3. Captura Automática OCR & SMS (100% Offline)
- **Lector OCR de Comprobantes:** Procesa pantallazos y facturas en milisegundos con **Google ML Kit Text Recognition** sin consumir datos de red.
- **SmsBancarioReceiver:** Escucha pasiva de SMS bancarios con normalización automática de horas 12h AM/PM a 24h (`HH:mm`).
- **Motor Anti-Duplicados:** Algoritmo por coincidencia de monto y ventana temporal (±5 min) para evitar registros dobles de un mismo recibo.

### 📅 4. Control Anual de Gastos Fijos
- **Matriz de Control Anual:** Visualización de los 12 meses del año en modo mensual o quincenal (`Q1`/`Q2`).
- **Excepción de Marcado Manual:** Soporta marcar periodos como *"Pagados por otro medio"* mediante la tabla persistente `control_anual_gastos_fijos`, conservando la validez del historial histórico sin alterar el balance de caja.

### 💳 5. Deudas Amortizables y Cajas de Ahorro
- **Control de Deudas:** Asignación bidireccional de movimientos tipo `"PAGO DEUDA"`, recalculando atómicamente el saldo restante (`totalPagado`).
- **Cajas de Ahorro Clasificadas:** Clasificación por ubicación física/virtual (*Cuenta Bancaria / Bolsillo*, *Alcancía / Efectivo*, *Inversión / CDT*).

### 🤖 6. Analítica Financiera Híbrida
- **`AnalizadorFinancieroLocal` (Motor Local <10 ms):** Evalúa la Regla 50/30/20, tasa de ahorro, fuga de capitales (Ley de Pareto 80/20), rigidez presupuestaria y ritmo de gasto diario (*Burn Rate*). Permite exportar informes completos en PDF.
- **`GeminiAdvisorDialogFragment` (IA Conversacional):** Asesor financiero con reglas de proporcionalidad: respuestas ultra-cortas (2-4 líneas) para consultas puntuales y planes estructurados para estrategias complejas.

### 🔒 7. Métodos de Pago y Seguridad
- Almacenamiento seguro en Firebase Realtime Database (`/usuarios/{uid}/metodos_pago`) guardando únicamente el alias y los últimos 4 dígitos por privacidad (`"Efectivo"` por defecto).

### ☁️ 8. Respaldo en Google Drive y Actualizaciones OTA
- **`BackupManager`:** Copias de seguridad cifradas/ocultas en el directorio privado `appDataFolder` de la cuenta de Google Drive del usuario.
- **`AppUpdateHelper`:** Sistema In-App OTA que detecta nuevas versiones en Firebase y descarga/instala el APK automáticamente vía `DownloadManager` y `FileProvider`.

---

## 🏗️ Arquitectura y Estructura del Proyecto

El proyecto sigue una arquitectura en capas basada en **Model-View-Binding (MVB)** con componentes de arquitectura Android (Jetpack, Coroutines, LiveData, WorkManager).

```
com.example.mylittlefinancontrol/
├── Adaptadores/             # RecyclerView Adapters (NotasAdaptador, DeudasAdapter, etc.)
├── Modelos/                # Data Classes (Movimiento, GastoFijo, Deuda, Ahorro, MetodoPago, etc.)
├── Fragmentos/             # UI Components (FragmentInicio, FragmentMovimiento, FragmentGastosFijos, etc.)
├── Servicios/              # Lógica de Negocio y Conectores
│   ├── AnalizadorFinancieroLocal.kt
│   ├── AppUpdateHelper.kt
│   ├── BackupManager.kt
│   ├── GoogleDriveService.kt
│   ├── SmsBancarioReceiver.kt
│   └── TransaccionBancariaParser.kt
├── AgregarMovimientoActivity.kt
├── DetallePeriodoActivity.kt
├── GeminiAdvisorDialogFragment.kt
├── MainActivity.kt
└── NotasDatabaseHelper.kt  # Capa de Persistencia SQLite Nivel Experto
```

---

## 💾 Esquema de Base de Datos SQLite (v14)

La persistencia local se gestiona mediante `NotasDatabaseHelper.kt` configurado en modo **Write-Ahead Logging (WAL)** para lecturas paralelas sin bloqueos en el hilo de interfaz.

| Tabla | Columnas Principales | Descripción |
| :--- | :--- | :--- |
| `movimiento` | `id`, `descripcion`, `valor`, `tipo_reg`, `fecha`, `categoria`, `metodo_pago`, `gasto_fijo_id`, `quincena_pagada`, `deuda_id`, `ahorro_id` | Transacciones contables de caja. |
| `gastos_fijos` | `id`, `concepto`, `monto`, `diaPago`, `periodo`, `categoria`, `recordatorio` | Registro de compromisos fijos. |
| `control_anual_gastos_fijos` | `gasto_fijo_id`, `anio_mes`, `quincena`, `pagado_manual` | Excepciones de marcado manual del checklist anual. |
| `deudas` | `id`, `tipo`, `concepto`, `totalDeuda`, `totalPagado`, `tasa_interes`, `cuotas`, `valor_cuota` | Deudas y pasivos con amortización. |
| `ahorros` | `id`, `titulo`, `categoria`, `monto_actual`, `monto_objetivo`, `ubicacion` | Cajas, fondos y metas de ahorro. |
| `categorias` | `id`, `nombre`, `modulo`, `icono` | Categorías dinámicas dinámicamente configurables. |
| `metodos_pago` | `id`, `nombre`, `tipo`, `ultimos4`, `es_predeterminado` | Métodos de pago registrados por el usuario. |

---

## 🛠️ Requisitos e Instalación para Desarrollo

### Requisitos Previos
- **Android Studio** Ladybug (2024.2.1) o superior.
- **JDK 11** o superior.
- Dispositivo físico o emulador con **Android 7.0 (API 24)** hasta **Android 14 (API 35)**.
- Proyecto en **Firebase Console** con *Authentication* (Google & Email) y *Realtime Database* activados.
- Proyecto en **Google Cloud Console** con *Google Drive API* habilitada.

### Archivos de Configuración Requeridos (No Versionados)

1. **`local.properties`** (en la raíz del proyecto):
   ```properties
   sdk.dir=C\:\\Users\\TU_USUARIO\\AppData\\Local\\Android\\Sdk
   GEMINI_API_KEY=tu_api_key_de_google_ai_studio
   ```

2. **`app/google-services.json`**:
   Descarga el archivo desde Firebase Console e inclúyelo en la carpeta `app/`. Asegúrate de registrar la huella digital **SHA-1** de tu clave de depuración (`debug.keystore`).

3. **Estructura del Nodo en Firebase Realtime Database**:
   ```json
   {
     "configuracion_app": {
       "actualizacion": {
         "versionCode": 3,
         "versionName": "1.2",
         "novedades": "• Nuevo módulo de In-App OTA Updater.\n• Optimización en balance de ahorros.",
         "urlApk": "https://github.com/TU_USUARIO/mylittlefinancontrol/releases/download/v1.2/DABAFINN_v1.2.apk",
         "obligatoria": false
       }
     }
   }
   ```

---

## 🚀 Flujo de Compilación y Despliegue (OTA Updates)

Para publicar una nueva versión OTA (Over-The-Air) para los usuarios:

1. Incrementa la versión en `app/build.gradle.kts`:
   ```kotlin
   versionCode = 3
   versionName = "1.2"
   ```
2. Genera el archivo APK desde el menú de Android Studio: `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
3. Crea un **Release** en tu repositorio de GitHub e sube el APK generado.
4. Copia el enlace directo de descarga del APK subido en GitHub Releases.
5. Actualiza los campos `versionCode`, `versionName`, `novedades` y `urlApk` en la consola de Firebase en `/configuracion_app/actualizacion`.
6. ¡Listo! La app detectará automáticamente la versión al iniciar o cuando el usuario pulse *"Buscar actualizaciones"* en su Perfil.

---

## 👨‍💻 Autor y Créditos

Desarrollado con pasión por:

**Jair Alexander Hernández Roser**  
*Ingeniero Informático & Desarrollador Android Senior*  
**DABA** (*Development Advanced/Basic Assistance*)  
*“Soluciones tecnológicas integrales”*

---

*Desarrollado para ofrecer control financiero total, 100% privado y de alto rendimiento.*
