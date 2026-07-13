package pt.controleobras.app.feature.workerform.ui

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import pt.controleobras.app.core.designsystem.components.IndustrialHeader
import pt.controleobras.app.core.designsystem.theme.IndustrialBorder
import pt.controleobras.app.core.designsystem.theme.IndustrialGlow
import pt.controleobras.app.core.designsystem.theme.IndustrialGlowDim
import pt.controleobras.app.core.designsystem.theme.IndustrialSteel
import pt.controleobras.app.core.designsystem.theme.IndustrialSurface2
import pt.controleobras.app.core.model.WorkerFormData
import pt.controleobras.app.feature.receiptflow.viewmodel.ReceiptFlowViewModel

/**
 * Ecrã de preenchimento obrigatório antes de fotografar o talão.
 *
 * Aproveita o tempo de preenchimento para pedir permissão de localização
 * — assim o GPS já está disponível quando o utilizador tirar a foto.
 */
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

    // Nº Funcionário: apenas dígitos
    val funcnFormatoInvalido = funcn.isNotBlank() && !funcn.all { it.isDigit() }
    // Centro de Custo e Observações: letras, números, espaços e pontuação básica
    val ccnomeFormatoInvalido = ccnome.isNotBlank() && ccnome.any {
        !it.isLetterOrDigit() && !it.isWhitespace()
    }
    val funobsFormatoInvalido = funobs.isNotBlank() && funobs.any {
        !it.isLetterOrDigit() && !it.isWhitespace() && it !in listOf('.', ',', '-', ':', '/')
    }

    val funcnErro  = tentouSubmeter && (funcn.isBlank() || funcnFormatoInvalido)
    val ccnomeErro = tentouSubmeter && (ccnome.isBlank() || ccnomeFormatoInvalido)
    val funobsErro = tentouSubmeter && (funobs.isBlank() || funobsFormatoInvalido)

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* GPS verificado internamente pelo FusedLocationProvider */ }

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

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // ─── Cabeçalho industrial ─────────────────────────────────────────────
        IndustrialHeader(
            titulo    = "Identificação",
            subtitulo = "Passo 1 de 2 — quem está a registar?",
            icone     = Icons.Default.Person
        )

        // ─── Formulário ───────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Aviso contextual — tema industrial
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(IndustrialGlowDim)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector        = Icons.Default.Info,
                    contentDescription = null,
                    tint               = IndustrialGlow,
                    modifier           = Modifier.size(18.dp)
                )
                Text(
                    text  = "Estes dados ficam associados ao registo da fatura para rastreabilidade.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            val textFieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = IndustrialGlow,
                unfocusedBorderColor = IndustrialBorder,
                focusedLabelColor    = IndustrialGlow,
                unfocusedLabelColor  = IndustrialSteel,
                focusedLeadingIconColor   = IndustrialGlow,
                unfocusedLeadingIconColor = IndustrialSteel,
                cursorColor          = IndustrialGlow,
                focusedTextColor     = Color.White,
                unfocusedTextColor   = Color.White,
                focusedContainerColor   = IndustrialSurface2,
                unfocusedContainerColor = IndustrialSurface2
            )

            OutlinedTextField(
                value = funcn,
                onValueChange = { novo ->
                    if (novo.all { it.isDigit() }) funcn = novo
                },
                label          = { Text("Nº Funcionário *") },
                placeholder    = { Text("Ex: 1023") },
                isError        = funcnErro,
                supportingText = when {
                    funcnErro && funcn.isBlank()     -> ({ Text("Campo obrigatório") })
                    funcnErro && funcnFormatoInvalido -> ({ Text("Apenas números são permitidos") })
                    else                              -> null
                },
                leadingIcon = {
                    Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction    = ImeAction.Next
                ),
                singleLine = true,
                shape      = RoundedCornerShape(12.dp),
                colors     = textFieldColors,
                modifier   = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = ccnome,
                onValueChange = { novo ->
                    if (novo.all { it.isLetterOrDigit() || it.isWhitespace() }) ccnome = novo
                },
                label          = { Text("Centro de Custo *") },
                placeholder    = { Text("Ex: Obra Lisboa Norte") },
                isError        = ccnomeErro,
                supportingText = when {
                    ccnomeErro && ccnome.isBlank()      -> ({ Text("Campo obrigatório") })
                    ccnomeErro && ccnomeFormatoInvalido -> ({ Text("Apenas letras, números e espaços") })
                    else                                -> null
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType   = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction      = ImeAction.Next
                ),
                singleLine = true,
                shape      = RoundedCornerShape(12.dp),
                colors     = textFieldColors,
                modifier   = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = funobs,
                onValueChange = { novo ->
                    if (novo.all { it.isLetterOrDigit() || it.isWhitespace() || it in listOf('.', ',', '-', ':', '/') }) {
                        funobs = novo
                    }
                },
                label          = { Text("Observações *") },
                placeholder    = { Text("Ex: Compra de material elétrico") },
                isError        = funobsErro,
                supportingText = when {
                    funobsErro && funobs.isBlank()      -> ({ Text("Campo obrigatório") })
                    funobsErro && funobsFormatoInvalido -> ({ Text("Apenas letras, números e pontuação básica") })
                    else                                -> null
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType   = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction      = ImeAction.Done
                ),
                minLines = 2,
                maxLines = 4,
                shape    = RoundedCornerShape(12.dp),
                colors   = textFieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                onClick = {
                    tentouSubmeter = true
                    val formularioValido = funcn.isNotBlank() && !funcnFormatoInvalido &&
                        ccnome.isNotBlank() && !ccnomeFormatoInvalido &&
                        funobs.isNotBlank() && !funobsFormatoInvalido
                    if (formularioValido) {
                        viewModel.definirFormulario(
                            WorkerFormData(
                                funcn  = funcn.trim(),
                                ccnome = ccnome.trim(),
                                funobs = funobs.trim()
                            )
                        )
                        onContinuar()
                    }
                }
            ) {
                Text(
                    "Continuar para fotografar",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
