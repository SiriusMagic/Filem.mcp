# Sin publicar

- **Reciente y las carpetas se actualizan solos**: una descarga nueva aparece sin cerrar Filem. Android avisa cuando cambian
  los archivos (MediaStore); en segundo plano Filem solo lo anota y lee al volver, solo si algo cambió; abierta, lee como
  mucho una vez cada 2 s (una descarga escribe muchas veces), sin barra de carga y sin perder la selección (`Freshness`).
- 38 pruebas.

# Cambios de Filem

## 0.4.0 (versionCode 4)
- **Resumen para Inicio**: la tarjeta «Documentos» de la pantalla «Hoy» de Inicio muestra los documentos más nuevos de
  los últimos 30 días (nombre y tamaño, hasta 5). Nunca la papelera. Si Filem no tiene acceso a los archivos, lo dice.
  Solo apps firmadas por el mismo desarrollador pueden leerlo (`SummaryProvider`); solo salen nombres y tamaños.

## 0.3.0 (versionCode 3)
- «Elegir archivo»: otras apps pueden abrir Filem para escoger un archivo (`ACTION_GET_CONTENT`). Lo usa Reloj en
  «Sonido de la alarma › Añadir nuevo»: Filem se abre con «Toca el archivo que quieres usar», y al tocar un archivo del
  tipo pedido (por ejemplo, audio) se lo entrega a esa app con permiso de lectura de una vez y se cierra. Si el archivo
  no es del tipo pedido, avisa. Atrás cancela.

## 0.2.0 (versionCode 2)
- Nuevo diseño al estilo de Archivos de Xiaomi (oscuro, tarjetas redondeadas). Barra inferior con «Reciente» y
  «Navegador» y un botón redondo de búsqueda aparte. La app abre en «Navegador».
- «Navegador»: tarjeta «Archivos locales» con el espacio usado y total y su barra; 8 categorías de colores (Imágenes,
  Videos, Documentos, Audio, APKs, Zips, Recibido, Descargas); «Ubicación» con Mi teléfono, Recientemente eliminado y
  Favoritos; ⋮ con Configuración, Papelera y Modo MCP (sustituye al cajón lateral).
- Categorías nuevas: «Zips» (zip, rar, 7z, tar, gz…) y «Recibido» (Download más las carpetas de WhatsApp, Bluetooth y
  Telegram que existan). También en MCP (`zips`, `received`); `files_search` con una carpeta en `under` que no existe
  ahora responde NOT_FOUND.
- «Reciente»: los archivos de los últimos 30 días en grupos que se abren y cierran (Hoy, Ayer, Últimos 7 días, Últimos
  30 días), filtro por tipo y ⋮ con Mostrar en la carpeta.
- Buscar: el campo está abajo junto al botón de volver; «Buscar historial» guarda las últimas 10 búsquedas (con papelera
  para borrarlas).
- APKs: se ve el icono real de la app, con su nombre y versión; tocar un APK lo instala (la primera vez Android pide
  permitir a Filem instalar apps y luego se abre el instalador).
- «Abrir con…»: primero nuestras apps según el tipo (videos → Stone Video y Stone; música → Stone y Stone Video;
  fotos → Luma; texto → Notas), luego las demás y «Otras apps» con el selector de Android.
- «Destacados» pasa a llamarse «Favoritos».

## 0.1.0 (versionCode 1)
- Primera versión: inicio con Recientes, Categorías, Colecciones y Almacenamiento interno; carpetas en lista o
  cuadrícula con ⋮ (Seleccionar todo, Ordenar por, Agregar carpeta nueva) y botón de carpeta nueva; selección con
  compartir, copiar, mover y papelera; búsqueda con filtros; Papelera con restaurar (30 días); Configuración con
  archivos ocultos.
- Modo MCP con tokens (LLM/Hub), «Redireccionar a Hub» y 11 herramientas `files_*`.
