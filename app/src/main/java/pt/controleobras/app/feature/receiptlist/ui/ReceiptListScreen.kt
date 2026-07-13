package pt.controleobras.app.feature.receiptlist.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pt.controleobras.app.core.designsystem.components.IndustrialHeader
import pt.controleobras.app.core.designsystem.theme.IndustrialBorder
import pt.controleobras.app.core.designsystem.theme.IndustrialGlow
import pt.controleobras.app.core.designsystem.theme.IndustrialGlowDim
import pt.controleobras.app.core.designsystem.theme.IndustrialSteel
import pt.controleobras.app.core.designsystem.theme.IndustrialSurface2
import pt.controleobras.app.core.export.partilharExportacao
import pt.controleobras.app.core.model.Talao
import pt.controleobras.app.feature.receiptlist.viewmodel.ReceiptListViewModel

@Composable
fun ReceiptListScreen(
    onAbrirTalao: (Long) -> Unit = {},
    viewModel: ReceiptListViewModel = hiltViewModel()
) {
    val talaes by viewModel.talaes.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        IndustrialHeader(
            titulo    = "Registo de Faturas",
            subtitulo = "${talaes.size} talão(ões) guardado(s)",
            icone     = Icons.Default.History
        )

        if (talaes.isEmpty()) {
            Box(
                modifier          = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment  = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(IndustrialGlowDim),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint     = IndustrialGlow,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Sem registos",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                    Text(
                        "Volta ao início e digitaliza o primeiro talão.",
                        style = MaterialTheme.typography.bodySmall,
                        color = IndustrialSteel
                    )
                }
            }
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding      = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
            ) {
                items(talaes, key = { it.id }) { talao ->
                    TalaoCard(talao = talao, onAbrir = { onAbrirTalao(talao.id) })
                }
            }
        }
    }
}

@Composable
private fun TalaoCard(talao: Talao, onAbrir: () -> Unit) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(IndustrialSurface2)
            .drawBehind {
                // Borda esquerda laranja
                drawLine(
                    color       = IndustrialGlow,
                    start       = Offset(0f, 0f),
                    end         = Offset(0f, size.height),
                    strokeWidth = 3.dp.toPx()
                )
            }
            .clickable(onClick = onAbrir)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ícone de empresa
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(IndustrialGlowDim),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text      = talao.empresa.take(1).uppercase(),
                style     = MaterialTheme.typography.titleMedium,
                color     = IndustrialGlow,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(12.dp))

        // Dados principais
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text      = talao.empresa.ifBlank { "Empresa desconhecida" },
                style     = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color     = Color.White
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                talao.data?.let {
                    Text(it.toString(), style = MaterialTheme.typography.bodySmall, color = IndustrialSteel)
                }
                talao.total?.let {
                    Text(
                        "${it.toPlainString()} €",
                        style      = MaterialTheme.typography.bodySmall,
                        color      = IndustrialGlow,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Ação de exportar + navegar
        IconButton(onClick = { partilharExportacao(context, talao.id, "xml") }) {
            Icon(
                Icons.Default.IosShare,
                contentDescription = "Exportar",
                tint     = IndustrialSteel,
                modifier = Modifier.size(18.dp)
            )
        }
        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint     = IndustrialBorder,
            modifier = Modifier.size(20.dp)
        )
    }
}
