package co.edu.ucc.ojodegallo.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.edu.ucc.ojodegallo.data.Diagnostico
import co.edu.ucc.ojodegallo.ml.Analisis
import co.edu.ucc.ojodegallo.ml.Certeza
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val formatoFecha = SimpleDateFormat("d MMM yyyy · h:mm a", Locale("es", "CO"))
private val formatoCorto = SimpleDateFormat("d MMM", Locale("es", "CO"))

// ----------------------------------------------------------------- Inicio
@Composable
fun InicioScreen(vm: AppViewModel, irCamara: () -> Unit, irResultado: () -> Unit, irHistorial: () -> Unit,
                 irGuia: () -> Unit, irInfo: () -> Unit, irPrueba: () -> Unit, abrirDetalle: (Long) -> Unit) {
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { vm.analizarDeGaleria(uri); irResultado() }
    }
    // Respaldo: explorador de archivos de Android (Descargas, WhatsApp, etc.), para fotos que no aparecen en la galería.
    val archivos = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { vm.analizarDeGaleria(uri); irResultado() }
    }
    val historial by vm.historial.collectAsState()
    val voz = recordarVoz()

    Column(Modifier.fillMaxSize().background(Papel).verticalScroll(rememberScrollState())
        .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Marca()
            Spacer(Modifier.weight(1f))
            BotonEscuchar(voz, "Gotera I A revisa hojas de café. Toque el botón verde grande para tomar una foto de una " +
                "hoja. La aplicación le dice si tiene ojo de gallo. Funciona sin internet.", etiqueta = "Oír")
        }
        Espacio(22.dp)

        // Acción principal: un solo botón grande, al alcance del pulgar.
        val forma = RoundedCornerShape(28.dp)
        Column(Modifier.fillMaxWidth().height(232.dp).clip(forma).background(VerdeHoja).clickable(onClick = irCamara)
            .padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(84.dp).clip(CircleShape).background(PapelClaro.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(Iconos.Camara, contentDescription = null, tint = PapelClaro, modifier = Modifier.size(48.dp))
            }
            Column {
                Text("Tomar foto", style = MaterialTheme.typography.displaySmall, color = PapelClaro)
                Text("de una hoja de café", style = MaterialTheme.typography.titleMedium, color = PapelClaro.copy(alpha = 0.85f))
            }
        }
        Espacio(12.dp)
        Row(Modifier.fillMaxWidth()) {
            Mosaico(Iconos.Galeria, "Galería", Modifier.weight(1f)) {
                galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            Spacer(Modifier.width(12.dp))
            Mosaico(Iconos.Encuadre, "Cómo tomarla", Modifier.weight(1f), onClick = irGuia)
        }
        TextButton(onClick = { archivos.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
            Text("¿No aparece tu foto? Buscar en archivos", style = MaterialTheme.typography.labelLarge, color = VerdeHoja)
        }

        Espacio(28.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mis hojas", style = MaterialTheme.typography.titleLarge, color = Tinta, modifier = Modifier.weight(1f))
            if (historial.isNotEmpty()) TextButton(onClick = irHistorial) {
                Text("Ver todas (${historial.size})", style = MaterialTheme.typography.labelLarge, color = VerdeHoja)
            }
        }
        Espacio(6.dp)
        if (historial.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 10.dp)) {
                Icon(Iconos.Hoja, null, tint = TintaSuave, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text("Aquí aparecen las hojas que revises.", style = MaterialTheme.typography.bodyLarge, color = TintaSuave)
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(historial.take(12), key = { it.id }) { d -> TarjetaHoja(d) { abrirDetalle(d.id) } }
            }
            Espacio(8.dp)
            Leyenda()
        }

        Espacio(24.dp)
        Divisor()
        FilaMenu(Iconos.Info, "Qué hace y sus límites", "Datos usados y créditos", irInfo)
        Divisor()
        Text("Modo de prueba (evaluación técnica)", style = MaterialTheme.typography.bodySmall, color = TintaSuave,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clickable(onClick = irPrueba).padding(vertical = 16.dp))
    }
}

