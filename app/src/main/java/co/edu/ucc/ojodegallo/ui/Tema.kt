package co.edu.ucc.ojodegallo.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import co.edu.ucc.ojodegallo.R

/*
 * Identidad visual de Gotera.
 * Se usa a pleno sol en el cafetal, por eso el tema es claro, de alto contraste y sin modo oscuro (RNF-05).
 * Colores tomados del propio cultivo: verde de la hoja, papel crema, terracota de la lesión y ocre de la roya.
 */
val Papel = Color(0xFFF3F0E6)          // fondo
val PapelClaro = Color(0xFFFBF9F2)     // superficies
val Linea = Color(0xFFD8D1BF)          // bordes y divisores
val Tinta = Color(0xFF1C2420)          // texto principal
val TintaSuave = Color(0xFF545B55)     // texto secundario
val VerdeHoja = Color(0xFF24452F)      // color de marca y acciones
val VerdeSano = Color(0xFF2E6B3E)      // resultado sin síntomas
val VerdeSanoFondo = Color(0xFFE1ECDD)
val Terracota = Color(0xFFA63A2B)      // síntomas compatibles con ojo de gallo
val TerracotaFondo = Color(0xFFF7E1DB)
val Ocre = Color(0xFF7D530F)           // no concluyente, imagen no válida, roya
val OcreFondo = Color(0xFFF4E8CC)

private val Atkinson = FontFamily(
    Font(R.font.atkinson_regular, FontWeight.Normal),
    Font(R.font.atkinson_bold, FontWeight.Bold),
)
private val Fraunces = FontFamily(Font(R.font.fraunces_semibold, FontWeight.SemiBold))

/** Tipografía del nombre de la aplicación. */
val FuenteMarca = FontFamily(Font(R.font.fraunces_marca, FontWeight.Medium))

private val Tipografia = Typography(
    displaySmall = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 40.sp),
    headlineSmall = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 25.sp, lineHeight = 31.sp),
    titleLarge = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp),
    labelMedium = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp),
)

private val Esquema: ColorScheme = lightColorScheme(
    primary = VerdeHoja, onPrimary = PapelClaro,
    primaryContainer = VerdeSanoFondo, onPrimaryContainer = VerdeHoja,
    secondary = Ocre, onSecondary = PapelClaro,
    secondaryContainer = OcreFondo, onSecondaryContainer = Ocre,
    tertiary = Terracota, onTertiary = PapelClaro,
    background = Papel, onBackground = Tinta,
    surface = Papel, onSurface = Tinta,
    surfaceVariant = PapelClaro, onSurfaceVariant = TintaSuave,
    surfaceTint = Papel,
    surfaceBright = PapelClaro, surfaceDim = Papel,
    surfaceContainerLowest = PapelClaro, surfaceContainerLow = PapelClaro,
    surfaceContainer = PapelClaro, surfaceContainerHigh = PapelClaro, surfaceContainerHighest = PapelClaro,
    outline = Linea, outlineVariant = Linea,
    error = Terracota, onError = PapelClaro,
)

@Composable
fun Tema(contenido: @Composable () -> Unit) = MaterialTheme(colorScheme = Esquema, typography = Tipografia) {
    CompositionLocalProvider(LocalContentColor provides Tinta, content = contenido)
}
