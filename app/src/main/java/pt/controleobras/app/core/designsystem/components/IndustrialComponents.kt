package pt.controleobras.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pt.controleobras.app.core.designsystem.theme.IndustrialBorder
import pt.controleobras.app.core.designsystem.theme.IndustrialGlow
import pt.controleobras.app.core.designsystem.theme.IndustrialGlowDim
import pt.controleobras.app.core.designsystem.theme.IndustrialSteel
import pt.controleobras.app.core.designsystem.theme.IndustrialSurface2

/**
 * Cabeçalho padrão dos ecrãs com tema industrial.
 *
 * Fundo escuro com linha laranja na base e gradiente subtil.
 * Suporta ícone opcional à esquerda do título.
 */
@Composable
fun IndustrialHeader(
    titulo: String,
    subtitulo: String? = null,
    icone: ImageVector? = null,
    modifier: Modifier = Modifier,
    acoes: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF111820))
            .drawBehind {
                drawLine(
                    color       = IndustrialGlow,
                    start       = Offset(0f, size.height),
                    end         = Offset(size.width, size.height),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icone != null) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(IndustrialGlowDim),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icone,
                    contentDescription = null,
                    tint               = IndustrialGlow,
                    modifier           = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = titulo,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color      = Color.White
            )
            if (subtitulo != null) {
                Text(
                    text  = subtitulo,
                    style = MaterialTheme.typography.labelSmall,
                    color = IndustrialSteel
                )
            }
        }
        acoes?.invoke()
    }
}

/**
 * Card industrial com borda lateral laranja e fundo escuro.
 * Usado para secções de dados, banners de estado, etc.
 */
@Composable
fun IndustrialCard(
    modifier: Modifier = Modifier,
    accent: Color = IndustrialGlow,
    mostrarBordaLateral: Boolean = true,
    conteudo: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(IndustrialSurface2)
    ) {
        if (mostrarBordaLateral) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(1000.dp)   // estende-se com o conteúdo
                    .background(accent)
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            conteudo()
        }
    }
}

/**
 * Linha de separação temática — traço de obra (laranja ténue).
 */
@Composable
fun IndustrialDivider(
    modifier: Modifier = Modifier,
    cor: Color = IndustrialBorder
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(cor)
    )
}

/**
 * Título de secção com traço laranja lateral — igual ao padrão existente no ReceiptReview,
 * agora centralizado aqui para reutilização.
 */
@Composable
fun SecaoTituloIndustrial(
    titulo: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(IndustrialGlow)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text       = titulo,
            style      = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color      = IndustrialSteel
        )
    }
}

