package co.edu.ucc.ojodegallo.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

private fun tienePermiso(c: Context) =
    ContextCompat.checkSelfPermission(c, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

/** Captura con CameraX y una guía en pantalla para encuadrar la hoja (RF-01, CU-01). */
@Composable
fun CamaraScreen(vm: AppViewModel, irResultado: () -> Unit, volver: () -> Unit) {
    val context = LocalContext.current
    var permiso by remember { mutableStateOf(tienePermiso(context)) }
    var negado by remember { mutableStateOf(false) }
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        permiso = ok; negado = !ok
    }
    LaunchedEffect(Unit) { if (!permiso) pedir.launch(Manifest.permission.CAMERA) }

    if (!permiso) {
        // CP-10: si se niega el permiso, la aplicación no se cierra y explica cómo continuar.
        Pantalla("Permiso de cámara", volver) {
            Parrafo(if (negado) "Sin permiso de cámara no se pueden tomar fotos. Puedes darlo ahora, activarlo en Ajustes > Aplicaciones > Gotera IA > Permisos, o elegir una foto de la galería desde el inicio."
                    else "La aplicación necesita la cámara para fotografiar la hoja.")
            Espacio(16.dp)
            BotonPrincipal("Dar permiso", Iconos.Camara) { pedir.launch(Manifest.permission.CAMERA) }
            Espacio(8.dp)
            BotonTexto("Volver al inicio", onClick = volver)
        }
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val captura = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    var error by remember { mutableStateOf<String?>(null) }
    var tomando by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { ctx ->
                    val vista = PreviewView(ctx)
                    val futuro = ProcessCameraProvider.getInstance(ctx)
                    futuro.addListener({
                        try {
                            val proveedor = futuro.get()
                            val preview = Preview.Builder().build().also { it.setSurfaceProvider(vista.surfaceProvider) }
                            proveedor.unbindAll()
                            proveedor.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, captura)
                        } catch (e: Exception) {
                            error = "No se pudo abrir la cámara. Usa la galería desde el inicio."
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    vista
                },
                modifier = Modifier.fillMaxSize(),
            )
            // Guía de encuadre (RF-01)
            Box(Modifier.fillMaxWidth(0.8f).aspectRatio(0.75f).border(2.dp, Color.White.copy(alpha = 0.9f), androidx.compose.foundation.shape.RoundedCornerShape(24.dp)))
            Text("Una sola hoja, de cerca y sin reflejos", color = Color.White,
                style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(50)).padding(horizontal = 16.dp, vertical = 8.dp))
        }
        Column(Modifier.background(Papel).padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)) {
            error?.let { Parrafo(it, negrita = true) }
            BotonPrincipal(if (tomando) "Tomando foto…" else "Tomar foto", Iconos.Camara) {
                if (tomando) return@BotonPrincipal
                tomando = true
                val archivo = vm.repo.nuevoArchivoFoto()
                captura.takePicture(ImageCapture.OutputFileOptions.Builder(archivo).build(),
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(r: ImageCapture.OutputFileResults) {
                            tomando = false; vm.analizar(archivo); irResultado()
                        }
                        override fun onError(e: ImageCaptureException) {
                            tomando = false; archivo.delete()
                            error = "No se pudo tomar la foto. Intenta de nuevo."
                        }
                    })
            }
            BotonTexto("Volver", onClick = volver)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            try { ProcessCameraProvider.getInstance(context).get().unbindAll() } catch (_: Exception) { }
        }
    }
}