/** Acceso secundario del inicio: icono grande y una o dos palabras. */
@Composable
private fun Mosaico(icono: ImageVector, texto: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val forma = RoundedCornerShape(22.dp)
    Column(modifier.height(112.dp).clip(forma).background(PapelClaro).border(1.dp, Linea, forma).clickable(onClick = onClick)
        .padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Icon(icono, null, tint = VerdeHoja, modifier = Modifier.size(32.dp))
        Text(texto, style = MaterialTheme.typography.titleMedium, color = Tinta)
    }
}

/** Miniatura del historial con el símbolo del resultado encima: se entiende sin leer. */
@Composable
private fun TarjetaHoja(d: Diagnostico, onClick: () -> Unit) {
    val t = textoDe(resultadoDesdeCodigo(d.resultado, d.certeza))
    Column(Modifier.width(104.dp).clickable(onClick = onClick)) {
        Box {
            Miniatura(d.rutaFoto, 104.dp)
            SimboloResultado(t.simbolo, 30.dp, Modifier.align(Alignment.BottomEnd).padding(6.dp))
        }
        Text(formatoCorto.format(Date(d.fecha)) + (d.lote?.let { " · $it" } ?: ""), style = MaterialTheme.typography.bodySmall,
            color = TintaSuave, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Leyenda de los símbolos. */
@Composable
private fun Leyenda() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        listOf(Simbolo.OJO to "Ojo de gallo", Simbolo.OJO_O_ROYA to "Ojo o roya", Simbolo.SANA to "Sin ojo de gallo").forEach { (s, n) ->
            SimboloResultado(s, 16.dp); Spacer(Modifier.width(5.dp))
            Text(n, style = MaterialTheme.typography.bodySmall, color = TintaSuave); Spacer(Modifier.width(12.dp))
        }
    }
}

// ----------------------------------------------------------------- Resultado
@Composable
fun ResultadoScreen(vm: AppViewModel, otraFoto: () -> Unit, inicio: () -> Unit) {
    val estado by vm.estado.collectAsState()
    val voz = recordarVoz()
    val haptica = LocalHapticFeedback.current
    Pantalla("Resultado", volver = inicio) {
        when (val e = estado) {
            EstadoAnalisis.Inactivo -> FotoHoja(null, cargando = true)
            is EstadoAnalisis.Procesando -> {
                FotoHoja(e.foto, cargando = true)
                Espacio(16.dp)
                Parrafo("Revisando la hoja…", color = TintaSuave)
            }
            is EstadoAnalisis.Error -> {
                Bloque(fondo = OcreFondo, borde = Ocre) { Parrafo(e.mensaje, negrita = true) }
                Espacio(16.dp)
                BotonPrincipal("Tomar otra foto", Iconos.Camara, onClick = otraFoto)
                BotonTexto("Volver al inicio", onClick = inicio)
            }
            is EstadoAnalisis.Listo -> {
                val t = textoDe(e.analisis.resultado)
                LaunchedEffect(e) {
                    haptica.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                // Aparición suave del resultado (no bloquea la lectura).
                var visible by remember(e) { mutableStateOf(false) }
                LaunchedEffect(e) { visible = true }
                val a by animateFloatAsState(if (visible) 1f else 0f, tween(260), label = "aparicion")
                Box {
                    FotoHoja(e.foto)
                    SimboloResultado(t.simbolo, 56.dp, Modifier.align(Alignment.BottomStart).padding(14.dp)
                        .graphicsLayer { scaleX = 0.6f + 0.4f * a; scaleY = 0.6f + 0.4f * a; alpha = a })
                }
                Espacio(16.dp)
                Column(Modifier.alpha(a)) { Veredicto(t, voz) }
                Espacio(10.dp)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                    if (e.guardado) { Icon(Iconos.Guardado, null, tint = TintaSuave, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
                    Text(if (e.guardado) "Guardado en Mis hojas" else "Esta foto no se guarda",
                        style = MaterialTheme.typography.bodyMedium, color = TintaSuave)
                }
                Espacio(18.dp)
                BotonPrincipal("Revisar otra hoja", Iconos.Camara, onClick = otraFoto)
                BotonTexto("Volver al inicio", onClick = inicio)
                Espacio(8.dp)
                DatosTecnicos(e.analisis)
            }
        }
    }
}

/** Bloque del resultado: símbolo, palabras clave, seguridad, acción y botón para escuchar. */
@Composable
fun Veredicto(t: TextoResultado, voz: Voz) {
    Bloque(fondo = t.fondo, borde = t.tono.copy(alpha = 0.5f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(t.corto, style = MaterialTheme.typography.displaySmall, color = t.tono, modifier = Modifier.weight(1f))
        }
        t.certeza?.let { Espacio(6.dp); Seguridad(it, t.tono) }
        Espacio(12.dp)
        Text(t.explicacion, style = MaterialTheme.typography.bodyLarge, color = Tinta)
        Espacio(14.dp)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)).background(PapelClaro).padding(14.dp)) {
            Icon(when (t.accion) { Accion.TECNICO -> Iconos.Persona; Accion.OTRAS_HOJAS -> Iconos.Hoja; Accion.OTRA_FOTO -> Iconos.Camara },
                null, tint = t.tono, modifier = Modifier.size(34.dp))
            Spacer(Modifier.width(14.dp))
            Text(t.textoAccion, style = MaterialTheme.typography.titleMedium, color = Tinta)
        }
        t.nota?.let {
            Espacio(10.dp)
            Text(it, style = MaterialTheme.typography.bodyMedium, color = TintaSuave)
        }
        Espacio(14.dp)
        BotonEscuchar(voz, t.paraEscuchar)
    }
}

