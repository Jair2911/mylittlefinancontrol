# Plan de Corrección: Posicionamiento del Botón Agregar (FAB)

Este plan corrige el problema de visibilidad donde el botón flotante (FAB) obstruye los iconos de editar y eliminar en la lista de movimientos.

## Proposed Changes

### [Componente] Interfaz de Usuario (Layouts)

#### [MODIFY] [fragment_movimiento.xml](file:///C:/proyectos/MyLittleFinanControl/app/src/main/res/layout/fragment_movimiento.xml)
- **Bajar el FAB:** Cambiar `android:layout_marginBottom="84dp"` a `16dp`. Actualmente el botón está muy arriba, lo que causa la interferencia mostrada en la imagen.
- **Ajustar Margen Lateral:** Cambiar `android:layout_marginEnd="26dp"` a `16dp` para alinearlo estándar.
- **Asegurar Espaciado del RecyclerView:** Mantener el `paddingBottom="80dp"` y `clipToPadding="false"`. Con el botón en la parte inferior, este padding permitirá que la lista suba lo suficiente para que los iconos queden libres.

## Verification Plan

### Manual Verification
1. Abrir la sección de Movimientos.
2. Verificar que el botón "+" esté en la esquina inferior derecha estándar.
3. Hacer scroll hasta el último elemento y confirmar que los iconos de editar/eliminar quedan por encima del botón "+".
