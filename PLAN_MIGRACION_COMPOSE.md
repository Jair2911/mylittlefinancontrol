# Plan de Migración a Jetpack Compose — MyLittleFinanControl

Este documento sirve como hoja de ruta interactiva para migrar **MyLittleFinanControl** al 100% a **Jetpack Compose** siguiendo una estrategia incremental **Bottom-Up** (sin romper la compilación en ningún paso).

---

## Fase 1: Configuración de Gradle y Compilador de Compose
- [x] **Paso 1.1:** Agregar el plugin `org.jetbrains.kotlin.plugin.compose` y el BOM de Jetpack Compose en `gradle/libs.versions.toml`.
- [x] **Paso 1.2:** Agregar dependencias en `app/build.gradle.kts`:
  - `androidx.compose.ui:ui`, `ui-graphics`, `ui-tooling-preview`, `material3`, `material-icons-extended`
  - `androidx.activity:activity-compose`
  - `androidx.lifecycle:lifecycle-viewmodel-compose` y `lifecycle-runtime-compose`
  - `io.coil-kt:coil-compose` (para reemplazar Glide y CircleImageView)
- [x] **Paso 1.3:** Activar `compose = true` dentro de `buildFeatures` en `app/build.gradle.kts` (manteniendo `viewBinding = true` temporalmente).
- [x] **Paso 1.4:** Ejecutar `gradle_sync` y verificar que el proyecto compile sin errores.

---

## Fase 2: Sistema de Diseño en Compose (`DABA Design System`)
Crear el paquete `com.example.mylittlefinancontrol.ui.theme` replicando los tokens exactos de `colors.xml` y `themes.xml`:
- [x] **Paso 2.1 (`Color.kt`):** Definir paleta **Technical Precision (Light)** y **Engineered Obsidian (Dark)** (`primary_emerald`, `surface_bg`, `surface_card`, `income_green`, `expense_red`, `debt_amber`, bordes y textos).
- [x] **Paso 2.2 (`Shape.kt` y `Type.kt`):** Definir `Shapes` (`Small = 50%`, `Medium = 12.dp`, `Large = 16.dp`) y tipografía base.
- [x] **Paso 2.3 (`Theme.kt`):** Implementar `MyLittleFinanControlTheme` con soporte para modo claro/oscuro sincronizado reactivamente con `SharedPreferences("TemaPrefs")` y `CompositionLocal` para colores semánticos financieros (ingresos, gastos, deudas, badges).

---

## Fase 3: Componentes Reutilizables e Ítems de Lista (`res/layout/item_*.xml`)
Crear componentes `@Composable` con sus respectivos `@Preview` para reemplazar las vistas de los adaptadores:
- [x] **Paso 3.1:** `MovimientoInicioItem` ← `item_movimiento_inicio.xml`
- [x] **Paso 3.2:** `MovimientoItem` y `MovimientoHeaderItem` ← `item_movimiento.xml` y `item_movimiento_header.xml`
- [x] **Paso 3.3:** `AhorroCardItem` ← `item_ahorro.xml`
- [x] **Paso 3.4:** `DeudaCardItem` ← `item_deuda.xml`
- [x] **Paso 3.5:** `GastoFijoCardItem` y `MesControlAnualItem` ← `item_gasto_fijo.xml` y `item_mes_control_anual.xml`
- [x] **Paso 3.6:** `CategoriaResumenItem` ← `item_categoria_resumen.xml`
- [x] **Paso 3.7:** `ChatMensajeItem` ← `item_chat_mensaje.xml`

---