/** Seguridad del resultado en puntos: tres llenos = alta, dos = media. Se entiende sin leer el número. */
@Composable
private fun Seguridad(c: Certeza, tono: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val llenos = if (c == Certeza.ALTA) 3 else 2
        repeat(3) { i ->
            Box(Modifier.size(12.dp).clip(CircleShape).background(if (i < llenos) tono else tono.copy(alpha = 0.2f)))
            Spacer(Modifier.width(5.dp))
        }
        Spacer(Modifier.width(6.dp))
        Text(textoCerteza(c), style = MaterialTheme.typography.labelMedium, color = tono)
    }
}

/** Panel desplegable con las salidas de los modelos: transparencia para el técnico y para la evaluación. */
@Composable
private fun DatosTecnicos(a: Analisis) {
    var abierto by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clickable { abierto = !abierto }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Iconos.Datos, null, tint = TintaSuave, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(if (abierto) "Ocultar datos técnicos" else "Ver datos técnicos", style = MaterialTheme.typography.labelLarge, color = TintaSuave)
    }
    AnimatedVisibility(abierto) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(PapelClaro).border(1.dp, Linea, RoundedCornerShape(12.dp))
            .padding(14.dp)) {
            fun f(x: Float) = if (x.isNaN()) "—" else String.format(Locale.US, "%.3f", x)
            listOf(
                "P(hoja de café)" to f(a.pHoja),
                "P(ojo de gallo), 4 vistas" to f(a.pOjo),
                "P(roya), 4 vistas" to f(a.pRoya),
                "Preparación de la imagen" to String.format(Locale.US, "%.0f ms", a.msPreprocesamiento),
                "Modelos" to String.format(Locale.US, "%.0f ms", a.msInferencia),
                "Resultado" to a.resultado.codigo,
            ).forEach { (k, v) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(k, style = MaterialTheme.typography.bodyMedium, color = TintaSuave, modifier = Modifier.weight(1f))
                    Text(v, style = MaterialTheme.typography.bodyMedium, color = Tinta, fontFamily = FontFamily.Monospace)
                }
            }
            Espacio(6.dp)
            Text("Todo se calcula en el teléfono, sin internet. Las probabilidades no están calibradas.",
                style = MaterialTheme.typography.bodySmall, color = TintaSuave)
        }
    }
}

