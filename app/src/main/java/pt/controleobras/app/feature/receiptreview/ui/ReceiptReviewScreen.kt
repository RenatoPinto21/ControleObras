package pt.controleobras.app.feature.receiptreview.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import pt.controleobras.app.core.designsystem.components.IndustrialHeader
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pt.controleobras.app.core.model.ItemTalaoDraft
import pt.controleobras.app.core.validation.FieldState
import pt.controleobras.app.core.validation.FieldValidation
import pt.controleobras.app.feature.receiptflow.viewmodel.ReceiptFlowViewModel

/**
 * Ecrã de revisão pós-OCR.
 *
 * A imagem da fatura é apresentada IMEDIATAMENTE ao navegar para este ecrã.
 * O processamento (OCR + extração + validação) corre em background.
 * Quando termina, cada campo recebe um indicador visual:
 *
 *   ● VALID   (verde)   — valor encontrado e validado
 *   ● SUSPECT (amarelo) — valor encontrado mas suspeito (ex: NIF com checksum errado)
 *   ● MISSING (cinzento) — campo não encontrado — utilizador deve verificar na imagem
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptReviewScreen(
    viewModel: ReceiptFlowViewModel,
    onGuardado: () -> Unit,
    onScanQr: () -> Unit,
    onVoltar: () -> Unit = {}
) {
    val context    = LocalContext.current
    val uiState    by viewModel.uiState.collectAsState()
    val draft      = uiState.draft
    val validacoes = uiState.validacoes

    // Controla visibilidade do diálogo de confirmação (NIF cliente em falta)
    var mostrarDialogoProblemas by remember { mutableStateOf(false) }

    // Controla visibilidade do diálogo de confirmação ao carregar "Voltar"
    var mostrarDialogoDescartar by remember { mutableStateOf(false) }

    // ── BackHandler — protege contra perda de dados ao carregar "Voltar" ─────
    // Num tablet em obra, o gesto de swipe-back ou o botão de hardware podem
    // ser acionados acidentalmente. Sem esta proteção, todos os dados do OCR
    // e da revisão seriam perdidos sem aviso.
    BackHandler(enabled = true) {
        mostrarDialogoDescartar = true
    }

    // Mostra o ecrã assim que a imagem estiver disponível, mesmo sem draft ainda
    val imagemPath = draft?.imagemPath ?: uiState.imagemCapturadaPath ?: return
    val snackbarHostState = remember { SnackbarHostState() }

    // Navegar quando guardado com sucesso — com feedback sensorial
    // A vibração + bip confirma ao utilizador que o talão foi guardado,
    // mesmo que esteja de luvas ou em ambiente ruidoso na obra.
    LaunchedEffect(uiState.savedTalaoId) {
        if (uiState.savedTalaoId != null) {
            pt.controleobras.app.core.common.FeedbackUtil.sucessoAoGuardar(context)
            onGuardado()
        }
    }

    // ─── Diálogo de confirmação ao carregar "Voltar" ───────────────────────────
    if (mostrarDialogoDescartar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoDescartar = false },
            title = {
                Text(
                    text       = "Descartar alterações?",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text  = "Os dados extraídos desta fatura serão perdidos. Tem a certeza que pretende sair?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogoDescartar = false
                        onVoltar()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Descartar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoDescartar = false }) {
                    Text("Continuar a editar")
                }
            }
        )
    }

    // ─── Diálogo obrigatório: NIF + valor manual quando não há QR code ─────────
    // Ao confirmar, guarda imediatamente (sem segundo clique em "Guardar fatura").
    if (uiState.mostrarDialogoNifManual) {
        DialogoNifManual(
            onConfirmar = { nif, valor ->
                viewModel.definirDadosManuaisEGuardar(context, nif, valor)
            }
        )
    }

    // ─── Diálogo: resumo de campos em falta / suspeitos ─────────────────────
    if (mostrarDialogoProblemas && draft != null) {
        DialogoResumoProblemas(
            validacoes  = validacoes,
            onGuardar   = {
                mostrarDialogoProblemas = false
                viewModel.confirmarEGuardar(context)
            },
            onCancelar  = { mostrarDialogoProblemas = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        IndustrialHeader(
            titulo    = "Dados da Fatura",
            subtitulo = if (uiState.qrDetectado) "✓ QR code AT verificado"
                        else if (draft != null)   "Apenas OCR — confirme os campos"
                        else                      "A processar...",
            icone     = Icons.Default.QrCodeScanner
        )

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
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

            // ─── Imagem — visível IMEDIATAMENTE ───────────────────────────────
            item {
                Spacer(modifier = Modifier.height(8.dp))
                ImagemFatura(imagemPath = imagemPath)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ─── Spinner durante processamento ────────────────────────────────
            if (draft == null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = uiState.statusProcessamento.ifBlank { "A processar fatura..." },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                return@LazyColumn
            }

            // ─── Legenda dos estados ──────────────────────────────────────────
            item {
                LegendaEstados(temQr = uiState.qrDetectado)
                Spacer(modifier = Modifier.height(4.dp))
            }

            // ─── Botão re-escanear QR (apenas quando QR não foi detetado) ────
            if (!uiState.qrDetectado && !uiState.isProcessing) {
                item {
                    BotaoReescanearQr(onClick = onScanQr)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // ─── Fornecedor ───────────────────────────────────────────────────
            item {
                SecaoTitulo("Fornecedor")
            }
            item { CampoValidado("Empresa",          draft.empresa,       validacoes["empresa"]) }
            item { CampoValidado("NIF do fornecedor", draft.nif,          validacoes["nif"]) }
            item { CampoValidado("Morada",            draft.morada,       validacoes["morada"]) }

            // ─── Vosso NIF ───────────────────────────────────────────────────
            item { CampoValidado("NIF do cliente (vosso)", draft.nifCliente, validacoes["nifCliente"]) }

            // ─── Documento ───────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                SecaoTitulo("Documento")
            }
            item { CampoValidado("Número de fatura",   draft.numeroFatura,       validacoes["numeroFatura"]) }
            item { CampoValidado("Data",               draft.data?.toString(),   validacoes["data"]) }
            item { CampoValidado("Hora",               draft.hora?.toString(),   validacoes["hora"]) }
            item { CampoValidado("Método de pagamento", draft.metodoPagamento,   validacoes["metodoPagamento"]) }

            // ─── Valores ─────────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                SecaoTitulo("Valores")
            }
            item { CampoValidado("IVA (€)",    draft.iva.let { if (it.isNotBlank()) "$it €" else it }, validacoes["iva"]) }
            item { CampoValidado("Total (€)",  draft.total.let { if (it.isNotBlank()) "$it €" else it }, validacoes["total"]) }

            // ─── Produtos ────────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                SecaoTitulo("Produtos")
            }
            // Aviso de soma de produtos ≠ total (quando detetado pelo validador)
            validacoes["produtos"]?.let { v ->
                if (v.state == FieldState.SUSPECT) {
                    item {
                        CampoValidado(
                            rotulo    = "Verificação de totais",
                            valor     = v.hint,
                            validacao = v
                        )
                    }
                }
            }
            if (draft.itens.isEmpty()) {
                item  {
                    CampoValidado(
                        rotulo = "Produtos",
                        valor  = "",
                        validacao = FieldValidation.missing("Nenhum produto identificado na fatura")
                    )
                }
            } else {
                items(draft.itens.mapIndexed { i, it -> Pair(i, it) }) { (idx, item) ->
                    CardProduto(
                        item = item,
                        totalValidacao   = validacoes["item_${idx}_total"],
                        taxaIvaValidacao = validacoes["item_${idx}_taxaIva"]
                    )
                }
            }

            // ─── Observações / erro / botão guardar ──────────────────────────
            item {
                Spacer(modifier = Modifier.height(8.dp))
                if (draft.observacoes.isNotBlank()) {
                    CampoValidado("Observações", draft.observacoes, null)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                uiState.errorMessage?.let { erro ->
                    Text(
                        text = erro,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                // Sem QR: bloqueado até o utilizador confirmar NIF e valor no diálogo
                val semQrSemDados = !uiState.qrDetectado && !uiState.dadosManuaisConfirmados
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (semQrSemDados)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        else
                            MaterialTheme.colorScheme.primary
                    ),
                    onClick = {
                        if (semQrSemDados) {
                            // Reabre o diálogo obrigatório
                            viewModel.reabrirDialogoNifManual()
                        } else {
                            val temProblemas = validacoes.values.any { it.state != FieldState.VALID }
                            if (temProblemas && validacoes.isNotEmpty()) {
                                mostrarDialogoProblemas = true
                            } else {
                                viewModel.confirmarEGuardar(context)
                            }
                        }
                    },
                    enabled = !uiState.isProcessing
                ) {
                    Text(
                        text = if (semQrSemDados) "Preencher NIF e Valor →" else "Guardar fatura",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        } // fim Scaffold inner
    } // fim Column
}

// ─────────────────────────────────────────────────────────────────────────────
// Componentes privados
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Imagem da fatura — carrega do path local ou content URI.
 */
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

    if (bitmap != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp, max = 340.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { mostrarFullscreen = true }
        ) {
            Image(
                bitmap             = bitmap.asImageBitmap(),
                contentDescription = "Imagem da fatura — toque para ampliar",
                contentScale       = ContentScale.Fit,
                modifier           = Modifier.fillMaxSize()
            )
            // Indicador de zoom
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
        }
    } else {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}

