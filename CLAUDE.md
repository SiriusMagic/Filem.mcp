# Filem (administrador de archivos MCP): reglas para agentes

- Lee `ARQUITECTURA.md` y abre solo los archivos que necesites.
- Pruebas: `cd android && ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease` (debe pasar todo).
  Nunca desactives ni saltes una prueba. Lint sin avisos nuevos.
- Cada versión: sube `versionCode` y `versionName`, copia el APK a `android/Filem-<versión>.apk` (borra el anterior)
  y anótalo en `CHANGELOG.md`.
- Seguridad de rutas: todo pasa por `FileStore.file()` / `FileRules.cleanPath()`. Nada fuera del almacenamiento ni dentro
  de `.filem/` (papelera). Borrar siempre va a la papelera; nada se elimina para siempre sin que el usuario lo pida.
- Privacidad: los archivos no salen del teléfono salvo por el Modo MCP que el usuario active; internet es solo para eso.
- Cada herramienta MCP nueva lleva su prueba en `McpTest`: lista de herramientas, llamada correcta, argumentos malos y
  «no encontrado».
- Prohibido: `eval`, motores de scripts, SQL concatenado.
- Firma: la de depuración del entorno, igual que Stone, Luma, Calculadora, Notas y Gateway Hub.