// ----------------------------------------------------------------- Historial
@Composable
private fun FilaHistorial(d: Diagnostico, onClick: () -> Unit) {
    val t = textoDe(resultadoDesdeCodigo(d.resultado, d.certeza))
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box {
            Miniatura(d.rutaFoto, 64.dp)
            SimboloResultado(t.simbolo, 22.dp, Modifier.align(Alignment.BottomEnd).padding(3.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(t.corto, style = MaterialTheme.typography.titleMedium, color = t.tono)
            Text(formatoFecha.format(Date(d.fecha)) + (t.certeza?.let { " · " + textoCerteza(it) } ?: ""),
                style = MaterialTheme.typography.bodyMedium, color = TintaSuave)
            listOfNotNull(d.finca, d.lote?.let { "Lote $it" }).takeIf { it.isNotEmpty() }?.let {
                Text(it.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = VerdeHoja)
            }
        }
        Icon(Iconos.Siguiente, null, tint = TintaSuave, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun HistorialScreen(vm: AppViewModel, volver: () -> Unit, abrir: (Long) -> Unit) {
    val lista by vm.historial.collectAsState()
    val lotes by vm.lotes.collectAsState()
    var filtro by remember { mutableStateOf<String?>(null) }
    val visibles = if (filtro == null) lista else lista.filter { it.lote == filtro }
    Column(Modifier.fillMaxSize().background(Papel)) {
        BarraSuperior("Mis hojas", volver)
        if (lista.isEmpty()) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Parrafo("Todavía no hay hojas revisadas.", negrita = true)
                Parrafo("Se guardan las fotos con resultado. Las fotos no válidas o no concluyentes no se guardan.", color = TintaSuave)
            }
        } else {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Leyenda()
                if (lotes.isNotEmpty()) {
                    Espacio(10.dp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf<String?>(null) + lotes) { l ->
                            val activo = filtro == l
                            val forma = RoundedCornerShape(50)
                            Box(Modifier.clip(forma).background(if (activo) VerdeHoja else PapelClaro).border(1.dp, if (activo) VerdeHoja else Linea, forma)
                                .clickable { filtro = l }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                Text(l?.let { "Lote $it" } ?: "Todos", style = MaterialTheme.typography.labelLarge,
                                    color = if (activo) PapelClaro else Tinta)
                            }
                        }
                    }
                }
            }
        }
        LazyColumn(Modifier.padding(horizontal = 20.dp)) {
            items(visibles, key = { it.id }) { d -> FilaHistorial(d) { abrir(d.id) }; Divisor() }
        }
    }
}

@Composable
fun DetalleScreen(vm: AppViewModel, id: Long, volver: () -> Unit) {
    var d by remember { mutableStateOf<Diagnostico?>(null) }
    var confirmar by remember { mutableStateOf(false) }
    val voz = recordarVoz()
    LaunchedEffect(id) { d = vm.porId(id) }
    val actual = d
    Pantalla("Revisión", volver = volver) {
        if (actual == null) { Parrafo("Cargando…", color = TintaSuave); return@Pantalla }
        Text(formatoFecha.format(Date(actual.fecha)), style = MaterialTheme.typography.bodyMedium, color = TintaSuave)
        Espacio(8.dp)
        val t = textoDe(resultadoDesdeCodigo(actual.resultado, actual.certeza))
        Box {
            FotoArchivo(actual.rutaFoto)
            SimboloResultado(t.simbolo, 52.dp, Modifier.align(Alignment.BottomStart).padding(14.dp))
        }
        Espacio(16.dp)
        Veredicto(t, voz)
        Espacio(16.dp)
        DatosDeLaHoja(actual, vm) { d = it }
        Espacio(20.dp)
        BotonSecundario("Borrar esta revisión", Iconos.Papelera) { confirmar = true }
    }
    if (confirmar && actual != null) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            containerColor = PapelClaro,
            title = { Text("¿Borrar la revisión?", style = MaterialTheme.typography.titleLarge) },
            text = { Text("Se borran la foto y el resultado de este teléfono.", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = { TextButton(onClick = { vm.eliminar(actual); confirmar = false; volver() }) { Text("Borrar", color = Terracota) } },
            dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Cancelar", color = VerdeHoja) } },
        )
    }
}

