package pt.controleobras.app.feature.receiptreview.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pt.controleobras.app.core.model.ItemTalaoDraft
import pt.controleobras.app.feature.receiptflow.viewmodel.ReceiptFlowViewModel

/**
 * Ecrã de revisão pós-OCR.
 *
 * O utilizador NÃO escreve nada — a app extrai automaticamente toda a informação.
 * Campos não encontrados mostram um aviso visual a indicar que deve verificar na imagem.
 * A imagem original da fatura é apresentada no topo para consulta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptReviewScreen(
    viewModel: ReceiptFlowViewModel,
    onGuardado: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val draft = uiState.draft ?: return
    val snackbarHostState = remember { SnackbarHostState() }

    // Navegar quando guardado
    LaunchedEffect(uiState.savedTalaoId) {
        if (uiState.savedTalaoId != null) onGuardado()
    }

    // Toast rápido quando não há QR code (só uma vez por draft)
    LaunchedEffect(draft) {
        if (!uiState.qrDetectado) {
            snackbarHostState.showSnackbar("Sem QR code detetado na imagem")
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Dados da fatura") }) },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                ImagemFatura(imagemPath = draft.imagemPath)
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                Text(
                    text = "Informação extraída automaticamente da fatura",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            item { CampoLeitura("Empresa", draft.empresa) }
            item { CampoLeitura("NIF", draft.nif) }
            item { CampoLeitura("Morada", draft.morada) }
            item { CampoLeitura("Data", draft.data?.toString()) }
            item { CampoLeitura("Hora", draft.hora?.toString()) }
            item { CampoLeitura("Número da fatura", draft.numeroFatura) }
            item { CampoLeitura("IVA", draft.iva) }
            item { CampoLeitura("Total", draft.total) }
            item { CampoLeitura("Observações", draft.observacoes) }
            item {
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                Text("Produtos", style = MaterialTheme.typography.titleMedium)
            }
            if (draft.itens.isEmpty()) {
                item { AvisoCampoNaoEncontrado("Produtos") }
            } else {
                items(draft.itens) { item ->
                    CardProduto(item)
                }
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                uiState.errorMessage?.let { erro ->
                    Text(
                        text = erro,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.confirmarEGuardar() },
                    enabled = !uiState.isProcessing
                ) {
                    Text("Guardar fatura")
                }
                Spacer(modifier = Modifier.height(16.dp))
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
        // Fallback caso a imagem não seja acessível
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}

/**
 * Campo de leitura.
 * Se o valor estiver vazio/nulo, mostra aviso de verificação na imagem.
 */
@Composable
private fun CampoLeitura(rotulo: String, valor: String?) {
    if (!valor.isNullOrBlank()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    text = rotulo,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = valor,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    } else {
        AvisoCampoNaoEncontrado(rotulo)
    }
}

/**
 * Card de aviso para campos não encontrados automaticamente.
 */
@Composable
private fun AvisoCampoNaoEncontrado(rotulo: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFF57F17),
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = rotulo,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF795548)
                )
                Text(
                    text = "Verifique na imagem esta informação",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFF57F17),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Card de produto extraído pelo parser.
 */
@Composable
private fun CardProduto(item: ItemTalaoDraft) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                text = item.descricao.ifBlank { "(sem descrição)" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (item.quantidade.isNotBlank()) {
                    Text("Qtd: ${item.quantidade}", style = MaterialTheme.typography.bodySmall)
                }
                if (item.precoUnitario.isNotBlank()) {
                    Text("Preço: ${item.precoUnitario} €", style = MaterialTheme.typography.bodySmall)
                }
                if (item.total.isNotBlank()) {
                    Text("Total: ${item.total} €", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
