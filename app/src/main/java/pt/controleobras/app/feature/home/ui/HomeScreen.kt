package pt.controleobras.app.feature.home.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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

    val mostrarBoasVindas  by viewModel.mostrarBoasVindas.collectAsState()
    val driveConfigurado   by viewModel.driveConfigurado.collectAsState()
    val modeloDisponivel   by viewModel.modeloIaDisponivel.collectAsState()
    val downloadProgress   by viewModel.downloadProgress.collectAsState()

    // Re-verificar modelo ao voltar ao ecrã (utilizador pode ter copiado ficheiro)
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.verificarModeloIa()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    // SAF: picker para selecionar pasta Google Drive
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(text = stringResource(R.string.app_name)) })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.home_placeholder),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.home_instrucao),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(24.dp))

            // ── Google Drive ─────────────────────────────────────────────────
            StatusBanner(
                titulo = "Google Drive",
                descricao = if (driveConfigurado)
                    "Pasta configurada — uploads automáticos"
                else
                    "Sem pasta configurada — toca em Configurar",
                configurado = driveConfigurado,
                labelBotao = if (driveConfigurado) "Alterar" else "Configurar",
                onAcao = { driveLauncher.launch(null) }
            )

            Spacer(Modifier.height(8.dp))

            // ── Modelo IA ────────────────────────────────────────────────────
            IaBanner(
                modeloDisponivel = modeloDisponivel,
                downloadProgress = downloadProgress,
                onDescarregar = { viewModel.iniciarDownloadModelo() },
                onCancelar = { viewModel.cancelarDownload() }
            )

            Spacer(Modifier.height(24.dp))

            // ── Ações principais ─────────────────────────────────────────────
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onNovoTalao
            ) {
                Text(text = stringResource(R.string.home_novo_talao))
            }
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                onClick = onHistorico
            ) {
                Text(text = stringResource(R.string.home_historico))
            }
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
// Banner de estado do modelo IA (com progresso de download)
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

    val corFundo  = if (modeloDisponivel) Color(0xFFE8F5E9) else Color(0xFFFFF8E1)
    val corTexto  = if (modeloDisponivel) Color(0xFF388E3C) else Color(0xFFF57F17)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = corFundo,
        tonalElevation = 0.dp
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
                        color = corTexto
                    )
                    Text(
                        text = when {
                            modeloDisponivel  -> "Modelo instalado — extração avançada ativa"
                            aDescarregar      ->
                                if (downloadProgress.percentagem >= 0)
                                    "A descarregar... ${downloadProgress.percentagem}% (${downloadProgress.descricaoTamanho})"
                                else
                                    "A descarregar... (${downloadProgress.descricaoTamanho})"
                            erroDownload      -> "Erro no download — verifica a ligação e tenta de novo"
                            else              -> "Modelo não instalado — necessário para extração inteligente"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = corTexto
                    )
                }

                // Botão de ação
                when {
                    modeloDisponivel -> { /* nada — já está instalado */ }
                    aDescarregar -> TextButton(onClick = onCancelar) {
                        Text("Cancelar", color = corTexto)
                    }
                    else -> TextButton(onClick = onDescarregar) {
                        Text(if (erroDownload) "Tentar de novo" else "Descarregar", color = corTexto)
                    }
                }
            }

            // Barra de progresso (visível só durante download)
            if (aDescarregar) {
                Spacer(Modifier.height(8.dp))
                if (downloadProgress.percentagem in 0..100) {
                    LinearProgressIndicator(
                        progress = { downloadProgress.percentagem / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = corTexto,
                        trackColor = corTexto.copy(alpha = 0.2f)
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = corTexto,
                        trackColor = corTexto.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Banner genérico reutilizável (Drive, etc.)
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
    val corTexto = if (configurado) Color(0xFF388E3C) else Color(0xFFF57F17)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = corFundo,
        tonalElevation = 0.dp
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
                Text(titulo, style = MaterialTheme.typography.labelMedium, color = corTexto)
                Text(descricao, style = MaterialTheme.typography.bodySmall, color = corTexto)
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