/**
 * Diálogo que lista todos os campos em falta (MISSING) e suspeitos (SUSPECT)
 * antes de guardar. Nenhum campo é obrigatório — o utilizador decide sempre.
 */
@Composable
private fun DialogoResumoProblemas(
    validacoes: Map<String, FieldValidation>,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit
) {
    // Nomes legíveis para cada chave de validação
    val nomesLegiveis = mapOf(
        "empresa"         to "Nome da empresa",
        "nif"             to "NIF do fornecedor",
        "nifCliente"      to "NIF do destinatário (vosso NIF)",
        "morada"          to "Morada",
        "data"            to "Data",
        "hora"            to "Hora",
        "numeroFatura"    to "Número de fatura",
        "metodoPagamento" to "Método de pagamento",
        "iva"             to "IVA",
        "total"           to "Total",
        "produtos"        to "Verificação de totais de artigos"
    )

    val emFalta   = validacoes.filter { it.value.state == FieldState.MISSING }
    val suspeitos = validacoes.filter { it.value.state == FieldState.SUSPECT }
        .filterKeys { !it.startsWith("item_") } // Não listar erros de linha individualmente

    AlertDialog(
        onDismissRequest = onCancelar,
        title = {
            Text(
                text  = "Informação incompleta",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text  = "A seguinte informação não foi encontrada ou precisa de verificação:",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (emFalta.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text  = "Não encontrado:",
                            style = MaterialTheme.typography.labelMedium,
                            color = CorMissing,
                            fontWeight = FontWeight.SemiBold
                        )
                        emFalta.forEach { (chave, _) ->
                            val nome = nomesLegiveis[chave] ?: return@forEach
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = CorMissing,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(nome, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                if (suspeitos.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text  = "Verificar na fatura:",
                            style = MaterialTheme.typography.labelMedium,
                            color = CorSuspect,
                            fontWeight = FontWeight.SemiBold
                        )
                        suspeitos.forEach { (chave, validacao) ->
                            val nome = nomesLegiveis[chave] ?: return@forEach
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = CorSuspect,
                                    modifier = Modifier.size(14.dp)
                                )
                                Column {
                                    Text(nome, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    validacao.hint?.let {
                                        Text(it, style = MaterialTheme.typography.labelSmall, color = CorSuspect)
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text  = "Pode guardar assim mesmo — pode sempre corrigir mais tarde no histórico.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onGuardar) {
                Text("Guardar mesmo assim")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Voltar a verificar")
            }
        }
    )
}

/**
 * Card de alerta + botão para ler o QR code AT.
 * Aparece apenas quando o QR não foi detetado — destaca-se visualmente
 * porque a ausência de QR reduz a confiança em todos os campos críticos.
 */
@Composable
private fun BotaoReescanearQr(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1800)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = Color(0xFFFF6D00),
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = "QR code AT não detetado",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFF6D00)
                    )
                    Text(
                        text = "Os campos críticos (NIF, Total, IVA, Data) têm confiança reduzida.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }
            }
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE65100),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "Ler QR code AT com a câmara",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Pequena legenda que explica o significado dos 3 estados visuais.
 */
@Composable
private fun LegendaEstados(temQr: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = Color(0xFF1E262F)
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendaItem(Icons.Default.CheckCircle, CorValid,   "Válido")
            LegendaItem(Icons.Default.Warning,     CorSuspect, "Verificar")
            LegendaItem(Icons.Default.Info,        CorMissing, "Não encontrado")
        }
    }
}

