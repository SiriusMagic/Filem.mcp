# Arquitectura de Filem

Carpeta = package (`android/app/src/main/java/com/zirakusa/filem/`):

| Carpeta | Qué hay |
|---|---|
| `data/` | `FileItem`, `Category` y `Sort` (modelos); `FileRules` (reglas puras: nombres válidos, rutas que no salen del almacenamiento, orden natural con carpetas primero, búsqueda sin acentos, «foto (1).jpg», tamaños «1,2 GB»; con pruebas); `FileStore` (archivos reales bajo la raíz: listar, buscar, categorías, crear carpeta, renombrar, copiar/mover, papelera en `.filem/trash` con restaurar y 30 días); `Prefs` (destacados, vista, orden, ocultos). |
| `ui/` | `FilesState` (único dueño del estado: pantalla y pila de atrás, lista, selección, «Pegar aquí», mensajes), `Actions` (abrir y compartir, lo hace `MainActivity`) y `FilemApp` (cajón con Papelera y Configuración, y la pantalla activa). |
| `ui/screens/` | `HomeScreen` (Recientes, Categorías, Colecciones, Todo el almacenamiento), `BrowserScreen` (carpeta: lista o cuadrícula, ⋮ Seleccionar todo / Ordenar por / Agregar carpeta nueva, ruta verde, botón de carpeta nueva, barra de selección), `Listing` (filas, mosaicos y el ⋮ de cada archivo), `SearchScreen` (filtros de tipo, fecha y orden), `TrashScreen`, `SettingsScreen`, `McpScreen`, `PermissionScreen`, `Dialogs` y `Chrome`. Solo pintan y avisan. |
| `ui/theme/` | `FilemColors` y `FilemTheme` (oscuro, verde suave). |
| `mcp/` | Modo MCP, como Notas: `Tokens`, `SecretBox`, `FileTools` (herramientas `files_*`), `McpHandler`, `RelayLink` (relay de Stone), `McpMode` y `HubLink`. |
| `MainActivity.kt` | Pide «Acceso a todos los archivos», abre y comparte con `FileProvider`, arranca el Modo MCP y vacía lo de la papelera con más de 30 días. |

Reglas:
- Texto visible en `res/values/strings.xml` (español) y `values-en`.
- Colores siempre de `FilemColors`.
- Archivos de pantalla de unas 150 líneas como mucho; si crece, se divide.
