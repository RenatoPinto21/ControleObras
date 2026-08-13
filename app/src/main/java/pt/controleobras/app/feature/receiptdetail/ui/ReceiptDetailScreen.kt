package pt.controleobras.app.feature.receiptdetail.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pt.controleobras.app.core.designsystem.components.IndustrialCard
import pt.controleobras.app.core.designsystem.components.IndustrialDivider
import pt.controleobras.app.core.designsystem.components.IndustrialHeader
import pt.controleobras.app.core.designsystem.components.SecaoTituloIndustrial
import pt.controleobras.app.core.designsystem.theme.IndustrialBorder
import pt.controleobras.app.core.designsystem.theme.IndustrialGlow
import pt.controleobras.app.core.designsystem.theme.IndustrialGlowDim
import pt.controleobras.app.core.designsystem.theme.IndustrialSteel
import pt.controleobras.app.core.designsystem.theme.IndustrialSurface
import pt.controleobras.app.core.designsystem.theme.IndustrialSurface2
import pt.controleobras.app.core.export.partilharExportacao
import pt.controleobras.app.feature.receiptdetail.viewmodel.ReceiptDetailViewModel
import java.io.File

/**
 * Ecrã de detalhe de um talão guardado — tema industrial.
 * Mostra a imagem original, todos os campos extraídos pelo OCR,
 * e permite exportar XML/CSV.
 */