// ----------------------------------------------------------------- Guía con dibujos
@Composable
fun GuiaScreen(volver: () -> Unit) {
    val voz = recordarVoz()
    Pantalla("Cómo tomar la foto", volver) {
        BotonEscuchar(voz, "Para una buena foto: una sola hoja, de cerca, que llene el recuadro. No tome varias hojas a la vez. " +
            "Evite el sol directo y los brillos sobre la hoja. Quédese quieto para que la foto no salga movida. " +
            "Si la mancha está por debajo de la hoja, tome la foto de ese lado.", etiqueta = "Escuchar los consejos")
        Espacio(16.dp)
        Row(Modifier.fillMaxWidth()) {
            Escenario(Escena.UNA_HOJA, true, "Una hoja, de cerca", Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Escenario(Escena.VARIAS_HOJAS, false, "Varias hojas", Modifier.weight(1f))
        }
        Espacio(12.dp)
        Row(Modifier.fillMaxWidth()) {
            Escenario(Escena.REFLEJO, false, "Sol y brillos", Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Escenario(Escena.BORROSA, false, "Foto movida", Modifier.weight(1f))
        }
        Espacio(18.dp)
        Bloque(fondo = OcreFondo, borde = Ocre.copy(alpha = 0.5f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SimboloResultado(Simbolo.DUDA, 34.dp); Spacer(Modifier.width(12.dp))
                Text("Si dice «Repite la foto», toma otra de la misma hoja, más cerca o con otra luz.",
                    style = MaterialTheme.typography.bodyLarge, color = Tinta)
            }
        }
    }
}

@Composable
private fun Escenario(e: Escena, correcto: Boolean, texto: String, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(18.dp)
    Column(modifier.clip(forma).background(PapelClaro).border(1.5.dp, if (correcto) VerdeSano else Linea, forma).padding(12.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            PictogramaEscena(e, Modifier.fillMaxSize())
            Box(Modifier.align(Alignment.TopEnd)) { MarcaSiNo(correcto) }
        }
        Espacio(8.dp)
        Text(texto, style = MaterialTheme.typography.titleSmall, color = Tinta, fontWeight = FontWeight.Bold)
    }
}

// ----------------------------------------------------------------- Información
@Composable
fun InfoScreen(volver: () -> Unit) = Pantalla("Qué hace y sus límites", volver) {
    Subtitulo("Qué hace")
    Parrafo("Busca en la foto de una hoja de café síntomas de ojo de gallo (Mycena citricolor), también llamado gotera. " +
        "Si las manchas también podrían ser de roya, no decide y te lo avisa. Todo se calcula en el teléfono, sin internet.")
    Espacio(16.dp)
    Bloque(fondo = OcreFondo, borde = Ocre.copy(alpha = 0.5f)) {
        Subtitulo("Límites", Ocre)
        Parrafo("Es una herramienta de apoyo: no reemplaza al técnico, al extensionista ni a un análisis de laboratorio.")
        Parrafo("Puede equivocarse, sobre todo entre roya y ojo de gallo y con fotos muy distintas a las de entrenamiento.")
        Parrafo("El modelo de ojo de gallo se entrenó con fotos de Perú. Con fotos de otro país dejó pasar varias hojas enfermas: un resultado sin ojo de gallo no es una garantía. Todavía no se ha probado con hojas de Minca.")
        Parrafo("La seguridad alta o media es una guía, no una probabilidad.")
    }
    Espacio(20.dp)
    Subtitulo("Datos usados")
    Parrafo("Santa Maria Pinedo, J. C. (2026). Coffee leaf dataset by phytosanitary class (versión 1). Mendeley Data. " +
        "https://doi.org/10.17632/mfpxg4y65r.1. CC BY 4.0.", color = TintaSuave)
    Parrafo("Parraga-Alava, J. y otros (2019). RoCoLe: robusta coffee leaf images dataset. Mendeley Data. " +
        "https://doi.org/10.17632/c5yvn32dzg.2. CC BY 4.0.", color = TintaSuave)
    Parrafo("Krohling, R. A., Esgario, G. J. M. y Ventura, J. A. (2019). BRACOL. Mendeley Data. " +
        "https://doi.org/10.17632/yy2k5y8mxg.1. CC BY 4.0.", color = TintaSuave)
    Espacio(12.dp)
    Subtitulo("Tipografías")
    Parrafo("Atkinson Hyperlegible (Braille Institute of America) y Fraunces (The Fraunces Project Authors), " +
        "con licencia SIL Open Font License 1.1.", color = TintaSuave)
    Espacio(12.dp)
    Subtitulo("Autor")
    Parrafo("Rodrigo Manuel Alfaro Giraldo · Ingeniería de Software · Universidad Cooperativa de Colombia, Santa Marta. " +
        "Gotera IA, versión 1.4.", color = TintaSuave)
}
