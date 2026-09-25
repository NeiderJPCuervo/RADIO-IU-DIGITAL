package co.edu.iudigital.iudigitalradio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import co.edu.iudigital.iudigitalradio.model.Station
import co.edu.iudigital.iudigitalradio.model.sampleStations
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    IUDigitalRadioApp()
                }
            }
        }
    }
}

/**
 * RF-05: Encapsula el acceso al servicio de sistema Vibrator/VibratorManager
 * y dispara una pulsación háptica corta compatible con API 24+.
 */
fun triggerHapticFeedback(context: Context) {
    val vibrator: Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(60)
    }
}

/**
 * Crea un archivo temporal en cache/images y devuelve una Uri segura
 * (vía FileProvider) que se entrega a la cámara nativa del sistema
 * como destino donde guardar la foto de alta resolución (RF-02).
 */
fun createImageUri(context: Context): Uri {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    val imageFile = File(imagesDir, "profile_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

@Composable
fun IUDigitalRadioApp() {
    val context = LocalContext.current

    // ---------- RF-04: Estado de la app, sobrevive a rotación de pantalla ----------
    var profileImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var pendingCameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    // NOTA: "Station" es una data class normal (no Parcelable), así que no se
    // puede guardar directamente con rememberSaveable. En su lugar guardamos
    // solo el id (Int, nativamente soportado) y derivamos el objeto completo.
    var selectedStationId by rememberSaveable { mutableStateOf(sampleStations.first().id) }
    val selectedStation = sampleStations.first { it.id == selectedStationId }
    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var isMuted by rememberSaveable { mutableStateOf(false) }
    var hasCameraPermission by rememberSaveable {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    // ---------- RF-07: ExoPlayer, ligado al ciclo de vida del composable ----------
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = false
        }
    }
    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }
    // Cambia la fuente cuando cambia la emisora seleccionada
    LaunchedEffect(selectedStationId) {
        exoPlayer.setMediaItem(MediaItem.fromUri(selectedStation.streamUrl))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = isPlaying
    }

    // ---------- RF-02: Lanzador de cámara nativa (guarda foto en alta resolución) ----------
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraUri != null) {
            profileImageUri = pendingCameraUri
        }
    }

    // ---------- RF-03: Solicitud de permiso de cámara en tiempo de ejecución ----------
    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            val uri = createImageUri(context)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        }
    }

    fun onCameraButtonClick() {
        if (hasCameraPermission) {
            val uri = createImageUri(context)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "IU Digital Radio",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // ---------- Sección superior: Perfil ----------
        ProfileHeader(
            imageUri = profileImageUri,
            onCameraClick = { onCameraButtonClick() }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ---------- Sección central: Reproductor principal ----------
        PlayerCard(
            station = selectedStation,
            isPlaying = isPlaying,
            isMuted = isMuted,
            onPlayPauseClick = {
                isPlaying = !isPlaying
                exoPlayer.playWhenReady = isPlaying
                triggerHapticFeedback(context)
            },
            onMuteClick = {
                isMuted = !isMuted
                exoPlayer.volume = if (isMuted) 0f else 1f
                triggerHapticFeedback(context)
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Emisoras disponibles",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        // ---------- Sección inferior: Catálogo (RF-06) ----------
        StationList(
            stations = sampleStations,
            selectedStation = selectedStation,
            onStationSelected = { station ->
                selectedStationId = station.id
                triggerHapticFeedback(context)
            }
        )
    }
}

@Composable
fun ProfileHeader(imageUri: Uri?, onCameraClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncUriImage(uri = imageUri)
                } else {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = "Mi perfil", fontWeight = FontWeight.Bold)
                Text(text = "Toca la cámara para actualizar tu foto", fontSize = 12.sp)
            }
        }

        IconButton(
            onClick = onCameraClick,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = "Tomar foto",
                tint = Color.White
            )
        }
    }
}

/**
 * Decodifica y muestra una imagen a partir de una Uri de archivo local
 * (la foto capturada por la cámara nativa vía FileProvider).
 */
@Composable
fun AsyncUriImage(uri: Uri) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        context.contentResolver.openInputStream(uri)?.use {
            android.graphics.BitmapFactory.decodeStream(it)
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Foto de perfil capturada",
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun PlayerCard(
    station: Station,
    isPlaying: Boolean,
    isMuted: Boolean,
    onPlayPauseClick: () -> Unit,
    onMuteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = station.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = station.genre, fontSize = 13.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onMuteClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                        contentDescription = "Mute"
                    )
                }

                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Espacio simétrico para balancear el diseño (misma fila de controles)
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Composable
fun StationList(
    stations: List<Station>,
    selectedStation: Station,
    onStationSelected: (Station) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(stations, key = { it.id }) { station ->
            val isSelected = station.id == selectedStation.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStationSelected(station) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = station.name, fontWeight = FontWeight.Medium)
                        Text(text = station.genre, fontSize = 12.sp, color = Color.Gray)
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = "Emisora activa",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
