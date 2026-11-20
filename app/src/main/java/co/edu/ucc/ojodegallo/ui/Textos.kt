package co.edu.ucc.ojodegallo.ui

import androidx.compose.ui.graphics.Color
import co.edu.ucc.ojodegallo.ml.Certeza
import co.edu.ucc.ojodegallo.ml.Resultado

/** Forma con la que se reconoce cada resultado sin necesidad de leer (además del color). */
enum class Simbolo { SANA, OJO, ROYA, OJO_O_ROYA, DUDA, NO_VALIDA }

/** Qué se le pide hacer a la persona después del resultado. */
enum class Accion { TECNICO, OTRAS_HOJAS, OTRA_FOTO }

/**
 * Textos de cada resultado (RF-05, RF-06), pensados para personas que leen con dificultad:
 * un título de pocas palabras, una frase para escuchar y una acción concreta.
 */
data class TextoResultado(
    val corto: String,             // dos o tres palabras, lo primero que se ve
    val titulo: String,            // frase completa para el historial
    val certeza: Certeza?,         // null si no hay diagnóstico
    val explicacion: String,
    val accion: Accion,
    val textoAccion: String,
    val nota: String?,
    val tono: Color,
    val fondo: Color,
    val simbolo: Simbolo,
) {
    /** Lo que lee la voz del teléfono. */
    val paraEscuchar: String
        get() = listOfNotNull(titulo + ".", certeza?.let { textoCerteza(it) + "." }, explicacion, textoAccion + ".", nota)
            .joinToString(" ")
}

fun textoCerteza(c: Certeza) = if (c == Certeza.ALTA) "Seguridad alta" else "Seguridad media"

fun textoDe(r: Resultado): TextoResultado = when (r) {
    Resultado.ImagenNoValida -> TextoResultado(
        corto = "No es hoja de café",
        titulo = "No se reconoce una hoja de café",
        certeza = null,
        explicacion = "La aplicación no reconoce una hoja de café en esta foto. Solo revisa hojas de café.",
        accion = Accion.OTRA_FOTO, textoAccion = "Si sí es una hoja de café, toma otra foto: una sola hoja, de cerca y con luz de día",
        nota = null, tono = Ocre, fondo = OcreFondo, simbolo = Simbolo.NO_VALIDA)
    Resultado.NoConcluyente -> TextoResultado(
        corto = "Repite la foto",
        titulo = "Resultado no concluyente",
        certeza = null,
        explicacion = "Con esta foto la aplicación no está segura. Prefiere no responder antes que equivocarse.",
        accion = Accion.OTRA_FOTO, textoAccion = "Toma otra foto de la misma hoja, más cerca y sin reflejos de sol",
        nota = null, tono = Ocre, fondo = OcreFondo, simbolo = Simbolo.DUDA)
    is Resultado.Compatible -> TextoResultado(
        corto = "Ojo de gallo",
        titulo = "Síntomas compatibles con ojo de gallo",
        certeza = r.certeza,
        explicacion = "La hoja tiene manchas parecidas a las del ojo de gallo.",
        accion = Accion.TECNICO, textoAccion = "Muéstrale la hoja al técnico o extensionista",
        nota = "Revisa también la parte de abajo de la hoja: si tiene polvillo anaranjado, puede ser roya.",
        tono = Terracota, fondo = TerracotaFondo, simbolo = Simbolo.OJO)
    is Resultado.CompatibleRoya -> TextoResultado(
        corto = "Parece roya",
        titulo = "Síntomas compatibles con roya",
        certeza = r.certeza,
        explicacion = "Las manchas se parecen a las de la roya, no a las del ojo de gallo.",
        accion = Accion.TECNICO, textoAccion = "Muéstrale la hoja al técnico o extensionista",
        nota = "La roya deja polvillo anaranjado en la parte de abajo de la hoja.",
        tono = Ocre, fondo = OcreFondo, simbolo = Simbolo.ROYA)
    Resultado.OjoORoya -> TextoResultado(
        corto = "Ojo de gallo o roya",
        titulo = "Manchas de ojo de gallo o de roya",
        certeza = null,
        explicacion = "La hoja tiene manchas, pero la aplicación no puede asegurar si son de ojo de gallo o de roya.",
        accion = Accion.TECNICO, textoAccion = "Muéstrale la hoja al técnico o extensionista",
        nota = "Mira la parte de abajo de la hoja. Si tiene polvillo anaranjado, puede ser roya. " +
            "Si las manchas son redondas, de borde marcado y sin polvillo, puede ser ojo de gallo.",
        tono = Terracota, fondo = TerracotaFondo, simbolo = Simbolo.OJO_O_ROYA)
    is Resultado.SinSintomas -> TextoResultado(
        corto = "Sin ojo de gallo",
        titulo = "No se observan síntomas de ojo de gallo",
        certeza = r.certeza,
        explicacion = "En esta foto no se ven manchas parecidas a las del ojo de gallo. Eso no descarta la enfermedad en otras hojas.",
        accion = Accion.OTRAS_HOJAS, textoAccion = "Revisa otras hojas de la planta",
        nota = "Esta aplicación solo busca ojo de gallo. Si ves otras manchas o tienes dudas, consulta al técnico.",
        tono = VerdeSano, fondo = VerdeSanoFondo, simbolo = Simbolo.SANA)
}

fun resultadoDesdeCodigo(codigo: String, certeza: String?): Resultado {
    val c = if (certeza == "ALTA") Certeza.ALTA else Certeza.MEDIA
    return when (codigo) {
        "COMPATIBLE_OJO_DE_GALLO" -> Resultado.Compatible(c)
        "COMPATIBLE_ROYA" -> Resultado.CompatibleRoya(c)
        "OJO_DE_GALLO_O_ROYA" -> Resultado.OjoORoya
        "SIN_SINTOMAS" -> Resultado.SinSintomas(c)
        "NO_CONCLUYENTE" -> Resultado.NoConcluyente
        else -> Resultado.ImagenNoValida
    }
}
