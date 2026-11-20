package co.edu.ucc.ojodegallo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import co.edu.ucc.ojodegallo.ui.AppViewModel
import co.edu.ucc.ojodegallo.ui.CamaraScreen
import co.edu.ucc.ojodegallo.ui.DetalleScreen
import co.edu.ucc.ojodegallo.ui.GuiaScreen
import co.edu.ucc.ojodegallo.ui.HistorialScreen
import co.edu.ucc.ojodegallo.ui.InfoScreen
import co.edu.ucc.ojodegallo.ui.InicioScreen
import co.edu.ucc.ojodegallo.ui.Papel
import co.edu.ucc.ojodegallo.ui.PruebaScreen
import co.edu.ucc.ojodegallo.ui.ResultadoScreen
import co.edu.ucc.ojodegallo.ui.Tema

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Barras del sistema del mismo color del fondo, con iconos oscuros (tema claro para usar a pleno sol).
        val barras = SystemBarStyle.light(Papel.toArgb(), Papel.toArgb())
        enableEdgeToEdge(statusBarStyle = barras, navigationBarStyle = barras)
        super.onCreate(savedInstanceState)
        setContent {
            Tema {
                Surface(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), color = Papel) {
                    val nav = rememberNavController()
                    fun inicio() = nav.popBackStack("inicio", inclusive = false)
                    fun atras() = nav.popBackStack()
                    NavHost(nav, startDestination = "inicio") {
                        composable("inicio") {
                            InicioScreen(vm,
                                irCamara = { nav.navigate("camara") },
                                irResultado = { nav.navigate("resultado") },
                                irHistorial = { nav.navigate("historial") },
                                irGuia = { nav.navigate("guia") },
                                irInfo = { nav.navigate("info") },
                                irPrueba = { nav.navigate("prueba") },
                                abrirDetalle = { id -> nav.navigate("detalle/$id") })
                        }
                        composable("camara") {
                            CamaraScreen(vm,
                                irResultado = { nav.navigate("resultado") { popUpTo("inicio") } },
                                volver = { inicio() })
                        }
                        composable("resultado") {
                            ResultadoScreen(vm,
                                otraFoto = { nav.navigate("camara") { popUpTo("inicio") } },
                                inicio = { inicio() })
                        }
                        composable("historial") {
                            HistorialScreen(vm, volver = { atras() }) { id -> nav.navigate("detalle/$id") }
                        }
                        composable("detalle/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                            DetalleScreen(vm, e.arguments?.getLong("id") ?: 0L) { atras() }
                        }
                        composable("guia") { GuiaScreen(volver = { atras() }) }
                        composable("info") { InfoScreen(volver = { atras() }) }
                        composable("prueba") { PruebaScreen(vm, volver = { atras() }) }
                    }
                }
            }
        }
    }
}