## Fase 4: Diálogos y BottomSheets (`res/layout/dialog_*.xml`)
Migrar cada cuadro de diálogo a `AlertDialog` o `ModalBottomSheet` de Material 3:
- [x] **Paso 4.1:** Asesor IA Gemini — `GeminiAdvisorDialogFragment.kt` y `layout_chat_gemini.xml`.
- [x] **Paso 4.2:** Diálogos informativos — `dialog_actualizacion_app.xml` (`AppUpdateHelper.kt`) y `dialog_centro_ayuda.xml` (`FragmentPerfil.kt`).
- [x] **Paso 4.3:** Diálogos rápidos de abono y métodos de pago — `dialog_abonar_ahorro.xml`, `dialog_abonar_deuda.xml` y `dialog_metodo_pago.xml`.
- [x] **Paso 4.4:** Diálogos de detalle — `dialog_detalle_categoria_bs.xml` (`DetallePeriodoActivity.kt`) y `dialog_control_anual_gasto.xml` (`FragmentGastosFijos.kt`).
- [x] **Paso 4.5:** Diálogos de formularios completos — `dialog_agregar_ahorro.xml`, `dialog_agregar_deuda.xml`, `dialog_agregar_gasto_fijo.xml` y `dialog_agregar_movimiento.xml`.

---

## Fase 5: Activities Secundarias y Autenticación (`setContent { ... }`)
Reemplazar `setContentView` por `setContent` en cada `Activity` independiente:
- [x] **Paso 5.1 (Flujo Auth):**
  - `OpcionesLoginActivity.kt` (`activity_opciones_login.xml`)
  - `LoginEmailActivity.kt` (`activity_login_email.xml`)
  - `RegistroEmailActivity.kt` (`activity_registro_email.xml`)
- [x] **Paso 5.2 (Edición de Perfil):** `EditarInformacion.kt` (`activity_editar_informacion.xml`).
- [x] **Paso 5.3 (Operaciones de Movimiento):**
  - `EliminarMovimientoActivity.kt`
  - `ActualizarMovimientoActivity.kt` (`activity_actualizar_movimiento.xml`)
  - `AgregarMovimientoActivity.kt` (`activity_agregar_movimiento.xml`)
- [x] **Paso 5.4 (Detalle de Periodo):** `DetallePeriodoActivity.kt` (`activity_detalle_periodo.xml`).

---

## Fase 6: Los 6 Fragments Principales (con `@Composable` Stateful + Stateless Screens)
Convertir la vista de cada sección principal a pantallas `@Composable`:
- [x] **Paso 6.1 (Inicio):** `FragmentInicio.kt` (`InicioStatefulScreen` + `InicioScreen`).
- [x] **Paso 6.2 (Ahorros):** `FragmentAhorros.kt` (`AhorrosStatefulScreen` + `AhorrosScreen`).
- [x] **Paso 6.3 (Deudas / Balance):** `FragmentBalance.kt` (`DeudasStatefulScreen` + `DeudasScreen`).
- [x] **Paso 6.4 (Gastos Fijos):** `FragmentGastosFijos.kt` (`GastosFijosStatefulScreen` + `GastosFijosScreen`).
- [x] **Paso 6.5 (Perfil y Ajustes):** `FragmentPerfil.kt` (`PerfilStatefulScreen` + `PerfilScreen`).
- [x] **Paso 6.6 (Movimientos y OCR):** `FragmentMovimiento.kt` (`MovimientoStatefulScreen` + `MovimientoScreen`).

---

## Fase 7: `MainActivity`, Navegación en Compose y Tour Guiado
- [x] **Paso 7.1:** Migrar `MainActivity.kt` (`activity_main.xml`) a `setContent` con `Scaffold` (`MainTopHeaderBar` con avatar e iniciales + `MainBottomNavigationBar` de 5 secciones).
- [x] **Paso 7.2:** Reemplazar transacciones de `FragmentManager` por navegación declarativa en Compose (`MainHostScreen` + `MainSection`).
- [x] **Paso 7.3:** Migrar el tour guiado de bienvenida a Compose (`OnboardingTourDialog`).
- [ ] **Paso 7.4 (Opcional tras validación final):** Eliminar archivos XML heredados de `app/src/main/res/layout/`, adaptadores antiguos y desactivar `viewBinding = false`.
