package pt.controleobras.app.feature.home.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import pt.controleobras.app.R
import pt.controleobras.app.core.designsystem.theme.IndustrialBorder
import pt.controleobras.app.core.designsystem.theme.IndustrialGlow
import pt.controleobras.app.core.designsystem.theme.IndustrialGlowDim
import pt.controleobras.app.core.designsystem.theme.IndustrialSteel
import pt.controleobras.app.core.designsystem.theme.IndustrialSurface
import pt.controleobras.app.core.designsystem.theme.IndustrialSurface2
import pt.controleobras.app.core.designsystem.theme.IndustrialSurface3
import pt.controleobras.app.core.llm.LlmDownloadEstado
import pt.controleobras.app.feature.home.viewmodel.EstadoBd
import pt.controleobras.app.feature.home.viewmodel.FaturaRecente
import pt.controleobras.app.feature.home.viewmodel.FeedbackFatura
import pt.controleobras.app.feature.home.viewmodel.HomeViewModel
import pt.controleobras.app.feature.home.viewmodel.ResumoDia
import pt.controleobras.app.feature.home.viewmodel.ResumoPeriodo
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Ecrã principal (Home) da aplicação Controle Obras.
 *
 * Este ecrã é a primeira coisa que o utilizador vê ao abrir a app.
 * Contém:
 *  - Cabeçalho com logo e estado da ligação à BD (chip colorido)
 *  - Banner de feedback quando uma fatura acabou de ser guardada
 *  - Botão principal "SCAN" para digitalizar um novo talão
 *  - Botão de acesso ao histórico de faturas
 *  - Secção de estado dos serviços (Google Drive, modelo IA, BD)
 *  - Diálogo de boas-vindas (apenas na primeira utilização)
 *  - Diálogo de configuração do Google Drive
 *
 * @param modifier    Modificador externo (tipicamente passado pelo Scaffold)
 * @param onNovoTalao Callback que abre o fluxo de digitalização (WorkerForm → Câmara → Revisão)
 * @param onHistorico Callback que navega para a lista de faturas guardadas
 * @param viewModel   Injetado automaticamente pelo Hilt
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNovoTalao: () -> Unit = {},
    onHistorico: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ── Recolher estados observáveis do ViewModel ─────────────────────────
    // O Compose redesenha automaticamente quando qualquer um destes muda.
    val mostrarBoasVindas  by viewModel.mostrarBoasVindas.collectAsState()
    val driveConfigurado   by viewModel.driveConfigurado.collectAsState()
    val modeloDisponivel   by viewModel.modeloIaDisponivel.collectAsState()
    val downloadProgress   by viewModel.downloadProgress.collectAsState()
    val feedbackFatura     by viewModel.feedbackUltimaFatura.collectAsState()
    val estadoBd           by viewModel.estadoBd.collectAsState()
    val erroBd             by viewModel.erroBd.collectAsState()
    val ultimaSync         by viewModel.ultimaSync.collectAsState()
    val resumoDia          by viewModel.resumoDia.collectAsState()
    val resumoPeriodo      by viewModel.resumoPeriodo.collectAsState()
    val ultimasFaturas     by viewModel.ultimasFaturas.collectAsState()

    // Estado local — controla se o diálogo de diagnóstico da BD está visível
    var mostrarDiagnosticoBd by remember { mutableStateOf(false) }

    // ── Verificar estados ao retomar o ecrã ───────────────────────────────
    // Quando o utilizador volta ao Home (ex: depois de guardar uma fatura),
    // verificamos se o modelo IA existe e se há feedback para mostrar.
    // Usamos DisposableEffect + LifecycleEventObserver para reagir ao ON_RESUME.
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.verificarModeloIa()
                viewModel.verificarFeedbackFatura()
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    // ── Launcher para escolher pasta do Google Drive ──────────────────────
    // O utilizador escolhe uma pasta no dispositivo; a app guarda o URI
    // com permissão persistente para poder fazer upload de ficheiros mais tarde.
    val driveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            // Garantir que a permissão de leitura/escrita sobrevive a reinícios da app
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.guardarDriveFolderUri(uri.toString())
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header com saudação contextual ───────────────────────────────
            HomeHeader(
                estadoBd    = estadoBd,
                ultimaSync  = ultimaSync,
                onChipClick = { if (estadoBd == EstadoBd.ERRO) mostrarDiagnosticoBd = true }
            )

            // Banner de feedback — aparece ao regressar após guardar fatura
            AnimatedVisibility(
                visible = feedbackFatura != null,
                enter   = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit    = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            ) {
                feedbackFatura?.let { fb ->
                    BannerFaturaGuardada(
                        empresa  = fb.empresa,
                        total    = fb.total,
                        onFechar = viewModel::fecharFeedbackFatura
                    )
                }
            }

            // ── Conteúdo principal scrollável ────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Cards resumo do dia (2 lado a lado) — sempre visíveis
                SecaoResumoDia(resumoDia = resumoDia)

                // Ação principal — Scan
                BotaoScanPrincipal(onClick = onNovoTalao)

                // Resumo semanal/mensal
                SecaoResumoPeriodo(resumoPeriodo = resumoPeriodo)

                // Últimas faturas registadas
                AnimatedVisibility(
                    visible = ultimasFaturas.isNotEmpty(),
                    enter   = expandVertically() + fadeIn(),
                    exit    = shrinkVertically() + fadeOut()
                ) {
                    SecaoUltimasFaturas(faturas = ultimasFaturas)
                }

                // Ação secundária — Histórico
                BotaoHistorico(onClick = onHistorico)

                // Estado dos serviços (discreto)
                SecaoEstadoServicos(
                    driveConfigurado  = driveConfigurado,
                    modeloDisponivel  = modeloDisponivel,
                    downloadProgress  = downloadProgress,
                    onConfigurarDrive = { driveLauncher.launch(null) },
                    onDescarregar     = { viewModel.iniciarDownloadModelo() },
                    onCancelar        = { viewModel.cancelarDownload() }
                )
            }
        }
    }

    if (mostrarDiagnosticoBd) {
        AlertDialog(
            onDismissRequest = { mostrarDiagnosticoBd = false },
            containerColor   = IndustrialSurface,
            title = {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFEF5350))
                    )
                    Text(
                        "Diagnóstico BD",
                        color      = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "A aplicação não consegue ligar à base de dados.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    if (erroBd != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F1318))
                                .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "ERRO TÉCNICO",
                                style     = MaterialTheme.typography.labelSmall,
                                color     = IndustrialGlow,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                erroBd ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFEF5350)
                            )
                        }
                    }
                    Text(
                        "Verifique:\n" +
                        "• O tablet está ligado à rede da empresa\n" +
                        "• O servidor de base de dados está ativo\n" +
                        "• O ficheiro de configuração está correto",
                        style = MaterialTheme.typography.bodySmall,
                        color = IndustrialSteel
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDiagnosticoBd = false
                        viewModel.tentarLigarBd()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndustrialGlow),
                    shape  = RoundedCornerShape(12.dp)
                ) {
                    Text("Tentar novamente", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDiagnosticoBd = false }) {
                    Text("Fechar", color = IndustrialSteel)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (mostrarBoasVindas) {
        AlertDialog(
            onDismissRequest = viewModel::fecharBoasVindas,
            containerColor   = IndustrialSurface,
            shape            = RoundedCornerShape(20.dp),
            title = {
                Text(
                    stringResource(R.string.boasvindas_titulo),
                    color      = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        stringResource(R.string.boasvindas_passo1),
                        stringResource(R.string.boasvindas_passo2),
                        stringResource(R.string.boasvindas_passo3),
                        stringResource(R.string.boasvindas_passo4)
                    ).forEachIndexed { i, passo ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text      = "${i + 1}",
                                style     = MaterialTheme.typography.labelSmall,
                                color     = IndustrialGlow,
                                fontWeight = FontWeight.Bold,
                                modifier  = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                passo,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::fecharBoasVindas,
                    colors  = ButtonDefaults.buttonColors(containerColor = IndustrialGlow),
                    shape   = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.boasvindas_entendido), color = Color.White)
                }
            }
        )
    }

}

