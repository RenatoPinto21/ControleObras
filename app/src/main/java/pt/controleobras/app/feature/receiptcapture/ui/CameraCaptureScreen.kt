package pt.controleobras.app.feature.receiptcapture.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import pt.controleobras.app.feature.receiptflow.viewmodel.ReceiptFlowViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ecrã de captura: pré-visualização da câmara (CameraX) com opção de
 * fotografar ou escolher uma imagem da galeria (Photo Picker).
 */
@Composable
fun CameraCaptureScreen(
    viewModel: ReceiptFlowViewModel,
    onImagemProcessada: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    var temPermissaoCamara by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissaoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedida -> temPermissaoCamara = concedida }

    LaunchedEffect(Unit) {
        if (!temPermissaoCamara) permissaoLauncher.launch(Manifest.permission.CAMERA)
    }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        onDispose { cameraProvider?.unbindAll() }
    }

    val galeriaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val destino = copiarParaArmazenamentoInterno(context, uri)
            val uriDestino = obterUriPublica(context, destino)
            viewModel.processarImagem(context, uriDestino, destino.absolutePath)
        }
    }

    LaunchedEffect(uiState.draft) {
        if (uiState.draft != null) onImagemProcessada()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (temPermissaoCamara) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val provider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val captura = ImageCapture.Builder().build()
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            captura
                        )
                        imageCapture = captura
                        cameraProvider = provider
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                }
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("É necessária permissão da câmara para fotografar talões.")
            }
        }

        if (temPermissaoCamara) {
            Text(
                text = "Aponta a câmara ao talão, mantém-no bem enquadrado e nítido, " +
                    "e toca em \"Fotografar talão\". Também podes escolher uma foto já tirada.",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(12.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isProcessing) {
                CircularProgressIndicator()
                Text("A processar imagem…")
            } else {
                Button(
                    enabled = temPermissaoCamara,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val captura = imageCapture ?: return@Button
                        val ficheiro = criarFicheiroImagem(context)
                        val opcoes = ImageCapture.OutputFileOptions.Builder(ficheiro).build()
                        captura.takePicture(
                            opcoes,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    val uri = obterUriPublica(context, ficheiro)
                                    viewModel.processarImagem(context, uri, ficheiro.absolutePath)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Toast.makeText(context, exception.message, Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    }
                ) {
                    Text("Fotografar talão")
                }
                Button(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    onClick = {
                        galeriaLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Text("Escolher da galeria")
                }
            }
            uiState.errorMessage?.let { erro -> Text(erro) }
        }
    }
}

private fun criarFicheiroImagem(context: Context): File {
    val pasta = File(context.filesDir, "receipts").apply { mkdirs() }
    val nome = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    return File(pasta, "talao_$nome.jpg")
}

private fun copiarParaArmazenamentoInterno(context: Context, origem: Uri): File {
    val destino = criarFicheiroImagem(context)
    context.contentResolver.openInputStream(origem)?.use { input ->
        destino.outputStream().use { output -> input.copyTo(output) }
    }
    return destino
}

private fun obterUriPublica(context: Context, ficheiro: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", ficheiro)