@Composable
private fun LegendaItem(icon: androidx.compose.ui.graphics.vector.ImageVector, cor: Color, texto: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = cor, modifier = Modifier.size(14.dp))
        Text(text = texto, style = MaterialTheme.typography.labelSmall, color = cor)
    }
}

/**
 * Título de secção com traço lateral em laranja.
 */
@Composable
private fun SecaoTitulo(titulo: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(
            text  = titulo,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Campo com indicador visual de validação.
 *
 * VALID   → fundo verde claro + ícone check
 * SUSPECT → fundo amarelo + ícone aviso + mensagem de hint
 * MISSING → fundo cinzento + ícone interrogação + mensagem
 * null    → comportamento simples (sem validação — ex: observações)
 */
@Composable
private fun CampoValidado(
    rotulo: String,
    valor: String?,
    validacao: FieldValidation?
) {
    val estado = validacao?.state
    val hint   = validacao?.hint

    val corFundo: Color
    val corIcone: Color
    val icone: androidx.compose.ui.graphics.vector.ImageVector

    when (estado) {
        FieldState.VALID -> {
            corFundo = CorValidFundo
            corIcone = CorValid
            icone    = Icons.Default.CheckCircle
        }
        FieldState.SUSPECT -> {
            corFundo = CorSuspectFundo
            corIcone = CorSuspect
            icone    = Icons.Default.Warning
        }
        FieldState.MISSING, null -> {
            corFundo = if (estado == FieldState.MISSING) CorMissingFundo
                       else MaterialTheme.colorScheme.surfaceVariant
            corIcone = CorMissing
            icone    = Icons.Default.Info
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = corFundo),
        shape    = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier  = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (estado != null) {
                Icon(
                    imageVector        = icone,
                    contentDescription = null,
                    tint               = corIcone,
                    modifier           = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = rotulo.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.45f)
                )
                when (estado) {
                    FieldState.MISSING -> Text(
                        text       = hint ?: "Verifique na imagem original",
                        style      = MaterialTheme.typography.bodyMedium,
                        color      = CorMissing,
                        fontWeight = FontWeight.Medium
                    )
                    FieldState.SUSPECT -> {
                        if (!valor.isNullOrBlank()) {
                            Text(
                                text       = valor,
                                style      = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color      = Color.White
                            )
                        }
                        if (!hint.isNullOrBlank()) {
                            Text(
                                text  = hint,
                                style = MaterialTheme.typography.bodySmall,
                                color = CorSuspect
                            )
                        }
                    }
                    else -> Text(
                        text       = valor.orEmpty().ifBlank { "—" },
                        style      = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Card de produto com indicadores de validação opcionais nas colunas numéricas.
 */
@Composable
private fun CardProduto(
    item: ItemTalaoDraft,
    totalValidacao: FieldValidation?,
    taxaIvaValidacao: FieldValidation?
) {
    val corFundo = when {
        totalValidacao?.state == FieldState.SUSPECT   -> CorSuspectFundo
        taxaIvaValidacao?.state == FieldState.SUSPECT -> CorSuspectFundo
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = corFundo),
        shape     = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text       = item.descricao.ifBlank { "(sem descrição)" },
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color      = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (item.quantidade.isNotBlank()) {
                    Text("Qtd: ${item.quantidade}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.65f))
                }
                if (item.precoUnitario.isNotBlank()) {
                    Text("Preço: ${item.precoUnitario} €", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.65f))
                }
                if (item.total.isNotBlank()) {
                    val corTotal = if (totalValidacao?.state == FieldState.SUSPECT) CorSuspect else Color(0xFFFF6D00)
                    Text(
                        text       = "Total: ${item.total} €",
                        style      = MaterialTheme.typography.bodySmall,
                        color      = corTotal,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            // Hint de validação do total da linha
            totalValidacao?.hint?.let { hint ->
                if (totalValidacao.state == FieldState.SUSPECT) {
                    Text(
                        text  = hint,
                        style = MaterialTheme.typography.labelSmall,
                        color = CorSuspect
                    )
                }
            }
            // Hint de validação da taxa IVA
            taxaIvaValidacao?.hint?.let { hint ->
                if (taxaIvaValidacao.state == FieldState.SUSPECT) {
                    Text(
                        text  = hint,
                        style = MaterialTheme.typography.labelSmall,
                        color = CorSuspect
                    )
                }
            }
        }
    }
}

/**
 * Diálogo que aparece AUTOMATICAMENTE quando o processamento termina sem QR code AT.
 *
 * Pede ao utilizador que introduza manualmente:
 *  - NIF do fornecedor (9 dígitos)
 *  - Valor total do talão
 *
 * Estes valores são guardados em colunas separadas (MNIF, MVALOR) no CSV,
 * distinguindo-os claramente dos valores extraídos por OCR.
 * O utilizador pode sempre ignorar e preencher depois no ecrã de revisão.
 */
@Composable
private fun DialogoNifManual(
    onConfirmar: (nif: String, valor: String) -> Unit
) {
    var nif   by remember { mutableStateOf("") }
    var valor by remember { mutableStateOf("") }

    // onDismissRequest vazio — impede fechar ao clicar fora ou premir VOLTAR
    AlertDialog(
        onDismissRequest = { /* obrigatório — não fecha */ },
        icon = {
            Icon(
                imageVector        = Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint               = Color(0xFFE65100),
                modifier           = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text       = "QR code não encontrado",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Explicação clara do porquê deste diálogo:
                // O QR code AT contém NIF e valor assinados digitalmente.
                // Sem ele, o OCR pode errar — pedimos confirmação manual.
                Text(
                    text  = "Esta fatura não tem QR code AT (código obrigatório " +
                            "nas faturas portuguesas desde 2022).\n\n" +
                            "Sem o QR, o NIF e o valor foram lidos por OCR e " +
                            "podem conter erros. Confirme ou corrija os valores abaixo " +
                            "para garantir um registo correto.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val tfColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor    = Color(0xFFFF6D00),
                    unfocusedBorderColor  = Color(0xFF2E3A44),
                    focusedLabelColor     = Color(0xFFFF6D00),
                    unfocusedLabelColor   = Color(0xFF8A9BAB),
                    cursorColor           = Color(0xFFFF6D00),
                    focusedTextColor      = Color.White,
                    unfocusedTextColor    = Color.White,
                    focusedContainerColor   = Color(0xFF1E262F),
                    unfocusedContainerColor = Color(0xFF1E262F)
                )
                OutlinedTextField(
                    value         = nif,
                    onValueChange = { if (it.length <= 9 && it.all { c -> c.isDigit() }) nif = it },
                    label         = { Text("NIF do fornecedor") },
                    placeholder   = { Text("9 dígitos") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine    = true,
                    shape         = RoundedCornerShape(8.dp),
                    colors        = tfColors,
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = valor,
                    onValueChange = { novo ->
                        if (novo.all { it.isDigit() || it == '.' || it == ',' }) valor = novo
                    },
                    label         = { Text("Valor total (€)") },
                    placeholder   = { Text("Ex: 47.80") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine    = true,
                    shape         = RoundedCornerShape(8.dp),
                    colors        = tfColors,
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmar(nif, valor) },
                colors  = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE65100)
                )
            ) {
                Text("Guardar")
            }
        },
        // Sem botão de ignorar — sem QR o preenchimento é obrigatório para guardar
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Paleta de cores dos estados
// ─────────────────────────────────────────────────────────────────────────────

// Paleta dark — tema industrial (fundos escuros, texto sempre legível)
private val CorValid        = Color(0xFF4CAF50)   // verde
private val CorValidFundo   = Color(0xFF1B2E1B)   // verde escuríssimo
private val CorSuspect      = Color(0xFFFFB300)   // âmbar
private val CorSuspectFundo = Color(0xFF2C2200)   // âmbar escuríssimo
private val CorMissing      = Color(0xFF78909C)   // cinzento azulado
private val CorMissingFundo = Color(0xFF1E262F)   // IndustrialSurface2
