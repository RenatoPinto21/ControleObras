package pt.controleobras.app.feature.workerform.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import pt.controleobras.app.core.model.CentroCusto
import pt.controleobras.app.core.model.WorkerFormData
import pt.controleobras.app.feature.receiptflow.viewmodel.ReceiptFlowViewModel

/**
 * Ecrã de preenchimento obrigatório antes de fotografar o talão.
 *
 * O utilizador seleciona o Centro de Custo (obra) a partir de um dropdown
 * alimentado pela cache local do Room (sincronizada com MariaDB ao abrir a app).
 * O Encarregado é preenchido automaticamente com base no AGNOME do CC selecionado.
 *
 * Aproveita o tempo de preenchimento para pedir permissão de localização.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerFormScreen(
    viewModel: ReceiptFlowViewModel,
    onContinuar: () -> Unit,
    onVoltar: () -> Unit = {}
) {
    val context = LocalContext.current

    // ─── Estado do formulário ─────────────────────────────────────────────────
    var funcn                   by remember { mutableStateOf("") }
    var centroCustoSelecionado  by remember { mutableStateOf<CentroCusto?>(null) }
    var dropdownExpanded        by remember { mutableStateOf(false) }
    var funobs                  by remember { mutableStateOf("") }
    var tentouSubmeter          by remember { mutableStateOf(false) }
    var mostrarDialogoDescartar by remember { mutableStateOf(false) }

    // ── BackHandler — protege contra perda de dados ao carregar "Voltar" ─────
    // Se o utilizador preencheu algum campo, mostra diálogo de confirmação.
    // Se o formulário está vazio, volta diretamente sem perguntar.
    val temDadosPreenchidos = funcn.isNotBlank() || centroCustoSelecionado != null || funobs.isNotBlank()

    BackHandler(enabled = true) {
        if (temDadosPreenchidos) {
            mostrarDialogoDescartar = true
        } else {
            onVoltar()
        }
    }

    // Lista de centros de custo da cache Room
    val centroCustos by viewModel.centroCustos.collectAsState()

    // ─── Validações ───────────────────────────────────────────────────────────
    val funcnFormatoInvalido = funcn.isNotBlank() && !funcn.all { it.isDigit() }
    val funobsFormatoInvalido = funobs.isNotBlank() && funobs.any {
        !it.isLetterOrDigit() && !it.isWhitespace() && it !in listOf('.', ',', '-', ':', '/')
    }

    val funcnErro = tentouSubmeter && (funcn.isBlank() || funcnFormatoInvalido)
    val ccErro    = tentouSubmeter && centroCustoSelecionado == null
    // Observações é opcional — só mostra erro se o utilizador escreveu algo com formato inválido
    val funobsErro = tentouSubmeter && funobs.isNotBlank() && funobsFormatoInvalido

    // ─── Permissões antecipadas ───────────────────────────────────────────────
    val permissaoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* resultado tratado internamente por cada provider */ }

    LaunchedEffect(Unit) {
        val pendentes = buildList {
            val fineGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarseGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!fineGranted && !coarseGranted) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            // READ_PHONE_STATE removido — DeviceInfo usa ANDROID_ID (sem permissão)
        }
        if (pendentes.isNotEmpty()) {
            permissaoLauncher.launch(pendentes.toTypedArray())
        }
    }

    // ─── Diálogo de confirmação ao carregar "Voltar" ───────────────────────────
    if (mostrarDialogoDescartar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoDescartar = false },
            title = {
                Text(
                    text       = "Descartar dados?",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text  = "Os dados preenchidos neste formulário serão perdidos.",
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
                    Text("Continuar a preencher")
                }
            }
        )
    }

    // ─── UI ───────────────────────────────────────────────────────────────────
    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {

        IndustrialHeader(
            titulo    = "Identificação",
            subtitulo = "Passo 1 de 2 — quem está a registar?",
            icone     = Icons.Default.Person
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Aviso contextual
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
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
                focusedBorderColor        = IndustrialGlow,
                unfocusedBorderColor      = IndustrialBorder,
                focusedLabelColor         = IndustrialGlow,
                unfocusedLabelColor       = IndustrialSteel,
                focusedLeadingIconColor   = IndustrialGlow,
                unfocusedLeadingIconColor = IndustrialSteel,
                cursorColor               = IndustrialGlow,
                focusedTextColor          = Color.White,
                unfocusedTextColor        = Color.White,
                focusedContainerColor     = IndustrialSurface2,
                unfocusedContainerColor   = IndustrialSurface2,
                errorBorderColor          = MaterialTheme.colorScheme.error,
                errorLabelColor           = MaterialTheme.colorScheme.error,
                errorLeadingIconColor     = MaterialTheme.colorScheme.error
            )

            // ── Nº Funcionário ────────────────────────────────────────────────
            OutlinedTextField(
                value         = funcn,
                onValueChange = { novo -> if (novo.all { it.isDigit() }) funcn = novo },
                label          = { Text("Nº Funcionário *") },
                placeholder    = { Text("Ex: 1023") },
                isError        = funcnErro,
                supportingText = when {
                    funcnErro && funcn.isBlank()      -> ({ Text("Campo obrigatório") })
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
                shape      = RoundedCornerShape(8.dp),
                colors     = textFieldColors,
                modifier   = Modifier.fillMaxWidth()
            )

            // ── Centro de Custo — dropdown ────────────────────────────────────
            ExposedDropdownMenuBox(
                expanded         = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value         = centroCustoSelecionado?.labelDropdown ?: "",
                    onValueChange = {},
                    readOnly      = true,
                    label         = { Text("Centro de Custo *") },
                    placeholder   = { Text(if (centroCustos.isEmpty()) "A sincronizar..." else "Selecione a obra") },
                    isError       = ccErro,
                    supportingText = if (ccErro) ({ Text("Selecione um centro de custo") }) else null,
                    leadingIcon = {
                        Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                    },
                    singleLine = true,
                    shape      = RoundedCornerShape(8.dp),
                    colors     = textFieldColors,
                    modifier   = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )

                ExposedDropdownMenu(
                    expanded         = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier         = Modifier.background(IndustrialSurface2)
                ) {
                    if (centroCustos.isEmpty()) {
                        DropdownMenuItem(
                            text    = { Text("Sem dados — verifique a ligação", color = IndustrialSteel) },
                            onClick = { dropdownExpanded = false }
                        )
                    } else {
                        centroCustos.forEach { cc ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text  = cc.labelDropdown,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White
                                        )
                                        if (cc.agnome.isNotBlank()) {
                                            Text(
                                                text = cc.agnome,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = IndustrialSteel
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    centroCustoSelecionado = cc
                                    dropdownExpanded       = false
                                }
                            )
                        }
                    }
                }
            }

            // ── Encarregado (só leitura — preenchido automaticamente) ─────────
            // Mostra o estado do campo AGNOME na BD para diagnóstico rápido:
            //  - Sem CC selecionado → sem mensagem
            //  - AGNOME vazio/nulo  → aviso amarelo "vazio na BD"
            //  - AGNOME preenchido  → info verde "preenchido na BD"
            val estadoEncarregado = centroCustoSelecionado?.let { cc ->
                if (cc.agnome.isBlank()) "⚠ Campo encarregado vazio ou não atribuído"
                else "✓ AGNOME preenchido na BD: \"${cc.agnome}\""
            }
            val corEstadoEncarregado = centroCustoSelecionado?.let { cc ->
                if (cc.agnome.isBlank()) Color(0xFFFFB300) else Color(0xFF4CAF50)
            } ?: IndustrialSteel

            OutlinedTextField(
                value         = centroCustoSelecionado?.agnome ?: "",
                onValueChange = {},
                readOnly      = true,
                enabled       = false,
                label         = { Text("Encarregado") },
                placeholder   = { Text("Preenchido automaticamente") },
                supportingText = estadoEncarregado?.let { msg ->
                    { Text(text = msg, color = corEstadoEncarregado) }
                },
                leadingIcon = {
                    Icon(Icons.Default.SupervisorAccount, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                singleLine = true,
                shape      = RoundedCornerShape(8.dp),
                colors     = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor      = if (centroCustoSelecionado != null && centroCustoSelecionado!!.agnome.isBlank())
                        Color(0xFFFFB300).copy(alpha = 0.5f) else IndustrialBorder,
                    disabledLabelColor       = IndustrialSteel,
                    disabledLeadingIconColor = IndustrialSteel,
                    disabledTextColor        = Color.White.copy(alpha = 0.6f),
                    disabledContainerColor   = IndustrialSurface2.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // ── Observações ───────────────────────────────────────────────────
            OutlinedTextField(
                value         = funobs,
                onValueChange = { novo ->
                    if (novo.all { it.isLetterOrDigit() || it.isWhitespace() || it in listOf('.', ',', '-', ':', '/') }) {
                        funobs = novo
                    }
                },
                // Campo opcional — o asterisco (*) foi removido
                label          = { Text("Observações") },
                placeholder    = { Text("Ex: Compra de material elétrico (opcional)") },
                isError        = funobsErro,
                supportingText = when {
                    funobsErro -> ({ Text("Apenas letras, números e pontuação básica") })
                    else       -> null
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType   = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction      = ImeAction.Done
                ),
                minLines = 2,
                maxLines = 4,
                shape    = RoundedCornerShape(8.dp),
                colors   = textFieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape  = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                onClick = {
                    tentouSubmeter = true
                    // Observações é opcional — basta que funcn e centro de custo estejam válidos.
                    // Se funobs estiver preenchido, verifica-se o formato (letras, números, pontuação básica).
                    val formularioValido = funcn.isNotBlank() && !funcnFormatoInvalido &&
                        centroCustoSelecionado != null &&
                        !funobsFormatoInvalido

                    if (formularioValido) {
                        viewModel.definirFormulario(
                            WorkerFormData(
                                funcn        = funcn,
                                centroCusto  = centroCustoSelecionado,
                                funobs       = funobs
                            )
                        )
                        onContinuar()
                    }
                }
            ) {
                Text(
                    text       = "Continuar para fotografia",
                    style      = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
