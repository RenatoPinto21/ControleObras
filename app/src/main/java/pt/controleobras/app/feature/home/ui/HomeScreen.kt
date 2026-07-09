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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import pt.controleobras.app.R
import pt.controleobras.app.core.designsystem.theme.ControleObrasTheme
import pt.controleobras.app.feature.home.viewmodel.HomeViewModel

/**
 * Ecrã inicial.
 *
 * Inclui:
 * - Indicador de configuração do Google Drive com botão SAF para selecionar pasta
 * - Botão "Novo Talão" → fluxo WorkerForm → Câmara → Revisão
 * - Botão "Histórico" → lista de talões guardados
 *
 * A pasta Drive selecionada fica guardada em AppPreferences com permissão persistente
 * e é usada automaticamente no upload após cada captura.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNovoTalao: () -> Unit = {},
    onHistorico: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val mostrarBoasVindas by viewModel.mostrarBoasVindas.collectAsState()
    val driveConfigurado by viewModel.driveConfigurado.collectAsState()

    // Launcher SAF para selecionar pasta do Google Drive
    val driveFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            // Persistir permissão de leitura + escrita para esta pasta entre sessões
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
            TopAppBar(title = { Text(text = stringResource(id = R.string.app_name)) })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = stringResource(id = R.string.home_placeholder))
            Text(text = stringResource(id = R.string.home_instrucao))

            Spacer(modifier = Modifier.height(24.dp))

            // ── Configuração Google Drive ────────────────────────────────────
            DriveStatusBanner(
                configurado = driveConfigurado,
                onConfigurar = { driveFolderLauncher.launch(null) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Ações principais ─────────────────────────────────────────────
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onNovoTalao
            ) {
                Text(text = stringResource(id = R.string.home_novo_talao))
            }
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                onClick = onHistorico
            ) {
                Text(text = stringResource(id = R.string.home_historico))
            }
        }
    }

    if (mostrarBoasVindas) {
        AlertDialog(
            onDismissRequest = viewModel::fecharBoasVindas,
            title = { Text(stringResource(id = R.string.boasvindas_titulo)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(id = R.string.boasvindas_passo1))
                    Text(stringResource(id = R.string.boasvindas_passo2))
                    Text(stringResource(id = R.string.boasvindas_passo3))
                    Text(stringResource(id = R.string.boasvindas_passo4))
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::fecharBoasVindas) {
                    Text(stringResource(id = R.string.boasvindas_entendido))
                }
            }
        )
    }
}

/**
 * Banner de estado da ligação ao Google Drive.
 * Verde quando a pasta está configurada; amarelo quando não está.
 */
@Composable
private fun DriveStatusBanner(
    configurado: Boolean,
    onConfigurar: () -> Unit
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
                Text(
                    text = "Google Drive",
                    style = MaterialTheme.typography.labelMedium,
                    color = corTexto
                )
                Text(
                    text = if (configurado)
                        "Pasta configurada — uploads automáticos"
                    else
                        "Sem pasta configurada — toca em Configurar",
                    style = MaterialTheme.typography.bodySmall,
                    color = corTexto
                )
            }
            TextButton(onClick = onConfigurar) {
                Text(
                    text = if (configurado) "Alterar" else "Configurar",
                    color = corTexto
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    ControleObrasTheme {
        HomeScreen()
    }
}
