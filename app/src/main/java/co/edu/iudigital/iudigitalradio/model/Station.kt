package co.edu.iudigital.iudigitalradio.model

/**
 * RF-06: Modelo de datos para cada emisora/pista de la lista dinámica.
 *
 * [streamUrl] debe apuntar a un stream de audio real (mp3/aac) accesible
 * por HTTP para que RF-07 reproduzca contenido físico con ExoPlayer.
 * Reemplaza las URLs de ejemplo por las emisoras oficiales de tu proyecto.
 */
data class Station(
    val id: Int,
    val name: String,
    val genre: String,
    val streamUrl: String
)

/**
 * Catálogo de ejemplo. Ajusta nombres/URLs a los requerimientos reales
 * del proyecto "IU Digital Radio".
 */
val sampleStations = listOf(
    Station(
        id = 1,
        name = "Caracol Radio",
        genre = "Noticias",
        streamUrl = "https://27603.live.streamtheworld.com/CARACOL_RADIOAAC_SC?csegid=2000"
    ),
    Station(
        id = 2,
        name = "Mix 89.9 fm medellin",
        genre = "Musica",
        streamUrl = "https://26493.live.streamtheworld.com/MIX_MEDELLINAAC_SC?dist=MixMedWeb"
    ),
    Station(
        id = 3,
        name = "Blu Radio",
        genre = "Noticias",
        streamUrl = "https://27393.live.streamtheworld.com/BLURADIO_ADP_SC?dist=+blu_web"
    ),
    Station(
        id = 4,
        name = "La fm medellin",
        genre = "Noticias",
        streamUrl = "https://co-e1-p-e-aa1-audio.cdn.mdstrm.com/live-audio-aw/632c9d81bc02c60329991f25"
    )
)
