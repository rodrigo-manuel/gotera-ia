package co.edu.ucc.ojodegallo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Pictogramas dibujados con código (Canvas) para que la aplicación se entienda sin leer:
 * el símbolo de cada resultado y las escenas de la guía para tomar la foto.
 */

/** Símbolo de un resultado: forma + color + marca. La forma distingue el resultado aun sin ver colores. */
@Composable
fun SimboloResultado(s: Simbolo, tam: Dp = 44.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(tam)) {
        val w = size.width; val trazo = Stroke(w * 0.09f, cap = StrokeCap.Round)
        when (s) {
            Simbolo.SANA -> {
                drawCircle(VerdeSano, w / 2)
                val p = Path().apply { moveTo(w * .28f, w * .52f); lineTo(w * .44f, w * .68f); lineTo(w * .73f, w * .36f) }
                drawPath(p, PapelClaro, style = trazo)
            }
            Simbolo.OJO -> {
                val p = Path().apply { moveTo(w * .5f, w * .06f); lineTo(w * .97f, w * .9f); lineTo(w * .03f, w * .9f); close() }
                drawPath(p, Terracota)
                drawLine(PapelClaro, Offset(w * .5f, w * .36f), Offset(w * .5f, w * .6f), w * .09f, StrokeCap.Round)
                drawCircle(PapelClaro, w * .055f, Offset(w * .5f, w * .76f))
            }
            Simbolo.ROYA -> {
                val p = Path().apply { moveTo(w * .5f, 0f); lineTo(w, w * .5f); lineTo(w * .5f, w); lineTo(0f, w * .5f); close() }
                drawPath(p, Ocre)
                listOf(Offset(.38f, .42f), Offset(.6f, .38f), Offset(.5f, .6f), Offset(.36f, .62f), Offset(.64f, .6f))
                    .forEach { drawCircle(Color(0xFFF2B33D), w * .06f, Offset(w * it.x, w * it.y)) }
            }
            Simbolo.OJO_O_ROYA -> {
                // Triángulo de alerta partido: mitad ojo de gallo (terracota) y mitad roya (ocre).
                val izq = Path().apply { moveTo(w * .5f, w * .06f); lineTo(w * .5f, w * .9f); lineTo(w * .03f, w * .9f); close() }
                val der = Path().apply { moveTo(w * .5f, w * .06f); lineTo(w * .97f, w * .9f); lineTo(w * .5f, w * .9f); close() }
                drawPath(izq, Terracota); drawPath(der, Ocre)
                drawLine(PapelClaro, Offset(w * .5f, w * .36f), Offset(w * .5f, w * .6f), w * .09f, StrokeCap.Round)
                drawCircle(PapelClaro, w * .055f, Offset(w * .5f, w * .76f))
            }
            Simbolo.DUDA -> {
                drawRoundRect(Ocre, cornerRadius = CornerRadius(w * .22f))
                val p = Path().apply {
                    moveTo(w * .36f, w * .38f)
                    cubicTo(w * .36f, w * .2f, w * .66f, w * .18f, w * .66f, w * .37f)
                    cubicTo(w * .66f, w * .5f, w * .5f, w * .5f, w * .5f, w * .62f)
                }
                drawPath(p, PapelClaro, style = trazo)
                drawCircle(PapelClaro, w * .055f, Offset(w * .5f, w * .79f))
            }
            Simbolo.NO_VALIDA -> {
                drawRoundRect(TintaSuave, cornerRadius = CornerRadius(w * .22f))
                drawLine(PapelClaro, Offset(w * .3f, w * .3f), Offset(w * .7f, w * .7f), w * .09f, StrokeCap.Round)
                drawLine(PapelClaro, Offset(w * .7f, w * .3f), Offset(w * .3f, w * .7f), w * .09f, StrokeCap.Round)
            }
        }
    }
}

/** Escenas de la guía para tomar la foto. */
enum class Escena { UNA_HOJA, VARIAS_HOJAS, REFLEJO, BORROSA }

private val VerdeDibujo = Color(0xFF3E7A4C)
private val VerdeNervio = Color(0xFF24452F)