// ─────────────────────────────────────────────────────────────────────────────
// Resumo do dia — 2 cards dashboard lado a lado
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Secção de resumo com 2 cards lado a lado estilo dashboard:
 *  - Card esquerdo: total de talões registados hoje
 *  - Card direito: total de despesas em euros
 *
 * Design inspirado em dashboards financeiros modernos.
 * Números grandes com ícone e label descritiva.
 */
@Composable
private fun SecaoResumoDia(resumoDia: ResumoDia) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Título da secção
        Text(
            text       = "Resumo de hoje",
            style      = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color      = IndustrialSteel,
            modifier   = Modifier.padding(start = 4.dp)
        )

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card — Talões
            CardResumoItem(
                icone    = Icons.Outlined.Receipt,
                valor    = "${resumoDia.totalTaloes}",
                label    = "Talões",
                corIcone = IndustrialGlow,
                destaque = false,
                modifier = Modifier.weight(1f)
            )

            // Card — Despesas (com destaque laranja)
            CardResumoItem(
                icone    = Icons.Outlined.Payments,
                valor    = "%.2f €".format(resumoDia.totalDespesas),
                label    = "Despesas",
                corIcone = IndustrialGlow,
                destaque = true,
                modifier = Modifier.weight(1f)
            )

            // Card — Presenças
            CardResumoItem(
                icone    = Icons.Outlined.People,
                valor    = "${resumoDia.totalPresencas}",
                label    = "Presenças",
                corIcone = Color(0xFF42A5F5),
                destaque = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Card individual de resumo — número grande, ícone e label.
 * Quando [destaque] é true, aplica fundo com glow laranja subtil.
 */
@Composable
private fun CardResumoItem(
    icone:    ImageVector,
    valor:    String,
    label:    String,
    corIcone: Color,
    destaque: Boolean,
    modifier: Modifier = Modifier
) {
    val fundoCard = if (destaque) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1E2530),
                Color(0xFF1A1E28)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(IndustrialSurface2, IndustrialSurface2)
        )
    }

    val bordaCor = if (destaque) IndustrialGlow.copy(alpha = 0.25f) else IndustrialBorder

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(fundoCard)
            .border(1.dp, bordaCor, RoundedCornerShape(16.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Ícone com fundo
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (destaque) IndustrialGlowDim
                    else Color(0xFF242E38)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = icone,
                contentDescription = null,
                tint               = corIcone,
                modifier           = Modifier.size(20.dp)
            )
        }

        // Valor grande
        Text(
            text       = valor,
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color      = if (destaque) IndustrialGlow else Color.White
        )

        // Label
        Text(
            text  = label,
            style = MaterialTheme.typography.labelMedium,
            color = IndustrialSteel
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header — saudação contextual com data e estado BD
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Header do ecrã principal com:
 *  - Logo e chip de estado BD (linha superior)
 *  - Saudação contextual ("Bom dia", "Boa tarde", "Boa noite")
 *  - Data atual formatada em português
 */
@Composable
private fun HomeHeader(
    estadoBd: EstadoBd = EstadoBd.DESCONHECIDO,
    ultimaSync: Long = 0L,
    onChipClick: () -> Unit = {}
) {
    // Determinar saudação com base na hora
    val agora = remember { LocalTime.now() }
    val saudacao = remember(agora) {
        when (agora.hour) {
            in 6..11  -> "Bom dia"
            in 12..18 -> "Boa tarde"
            else      -> "Boa noite"
        }
    }
    // Data formatada: "23 de julho de 2026"
    val dataFormatada = remember {
        LocalDate.now().format(
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale("pt", "PT"))
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0C1016), MaterialTheme.colorScheme.background)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Linha superior: logo + chip BD
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter            = painterResource(R.drawable.ic_logo),
                    contentDescription = "Controle Obras",
                    modifier           = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Text(
                    text       = "CONTROLE OBRAS",
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color      = IndustrialSteel,
                    letterSpacing = 1.sp
                )
            }
            ChipEstadoBd(estado = estadoBd, ultimaSync = ultimaSync, onClick = onChipClick)
        }

        // Saudação e data
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text       = saudacao,
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
            Text(
                text  = dataFormatada,
                style = MaterialTheme.typography.bodyMedium,
                color = IndustrialSteel
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Chip de estado da ligação à BD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChipEstadoBd(estado: EstadoBd, ultimaSync: Long = 0L, onClick: () -> Unit = {}) {
    val (corFundo, corTexto, corBorda, label) = when (estado) {
        EstadoBd.LIGADO        -> Quadruplo(Color(0x2243A047), Color(0xFF66BB6A), Color(0x4443A047), "ONLINE")
        EstadoBd.ERRO          -> Quadruplo(Color(0x22EF5350), Color(0xFFEF5350), Color(0x44EF5350), "OFFLINE")
        EstadoBd.A_SINCRONIZAR -> Quadruplo(IndustrialSurface2, IndustrialSteel, IndustrialBorder, "SYNC")
        EstadoBd.DESCONHECIDO  -> Quadruplo(IndustrialSurface2, IndustrialSteel, IndustrialBorder, "BD")
    }

    // Formatar hora da última sync (ex: "14:32")
    val horaSync = remember(ultimaSync) {
        if (ultimaSync > 0L) {
            val instant = java.time.Instant.ofEpochMilli(ultimaSync)
            val hora = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
            hora.format(DateTimeFormatter.ofPattern("HH:mm"))
        } else null
    }

    val clickModifier = if (estado == EstadoBd.ERRO) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(corFundo)
            .border(1.dp, corBorda, RoundedCornerShape(20.dp))
            .then(clickModifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (estado == EstadoBd.A_SINCRONIZAR) {
            CircularProgressIndicator(
                modifier    = Modifier.size(7.dp),
                color       = IndustrialSteel,
                strokeWidth = 1.5.dp
            )
        } else {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(corTexto)
            )
        }
        Text(
            text       = label,
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color      = corTexto
        )
        // Mostrar hora da última sync quando ligado
        if (estado == EstadoBd.LIGADO && horaSync != null) {
            Text(
                text  = horaSync,
                style = MaterialTheme.typography.labelSmall,
                color = corTexto.copy(alpha = 0.6f)
            )
        }
    }
}

