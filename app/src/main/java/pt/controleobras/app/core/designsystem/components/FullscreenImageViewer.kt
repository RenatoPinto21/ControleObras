package pt.controleobras.app.core.designsystem.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Visualizador de imagem em fullscreen com pinch-to-zoom e pan.
 *
 * Usado para inspecionar imagens de faturas/talões — permite ao
 * utilizador ampliar texto pequeno para conferir dados do OCR.
 *
 * Funcionalidades:
 *  - Pinch-to-zoom (1x a 5x)
 *  - Pan/arrastar quando ampliado
 *  - Duplo toque para repor zoom
 *  - Botão fechar no canto superior direito
 *
 * Não requer dependências externas — usa apenas APIs nativas do Compose.
 *
 * @param bitmap    Bitmap da imagem a visualizar
 * @param onFechar  Callback para fechar o visualizador
 */
@Composable
fun FullscreenImageViewer(
    bitmap: Bitmap,
    onFechar: () -> Unit
) {
    Dialog(
        onDismissRequest = onFechar,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress      = true,
            dismissOnClickOutside   = false
        )
    ) {
        var escala  by remember { mutableFloatStateOf(1f) }
        var offset  by remember { mutableStateOf(Offset.Zero) }

        val transformState = rememberTransformableState { zoomChange, panChange, _ ->
            escala = (escala * zoomChange).coerceIn(1f, 5f)
            if (escala > 1f) {
                offset += panChange
            } else {
                offset = Offset.Zero
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .pointerInput(Unit) {
                    // Duplo toque repõe zoom
                    detectTapGestures(
                        onDoubleTap = {
                            escala = 1f
                            offset = Offset.Zero
                        }
                    )
                }
        ) {
            Image(
                bitmap             = bitmap.asImageBitmap(),
                contentDescription = "Imagem da fatura ampliada",
                contentScale       = ContentScale.Fit,
                modifier           = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX       = escala
                        scaleY       = escala
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .transformable(state = transformState)
            )

            // Botão fechar — canto superior direito
            IconButton(
                onClick  = onFechar,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector        = Icons.Default.Close,
                    contentDescription = "Fechar visualizador",
                    tint               = Color.White,
                    modifier           = Modifier.size(24.dp)
                )
            }
        }
    }
}
