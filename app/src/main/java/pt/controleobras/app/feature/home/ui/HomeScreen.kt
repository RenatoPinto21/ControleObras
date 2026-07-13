package pt.controleobras.app.feature.home.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
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
import pt.controleobras.app.core.llm.LlmDownloadEstado
import pt.controleobras.app.feature.home.viewmodel.FeedbackFatura
import pt.controleobras.app.feature.home.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNovoTalao: () -> Unit = {},
    onHistorico: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mostrarBoasVindas  by viewModel.mostrarBoasVindas.collectAsState()
    val driveConfigurado   by viewModel.driveConfigurado.collectAsState()
    val modeloDisponivel   by viewModel.modeloIaDisponivel.collectAsState()
    val downloadProgress   by viewModel.downloadProgress.collectAsState()
    val feedbackFatura     by viewModel.feedbackUltimaFatura.collectAsState()

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

    val driveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
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

            HomeHeader()

            // Banner de feedback — aparece ao regressar após guardar fatura
            feedbackFatura?.let { fb ->
                BannerFaturaGuardada(
                    empresa  = fb.empresa,
                    total    = fb.total,
                    onFechar = viewModel::fecharFeedbackFatura
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 28.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BotaoScanPrincipal(onClick = onNovoTalao)

                BotaoHistorico(onClick = onHistorico)

                Spacer(Modifier.height(4.dp))

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

    if (mostrarBoasVindas) {
        AlertDialog(
            onDismissRequest = viewModel::fecharBoasVindas,
            containerColor   = IndustrialSurface,
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
                    shape   = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.boasvindas_entendido), color = Color.White)
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header — limpo, sem gradiente desnecessário
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader() {
    Row(
        modifier = Modifier
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
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Marca lateral laranja
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(32.dp)
                .background(IndustrialGlow)
        )
        Column {
            Text(
                text       = "Controle Obras",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
            Text(
                text  = "Gestão de despesas",
                style = MaterialTheme.typography.labelSmall,
                color = IndustrialSteel
            )
        }
        Spacer(Modifier.weight(1f))
        // Badge de versão
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(IndustrialSurface2)
                .border(1.dp, IndustrialBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text  = "v1.0",
                style = MaterialTheme.typography.labelSmall,
                color = IndustrialSteel
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Botão principal de scan — sóbrio, profissional
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BotaoScanPrincipal(onClick: () -> Unit) {
    // Pulso subtil apenas no anel exterior
    val infiniteTransition = rememberInfiniteTransition(label = "scan_pulse")
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.25f,
        targetValue   = 0.55f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161C22))
            .border(1.dp, IndustrialBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Ícone com anel de pulso
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .drawBehind {
                        // Anel externo pulsante
                        drawCircle(
                            color  = IndustrialGlow.copy(alpha = ringAlpha),
                            radius = size.minDimension / 2f + 10.dp.toPx(),
                            style  = Stroke(width = 1.dp.toPx())
                        )
                        // Anel médio fixo
                        drawCircle(
                            color  = IndustrialGlow.copy(alpha = 0.12f),
                            radius = size.minDimension / 2f + 22.dp.toPx(),
                            style  = Stroke(width = 1.dp.toPx())
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(IndustrialGlow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.CameraAlt,
                        contentDescription = "Digitalizar fatura",
                        tint               = Color.White,
                        modifier           = Modifier.size(36.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text       = "Digitalizar Fatura",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White
                )
                Text(
                    text  = "Fotografar ou escolher da galeria",
                    style = MaterialTheme.typography.bodySmall,
                    color = IndustrialSteel
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Histórico
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BotaoHistorico(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(IndustrialSurface2)
            .border(1.dp, IndustrialBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(IndustrialGlowDim),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.History,
                contentDescription = null,
                tint               = IndustrialGlow,
                modifier           = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = "Histórico de Faturas",
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color      = Color.White
            )
            Text(
                text  = "Ver todos os registos guardados",
                style = MaterialTheme.typography.bodySmall,
                color = IndustrialSteel
            )
        }
        Icon(
            imageVector        = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint               = IndustrialSteel,
            modifier           = Modifier.size(20.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Serviços
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
    Column(
        modifier            = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(1.dp)  // bordas coladas — estilo painel de controlo
    ) {
        // Título da secção — linha simples, sem traço laranja overdone
        Text(
            text      = "Estado dos serviços",
            style     = MaterialTheme.typography.labelSmall,
            color     = IndustrialSteel,
            modifier  = Modifier.padding(bottom = 6.dp, start = 2.dp)
        )

        ServicoItem(
            titulo    = "Google Drive",
            descricao = if (driveConfigurado) "Sincronização ativa" else "Não configurado",
            ativo     = driveConfigurado,
            labelAcao = if (driveConfigurado) "Alterar" else "Configurar",
            onAcao    = onConfigurarDrive,
            arredondamentoTopo = true,
            arredondamentoBase = false
        )

        // Divisor
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF0F1318)))

        IaServicoItem(
            modeloDisponivel = modeloDisponivel,
            downloadProgress = downloadProgress,
            onDescarregar    = onDescarregar,
            onCancelar       = onCancelar
        )
    }
}

@Composable
private fun ServicoItem(
    titulo:              String,
    descricao:           String,
    ativo:               Boolean,
    labelAcao:           String?,
    onAcao:              () -> Unit,
    arredondamentoTopo:  Boolean = true,
    arredondamentoBase:  Boolean = true
) {
    val cornerTopo = if (arredondamentoTopo) 10.dp else 0.dp
    val cornerBase = if (arredondamentoBase) 10.dp else 0.dp
    val shape = RoundedCornerShape(
        topStart    = cornerTopo, topEnd    = cornerTopo,
        bottomStart = cornerBase, bottomEnd = cornerBase
    )
    val corPonto = if (ativo) Color(0xFF4CAF50) else Color(0xFF78909C)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(IndustrialSurface2)
            .border(1.dp, IndustrialBorder, shape)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(corPonto)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                titulo,
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color      = Color.White
            )
            Text(
                descricao,
                style = MaterialTheme.typography.labelSmall,
                color = IndustrialSteel
            )
        }
        if (labelAcao != null) {
            TextButton(onClick = onAcao, modifier = Modifier.height(32.dp)) {
                Text(
                    labelAcao,
                    style = MaterialTheme.typography.labelSmall,
                    color = IndustrialGlow
                )
            }
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
    val erroDownload  = downloadProgress.estado == LlmDownloadEstado.ERRO
    val corPonto     = if (modeloDisponivel) Color(0xFF4CAF50) else Color(0xFF78909C)

    val shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 10.dp, bottomEnd = 10.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(IndustrialSurface2)
            .border(1.dp, IndustrialBorder, shape)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(corPonto)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "IA local",
                    style      = MaterialTheme.typography.bodySmall,
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
                    style = MaterialTheme.typography.labelSmall,
                    color = IndustrialSteel
                )
            }
            when {
                modeloDisponivel ->
                    Icon(
                        Icons.Default.CheckCircle,
                        null,
                        tint     = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                aDescarregar ->
                    TextButton(onClick = onCancelar, modifier = Modifier.height(32.dp)) {
                        Text("Cancelar", color = IndustrialSteel, style = MaterialTheme.typography.labelSmall)
                    }
                else ->
                    TextButton(onClick = onDescarregar, modifier = Modifier.height(32.dp)) {
                        Text(
                            if (erroDownload) "Tentar" else "Instalar",
                            color = IndustrialGlow,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
            }
        }
        if (aDescarregar) {
            val progresso: Float = if (downloadProgress.percentagem in 0..100)
                downloadProgress.percentagem / 100f else 0f
            LinearProgressIndicator(
                progress   = progresso,
                modifier   = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp)),
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
            .background(Color(0xFF1B2E1B))
            .drawBehind {
                drawLine(
                    color       = Color(0xFF4CAF50),
                    start       = Offset(0f, size.height),
                    end         = Offset(size.width, size.height),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector        = Icons.Default.CheckCircle,
            contentDescription = null,
            tint               = Color(0xFF4CAF50),
            modifier           = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = "Fatura guardada",
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color      = Color(0xFF4CAF50)
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
                color = Color.White.copy(alpha = 0.75f)
            )
        }
        androidx.compose.material3.IconButton(onClick = onFechar) {
            Icon(
                imageVector        = Icons.Default.Close,
                contentDescription = "Fechar",
                tint               = Color.White.copy(alpha = 0.5f),
                modifier           = Modifier.size(16.dp)
            )
        }
    }
}
