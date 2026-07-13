package pt.controleobras.app.feature.home.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import pt.controleobras.app.R
import pt.controleobras.app.core.designsystem.theme.ControleObrasTheme
import pt.controleobras.app.core.llm.LlmDownloadEstado
import pt.controleobras.app.feature.home.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNovoTalao: () -> Unit = {},
    onHistorico: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mostrarBoasVindas by viewModel.mostrarBoasVindas.collectAsState()
    val driveConfigurado  by viewModel.driveConfigurado.collectAsState()
    val modeloDisponivel  by viewModel.modeloIaDisponivel.collectAsState()
    val downloadProgress  by viewModel.downloadProgress.collectAsState()

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.verificarModeloIa()
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

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // ─── Cabeçalho com gradiente laranja ──────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Gestão de despesas em obra",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        // ─── Conteúdo principal ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Ações principais ──────────────────────────────────────────────
            Text(
                text = "O que pretende fazer?",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            // Botão principal — registar talão
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                onClick = onNovoTalao,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.home_novo_talao),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Botão secundário — histórico
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                onClick = onHistorico,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.home_historico),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Estado dos serviços ───────────────────────────────────────────
            Text(
                text = "Estado dos serviços",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            StatusBanner(
                titulo = "Google Drive",
                descricao = if (driveConfigurado)
                    "Pasta configurada — uploads automáticos"
                else
                    "Sem pasta — backups desativados",
                configurado = driveConfigurado,
                labelBotao = if (driveConfigurado) "Alterar" else "Configurar",
                onAcao = { driveLauncher.launch(null) }
            )

            IaBanner(
                modeloDisponivel = modeloDisponivel,
                downloadProgress = downloadProgress,
                onDescarregar    = { viewModel.iniciarDownloadModelo() },
                onCancelar       = { viewModel.cancelarDownload() }
            )
        }
    }

    if (mostrarBoasVindas) {
        AlertDialog(
            onDismissRequest = viewModel::fecharBoasVindas,
            title = { Text(stringResource(R.string.boasvindas_titulo)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.boasvindas_passo1))
                    Text(stringResource(R.string.boasvindas_passo2))
                    Text(stringResource(R.string.boasvindas_passo3))
                    Text(stringResource(R.string.boasvindas_passo4))
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::fecharBoasVindas) {
                    Text(stringResource(R.string.boasvindas_entendido))
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Banner de estado do modelo IA
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun IaBanner(
    modeloDisponivel: Boolean,
    downloadProgress: pt.controleobras.app.core.llm.LlmDownloadProgress,
    onDescarregar: () -> Unit,
    onCancelar: () -> Unit
) {
    val aDescarregar = downloadProgress.estado == LlmDownloadEstado.A_DESCARREGAR
    val erroDownload  = downloadProgress.estado == LlmDownloadEstado.ERRO

    val corFundo = if (modeloDisponivel) Color(0xFFE8F5E9) else Color(0xFFFFF8E1)
    val corTexto = if (modeloDisponivel) Color(0xFF2E7D32) else Color(0xFFF57F17)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = corFundo),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (modeloDisponivel) Icons.Default.CheckCircle
                                  else Icons.Default.Warning,
                    contentDescription = null,
                    tint = corTexto,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Inteligência Artificial (Gemma 2B)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = corTexto
                    )
                    Text(
                        text = when {
                            modeloDisponivel -> "Modelo instalado — extração avançada ativa"
                            aDescarregar ->
                                if (downloadProgress.percentagem >= 0)
                                    "A descarregar... ${downloadProgress.percentagem}% (${downloadProgress.descricaoTamanho})"
                                else
                                    "A descarregar... (${downloadProgress.descricaoTamanho})"
                            erroDownload -> "Erro no download — verifique a ligação"
                            else -> "Não instalado — necessário para extração avançada"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = corTexto.copy(alpha = 0.8f)
                    )
                }
                when {
                    modeloDisponivel -> { /* já instalado */ }
                    aDescarregar -> TextButton(onClick = onCancelar) {
                        Text("Cancelar", color = corTexto)
                    }
                    else -> TextButton(onClick = onDescarregar) {
                        Text(if (erroDownload) "Tentar de novo" else "Descarregar", color = corTexto)
                    }
                }
            }
            if (aDescarregar) {
                Spacer(Modifier.height(8.dp))
                if (downloadProgress.percentagem in 0..100) {
                    LinearProgressIndicator(
                        progress = { downloadProgress.percentagem / 100f },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                        color = corTexto,
                        trackColor = corTexto.copy(alpha = 0.2f)
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                        color = corTexto,
                        trackColor = corTexto.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Banner genérico (Drive, etc.)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatusBanner(
    titulo: String,
    descricao: String,
    configurado: Boolean,
    labelBotao: String?,
    onAcao: () -> Unit
) {
    val corFundo = if (configurado) Color(0xFFE8F5E9) else Color(0xFFFFF8E1)
    val corTexto = if (configurado) Color(0xFF2E7D32) else Color(0xFFF57F17)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = corFundo),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (configurado) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = corTexto,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    titulo,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = corTexto
                )
                Text(
                    descricao,
                    style = MaterialTheme.typography.bodySmall,
                    color = corTexto.copy(alpha = 0.8f)
                )
            }
            if (labelBotao != null) {
                TextButton(onClick = onAcao) {
                    Text(labelBotao, color = corTexto)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    ControleObrasTheme { HomeScreen() }
}
