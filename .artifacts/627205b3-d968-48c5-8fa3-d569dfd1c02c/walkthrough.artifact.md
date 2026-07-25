# Ajuste de Temas y Modo Oscuro

Se ha realizado una actualización integral del sistema de diseño para soportar correctamente el modo oscuro y estandarizar el uso de colores mediante atributos de tema.

## Cambios Principales

### 1. Paleta de Colores (`colors.xml`)
- Se definieron colores base para fondos claros (`background_light`) y oscuros (`background_dark`).
- Se añadieron variantes para componentes en modo noche (`surface_dark`).
- Se estandarizaron los nombres de los colores principales (`primary`, `secondary`, `accent_alerta`).

### 2. Temas Dinámicos (`themes.xml`)
- **Modo Claro:** Configurado con fondos claros y texto negro para máxima legibilidad.
- **Modo Oscuro:** Configurado con fondos casi negros (`#121212`) y textos en blanco/verde claro. Se ajustó la barra de estado para que no use el modo de iconos claros sobre fondo oscuro.

### 3. Layouts Adaptativos
Se migraron múltiples layouts para usar atributos semánticos (`?attr/colorPrimary`, `?attr/colorSurface`, etc.) en lugar de colores fijos. Esto incluye:
- `activity_actualizar_movimiento.xml`
- `activity_login_email.xml`
- `dialog_agregar_movimiento.xml`
- `activity_main.xml`

## Verificación Realizada
- Los layouts de los items (ej. `item_movimiento.xml`) ya estaban usando atributos de tema, por lo que heredarán automáticamente los nuevos colores.
- Se aseguró que los contrastes en modo noche cumplan con las guías de Material Design 3.

> [!TIP]
> Para probar los cambios, simplemente cambia el tema del sistema de tu dispositivo a "Modo Oscuro". La aplicación se adaptará instantáneamente.
