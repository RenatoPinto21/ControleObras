package pt.controleobras.app.feature.receiptdetail.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pt.controleobras.app.core.export.partilharExportacao
import pt.controleobras.app.feature.receiptdetail.viewmodel.ReceiptDetailViewModel
import java.io.File

/**
 * Ecrã de detalhe de um talão guardado.
 * Mostra a imagem original da fatura no topo, seguida de todos os campos,
 * e permite exportar/partilhar os ficheiros XML e CSV gerados ao guardar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetailScreen(
    onVoltar: () -> Unit,
    viewModel: ReceiptDetailViewModel = hiltViewModel()
) {
    val talao by viewModel.talao.collectAsState()
    val context = LocalContext.current
    var mostrarTextoOcr by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhe da fatura") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        val talaoAtual = talao
        if (talaoAtual == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    ImagemFatura(imagemPath = talaoAtual.imagemPath)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item { CampoDetalhe("Empresa", talaoAtual.empresa) }
                item { CampoDetalhe("NIF", talaoAtual.nif) }
                item { CampoDetalhe("Morada", talaoAtual.morada) }
                item { CampoDetalhe("Data", talaoAtual.data?.toString()) }
                item { CampoDetalhe("Hora", talaoAtual.hora?.toString()) }
                item { CampoDetalhe("Número da fatura", talaoAtual.numeroFatura) }
                item {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Produtos", style = MaterialTheme.typography.titleMedium)
                }
                items(talaoAtual.itens) { item ->
                    Column {
                        Text(item.descricao, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Qtd: ${item.quantidade.toPlainString()}  ·  " +
                                "Preço: ${item.precoUnitario.toPlainString()} €  ·  " +
                                "Total: ${item.total.toPlainString()} €",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                item {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(4.dp))
                }
                item { CampoDetalhe("IVA", talaoAtual.iva?.toPlainString()) }
                item { CampoDetalhe("Total", talaoAtual.total?.toPlainString()) }
                item { CampoDetalhe("Observações", talaoAtual.observacoes) }
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = { partilharExportacao(context, talaoAtual.id, "xml") }
                        ) {
                            Text("Exportar XML")
                        }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = { partilharExportacao(context, talaoAtual.id, "csv") }
                        ) {
                            Text("Exportar CSV")
                        }
                    }
                }
                item {
                    HorizontalDivider()
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { mostrarTextoOcr = !mostrarTextoOcr }
                    ) {
                        Text(
                            if (mostrarTextoOcr) "Ocultar texto reconhecido (OCR)"
                            else "Ver texto reconhecido (OCR)"
                        )
                    }
                    if (mostrarTextoOcr) {
                        Text(
                            text = talaoAtual.textoOcr?.ifBlank { null } ?: "(sem texto reconhecido)",
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Componentes privados
// ---------------------------------------------------------------------------

/**
 * Carrega e mostra a imagem da fatura sem dependências externas.
 * Suporta path de ficheiro local e content URI.
 */
@Composable
private fun ImagemFatura(imagemPath: String) {
    val context = LocalContext.current

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

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Imagem da fatura",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp, max = 340.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    } else {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}

@Composable
private fun CampoDetalhe(rotulo: String, valor: String?) {
    if (!valor.isNullOrBlank()) {
        Column {
            Text(rotulo, style = MaterialTheme.typography.labelMedium)
            Text(valor, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
