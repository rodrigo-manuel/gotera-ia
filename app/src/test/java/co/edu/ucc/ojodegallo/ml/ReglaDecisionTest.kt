package co.edu.ucc.ojodegallo.ml

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pruebas unitarias de la regla de decisión (CP-11 y RNF-07). */
class ReglaDecisionTest {

    @Test
    fun imagenQueNoEsHojaSeRechaza() {
        assertEquals(Resultado.ImagenNoValida, ReglaDecision.decidir(pHoja = 0.49f, pOjo = 0.99f))
    }

    @Test
    fun cp11_probabilidadEntre030y070EsNoConcluyente() {
        for (p in listOf(0.31f, 0.50f, 0.69f)) {
            assertEquals(Resultado.NoConcluyente, ReglaDecision.decidir(pHoja = 0.9f, pOjo = p))
        }
    }

    @Test
    fun limitesDeLaBanda() {
        assertEquals(Resultado.Compatible(Certeza.MEDIA), ReglaDecision.decidir(0.9f, 0.70f))
        assertEquals(Resultado.SinSintomas(Certeza.MEDIA), ReglaDecision.decidir(0.9f, 0.30f))
    }

    @Test
    fun certezaAltaYMedia() {
        assertEquals(Resultado.Compatible(Certeza.ALTA), ReglaDecision.decidir(0.9f, 0.95f))
        assertEquals(Resultado.Compatible(Certeza.MEDIA), ReglaDecision.decidir(0.9f, 0.80f))
        assertEquals(Resultado.SinSintomas(Certeza.ALTA), ReglaDecision.decidir(0.9f, 0.05f))
        assertEquals(Resultado.SinSintomas(Certeza.MEDIA), ReglaDecision.decidir(0.9f, 0.20f))
    }

    @Test
    fun umbralDeHojaExacto() {
        assertEquals(Resultado.SinSintomas(Certeza.ALTA), ReglaDecision.decidir(0.5f, 0.0f))
    }

    @Test(expected = IllegalArgumentException::class)
    fun probabilidadInvalidaLanzaError() {
        ReglaDecision.decidir(Float.NaN, 0.5f)
    }

    // ---- Modelo de tres clases (ojo de gallo, roya, sana)
    @Test
    fun tresClases_ojoDeGallo() {
        assertEquals(Resultado.Compatible(Certeza.ALTA), ReglaDecision.decidirTresClases(0.9f, 0.95f, 0.03f, 0.02f))
    }

    @Test
    fun tresClases_royaNoSeConfundeConOjo() {
        assertEquals(Resultado.CompatibleRoya(Certeza.MEDIA), ReglaDecision.decidirTresClases(0.9f, 0.10f, 0.80f, 0.10f))
    }

    @Test
    fun tresClases_sinSintomas() {
        assertEquals(Resultado.SinSintomas(Certeza.ALTA), ReglaDecision.decidirTresClases(0.9f, 0.02f, 0.03f, 0.95f))
    }

    @Test
    fun tresClases_ningunaLlegaA070EsNoConcluyente() {
        assertEquals(Resultado.NoConcluyente, ReglaDecision.decidirTresClases(0.9f, 0.45f, 0.40f, 0.15f))
    }

    @Test
    fun tresClases_imagenNoValida() {
        assertEquals(Resultado.ImagenNoValida, ReglaDecision.decidirTresClases(0.2f, 0.95f, 0.03f, 0.02f))
    }

    @Test
    fun conRoya_sinDetectorEsIgualALaReglaBase() {
        assertEquals(ReglaDecision.decidir(0.9f, 0.95f), ReglaDecision.decidirConRoya(0.9f, 0.95f, Float.NaN))
    }

    @Test
    fun conRoya_siElDetectorReaccionaLaAppNoDecide() {
        assertEquals(Resultado.OjoORoya, ReglaDecision.decidirConRoya(0.99f, 0.99f, 0.95f, 0.5f))
        assertEquals(Resultado.OjoORoya, ReglaDecision.decidirConRoya(0.99f, 0.50f, 0.60f, 0.5f))
    }

    @Test
    fun conRoya_bajoElUmbralSigueLaReglaBase() {
        assertEquals(Resultado.Compatible(Certeza.ALTA), ReglaDecision.decidirConRoya(0.99f, 0.99f, 0.49f, 0.5f))
    }

    @Test
    fun conRoya_hojaSinLesionesNoSeMarcaComoRoya() {
        assertEquals(Resultado.SinSintomas(Certeza.ALTA), ReglaDecision.decidirConRoya(0.99f, 0.05f, 0.90f, 0.5f))
    }

    @Test
    fun conRoya_imagenNoValidaTienePrioridad() {
        assertEquals(Resultado.ImagenNoValida, ReglaDecision.decidirConRoya(0.2f, 0.99f, 0.99f, 0.5f))
    }
}
