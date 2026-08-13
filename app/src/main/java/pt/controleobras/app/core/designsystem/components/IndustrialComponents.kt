package pt.controleobras.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pt.controleobras.app.core.designsystem.theme.IndustrialBorder
import pt.controleobras.app.core.designsystem.theme.IndustrialDeep
import pt.controleobras.app.core.designsystem.theme.IndustrialGlow
import pt.controleobras.app.core.designsystem.theme.IndustrialGlowDim
import pt.controleobras.app.core.designsystem.theme.IndustrialSteel
import pt.controleobras.app.core.designsystem.theme.IndustrialSteelLight
import pt.controleobras.app.core.designsystem.theme.IndustrialSurface2
import pt.controleobras.app.core.designsystem.theme.IndustrialSurface3

/**
 * Cabeçalho padrão dos ecrãs com tema industrial.
 *
 * Fundo IndustrialDeep com linha laranja na base.
 * Traço vertical laranja + título + subtítulo + ações opcionais.
 */
@Composable
fun IndustrialHeader(
    titulo:    String,
    subtitulo: String?   = null,
    icone:     ImageVector? = null,
    modifier:  Modifier  = Modifier,
    acoes:     @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(IndustrialDeep)
            .drawBehind {
                // Linha laranja base
                drawLine(
                    color       = IndustrialGlow,
                    start       = Offset(0f, size.height),
                    end         = Offset(size.width, size.height),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp)
    ) {
        // Traço laranja vertical — marca de secção
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(if (subtitulo != null) 32.dp else 22.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(IndustrialGlow)
        )

        if (icone != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(IndustrialGlowDim)
                    .border(1.dp, IndustrialGlow.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icone,
                    contentDescription = null,
                    tint               = IndustrialGlow,
                    modifier           = Modifier.size(18.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = titulo,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
            if (subtitulo != null) {
                Text(
                    text  = subtitulo,
                    style = MaterialTheme.typography.labelSmall,
                    color = IndustrialSteelLight
                )
            }
        }
        acoes?.invoke()
    }
}

/**
 * Card industrial com borda lateral colorida e fundo escuro.
 * Usa IntrinsicSize.Min para que a borda acompanhe a altura real do conteúdo.
 */
@Composable
fun IndustrialCard(
    modifier:            Modifier = Modifier,
    accent:              Color    = IndustrialGlow,
    espessuraBorda:      Dp      = 3.dp,
    mostrarBordaLateral: Boolean  = true,
    conteudo: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)          // ← fix: acompanha altura real
            .clip(RoundedCornerShape(16.dp))
            .background(IndustrialSurface2)
            .border(1.dp, IndustrialBorder, RoundedCornerShape(16.dp))
    ) {
        if (mostrarBordaLateral) {
            Box(
                modifier = Modifier
                    .width(espessuraBorda)
                    .fillMaxHeight()            // ← preenche exatamente o card
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(accent, accent.copy(alpha = 0.6f))
                        )
                    )
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
 * Título de secção com traço laranja lateral.
 * Aceita uma ação opcional à direita (ex: botão "ver tudo").
 */
@Composable
fun SecaoTituloIndustrial(
    titulo:  String,
    modifier: Modifier = Modifier,
    acao: @Composable (() -> Unit)? = null
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(width = 2.dp, height = 14.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(IndustrialGlow)
        )
        Text(
            text       = titulo.uppercase(),
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color      = IndustrialSteel,
            modifier   = Modifier.weight(1f)
        )
        acao?.invoke()
    }
}

/**
 * Chip de estatística — número grande + label pequena.
 * Ex: "12 faturas", "€ 345.00".
 */
@Composable
fun IndustrialStatChip(
    valor:    String,
    label:    String,
    corValor: Color   = Color.White,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(IndustrialSurface3)
            .border(1.dp, IndustrialBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text       = valor,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color      = corValor
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = IndustrialSteel
        )
    }
}

/**
 * Tag inline — pequeno chip de texto com fundo semitransparente.
 * Ex: "FREF", "NIF", etiquetas de campo.
 */
@Composable
fun IndustrialTag(
    texto:     String,
    cor:       Color   = IndustrialGlow,
    modifier:  Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(cor.copy(alpha = 0.12f))
            .border(1.dp, cor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text       = texto,
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color      = cor
        )
    }
}

/**
 * Ponto de estado circular com label — reutilizável em cards de serviço.
 */
@Composable
fun PontoEstado(
    ativo:    Boolean,
    label:    String,
    modifier: Modifier = Modifier
) {
    val cor = if (ativo) IndustrialGlow else IndustrialSteel
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(cor)
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = cor
        )
    }
}

