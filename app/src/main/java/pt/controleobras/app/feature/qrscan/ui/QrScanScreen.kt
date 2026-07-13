package pt.controleobras.app.feature.qrscan.ui

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import pt.controleobras.app.core.designsystem.theme.IndustrialGlow
import pt.controleobras.app.feature.receiptflow.viewmodel.ReceiptFlowViewModel
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Ecrã dedicado à leitura do QR code AT — tema industrial.
 *
 * Câmara imersiva a ecrã completo com cantos laranja de enquadramento.
 * Deteção automática: assim que um QR code AT é reconhecido, navega de volta.
 */
@Composable
fun QrScanScreen(
    viewModel: ReceiptFlowViewModel,
    onQrDetectado: () -> Unit,
    onVoltar: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val jaDetectou = remember { AtomicBoolean(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ─── Pré-visualização da câmara ───────────────────────────────────────
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory  = { ctx ->
                val previewView = PreviewView(ctx)
                val executor = Executors.newSingleThreadExecutor()

                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
                val scanner = BarcodeScanning.getClient(options)

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    imageAnalysis.setAnalyzer(executor) { imageProxy: ImageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null && !jaDetectou.get()) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    val qr = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                                    if (qr != null && jaDetectou.compareAndSet(false, true)) {
                                        viewModel.processarQrEscaneado(qr.rawValue!!)
                                        ContextCompat.getMainExecutor(ctx).execute { onQrDetectado() }
                                    }
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        } else {
                            imageProxy.close()
                        }
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            }
        )

        // ─── Cantos laranja de enquadramento QR ───────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    val cornerLen = 56.dp.toPx()
                    val stroke    = 3.5f.dp.toPx()
                    val cx        = size.width / 2f
                    val cy        = size.height / 2f
                    val half      = minOf(size.width, size.height) * 0.35f
                    val left      = cx - half
                    val top       = cy - half
                    val right     = cx + half
                    val bottom    = cy + half
                    val c         = IndustrialGlow

                    drawLine(c, Offset(left, top + cornerLen), Offset(left, top), strokeWidth = stroke)
                    drawLine(c, Offset(left, top), Offset(left + cornerLen, top), strokeWidth = stroke)
                    drawLine(c, Offset(right - cornerLen, top), Offset(right, top), strokeWidth = stroke)
                    drawLine(c, Offset(right, top), Offset(right, top + cornerLen), strokeWidth = stroke)
                    drawLine(c, Offset(left, bottom - cornerLen), Offset(left, bottom), strokeWidth = stroke)
                    drawLine(c, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeWidth = stroke)
                    drawLine(c, Offset(right - cornerLen, bottom), Offset(right, bottom), strokeWidth = stroke)
                    drawLine(c, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeWidth = stroke)
                }
        )

        // ─── Painel de instrução na base — industrial ─────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xCC161C22), Color(0xFF0F1318))
                    )
                )
                .drawWithContent {
                    drawContent()
                    drawLine(
                        color       = IndustrialGlow,
                        start       = Offset(0f, 0f),
                        end         = Offset(size.width, 0f),
                        strokeWidth = 2.dp.toPx()
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint               = IndustrialGlow,
                    modifier           = Modifier.size(36.dp)
                )
                Text(
                    text       = "Aponte para o QR code AT da fatura",
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White,
                    textAlign  = TextAlign.Center
                )
                Text(
                    text      = "Detetado automaticamente — não precisa de tocar no ecrã",
                    style     = MaterialTheme.typography.bodySmall,
                    color     = Color.White.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