/** Hoja de café estilizada: forma de lanza con nervio central y nervios laterales. */
private fun DrawScope.hoja(centro: Offset, largo: Float, angulo: Float, color: Color = VerdeDibujo, alfa: Float = 1f) {
    rotate(angulo, centro) {
        val l = largo / 2; val a = largo * 0.26f
        val p = Path().apply {
            moveTo(centro.x - l, centro.y)
            cubicTo(centro.x - l * .4f, centro.y - a * 1.1f, centro.x + l * .5f, centro.y - a, centro.x + l, centro.y)
            cubicTo(centro.x + l * .5f, centro.y + a, centro.x - l * .4f, centro.y + a * 1.1f, centro.x - l, centro.y)
            close()
        }
        drawPath(p, color.copy(alpha = alfa))
        drawLine(VerdeNervio.copy(alpha = alfa), Offset(centro.x - l * .92f, centro.y), Offset(centro.x + l * .9f, centro.y),
            largo * .025f, StrokeCap.Round)
        for (k in -2..2) {
            val x = centro.x + k * l * .3f
            drawLine(VerdeNervio.copy(alpha = alfa * .8f), Offset(x, centro.y), Offset(x + l * .18f, centro.y - a * .6f), largo * .015f, StrokeCap.Round)
            drawLine(VerdeNervio.copy(alpha = alfa * .8f), Offset(x, centro.y), Offset(x + l * .18f, centro.y + a * .6f), largo * .015f, StrokeCap.Round)
        }
    }
}

/** Esquinas del recuadro de la cámara. */
private fun DrawScope.recuadro(margen: Float) {
    val w = size.width; val h = size.height; val k = w * .14f; val t = w * .025f; val col = TintaSuave
    listOf(Offset(margen, margen) to Pair(1, 1), Offset(w - margen, margen) to Pair(-1, 1),
        Offset(margen, h - margen) to Pair(1, -1), Offset(w - margen, h - margen) to Pair(-1, -1)).forEach { (o, d) ->
        drawLine(col, o, Offset(o.x + d.first * k, o.y), t, StrokeCap.Round)
        drawLine(col, o, Offset(o.x, o.y + d.second * k), t, StrokeCap.Round)
    }
}

@Composable
fun PictogramaEscena(e: Escena, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height; val c = Offset(w / 2, h / 2)
        recuadro(w * .08f)
        when (e) {
            Escena.UNA_HOJA -> hoja(c, w * .78f, -32f)
            Escena.VARIAS_HOJAS -> {
                hoja(Offset(w * .34f, h * .38f), w * .34f, -20f)
                hoja(Offset(w * .66f, h * .42f), w * .32f, 25f)
                hoja(Offset(w * .48f, h * .68f), w * .34f, -5f)
            }
            Escena.REFLEJO -> {
                hoja(Offset(w * .46f, h * .58f), w * .7f, -25f)
                drawOval(Color.White.copy(alpha = .9f), Offset(w * .36f, h * .48f), Size(w * .2f, h * .1f))
                val sol = Offset(w * .78f, h * .22f)
                drawCircle(Color(0xFFE9A626), w * .07f, sol)
                for (i in 0 until 8) {
                    val ang = Math.toRadians(i * 45.0)
                    val d1 = w * .1f; val d2 = w * .15f
                    drawLine(Color(0xFFE9A626), Offset(sol.x + (d1 * Math.cos(ang)).toFloat(), sol.y + (d1 * Math.sin(ang)).toFloat()),
                        Offset(sol.x + (d2 * Math.cos(ang)).toFloat(), sol.y + (d2 * Math.sin(ang)).toFloat()), w * .02f, StrokeCap.Round)
                }
            }
            Escena.BORROSA -> {
                hoja(Offset(c.x - w * .05f, c.y), w * .72f, -30f, alfa = .25f)
                hoja(Offset(c.x + w * .05f, c.y), w * .72f, -30f, alfa = .25f)
                hoja(c, w * .72f, -30f, alfa = .45f)
            }
        }
    }
}

/** Marca de "sí" o "no" para las escenas de la guía. */
@Composable
fun MarcaSiNo(correcto: Boolean, tam: Dp = 30.dp) {
    Canvas(Modifier.size(tam)) {
        val w = size.width
        drawCircle(if (correcto) VerdeSano else Terracota, w / 2)
        if (correcto) {
            val p = Path().apply { moveTo(w * .28f, w * .52f); lineTo(w * .44f, w * .68f); lineTo(w * .73f, w * .36f) }
            drawPath(p, PapelClaro, style = Stroke(w * .1f, cap = StrokeCap.Round))
        } else {
            drawLine(PapelClaro, Offset(w * .32f, w * .32f), Offset(w * .68f, w * .68f), w * .1f, StrokeCap.Round)
            drawLine(PapelClaro, Offset(w * .68f, w * .32f), Offset(w * .32f, w * .68f), w * .1f, StrokeCap.Round)
        }
    }
}
