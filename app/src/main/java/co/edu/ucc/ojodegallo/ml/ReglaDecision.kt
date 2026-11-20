package co.edu.ucc.ojodegallo.ml

/** Nivel cualitativo de certeza. La salida del modelo no es una probabilidad calibrada (RF-05). */
enum class Certeza { ALTA, MEDIA }

/** Los cuatro resultados posibles del análisis de una foto. */
sealed class Resultado {
    /** El verificador no reconoce una hoja de café (RF-03). */
    object ImagenNoValida : Resultado()
    /** La probabilidad de ojo de gallo cae dentro de la banda no concluyente (RF-05). */
    object NoConcluyente : Resultado()
    /** Síntomas compatibles con ojo de gallo (RF-04). */
    data class Compatible(val certeza: Certeza) : Resultado()
    /** Síntomas compatibles con roya: se informa para no confundirla con ojo de gallo (modelo de tres clases). */
    data class CompatibleRoya(val certeza: Certeza) : Resultado()
    /**
     * Hay manchas y el detector de roya también reacciona: la aplicación no decide entre ojo de gallo y roya
     * y pide revisar la hoja (Fase 2e). Nunca convierte un posible ojo de gallo en "sin síntomas".
     */
    object OjoORoya : Resultado()
    /** Sin síntomas de ojo de gallo (RF-04). */
    data class SinSintomas(val certeza: Certeza) : Resultado()

    /** Nombre corto que se guarda en el historial y en los archivos de prueba. */
    val codigo: String
        get() = when (this) {
            ImagenNoValida -> "IMAGEN_NO_VALIDA"
            NoConcluyente -> "NO_CONCLUYENTE"
            is Compatible -> "COMPATIBLE_OJO_DE_GALLO"
            is CompatibleRoya -> "COMPATIBLE_ROYA"
            OjoORoya -> "OJO_DE_GALLO_O_ROYA"
            is SinSintomas -> "SIN_SINTOMAS"
        }
}

/**
 * Regla de decisión de la aplicación (sección 3.3.2 de la tesis).
 * Es una función pura, sin dependencias de Android, para poder probarla con pruebas unitarias (CP-11, RNF-07).
 */
object ReglaDecision {
    const val UMBRAL_HOJA = 0.5f            // P(hoja de café) mínima para aceptar la imagen
    const val BANDA_INFERIOR = 0.30f        // P(ojo de gallo) <= 0,30 → sin síntomas
    const val BANDA_SUPERIOR = 0.70f        // P(ojo de gallo) >= 0,70 → síntomas compatibles
    const val LIMITE_CERTEZA_ALTA = 0.90f   // criterio de diseño: >= 0,90 o <= 0,10 → certeza alta

    fun decidir(pHoja: Float, pOjo: Float): Resultado {
        require(!pHoja.isNaN() && !pOjo.isNaN()) { "Las probabilidades no pueden ser NaN" }
        if (pHoja < UMBRAL_HOJA) return Resultado.ImagenNoValida
        return when {
            pOjo >= BANDA_SUPERIOR ->
                Resultado.Compatible(if (pOjo >= LIMITE_CERTEZA_ALTA) Certeza.ALTA else Certeza.MEDIA)
            pOjo <= BANDA_INFERIOR ->
                Resultado.SinSintomas(if (pOjo <= 1f - LIMITE_CERTEZA_ALTA) Certeza.ALTA else Certeza.MEDIA)
            else -> Resultado.NoConcluyente
        }
    }

    /** Umbral predeterminado del detector de roya; la app usa el que trae modelo_roya_umbral.txt (Fase 2e). */
    const val UMBRAL_ROYA_PREDETERMINADO = 0.5f

    /**
     * Regla en cascada con el detector de roya (Fase 2e).
     * Si la hoja muestra lesiones (P(ojo) >= 0,30) y el detector reacciona a la roya (P(roya) >= umbral), el resultado es
     * "ojo de gallo o roya": la aplicación no afirma ninguna de las dos, porque en la validación externa el detector
     * confundió ojo de gallo con roya. Así no se reporta la roya como ojo de gallo seguro y tampoco se oculta un posible
     * ojo de gallo. Sin detector (pRoya = NaN) es igual a decidir().
     */
    fun decidirConRoya(pHoja: Float, pOjo: Float, pRoya: Float, umbralRoya: Float = UMBRAL_ROYA_PREDETERMINADO): Resultado {
        if (pRoya.isNaN()) return decidir(pHoja, pOjo)
        require(!pHoja.isNaN() && !pOjo.isNaN()) { "Las probabilidades no pueden ser NaN" }
        if (pHoja < UMBRAL_HOJA) return Resultado.ImagenNoValida
        if (pOjo >= BANDA_INFERIOR && pRoya >= umbralRoya) {
            return Resultado.OjoORoya
        }
        return decidir(pHoja, pOjo)
    }

    /**
     * Regla del modelo de tres clases: la clase con probabilidad >= 0,70 decide; si ninguna llega, no es concluyente.
     * La certeza es alta cuando esa probabilidad es >= 0,90.
     */
    fun decidirTresClases(pHoja: Float, pOjo: Float, pRoya: Float, pSana: Float): Resultado {
        require(listOf(pHoja, pOjo, pRoya, pSana).none { it.isNaN() }) { "Las probabilidades no pueden ser NaN" }
        if (pHoja < UMBRAL_HOJA) return Resultado.ImagenNoValida
        val max = maxOf(pOjo, pRoya, pSana)
        if (max < BANDA_SUPERIOR) return Resultado.NoConcluyente
        val c = if (max >= LIMITE_CERTEZA_ALTA) Certeza.ALTA else Certeza.MEDIA
        return when (max) {
            pOjo -> Resultado.Compatible(c)
            pRoya -> Resultado.CompatibleRoya(c)
            else -> Resultado.SinSintomas(c)
        }
    }
}
