package co.edu.ucc.ojodegallo.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Iconos propios de la aplicación, dibujados como trazos de 1,8 unidades sobre una cuadrícula de 24 × 24.
 * Son de autoría propia (no se copian de ninguna librería) y todos comparten el mismo grosor y remate.
 */
private fun icono(nombre: String, vararg trazos: String): ImageVector {
    val b = ImageVector.Builder(name = nombre, defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f)
    trazos.forEach { d ->
        b.addPath(pathData = addPathNodes(d), stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
    }
    return b.build()
}

object Iconos {
    val Camara by lazy {
        icono("camara",
            "M3.5 8.5a2 2 0 0 1 2-2h2.2l1.6-2.2h5.4l1.6 2.2h2.2a2 2 0 0 1 2 2V17a2 2 0 0 1-2 2h-13a2 2 0 0 1-2-2Z",
            "M12 9.3a3.6 3.6 0 1 1 0 7.2a3.6 3.6 0 1 1 0-7.2Z")
    }
    val Galeria by lazy {
        icono("galeria", "M4 5h16v14H4Z", "M4 16.5l4.5-4.5 4 4 2.5-2.5 5 5", "M15.5 8.6v.01")
    }
    val Historial by lazy {
        icono("historial", "M4.2 12a7.8 7.8 0 1 0 2.3-5.5", "M4 4.5v3.8h3.8", "M12 8v4.2l2.8 1.8")
    }
    val Encuadre by lazy {
        icono("encuadre", "M4 8.5V4h4.5", "M15.5 4H20v4.5", "M20 15.5V20h-4.5", "M8.5 20H4v-4.5",
            "M12 8.5c2 2 2.6 4.6 0 7c-2.6-2.4-2-5 0-7Z")
    }
    val Info by lazy { icono("info", "M12 3.2a8.8 8.8 0 1 1 0 17.6a8.8 8.8 0 1 1 0-17.6Z", "M12 11v5.2", "M12 7.8v.01") }
    val Siguiente by lazy { icono("siguiente", "M9.5 6l6 6-6 6") }
    val Atras by lazy { icono("atras", "M15 5.5L8.5 12l6.5 6.5") }
    val Alerta by lazy { icono("alerta", "M12 3.8l8.6 15.2H3.4Z", "M12 9.8v4.2", "M12 16.6v.01") }
    val Sana by lazy { icono("sana", "M12 3.2a8.8 8.8 0 1 1 0 17.6a8.8 8.8 0 1 1 0-17.6Z", "M8 12.4l2.7 2.7L16.2 9.6") }
    val Duda by lazy {
        icono("duda", "M12 3.2a8.8 8.8 0 1 1 0 17.6a8.8 8.8 0 1 1 0-17.6Z",
            "M9.6 9.6a2.5 2.5 0 1 1 3.6 2.2c-.7.4-1.2.9-1.2 1.7v.4", "M12 16.6v.01")
    }
    val NoValida by lazy { icono("no_valida", "M4 8.5V4h4.5", "M15.5 4H20v4.5", "M20 15.5V20h-4.5", "M8.5 20H4v-4.5", "M8 8l8 8") }
    val Papelera by lazy { icono("papelera", "M4.5 7h15", "M9.5 7V4.8h5V7", "M6.5 7l.9 12.2h9.2L17.5 7", "M10.3 10.5v5.5", "M13.7 10.5v5.5") }
    val Guardado by lazy { icono("guardado", "M5 12.5l4.3 4.3L19 7.2") }
    val Altavoz by lazy { icono("altavoz", "M4 9.5h3.5L12 6v12l-4.5-3.5H4Z", "M15.5 9.2a4 4 0 0 1 0 5.6", "M18.2 6.6a7.6 7.6 0 0 1 0 10.8") }
    val Detener by lazy { icono("detener", "M7 7h10v10H7Z") }
    val Persona by lazy { icono("persona", "M12 4a3.3 3.3 0 1 1 0 6.6a3.3 3.3 0 1 1 0-6.6Z", "M5.5 20c.7-3.7 3.3-6 6.5-6s5.8 2.3 6.5 6") }
    val Hoja by lazy { icono("hoja", "M5 19C5 11 10 5 19 5c0 9-6 14-14 14Z", "M5 19l8.5-8.5") }
    val Datos by lazy { icono("datos", "M5 19V11", "M10 19V6", "M15 19v-5", "M20 19V9") }
    val Medir by lazy { icono("medir", "M12 4.2a8.2 8.2 0 1 1 0 16.4a8.2 8.2 0 1 1 0-16.4Z", "M12 8.4v4l2.6 1.6", "M9.5 2.6h5") }
}