@Composable
fun ReceiptDetailScreen(
    onVoltar: () -> Unit,
    viewModel: ReceiptDetailViewModel = hiltViewModel()
) {
    val talao by viewModel.talao.collectAsState()
    val context = LocalContext.current
    var mostrarTextoOcr by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        IndustrialHeader(
            titulo    = "Detalhe da Fatura",
            subtitulo = talao?.empresa?.ifBlank { null },
            icone     = Icons.Default.Description
        )

        val talaoAtual = talao
        if (talaoAtual == null) {
            Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = IndustrialGlow)
            }
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding      = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
            ) {
                // Imagem do talão
                item {
                    ImagemFatura(imagemPath = talaoAtual.imagemPath)
                }

                // Dados da empresa
                item {
                    SecaoTituloIndustrial("Fornecedor")
                    Spacer(Modifier.height(8.dp))
                    IndustrialCard {
                        Column(
                            modifier            = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CampoDetalhe("Empresa",  talaoAtual.empresa)
                            CampoDetalhe("NIF",      talaoAtual.nif)
                            CampoDetalhe("Morada",   talaoAtual.morada)
                        }
                    }
                }

                // Dados da fatura
                item {
                    SecaoTituloIndustrial("Documento")
                    Spacer(Modifier.height(8.dp))
                    IndustrialCard {
                        Column(
                            modifier            = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CampoDetalhe("Nº Fatura", talaoAtual.numeroFatura)
                            CampoDetalhe("Data",      talaoAtual.data?.toString())
                            CampoDetalhe("Hora",      talaoAtual.hora?.toString())
                        }
                    }
                }

                // Produtos
                if (talaoAtual.itens.isNotEmpty()) {
                    item {
                        SecaoTituloIndustrial("Artigos")
                        Spacer(Modifier.height(8.dp))
                    }
                    items(talaoAtual.itens) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(IndustrialSurface2)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.descricao,
                                    style     = MaterialTheme.typography.bodyMedium,
                                    color     = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Qtd: ${item.quantidade.toPlainString()}  ·  " +
                                    "P.Unit: ${item.precoUnitario.toPlainString()} €",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = IndustrialSteel
                                )
                            }
                            Text(
                                "${item.total.toPlainString()} €",
                                style      = MaterialTheme.typography.bodyMedium,
                                color      = IndustrialGlow,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Totais
                item {
                    SecaoTituloIndustrial("Totais")
                    Spacer(Modifier.height(8.dp))
                    IndustrialCard {
                        Column(
                            modifier            = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CampoDetalhe("IVA", talaoAtual.iva?.toPlainString()?.let { "$it €" })
                            // Total em destaque
                            if (talaoAtual.total != null) {
                                IndustrialDivider()
                                Row(
                                    modifier          = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "TOTAL",
                                        style      = MaterialTheme.typography.labelLarge,
                                        color      = IndustrialSteel,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${talaoAtual.total!!.toPlainString()} €",
                                        style      = MaterialTheme.typography.titleLarge,
                                        color      = IndustrialGlow,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            CampoDetalhe("Observações", talaoAtual.observacoes)
                        }
                    }
                }

                // Exportar
                item {
                    SecaoTituloIndustrial("Exportar")
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick  = { partilharExportacao(context, talaoAtual.id, "xml") },
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = IndustrialGlow,
                                contentColor   = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.IosShare, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("XML", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick  = { partilharExportacao(context, talaoAtual.id, "csv") },
                            shape    = RoundedCornerShape(8.dp),
                            border   = androidx.compose.foundation.BorderStroke(1.5.dp, IndustrialBorder)
                        ) {
                            Icon(Icons.Default.IosShare, null, modifier = Modifier.size(16.dp), tint = IndustrialSteel)
                            Spacer(Modifier.width(6.dp))
                            Text("CSV", color = IndustrialSteel, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Texto OCR (colapsável)
                item {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick  = { mostrarTextoOcr = !mostrarTextoOcr },
                        shape    = RoundedCornerShape(8.dp),
                        border   = androidx.compose.foundation.BorderStroke(1.dp, IndustrialBorder)
                    ) {
                        Icon(
                            if (mostrarTextoOcr) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null,
                            tint     = IndustrialSteel,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (mostrarTextoOcr) "Ocultar texto OCR" else "Ver texto OCR",
                            color = IndustrialSteel
                        )
                    }
                    AnimatedVisibility(
                        visible = mostrarTextoOcr,
                        enter   = expandVertically() + fadeIn(),
                        exit    = shrinkVertically() + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(IndustrialSurface2)
                                .padding(12.dp)
                        ) {
                            Text(
                                text  = talaoAtual.textoOcr?.ifBlank { null } ?: "(sem texto reconhecido)",
                                style = MaterialTheme.typography.bodySmall,
                                color = IndustrialSteel
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Componentes privados
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ImagemFatura(imagemPath: String) {
    val context = LocalContext.current
    var mostrarFullscreen by remember { mutableStateOf(false) }

    val bitmap = remember(imagemPath) {
        runCatching {
            if (imagemPath.startsWith("content://")) {
                context.contentResolver.openInputStream(Uri.parse(imagemPath))
                    ?.use { BitmapFactory.decodeStream(it) }
            } else {
                BitmapFactory.decodeFile(imagemPath)
            }
        }.getOrNull()
    }

    // Visualizador fullscreen com pinch-to-zoom
    if (mostrarFullscreen && bitmap != null) {
        pt.controleobras.app.core.designsystem.components.FullscreenImageViewer(
            bitmap   = bitmap,
            onFechar = { mostrarFullscreen = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp, max = 320.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(IndustrialSurface2)
            .clickable(enabled = bitmap != null) { mostrarFullscreen = true }
            .drawBehind {
                // Linha laranja inferior — imersão industrial
                drawLine(
                    color       = IndustrialGlow,
                    start       = Offset(0f, size.height),
                    end         = Offset(size.width, size.height),
                    strokeWidth = 2.dp.toPx()
                )
            }
    ) {
        if (bitmap != null) {
            Image(
                bitmap             = bitmap.asImageBitmap(),
                contentDescription = "Imagem da fatura — toque para ampliar",
                contentScale       = ContentScale.Fit,
                modifier           = Modifier.fillMaxSize()
            )
            // Indicador de zoom no canto inferior direito
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text  = "Toque para ampliar",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        } else {
            Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        tint     = IndustrialGlowDim,
                        modifier = Modifier.size(40.dp)
                    )
                    Text("Imagem não disponível", color = IndustrialSteel, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun CampoDetalhe(rotulo: String, valor: String?) {
    if (!valor.isNullOrBlank()) {
        Column {
            Text(
                text  = rotulo.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = IndustrialSteel
            )
            Text(
                text      = valor,
                style     = MaterialTheme.typography.bodyMedium,
                color     = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
