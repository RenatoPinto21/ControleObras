package pt.controleobras.app.feature.receiptlist.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pt.controleobras.app.core.designsystem.components.IndustrialHeader
import pt.controleobras.app.core.designsystem.theme.IndustrialBorder
import pt.controleobras.app.core.designsystem.theme.IndustrialGlow
import pt.controleobras.app.core.designsystem.theme.IndustrialGlowDim
import pt.controleobras.app.core.designsystem.theme.IndustrialSteel
import pt.controleobras.app.core.designsystem.theme.IndustrialSurface2
import pt.controleobras.app.core.export.partilharExportacao
import pt.controleobras.app.core.model.Talao
import pt.controleobras.app.feature.receiptlist.viewmodel.ReceiptListViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Ecrã de listagem de talões/faturas guardados.
 *
 * Funcionalidades:
 *  - Barra de pesquisa por empresa, NIF, nº fatura ou observações
 *  - Filtro por data (DatePicker)
 *  - Filtro por centro de custo (chips)
 *  - Estado vazio com botão para digitalizar o primeiro talão
 *
 * @param onAbrirTalao Callback quando o utilizador toca num talão — navega para o detalhe.
 * @param onNovoTalao  Callback para iniciar o fluxo de digitalização.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptListScreen(
    onAbrirTalao: (Long) -> Unit = {},
    onNovoTalao: () -> Unit = {},
    viewModel: ReceiptListViewModel = hiltViewModel()
) {
    val talaes             by viewModel.talaes.collectAsState()
    val termoPesquisa      by viewModel.termoPesquisa.collectAsState()
    val filtroData         by viewModel.filtroData.collectAsState()
    val filtroCentroCusto  by viewModel.filtroCentroCusto.collectAsState()
    val centrosCusto       by viewModel.centrosCustoDisponiveis.collectAsState()

    // Controla visibilidade do DatePicker
    var mostrarDatePicker by remember { mutableStateOf(false) }

    // Controlador do teclado — para fechar ao submeter pesquisa
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        IndustrialHeader(
            titulo    = "Registo de Faturas",
            subtitulo = "${talaes.size} resultado(s)",
            icone     = Icons.Default.History
        )

        // ── Barra de pesquisa ────────────────────────────────────────────
        OutlinedTextField(
            value         = termoPesquisa,
            onValueChange = viewModel::pesquisar,
            modifier      = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder   = { Text("Pesquisar empresa, NIF, nº fatura…", color = IndustrialSteel) },
            leadingIcon   = { Icon(Icons.Default.Search, null, tint = IndustrialSteel) },
            trailingIcon  = {
                if (termoPesquisa.isNotBlank()) {
                    IconButton(onClick = { viewModel.pesquisar("") }) {
                        Icon(Icons.Default.Clear, "Limpar pesquisa", tint = IndustrialSteel)
                    }
                }
            },
            singleLine    = true,
            shape         = RoundedCornerShape(8.dp),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor    = IndustrialGlow,
                unfocusedBorderColor  = IndustrialBorder,
                cursorColor           = IndustrialGlow,
                focusedTextColor      = Color.White,
                unfocusedTextColor    = Color.White
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
        )

        // ── Chips de filtro ──────────────────────────────────────────────
        LazyRow(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Chip de data
            item {
                FilterChip(
                    selected = filtroData != null,
                    onClick  = {
                        if (filtroData != null) {
                            // Se já tem filtro de data, limpa-o
                            viewModel.filtrarPorData(null)
                        } else {
                            mostrarDatePicker = true
                        }
                    },
                    label = {
                        Text(
                            filtroData?.format(
                                DateTimeFormatter.ofPattern("dd/MM/yyyy")
                            ) ?: "Data",
                            color = if (filtroData != null) Color.White else IndustrialSteel
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.CalendarToday,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = if (filtroData != null) Color.White else IndustrialSteel
                        )
                    },
                    trailingIcon = if (filtroData != null) {
                        {
                            Icon(
                                Icons.Default.Clear, null,
                                modifier = Modifier.size(14.dp),
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    } else null,
                    shape  = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor         = IndustrialSurface2,
                        selectedContainerColor = IndustrialGlow,
                        labelColor             = IndustrialSteel,
                        selectedLabelColor     = Color.White
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor         = IndustrialBorder,
                        selectedBorderColor = IndustrialGlow,
                        enabled = true,
                        selected = filtroData != null
                    )
                )
            }

            // Chips de centros de custo (só aparecem se existem CCs distintos)
            items(centrosCusto) { cc ->
                val selecionado = filtroCentroCusto == cc
                FilterChip(
                    selected = selecionado,
                    onClick  = {
                        viewModel.filtrarPorCentroCusto(if (selecionado) null else cc)
                    },
                    label = {
                        Text(
                            cc,
                            color = if (selecionado) Color.White else IndustrialSteel
                        )
                    },
                    shape  = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor         = IndustrialSurface2,
                        selectedContainerColor = IndustrialGlow,
                        labelColor             = IndustrialSteel,
                        selectedLabelColor     = Color.White
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor         = IndustrialBorder,
                        selectedBorderColor = IndustrialGlow,
                        enabled = true,
                        selected = selecionado
                    )
                )
            }

            // Chip "Limpar filtros" — só aparece quando há filtros ativos
            if (filtroData != null || !filtroCentroCusto.isNullOrBlank() || termoPesquisa.isNotBlank()) {
                item {
                    FilterChip(
                        selected = false,
                        onClick  = viewModel::limparFiltros,
                        label    = { Text("Limpar tudo", color = Color(0xFFEF5350)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Clear, null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFEF5350)
                            )
                        },
                        shape  = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = IndustrialSurface2
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color(0xFFEF5350).copy(alpha = 0.4f),
                            enabled = true,
                            selected = false
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Lista de resultados ou estado vazio ──────────────────────────
        if (talaes.isEmpty()) {
            // Verificar se é vazio por filtros ou realmente sem dados
            val temFiltros = termoPesquisa.isNotBlank() || filtroData != null || !filtroCentroCusto.isNullOrBlank()
            Box(
                modifier         = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(IndustrialGlowDim),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (temFiltros) Icons.Default.Search else Icons.Default.History,
                            contentDescription = null,
                            tint     = IndustrialGlow,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (temFiltros) "Sem resultados" else "Sem registos",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                    Text(
                        if (temFiltros)
                            "Tente ajustar os filtros de pesquisa."
                        else
                            "Ainda não existem registos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = IndustrialSteel
                    )
                    // Botão de ação — apenas no estado vazio sem filtros
                    if (!temFiltros) {
                        Spacer(Modifier.height(16.dp))
                        androidx.compose.material3.OutlinedButton(
                            onClick = onNovoTalao,
                            shape   = RoundedCornerShape(8.dp),
                            colors  = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                contentColor = IndustrialGlow
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, IndustrialGlow)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Digitalizar primeiro talão")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding      = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
            ) {
                items(talaes, key = { it.id }) { talao ->
                    TalaoCard(talao = talao, onAbrir = { onAbrirTalao(talao.id) })
                }
            }
        }
    }

    // ── DatePicker dialog ────────────────────────────────────────────────
    if (mostrarDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { mostrarDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarDatePicker = false
                        datePickerState.selectedDateMillis?.let { millis ->
                            val data = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            viewModel.filtrarPorData(data)
                        }
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Componentes privados
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Card individual de um talão na lista.
 *
 * Estrutura visual:
 *  [Linha laranja esquerda] [Inicial da empresa] [Nome + Data + Total + CC] [Exportar] [Seta]
 *
 * A linha laranja na borda esquerda é desenhada manualmente com drawBehind
 * para manter o design industrial da app.
 */
@Composable
private fun TalaoCard(talao: Talao, onAbrir: () -> Unit) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(IndustrialSurface2)
            .drawBehind {
                // Borda esquerda laranja
                drawLine(
                    color       = IndustrialGlow,
                    start       = Offset(0f, 0f),
                    end         = Offset(0f, size.height),
                    strokeWidth = 3.dp.toPx()
                )
            }
            .clickable(onClick = onAbrir)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ícone de empresa
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(IndustrialGlowDim),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = talao.empresa.take(1).uppercase(),
                style      = MaterialTheme.typography.titleMedium,
                color      = IndustrialGlow,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(12.dp))

        // Dados principais
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = talao.empresa.ifBlank { "Empresa desconhecida" },
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color      = Color.White
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                talao.data?.let {
                    Text(it.toString(), style = MaterialTheme.typography.bodySmall, color = IndustrialSteel)
                }
                talao.total?.let {
                    Text(
                        "${it.toPlainString()} €",
                        style      = MaterialTheme.typography.bodySmall,
                        color      = IndustrialGlow,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            // Mostrar centro de custo se existir — ajuda na identificação rápida
            if (talao.nmfref.isNotBlank()) {
                Text(
                    talao.nmfref,
                    style = MaterialTheme.typography.labelSmall,
                    color = IndustrialSteel.copy(alpha = 0.7f)
                )
            }
        }

        // Ação de exportar + navegar
        IconButton(onClick = { partilharExportacao(context, talao.id, "xml") }) {
            Icon(
                Icons.Default.IosShare,
                contentDescription = "Exportar",
                tint     = IndustrialSteel,
                modifier = Modifier.size(18.dp)
            )
        }
        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint     = IndustrialBorder,
            modifier = Modifier.size(20.dp)
        )
    }
}
