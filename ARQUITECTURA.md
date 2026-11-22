# Arquitectura de Filem

Carpeta = package (`android/app/src/main/java/com/zirakusa/filem/`):

| Carpeta | Qué hay |
|---|---|
| `data/` | `FileItem`, `Category` (por extensión, o «lugares» con carpetas: Descargas y Recibido) y `Sort` (modelos); `FileRules` (reglas puras: nombres válidos, rutas que no salen del almacenamiento, orden natural con carpetas primero, búsqueda sin acentos, «foto (1).jpg», tamaños «1,2 GB», carpetas de «Recibido», historial de búsqueda; con pruebas); `RecentRules` (grupos de «Reciente» por días del calendario; con pruebas); `Openers` (orden de «Abrir con…»: nuestras apps primero según el tipo; con pruebas); `FileStore` (archivos reales bajo la raíz: listar, buscar, categorías, crear carpeta, renombrar, copiar/mover, papelera en `.filem/trash` con restaurar y 30 días); `Prefs` (favoritos, vista, orden, ocultos, historial de búsqueda). |
| `ui/` | `FilesState` (único dueño del estado: pestañas y pila de atrás, lista, selección, «Pegar aquí», Reciente con filtro y grupos abiertos, historial, mensajes), `Actions` (abrir, instalar y compartir, lo hace `MainActivity`) y `FilemApp` (la pantalla activa y la barra inferior en las dos pestañas). |
| `ui/screens/` | `NavigatorScreen` (Archivos locales, 8 categorías, Ubicación y ⋮), `RecentScreen` (grupos Hoy/Ayer/7/30 días), `Tabs` (barra inferior, botón redondo y título grande), `ApkIcon` (icono, nombre y versión de los APK, en segundo plano y en caché), `OpenWithDialog` («Abrir con…»), `BrowserScreen` (carpeta: lista o cuadrícula, ⋮ Seleccionar todo / Ordenar por / Agregar carpeta nueva, ruta verde, botón de carpeta nueva, barra de selección), `Listing` (filas, mosaicos y el ⋮ de cada archivo), `SearchScreen` (filtros de tipo, fecha y orden, historial y campo abajo), `TrashScreen`, `SettingsScreen`, `McpScreen`, `PermissionScreen`, `Dialogs` y `Chrome`. Solo pintan y avisan. |
| `ui/theme/` | `FilemColors` y `FilemTheme` (oscuro, verde suave). |
| `mcp/` | Modo MCP, como Notas: `Tokens`, `SecretBox`, `FileTools` (herramientas `files_*`), `McpHandler`, `RelayLink` (relay de Stone), `McpMode` y `HubLink`. |
| `Summary.kt` | `SummaryLines` (pura, con prueba) y `SummaryProvider`: los documentos recientes para «Hoy» de Inicio (solo nombre y tamaño, nunca la papelera), solo para apps con la misma firma. |
| `MainActivity.kt` | `PickActivity` («Elegir archivo» para otras apps: la misma app, y tocar un archivo del tipo pedido lo devuelve con `Openers.matches`). Pide «Acceso a todos los archivos», abre («Abrir con…» con nuestras apps primero), instala APKs (pide «Instalar apps desconocidas» si hace falta) y comparte con `FileProvider`, arranca el Modo MCP y vacía lo de la papelera con más de 30 días. |

Reglas:
- Texto visible en `res/values/strings.xml` (español) y `values-en`.
- Colores siempre de `FilemColors`.
- Archivos de pantalla de unas 150 líneas como mucho; si crece, se divide.
