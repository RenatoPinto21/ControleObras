package pt.controleobras.app.feature.workerform.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import pt.controleobras.app.core.model.WorkerFormData
import pt.controleobras.app.feature.receiptflow.viewmodel.ReceiptFlowViewModel

/**
 * Ecrã de preenchimento obrigatório antes de fotografar o talão.
 *
 * Aproveita o tempo de preenchimento para pedir permissão de localização
 * — assim o GPS já está disponível quando o utilizador tirar a foto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerFormScreen(
    viewModel: ReceiptFlowViewModel,
    onContinuar: () -> Unit
) {
    val context = LocalContext.current
    var funcn by remember { mutableStateOf("") }
    var ccnome by remember { mutableStateOf("") }
    var funobs by remember { mutableStateOf("") }
    var tentouSubmeter by remember { mutableStateOf(false) }

    val funcnErro = tentouSubmeter && funcn.isBlank()
    val ccnomeErro = tentouSubmeter && ccnome.isBlank()
    val funobsErro = tentouSubmeter && funobs.isBlank()

    // Pedir permissão de localização assim que o ecrã abre.
    // Isto dá tempo ao GPS de aquecer enquanto o utilizador preenche o formulário.
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* resultado ignorado — FusedLocationProvider verifica internamente */ }

    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Identificação") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Preenche os campos antes de fotografar a fatura.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = funcn,
                onValueChange = { funcn = it },
                label = { Text("Nº Funcionário *") },
                placeholder = { Text("Ex: 1023") },
                isError = funcnErro,
                supportingText = if (funcnErro) ({ Text("Campo obrigatório") }) else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = ccnome,
                onValueChange = { ccnome = it },
                label = { Text("Centro de Custo *") },
                placeholder = { Text("Ex: Obra Lisboa Norte") },
                isError = ccnomeErro,
                supportingText = if (ccnomeErro) ({ Text("Campo obrigatório") }) else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = funobs,
                onValueChange = { funobs = it },
                label = { Text("Observações *") },
                placeholder = { Text("Ex: Compra de material") },
                isError = funobsErro,
                supportingText = if (funobsErro) ({ Text("Campo obrigatório") }) else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    tentouSubmeter = true
                    if (funcn.isNotBlank() && ccnome.isNotBlank() && funobs.isNotBlank()) {
                        viewModel.definirFormulario(
                            WorkerFormData(
                                funcn = funcn.trim(),
                                ccnome = ccnome.trim(),
                                funobs = funobs.trim()
                            )
                        )
                        onContinuar()
                    }
                }
            ) {
                Text("Continuar para fotografar")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
