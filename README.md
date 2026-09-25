# IU Digital Radio — Evidencia de Aprendizaje 3

Aplicación Android nativa (Kotlin + Jetpack Compose) que cumple el MVP y los
requerimientos funcionales RF-01 a RF-07 de la guía de trabajo.

## Cómo compilar

1. Abre esta carpeta (`IUDigitalRadio/`) en Android Studio (Koala o superior)
   con **Open an existing project**.
2. Deja que Gradle sincronice (usa AGP 8.5.2 / Kotlin 1.9.24 / compileSdk 34).
3. Conecta un dispositivo físico o inicia un emulador (API 24+).
4. Ejecuta `Run ▶` o genera el instalable con
   `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
5. El `.apk` de depuración queda en
   `app/build/outputs/apk/debug/app-debug.apk`.

> Este entorno de conversación no tiene Android SDK instalado, por lo que el
> código no pudo compilarse ni empaquetarse aquí. El proyecto está listo para
> abrirse y compilarse directamente en Android Studio.

## Antes de entregar — pendientes a tu cargo

- **Streaming real**: reemplaza las URLs de ejemplo en
  `model/Station.kt` (`sampleStations`) por streams de audio reales
  (mp3/aac vía HTTP) o por tus archivos locales en `res/raw`.
- **Ícono de la app**: agrega tu propio `ic_launcher` en `res/mipmap-*`
  (el manifiesto ya referencia `@mipmap/ic_launcher`).
- **Portada y datos del equipo**: completa el documento técnico con
  integrantes, modalidad elegida y roles.
- **Capturas y video**: genera las capturas de pantalla y el video de 2-4
  minutos ejecutando la app ya compilada, como pide la sección 5 de la guía.
- **Repositorio Git**: sube este proyecto a un repositorio público de
  GitHub/GitLab y añade el enlace al documento técnico.

## Arquitectura (para el Documento Técnico)

**Patrón**: Composición unidireccional de estado (*state hoisting*) dentro
de un único `ComponentActivity`, sin XML de vistas (cumple RF-01).

- `MainActivity.kt`
  - `IUDigitalRadioApp()`: composable raíz. Contiene todo el estado de la
    pantalla (`mutableStateOf` / `rememberSaveable`) y lo pasa hacia abajo a
    los composables hijos, que son *stateless* y solo reciben datos + callbacks.
  - `ProfileHeader()`: sección superior — foto de perfil circular + botón de
    cámara (RF-02).
  - `PlayerCard()`: sección central — tarjeta con nombre de emisora activa y
    controles Play/Pause/Mute (RF-04, RF-05).
  - `StationList()`: sección inferior — `LazyColumn` con el catálogo de
    emisoras; el ítem seleccionado se resalta (RF-06).
- `model/Station.kt`: modelo de datos inmutable (`data class Station`) y
  catálogo de ejemplo.

**Gestión de estado (RF-04)**

| Estado | Tipo | Persiste rotación |
|---|---|---|
| `profileImageUri` | `rememberSaveable<Uri?>` | Sí |
| `selectedStation` | `rememberSaveable<Station>` | Sí |
| `isPlaying` / `isMuted` | `rememberSaveable<Boolean>` | Sí |
| `hasCameraPermission` | `rememberSaveable<Boolean>` | Sí |

Se usa `rememberSaveable` (no solo `mutableStateOf`) precisamente para que el
estado sobreviva a cambios de configuración como la rotación de pantalla,
según exige RF-04. `Uri`, `String`, `Boolean` e `Int` son parcelables/nativos,
por lo que no requieren un `Saver` personalizado.

**Cámara nativa (RF-02, RF-03)**

Se usa `ActivityResultContracts.TakePicture()` (no `TakePicturePreview`) para
obtener la foto en resolución completa: la app crea un archivo temporal en
`cacheDir/images` y expone su `Uri` mediante `FileProvider` (declarado en el
manifiesto con `res/xml/file_paths.xml`). El permiso `CAMERA` se solicita en
tiempo de ejecución con `ActivityResultContracts.RequestPermission()` antes
de lanzar la cámara del sistema. La imagen resultante se decodifica con
`BitmapFactory` y se muestra recortada en un contenedor circular
(`Modifier.clip(CircleShape)`).

**Vibración (RF-05)**

`triggerHapticFeedback()` obtiene el servicio `VibratorManager` en API 31+ o
`Vibrator` (deprecado pero necesario) en versiones anteriores, y dispara un
`VibrationEffect.createOneShot(...)` de 60 ms al presionar Play/Pause, Mute o
al seleccionar una emisora. El permiso `VIBRATE` es un *permiso normal*: se
declara en el manifiesto pero Android lo concede automáticamente en la
instalación, sin diálogo en tiempo de ejecución.

**Reproducción de audio (RF-07)**

Se integra `androidx.media3:media3-exoplayer`. Un único `ExoPlayer` se crea
con `remember { ExoPlayer.Builder(context).build() }`, se libera en
`DisposableEffect(Unit) { onDispose { exoPlayer.release() } }`, y un
`LaunchedEffect(selectedStation)` reemplaza el `MediaItem` cada vez que
cambia la emisora seleccionada, respetando el estado `isPlaying` actual.

## Trazabilidad de requerimientos

| RF | Cumplido en |
|---|---|
| RF-01 | Toda la UI en `MainActivity.kt` usa Compose (`Column`, `Row`, `Card`, `LazyColumn`, `Modifier`); no hay layouts XML |
| RF-02 | `ProfileHeader` + `createImageUri` + `ActivityResultContracts.TakePicture` |
| RF-03 | `AndroidManifest.xml` + `requestCameraPermissionLauncher` |
| RF-04 | Variables `rememberSaveable` en `IUDigitalRadioApp` |
| RF-05 | `triggerHapticFeedback()` invocada en Play/Pause/Mute/selección de lista |
| RF-06 | `StationList()` con `LazyColumn` sobre `sampleStations` |
| RF-07 | `ExoPlayer` + `MediaItem.fromUri(station.streamUrl)` |
