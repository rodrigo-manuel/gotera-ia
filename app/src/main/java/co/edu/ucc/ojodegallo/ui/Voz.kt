package co.edu.ucc.ojodegallo.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Lectura en voz alta con el motor de voz del teléfono (TextToSpeech), sin internet.
 * Pensada para caficultores que leen con dificultad: cada resultado y la guía se pueden escuchar.
 * Si el teléfono no tiene voz en español, el botón no se muestra.
 */
class Voz(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    var disponible by mutableStateOf(false)
        private set
    var hablando by mutableStateOf(false)
        private set

    override fun onInit(estado: Int) {
        if (estado != TextToSpeech.SUCCESS) return
        val idioma = listOf(Locale("es", "CO"), Locale("es", "US"), Locale("es"))
            .firstOrNull { tts.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE }
        if (idioma == null) return
        tts.language = idioma
        tts.setSpeechRate(0.9f)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { hablando = true }
            override fun onDone(id: String?) { hablando = false }
            @Deprecated("Requerido por versiones anteriores de Android")
            override fun onError(id: String?) { hablando = false }
        })
        disponible = true
    }

    fun decir(texto: String) {
        if (disponible) tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "gotera")
    }

    fun callar() { tts.stop(); hablando = false }

    fun cerrar() { tts.stop(); tts.shutdown() }
}

@Composable
fun recordarVoz(): Voz {
    val contexto = LocalContext.current
    val voz = remember { Voz(contexto) }
    DisposableEffect(voz) { onDispose { voz.cerrar() } }
    return voz
}
