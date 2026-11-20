package co.edu.ucc.ojodegallo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.ucc.ojodegallo.R
import co.edu.ucc.ojodegallo.ml.Preprocesador
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Barra superior de las pantallas internas: botón para volver y título. */
@Composable
fun BarraSuperior(titulo: String, volver: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 4.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (volver != null) {
            IconButton(onClick = volver, modifier = Modifier.size(56.dp)) {
                Icon(Iconos.Atras, contentDescription = "Volver", tint = VerdeHoja, modifier = Modifier.size(26.dp))
            }
        } else Spacer(Modifier.width(16.dp))
        Text(titulo, style = MaterialTheme.typography.titleLarge, color = Tinta)
    }
}

/** Estructura común de una pantalla interna: barra superior y contenido desplazable. */
@Composable
fun Pantalla(titulo: String, volver: (() -> Unit)?, contenido: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Papel)) {
        BarraSuperior(titulo, volver)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp), content = contenido)
    }
}

/** Bloque con fondo y borde fino (sin sombras). */
@Composable
fun Bloque(fondo: Color = PapelClaro, borde: Color = Linea, modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    val forma = RoundedCornerShape(16.dp)
    Column(modifier.fillMaxWidth().clip(forma).background(fondo).border(1.dp, borde, forma).padding(18.dp),
        content = contenido)
}

@Composable
fun BotonPrincipal(texto: String, icono: ImageVector? = null, habilitado: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = habilitado,
        modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = VerdeHoja, contentColor = PapelClaro)) {
        if (icono != null) { Icon(icono, null, Modifier.size(24.dp)); Spacer(Modifier.width(12.dp)) }
        Text(texto, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun BotonSecundario(texto: String, icono: ImageVector? = null, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(14.dp), border = BorderStroke(1.5.dp, VerdeHoja),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = VerdeHoja)) {
        if (icono != null) { Icon(icono, null, Modifier.size(22.dp)); Spacer(Modifier.width(12.dp)) }
        Text(texto, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun BotonTexto(texto: String, color: Color = VerdeHoja, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(texto, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
fun Parrafo(texto: String, color: Color = Tinta, negrita: Boolean = false, modifier: Modifier = Modifier) {
    Text(texto, style = MaterialTheme.typography.bodyLarge, color = color,
        fontWeight = if (negrita) FontWeight.Bold else FontWeight.Normal, modifier = modifier.padding(vertical = 3.dp))
}

@Composable
fun Subtitulo(texto: String, color: Color = Tinta) {
    Text(texto, style = MaterialTheme.typography.titleMedium, color = color, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
}

/** Fila de menú: icono, título, detalle y flecha. Área táctil de al menos 48 dp. */
@Composable
fun FilaMenu(icono: ImageVector, titulo: String, detalle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, null, tint = VerdeHoja, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall, color = Tinta)
            Text(detalle, style = MaterialTheme.typography.bodyMedium, color = TintaSuave)
        }
        Icon(Iconos.Siguiente, null, tint = TintaSuave, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun Divisor() = HorizontalDivider(thickness = 1.dp, color = Linea)

/** Paso de la guía: número en la tipografía de títulos y texto. */
@Composable
fun PasoNumerado(n: Int, texto: String) {
    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Text("$n", style = MaterialTheme.typography.headlineSmall, color = VerdeHoja, modifier = Modifier.width(34.dp))
        Text(texto, style = MaterialTheme.typography.bodyLarge, color = Tinta, modifier = Modifier.weight(1f).padding(top = 2.dp))
    }
}

/** Sello de la aplicación (el logo sobre su fondo verde). */
@Composable
fun Sello(tam: Dp = 48.dp) {
    Box(Modifier.size(tam).clip(RoundedCornerShape(tam / 4)).background(VerdeHoja), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.fillMaxSize())
    }
}

/** Foto ya cargada en memoria, con proporción 4:3 y esquinas redondeadas. */
@Composable
fun FotoHoja(img: ImageBitmap?, cargando: Boolean = false) {
    val forma = RoundedCornerShape(18.dp)
    Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(forma).background(Color(0xFFE4DFD1)),
        contentAlignment = Alignment.Center) {
        if (img != null) Image(img, contentDescription = "Foto de la hoja analizada", contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize())
        if (cargando || img == null) CircularProgressIndicator(color = VerdeHoja, strokeWidth = 3.dp)
    }
}

/** Foto guardada en el teléfono (historial). */
@Composable
fun FotoArchivo(ruta: String) {
    val img by produceState<ImageBitmap?>(null, ruta) {
        value = withContext(Dispatchers.IO) { Preprocesador.miniatura(File(ruta))?.asImageBitmap() }
    }
    FotoHoja(img)
}

/** Miniatura cuadrada para las listas. */
@Composable
fun Miniatura(ruta: String, tam: Dp = 56.dp) {
    val img by produceState<ImageBitmap?>(null, ruta) {
        value = withContext(Dispatchers.IO) { Preprocesador.miniatura(File(ruta), 256)?.asImageBitmap() }
    }
    Box(Modifier.size(tam).clip(RoundedCornerShape(12.dp)).background(Color(0xFFE4DFD1))) {
        img?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
    }
}

/** Separación vertical estándar. */
@Composable
fun Espacio(alto: Dp = 12.dp) = Spacer(Modifier.height(alto))

/** Nombre de la aplicación: "Gotera" en la tipografía de marca y la etiqueta "IA". */
@Composable
fun Marca(tamSello: Dp = 40.dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Sello(tamSello)
        Spacer(Modifier.width(10.dp))
        Text("Gotera", fontFamily = FuenteMarca, fontSize = (tamSello.value * 0.78f).sp, color = VerdeHoja)
        Spacer(Modifier.width(6.dp))
        Text("IA", style = MaterialTheme.typography.labelMedium, color = PapelClaro,
            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Terracota).padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

/** Botón para escuchar un texto en voz alta. No aparece si el teléfono no tiene voz en español. */
@Composable
fun BotonEscuchar(voz: Voz, texto: String, etiqueta: String = "Escuchar", modifier: Modifier = Modifier) {
    if (!voz.disponible) return
    val forma = RoundedCornerShape(50)
    Row(modifier.clip(forma).border(1.5.dp, VerdeHoja, forma)
        .clickable { if (voz.hablando) voz.callar() else voz.decir(texto) }
        .heightIn(min = 48.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(if (voz.hablando) Iconos.Detener else Iconos.Altavoz, null, tint = VerdeHoja, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(if (voz.hablando) "Detener" else etiqueta, style = MaterialTheme.typography.labelLarge, color = VerdeHoja)
    }
}
