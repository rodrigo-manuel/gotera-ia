package co.edu.ucc.ojodegallo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import co.edu.ucc.ojodegallo.data.Diagnostico
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

private val formatoDia = SimpleDateFormat("EEEE d 'de' MMMM 'de' yyyy · h:mm a", Locale("es", "CO"))

/**
 * Datos de la hoja que el caficultor puede corregir o completar: fecha, finca o vereda, lote y observaciones.
 * Se escriben a mano (sin GPS) para no pedir permisos de ubicación; se sugieren los nombres ya usados.
 */
@Composable
fun DatosDeLaHoja(d: Diagnostico, vm: AppViewModel, alGuardar: (Diagnostico) -> Unit) {
    var editando by remember(d.id) { mutableStateOf(false) }
    if (!editando) {
        Bloque {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Datos de la hoja", style = MaterialTheme.typography.titleMedium, color = Tinta, modifier = Modifier.weight(1f))
                TextButton(onClick = { editando = true }) {
                    Text("Editar", style = MaterialTheme.typography.labelLarge, color = VerdeHoja)
                }
            }
            Dato("Fecha", formatoDia.format(Date(d.fecha)))
            Dato("Finca o vereda", d.finca)
            Dato("Lote", d.lote)
            Dato("Observaciones", d.notas)
        }
    } else {
        FormularioDatos(d, vm, cancelar = { editando = false }) { nuevo ->
            vm.actualizar(nuevo); alGuardar(nuevo); editando = false
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String?) {
    Column(Modifier.padding(vertical = 5.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = TintaSuave)
        Text(if (valor.isNullOrBlank()) "Sin anotar" else valor, style = MaterialTheme.typography.bodyLarge,
            color = if (valor.isNullOrBlank()) TintaSuave else Tinta)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormularioDatos(d: Diagnostico, vm: AppViewModel, cancelar: () -> Unit, guardar: (Diagnostico) -> Unit) {
    var fecha by remember { mutableStateOf(d.fecha) }
    var finca by remember { mutableStateOf(d.finca.orEmpty()) }
    var lote by remember { mutableStateOf(d.lote.orEmpty()) }
    var notas by remember { mutableStateOf(d.notas.orEmpty()) }
    var calendario by remember { mutableStateOf(false) }
    val fincas by vm.fincas.collectAsState()
    val lotes by vm.lotes.collectAsState()

    Bloque {
        Text("Editar datos de la hoja", style = MaterialTheme.typography.titleMedium, color = Tinta)
        Espacio(10.dp)
        Text("Fecha", style = MaterialTheme.typography.labelMedium, color = TintaSuave)
        val forma = RoundedCornerShape(12.dp)
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(forma).border(1.dp, Linea, forma)
            .clickable { calendario = true }.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(formatoDia.format(Date(fecha)), style = MaterialTheme.typography.bodyLarge, color = Tinta, modifier = Modifier.weight(1f))
            Text("Cambiar", style = MaterialTheme.typography.labelLarge, color = VerdeHoja)
        }
        Espacio(12.dp)
        Campo("Finca o vereda", finca, { finca = it }, fincas)
        Campo("Lote", lote, { lote = it }, lotes)
        Campo("Observaciones", notas, { notas = it }, emptyList(), lineas = 3)
        Espacio(8.dp)
        BotonPrincipal("Guardar", Iconos.Guardado) {
            guardar(d.copy(fecha = fecha, finca = finca.trim().ifEmpty { null }, lote = lote.trim().ifEmpty { null },
                notas = notas.trim().ifEmpty { null }))
        }
        BotonTexto("Cancelar", onClick = cancelar)
    }

    if (calendario) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = diaUtc(fecha),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= System.currentTimeMillis()
            })
        DatePickerDialog(
            onDismissRequest = { calendario = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { fecha = conNuevoDia(fecha, it) }
                    calendario = false
                }) { Text("Aceptar", color = VerdeHoja) }
            },
            dismissButton = { TextButton(onClick = { calendario = false }) { Text("Cancelar", color = VerdeHoja) } },
            colors = DatePickerDefaults.colors(containerColor = PapelClaro),
        ) {
            DatePicker(state = estado, colors = DatePickerDefaults.colors(containerColor = PapelClaro,
                selectedDayContainerColor = VerdeHoja, todayDateBorderColor = VerdeHoja, todayContentColor = VerdeHoja))
        }
    }
}

/** Campo de texto grande con los valores ya usados como sugerencias tocables. */
@Composable
private fun Campo(etiqueta: String, valor: String, cambiar: (String) -> Unit, sugerencias: List<String>, lineas: Int = 1) {
    OutlinedTextField(
        value = valor, onValueChange = { if (it.length <= 80) cambiar(it) },
        label = { Text(etiqueta) }, singleLine = lineas == 1, minLines = lineas,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VerdeHoja, focusedLabelColor = VerdeHoja,
            cursorColor = VerdeHoja, unfocusedBorderColor = Linea),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
    val otras = sugerencias.filter { it != valor }.take(8)
    if (otras.isNotEmpty()) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            otras.forEach { s ->
                val forma = RoundedCornerShape(50)
                Row(Modifier.clip(forma).background(VerdeSanoFondo).clickable { cambiar(s) }
                    .heightIn(min = 40.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Iconos.Hoja, null, tint = VerdeHoja, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(s, style = MaterialTheme.typography.labelLarge, color = VerdeHoja)
                }
            }
        }
    }
}

/** El calendario trabaja con medianoche UTC del día elegido. */
private fun diaUtc(millis: Long): Long =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** Cambia el día de la fecha y conserva la hora original. */
private fun conNuevoDia(original: Long, diaUtcMillis: Long): Long {
    val dia = Instant.ofEpochMilli(diaUtcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    return Instant.ofEpochMilli(original).atZone(ZoneId.systemDefault()).with(dia).toInstant().toEpochMilli()
}
