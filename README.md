# IU Digital Radio

Aplicación Android nativa en **Kotlin + Jetpack Compose** para escuchar emisoras de radio colombianas en streaming. Incluye foto de perfil tomada con la cámara del dispositivo, controles de reproducción con respuesta háptica y estado que sobrevive a la rotación de pantalla.

Proyecto académico (Evidencia de Aprendizaje 3, desarrollo de aplicaciones móviles) que implementa los requerimientos funcionales RF-01 a RF-07 de la guía de trabajo.

## Funcionalidades

- **Streaming de audio en vivo** con Media3 ExoPlayer: Caracol Radio, Mix 89.9 Medellín, Blu Radio y La FM Medellín.
- **Reproductor** con Play/Pausa y Silencio sobre la emisora activa.
- **Lista de emisoras** (`LazyColumn`) que resalta la seleccionada y cambia la fuente de audio al tocarla.
- **Foto de perfil con la cámara nativa**, en resolución completa y recortada en círculo.
- **Permiso de cámara en tiempo de ejecución**, solicitado solo cuando hace falta.
- **Vibración corta** (60 ms) al usar los controles o elegir una emisora.
- **Estado persistente** ante cambios de configuración (rotación de pantalla).

## Stack

| | |
|---|---|
| Lenguaje | Kotlin (JVM 17) |
| UI | Jetpack Compose + Material 3, sin layouts XML |
| Audio | AndroidX Media3 ExoPlayer |
| Cámara | `ActivityResultContracts.TakePicture` + `FileProvider` |
| Build | Gradle (Kotlin DSL), Android Gradle Plugin 8.5 |
| SDK | `minSdk 24` (Android 7.0), `targetSdk` / `compileSdk 34` |

## Cómo ejecutarlo

**Con Android Studio**

1. Clona el repositorio y ábrelo con *File → Open*.
2. Espera a que Gradle sincronice. Android Studio crea `local.properties` con la ruta de tu SDK.
3. Conecta un dispositivo o inicia un emulador (API 24 o superior) y pulsa *Run ▶*.

**Por línea de comandos** (requiere JDK 17 y el Android SDK configurado en `ANDROID_HOME` o en `local.properties`):

```bash
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`. Para la reproducción se necesita conexión a internet.

## Estructura

```
app/src/main/
├── AndroidManifest.xml                    # permisos CAMERA, VIBRATE, INTERNET y FileProvider
├── java/co/edu/iudigital/iudigitalradio/
│   ├── MainActivity.kt                    # UI en Compose, estado, cámara, vibración y ExoPlayer
│   └── model/Station.kt                   # data class Station y catálogo de emisoras
└── res/xml/file_paths.xml                 # carpeta compartida con la cámara (cache/images)
```

## Arquitectura

Una sola `ComponentActivity` con UI declarativa y **flujo de estado unidireccional (*state hoisting*)**: el composable raíz `IUDigitalRadioApp()` guarda todo el estado y lo pasa a composables hijos sin estado, que solo reciben datos y callbacks.

| Composable | Sección | Responsabilidad |
|---|---|---|
| `ProfileHeader` | Superior | Foto de perfil circular y botón de cámara |
| `PlayerCard` | Central | Emisora activa y controles Play/Pausa/Silencio |
| `StationList` | Inferior | Catálogo de emisoras con la selección resaltada |

### Decisiones técnicas

**Estado con `rememberSaveable`.** `profileImageUri`, `isPlaying`, `isMuted`, `hasCameraPermission` y la emisora seleccionada se guardan con `rememberSaveable` para que sobrevivan a la rotación. Como `Station` no es `Parcelable`, se guarda solo su `id` (`Int`) y el objeto se deriva del catálogo; así no hace falta un `Saver` personalizado.

**Foto en resolución completa.** Se usa `TakePicture()` en lugar de `TakePicturePreview()`, que solo devuelve una miniatura. La app crea un archivo temporal en `cacheDir/images` y lo comparte con la cámara del sistema mediante un `FileProvider`, sin pedir permisos de almacenamiento.

**ExoPlayer ligado al ciclo de vida del composable.** Se crea una sola instancia con `remember`, se libera en `DisposableEffect` y un `LaunchedEffect(selectedStationId)` cambia el `MediaItem` cuando cambia la emisora, respetando si estaba sonando.

**Vibración compatible con varias versiones.** `triggerHapticFeedback()` usa `VibratorManager` en Android 12+ (API 31) y `Vibrator` en versiones anteriores. `VIBRATE` es un permiso normal: se declara en el manifiesto y Android lo concede al instalar.

## Trazabilidad de requerimientos

| RF | Requerimiento | Implementación |
|---|---|---|
| RF-01 | UI declarativa | Toda la interfaz en Compose (`Column`, `Row`, `Card`, `LazyColumn`); no hay layouts XML |
| RF-02 | Cámara nativa | `ProfileHeader` + `createImageUri()` + `TakePicture()` |
| RF-03 | Permisos | `AndroidManifest.xml` + `RequestPermission()` en tiempo de ejecución |
| RF-04 | Estado persistente | Variables `rememberSaveable` en `IUDigitalRadioApp()` |
| RF-05 | Vibración | `triggerHapticFeedback()` en Play/Pausa, Silencio y selección de emisora |
| RF-06 | Lista dinámica | `StationList()` con `LazyColumn` sobre `sampleStations` |
| RF-07 | Audio real | `ExoPlayer` + `MediaItem.fromUri(station.streamUrl)` |

## Autor

**John Neider Cuervo Quintero** — estudiante de Ingeniería de Software y Datos, Medellín.
Portafolio: [portafolio-john-cuervo.netlify.app](https://portafolio-john-cuervo.netlify.app/)
