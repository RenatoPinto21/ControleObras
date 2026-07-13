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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
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

    // Navega para revisão assim que imagem estiver guardada — processamento continua em background
    LaunchedEffect(uiState.imagemCapturadaPath) {
        if (uiState.imagemCapturadaPath != null) onImagemProcessada()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ─── Pré-visualização da câmara ────────────────────────────────────────
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
            // Sem permissão
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "É necessária permissão da câmara para fotografar talões.",
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // ─── Instrução no topo ─────────────────────────────────────────────────
        if (temPermissaoCamara && !uiState.isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.55f))
            ) {
                Text(
                    text = "Enquadre o talão completo e mantenha-o estável",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }

        // ─── Painel de controlos na base ───────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color.Black.copy(alpha = 0.75f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = uiState.statusProcessamento.ifBlank { "A processar..." },
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botão galeria (esquerda, menor)
                        FilledTonalIconButton(
                            onClick = {
                                galeriaLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.size(52.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.White.copy(alpha = 0.15f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Escolher da galeria",
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Botão de captura (circular, destaque — cor primária laranja)
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(
                                    if (temPermissaoCamara) MaterialTheme.colorScheme.primary
                                    else Color.Gray
                                )
                                .clickable(enabled = temPermissaoCamara) {
                                    val captura = imageCapture ?: return@clickable
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
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Fotografar talão",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Espaço simétrico
                        Spacer(Modifier.size(52.dp))
                    }

                    Text(
                        text = "Toque para fotografar  ·  Galeria à esquerda",
                        color = Color.White.copy(alpha = 0.55f),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                }

                uiState.errorMessage?.let { erro ->
                    Text(
                        text = erro,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
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