/** Alias para desestruturação no when (evita Pair aninhado). */
private data class Quadruplo<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
private operator fun <A, B, C, D> Quadruplo<A, B, C, D>.component1() = a
private operator fun <A, B, C, D> Quadruplo<A, B, C, D>.component2() = b
private operator fun <A, B, C, D> Quadruplo<A, B, C, D>.component3() = c
private operator fun <A, B, C, D> Quadruplo<A, B, C, D>.component4() = d

// ─────────────────────────────────────────────────────────────────────────────
// Botão principal de scan — card proeminente com gradiente
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BotaoScanPrincipal(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF1E2530),
                        Color(0xFF1A1F28)
                    )
                )
            )
            .border(1.dp, IndustrialGlow.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Ícone com fundo laranja — destaque visual principal
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(IndustrialGlow),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.CameraAlt,
                contentDescription = null,
                tint               = Color.White,
                modifier           = Modifier.size(26.dp)
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text       = "Digitalizar Fatura",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
            Text(
                text  = "Câmara ou galeria de imagens",
                style = MaterialTheme.typography.bodySmall,
                color = IndustrialSteel
            )
        }

        // Seta com fundo subtil
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(IndustrialGlowDim),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint               = IndustrialGlow,
                modifier           = Modifier.size(20.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Resumo semanal/mensal — 2 cards lado a lado
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Cards de resumo semanal e mensal.
 * Mostra totais agregados dos últimos 7 dias e do mês corrente.
 */
@Composable
private fun SecaoResumoPeriodo(resumoPeriodo: ResumoPeriodo) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text       = "Visão geral",
            style      = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color      = IndustrialSteel,
            modifier   = Modifier.padding(start = 4.dp)
        )

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card Semana
            CardPeriodo(
                titulo   = "Últimos 7 dias",
                taloes   = resumoPeriodo.totalTaloesSemana,
                despesas = resumoPeriodo.totalDespesasSemana,
                modifier = Modifier.weight(1f)
            )
            // Card Mês
            CardPeriodo(
                titulo   = "Este mês",
                taloes   = resumoPeriodo.totalTaloesMes,
                despesas = resumoPeriodo.totalDespesasMes,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CardPeriodo(
    titulo:   String,
    taloes:   Int,
    despesas: Double,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(IndustrialSurface2)
            .border(1.dp, IndustrialBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text       = titulo,
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color      = IndustrialSteel
        )
        Text(
            text       = "%.2f €".format(despesas),
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color      = Color.White
        )
        Text(
            text  = "$taloes talões",
            style = MaterialTheme.typography.bodySmall,
            color = IndustrialSteel
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Últimas faturas — mini-lista com as 5 mais recentes
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Mini-lista das últimas faturas registadas.
 * Mostra empresa, valor e data de forma compacta.
 */
@Composable
private fun SecaoUltimasFaturas(faturas: List<FaturaRecente>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text       = "Atividade recente",
            style      = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color      = IndustrialSteel,
            modifier   = Modifier.padding(start = 4.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(IndustrialSurface2)
                .border(1.dp, IndustrialBorder, RoundedCornerShape(16.dp))
        ) {
            faturas.forEachIndexed { index, fatura ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Indicador colorido
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(IndustrialGlow.copy(alpha = 0.7f))
                    )

                    // Empresa
                    Text(
                        text     = fatura.empresa,
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = Color.White,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )

                    // Data
                    Text(
                        text  = fatura.data,
                        style = MaterialTheme.typography.labelSmall,
                        color = IndustrialSteel
                    )

                    // Total
                    Text(
                        text       = fatura.total,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = IndustrialGlow
                    )
                }

                // Separador entre itens (exceto último)
                if (index < faturas.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .height(1.dp)
                            .background(IndustrialBorder.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Histórico — card secundário limpo
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BotaoHistorico(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(IndustrialSurface2)
            .border(1.dp, IndustrialBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF242E38)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.History,
                contentDescription = null,
                tint               = IndustrialSteel,
                modifier           = Modifier.size(22.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = "Histórico",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = Color.White
            )
            Text(
                text  = "Faturas e registos guardados",
                style = MaterialTheme.typography.bodySmall,
                color = IndustrialSteel
            )
        }
        Icon(
            imageVector        = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint               = IndustrialSteel,
            modifier           = Modifier.size(18.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Serviços — cards individuais com cantos 16dp
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SecaoEstadoServicos(
    driveConfigurado: Boolean,
    modeloDisponivel: Boolean,
    downloadProgress: pt.controleobras.app.core.llm.LlmDownloadProgress,
    onConfigurarDrive: () -> Unit,
    onDescarregar: () -> Unit,
    onCancelar: () -> Unit
) {
    // Secção colapsável — começa fechada
    var expandido by remember { mutableStateOf(false) }

    Column(
        modifier            = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Título clicável com indicador de expansão
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { expandido = !expandido }
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text       = "Serviços",
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color      = IndustrialSteel,
                modifier   = Modifier.weight(1f)
            )

            // Indicadores resumidos (sempre visíveis)
            val driveIconColor = if (driveConfigurado) Color(0xFF4CAF50) else Color(0xFF78909C)
            val iaIconColor    = if (modeloDisponivel) Color(0xFF4CAF50) else Color(0xFF78909C)

            Box(Modifier.size(6.dp).clip(CircleShape).background(driveIconColor))
            Text("Drive", style = MaterialTheme.typography.labelSmall, color = IndustrialSteel)

            Spacer(Modifier.width(6.dp))

            Box(Modifier.size(6.dp).clip(CircleShape).background(iaIconColor))
            Text("IA", style = MaterialTheme.typography.labelSmall, color = IndustrialSteel)

            Spacer(Modifier.width(4.dp))

            Icon(
                imageVector = if (expandido)
                    Icons.Filled.KeyboardArrowUp
                else
                    Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expandido) "Recolher" else "Expandir",
                tint     = IndustrialSteel,
                modifier = Modifier.size(16.dp)
            )
        }

        // Conteúdo colapsável
        AnimatedVisibility(
            visible = expandido,
            enter   = expandVertically() + fadeIn(),
            exit    = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ServicoItem(
                    titulo    = "Google Drive",
                    descricao = if (driveConfigurado) "Sincronização ativa" else "Não configurado",
                    ativo     = driveConfigurado,
                    labelAcao = if (driveConfigurado) "Alterar" else "Configurar",
                    onAcao    = onConfigurarDrive
                )
                IaServicoItem(
                    modeloDisponivel = modeloDisponivel,
                    downloadProgress = downloadProgress,
                    onDescarregar    = onDescarregar,
                    onCancelar       = onCancelar
                )
            }
        }
    }
}

@Composable
private fun ServicoItem(
    titulo:    String,
    descricao: String,
    ativo:     Boolean,
    labelAcao: String?,
    onAcao:    () -> Unit
) {
    val corPonto = if (ativo) Color(0xFF4CAF50) else Color(0xFF78909C)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(IndustrialSurface2)
            .border(1.dp, IndustrialBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Indicador de estado
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(corPonto)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                titulo,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = Color.White
            )
            Text(
                descricao,
                style = MaterialTheme.typography.bodySmall,
                color = IndustrialSteel
            )
        }
        if (labelAcao != null) {
            Text(
                text     = labelAcao,
                style    = MaterialTheme.typography.labelMedium,
                color    = IndustrialGlow,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAcao)
                    .background(IndustrialGlowDim)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun IaServicoItem(
    modeloDisponivel: Boolean,
    downloadProgress: pt.controleobras.app.core.llm.LlmDownloadProgress,
    onDescarregar: () -> Unit,
    onCancelar: () -> Unit
) {
    val aDescarregar = downloadProgress.estado == LlmDownloadEstado.A_DESCARREGAR
    val erroDownload = downloadProgress.estado == LlmDownloadEstado.ERRO
    val corPonto     = if (modeloDisponivel) Color(0xFF4CAF50) else Color(0xFF78909C)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(IndustrialSurface2)
            .border(1.dp, IndustrialBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(corPonto)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "IA local",
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White
                )
                Text(
                    text = when {
                        modeloDisponivel -> "Modelo instalado"
                        aDescarregar     -> "A descarregar...${if (downloadProgress.percentagem >= 0) " ${downloadProgress.percentagem}%" else ""}"
                        erroDownload     -> "Erro no download"
                        else             -> "Não instalado"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = IndustrialSteel
                )
            }
            when {
                modeloDisponivel ->
                    Icon(
                        Icons.Default.CheckCircle,
                        null,
                        tint     = Color(0xFF4CAF50),
                        modifier = Modifier.size(18.dp)
                    )
                aDescarregar ->
                    Text(
                        text     = "Cancelar",
                        style    = MaterialTheme.typography.labelMedium,
                        color    = IndustrialSteel,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onCancelar)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                else ->
                    Text(
                        text       = if (erroDownload) "Tentar" else "Instalar",
                        style      = MaterialTheme.typography.labelMedium,
                        color      = IndustrialGlow,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onDescarregar)
                            .background(IndustrialGlowDim)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
            }
        }
        if (aDescarregar) {
            val progresso: Float = if (downloadProgress.percentagem in 0..100)
                downloadProgress.percentagem / 100f else 0f
            LinearProgressIndicator(
                progress   = { progresso },
                modifier   = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color      = IndustrialGlow,
                trackColor = IndustrialBorder
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Banner de feedback — fatura guardada com sucesso
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BannerFaturaGuardada(
    empresa:  String,
    total:    String,
    onFechar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1B2E1B))
            .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Ícone com fundo
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF4CAF50).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.CheckCircle,
                contentDescription = null,
                tint               = Color(0xFF4CAF50),
                modifier           = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = "Fatura guardada",
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = Color(0xFF66BB6A)
            )
            Text(
                text  = buildString {
                    if (empresa.isNotBlank()) append(empresa)
                    if (total.isNotBlank()) {
                        if (empresa.isNotBlank()) append("  ·  ")
                        append(total)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        androidx.compose.material3.IconButton(onClick = onFechar) {
            Icon(
                imageVector        = Icons.Default.Close,
                contentDescription = "Fechar",
                tint               = Color.White.copy(alpha = 0.4f),
                modifier           = Modifier.size(16.dp)
            )
        }
    }
}

