# Plan de Ajuste de Temas y Colores

Este plan detalla los cambios necesarios para estandarizar el uso de colores en la aplicación, mejorar la consistencia visual y asegurar que el modo oscuro esté correctamente implementado.

## User Review Required

> [!IMPORTANT]
> Se cambiará el fondo del modo oscuro de un verde claro a un verde muy oscuro (casi negro) para cumplir con los estándares de diseño de modo oscuro.
> Se reemplazarán los colores fijos (como `@color/primary`) por atributos de tema (como `?attr/colorPrimary`) en los layouts para permitir que la aplicación se adapte automáticamente al cambiar entre modo claro y oscuro.

## Proposed Changes

### [Componente] Recursos de Color y Temas

#### [MODIFY] [colors.xml](file:///C:/proyectos/MyLittleFinanControl/app/src/main/res/values/colors.xml)
- Definir una paleta más clara para el modo oscuro (tonos pasteles de verde) para asegurar legibilidad sobre fondos oscuros.
- Asegurar que los nombres de los colores sean descriptivos.

#### [MODIFY] [themes.xml (Light)](file:///C:/proyectos/MyLittleFinanControl/app/src/main/res/values/themes.xml)
- Ajustar `colorPrimary`, `colorSecondary`, `android:windowBackground`, y `colorSurface` para el modo claro.
- Usar `background_light` para el fondo.

#### [MODIFY] [themes.xml (Dark)](file:///C:/proyectos/MyLittleFinanControl/app/src/main/res/values-night/themes.xml)
- Ajustar `colorPrimary`, `colorSecondary`, `android:windowBackground`, y `colorSurface` para el modo oscuro.
- Usar `background_dark` o un color similar para el fondo.
- Cambiar `android:windowLightStatusBar` a `false`.

### [Componente] Layouts y Vistas

Se revisarán y actualizarán los layouts para usar atributos de tema en lugar de colores hardcoded.

#### [MODIFY] [activity_actualizar_movimiento.xml](file:///C:/proyectos/MyLittleFinanControl/app/src/main/res/layout/activity_actualizar_movimiento.xml)
- Cambiar `android:textColor="@color/btn_primary"` por `?attr/colorPrimary`.

#### [MODIFY] [activity_login_email.xml](file:///C:/proyectos/MyLittleFinanControl/app/src/main/res/layout/activity_login_email.xml)
- Cambiar `app:tint="@color/primary"` por `?attr/colorPrimary`.
- Cambiar `app:backgroundTint="@color/primary"` por `?attr/colorPrimary`.

#### [MODIFY] [dialog_agregar_movimiento.xml](file:///C:/proyectos/MyLittleFinanControl/app/src/main/res/layout/dialog_agregar_movimiento.xml)
- Cambiar `app:cardBackgroundColor="@android:color/white"` por `?attr/colorSurface`.
- Cambiar `app:backgroundTint="@color/primary"` por `?attr/colorPrimary`.

#### [MODIFY] Otros layouts...
- Se realizará una pasada general por los archivos `.xml` en `res/layout` para aplicar estos cambios de consistencia.

## Verification Plan

### Manual Verification
1. Desplegar la aplicación en un emulador o dispositivo físico.
2. Verificar que el modo claro se vea consistente con la paleta de colores deseada.
3. Cambiar el sistema a "Modo Oscuro" y verificar que:
    - El fondo sea oscuro.
    - El texto sea legible (blanco o verde claro).
    - Los componentes (botones, inputs, diálogos) se adapten correctamente.
4. Navegar por las pantallas principales (`MainActivity`, `Login`, `Agregar Movimiento`) para asegurar que no haya colores "fantasma" (por ejemplo, texto negro sobre fondo negro).
