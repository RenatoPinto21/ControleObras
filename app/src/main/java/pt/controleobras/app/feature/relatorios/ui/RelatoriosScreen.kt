package pt.controleobras.app.feature.relatorios.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import pt.controleobras.app.core.designsystem.theme.IndustrialBorder
import pt.controleobras.app.core.designsystem.theme.IndustrialGlow
import pt.controleobras.app.core.designsystem.theme.IndustrialGlowDim
import pt.controleobras.app.core.designsystem.theme.IndustrialSteel
import pt.controleobras.app.core.designsystem.theme.IndustrialSurface
import pt.controleobras.app.core.designsystem.theme.IndustrialSurface2
import pt.controleobras.app.core.relatorios.export.RelatorioExporter
import pt.controleobras.app.core.model.CentroCusto
import pt.controleobras.app.core.relatorios.model.DiaResumo
import pt.controleobras.app.core.relatorios.model.LinhaDespesa
import pt.controleobras.app.core.relatorios.model.LinhaPresencaReg
import pt.controleobras.app.core.relatorios.model.RelatorioDespesas
import pt.controleobras.app.core.relatorios.model.RelatorioPresencasReg
import pt.controleobras.app.feature.relatorios.viewmodel.PainelAtivo
import pt.controleobras.app.feature.relatorios.viewmodel.RelatoriosViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun RelatoriosScreen(
    viewModel: RelatoriosViewModel = hiltViewModel()
) {
    val context  = LocalContext.current
    val uiState  by viewModel.uiState.collectAsState()
    val exporter = RelatorioExporter()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        RelatoriosHeader(
            aCarregar = uiState.aCarregar,
            onRefresh = viewModel::refreshDiaAtual
        )

        // ── Calendário + Painel ───────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxSize()) {

            // Coluna esquerda: calendário
            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight()
                    .background(IndustrialSurface)
                    .drawBehind {
                        drawLine(
                            color       = IndustrialBorder,
                            start       = Offset(size.width, 0f),
                            end         = Offset(size.width, size.height),
                            strokeWidth = 1f
                        )
                    }
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NavegacaoMes(
                    mes        = uiState.mesSelecionado,
                    onAnterior = { viewModel.selecionarMes(uiState.mesSelecionado.minusMonths(1)) },
                    onSeguinte = { viewModel.selecionarMes(uiState.mesSelecionado.plusMonths(1)) }
                )
                CalendarioIndustrial(
                    mes           = uiState.mesSelecionado,
                    diaSelecionado = uiState.diaSelecionado,
                    resumosDias   = uiState.resumosDias,
                    onDiaClick    = viewModel::selecionarDia
                )
                Legenda()
            }

            // Coluna direita: painel de relatório
            Column(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                uiState.diaSelecionado?.let { dia ->
                    // Cards de seleção de painel
                    Row(
                        modifier            = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CardPainel(
                            titulo   = "DESPESAS",
                            valor    = uiState.despesas?.linhas?.size?.toString() ?: "0",
                            subValor = uiState.despesas?.totalGeral?.let { "%.2f €".format(it) } ?: "—",
                            ativo    = uiState.painelAtivo == PainelAtivo.DESPESAS,
                            corPonto = IndustrialGlow,
                            onClick  = { viewModel.selecionarPainel(PainelAtivo.DESPESAS) },
                            modifier = Modifier.weight(1f)
                        )
                        CardPainel(
                            titulo   = "PRESENÇAS",
                            valor    = uiState.presencasReg?.linhas?.size?.toString() ?: "0",
                            subValor = "${uiState.presencasReg?.linhas?.size ?: 0} funcionários",
                            ativo    = uiState.painelAtivo == PainelAtivo.PRESENCAS,
                            corPonto = Color.White,
                            onClick  = { viewModel.selecionarPainel(PainelAtivo.PRESENCAS) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Lista de detalhe
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(IndustrialSurface2)
                            .border(1.dp, IndustrialBorder, RoundedCornerShape(8.dp))
                    ) {
                        if (uiState.aCarregar) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center).size(24.dp),
                                color    = IndustrialGlow,
                                strokeWidth = 2.dp
                            )
                        } else when (uiState.painelAtivo) {
                            PainelAtivo.DESPESAS  -> PainelDespesas(uiState.despesas)
                            PainelAtivo.PRESENCAS -> PainelPresencasReg(
                                relatorio    = uiState.presencasReg,
                                centrosCusto = uiState.centrosCusto,
                                ccFiltro     = uiState.ccFiltro,
                                onFiltrarCC  = viewModel::filtrarPresencasPorCC
                            )
                        }
                    }

                    // Botões exportar
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BotaoExportar(
                            label    = "PDF",
                            icone    = Icons.Default.PictureAsPdf,
                            modifier = Modifier.weight(1f),
                            onClick  = {
                                when (uiState.painelAtivo) {
                                    PainelAtivo.DESPESAS  -> uiState.despesas?.let     { exporter.exportarDespesasPdf(context, it) }
                                    PainelAtivo.PRESENCAS -> uiState.presencasReg?.let { exporter.exportarPresencasRegPdf(context, it) }
                                }
                            }
                        )
                        BotaoExportar(
                            label    = "CSV",
                            icone    = Icons.Default.Download,
                            modifier = Modifier.weight(1f),
                            onClick  = {
                                when (uiState.painelAtivo) {
                                    PainelAtivo.DESPESAS  -> uiState.despesas?.let     { exporter.exportarDespesasCsv(context, it) }
                                    PainelAtivo.PRESENCAS -> uiState.presencasReg?.let { exporter.exportarPresencasRegCsv(context, it) }
                                }
                            }
                        )
                    }
                } ?: run {
                    // Nenhum dia selecionado
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Selecione um dia no calendário",
                            style = MaterialTheme.typography.bodyMedium,
                            color = IndustrialSteel
                        )
                    }
                }
            }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun RelatoriosHeader(
    aCarregar: Boolean,
    onRefresh: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "refresh_spin")
    val rotacao by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing)),
        label         = "refresh_angle"
    )

    // Botão refresh como ação do header partilhado
    val botaoRefresh: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(IndustrialSurface2)
                .border(
                    width = 1.dp,
                    color = if (aCarregar) IndustrialGlow else IndustrialBorder,
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable(enabled = !aCarregar, onClick = onRefresh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Refresh,
                contentDescription = "Atualizar",
                tint               = if (aCarregar) IndustrialGlow else Color.White.copy(alpha = 0.55f),
                modifier           = Modifier
                    .size(18.dp)
                    .rotate(if (aCarregar) rotacao else 0f)
            )
        }
    }

    pt.controleobras.app.core.designsystem.components.IndustrialHeader(
        titulo    = "Relatórios",
        subtitulo = "Despesas e presenças por dia",
        acoes     = botaoRefresh
    )
}

// ── Navegação de Mês ──────────────────────────────────────────────────────────

@Composable
private fun NavegacaoMes(
    mes: YearMonth,
    onAnterior: () -> Unit,
    onSeguinte: () -> Unit
) {
    val nomeMes = mes.month.getDisplayName(TextStyle.FULL, Locale("pt", "PT"))
        .replaceFirstChar { it.uppercase() }

    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick  = onAnterior,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(IndustrialSurface2)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Mês anterior",
                tint     = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text       = "$nomeMes ${mes.year}",
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color      = Color.White
        )
        IconButton(
            onClick  = onSeguinte,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(IndustrialSurface2)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Mês seguinte",
                tint     = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── Calendário ────────────────────────────────────────────────────────────────

@Composable
private fun CalendarioIndustrial(
    mes: YearMonth,
    diaSelecionado: LocalDate?,
    resumosDias: Map<LocalDate, DiaResumo>,
    onDiaClick: (LocalDate) -> Unit
) {
    val hoje       = LocalDate.now()
    val primeiroDia = mes.atDay(1)
    // Quantos dias em branco antes do dia 1 (Seg=0, Dom=6)
    val offsetInicio = (primeiroDia.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val totalDias   = mes.lengthOfMonth()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Cabeçalho dias da semana
        val diasSemana = listOf("S", "T", "Q", "Q", "S", "S", "D")
        Row(modifier = Modifier.fillMaxWidth()) {
            diasSemana.forEach { d ->
                Text(
                    text      = d,
                    modifier  = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style     = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color     = IndustrialSteel,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Grade de dias
        val totalCelulas = offsetInicio + totalDias
        val semanas = (totalCelulas + 6) / 7

        repeat(semanas) { semana ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                repeat(7) { coluna ->
                    val posicao  = semana * 7 + coluna
                    val numeroDia = posicao - offsetInicio + 1

                    if (numeroDia < 1 || numeroDia > totalDias) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val data      = mes.atDay(numeroDia)
                        val resumo    = resumosDias[data]
                        val selecionado = data == diaSelecionado
                        val ehHoje    = data == hoje

                        CelulaCalendario(
                            dia          = numeroDia,
                            selecionado  = selecionado,
                            ehHoje       = ehHoje,
                            temDespesas  = resumo?.temDespesas == true,
                            temPresencas = resumo?.temPresencas == true,
                            onClick      = { onDiaClick(data) },
                            modifier     = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CelulaCalendario(
    dia: Int,
    selecionado: Boolean,
    ehHoje: Boolean,
    temDespesas: Boolean,
    temPresencas: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val corFundo = when {
        selecionado -> IndustrialGlow
        else        -> Color.Transparent
    }
    val corTexto = when {
        selecionado -> Color.White
        ehHoje      -> IndustrialGlow
        else        -> Color.White.copy(alpha = 0.85f)
    }
    val bordaModifier = if (ehHoje && !selecionado)
        Modifier.border(1.dp, IndustrialGlow.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
    else Modifier

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(corFundo)
            .then(bordaModifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text      = dia.toString(),
                style     = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color     = corTexto,
                fontWeight = if (selecionado || ehHoje) FontWeight.Bold else FontWeight.Normal
            )
            // Indicadores
            if (temDespesas || temPresencas) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (temDespesas) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (selecionado) Color.White else IndustrialGlow)
                        )
                    }
                    if (temPresencas) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (selecionado) Color.White.copy(0.7f) else Color.White.copy(0.6f))
                        )
                    }
                }
            }
        }
    }
}

// ── Legenda ───────────────────────────────────────────────────────────────────

@Composable
private fun Legenda() {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(IndustrialGlow))
            Text("Despesas", style = MaterialTheme.typography.labelSmall, color = IndustrialSteel)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(Color.White.copy(0.6f)))
            Text("Presenças", style = MaterialTheme.typography.labelSmall, color = IndustrialSteel)
        }
    }
}

// ── Cards de Painel ───────────────────────────────────────────────────────────

@Composable
private fun CardPainel(
    titulo:   String,
    valor:    String,
    subValor: String,
    ativo:    Boolean,
    corPonto: Color,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
) {
    val corBorda = if (ativo) IndustrialGlow.copy(alpha = 0.7f) else IndustrialBorder
    val corFundo = if (ativo) IndustrialGlowDim else IndustrialSurface2

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(corFundo)
            .border(1.dp, corBorda, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Label + indicador
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(if (ativo) corPonto else IndustrialSteel)
            )
            Text(
                text       = titulo,
                style      = MaterialTheme.typography.labelSmall,
                color      = if (ativo) IndustrialGlow else IndustrialSteel,
                fontWeight = FontWeight.Bold
            )
        }
        // Valor principal
        Text(
            text       = valor,
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color      = if (ativo) Color.White else Color.White.copy(alpha = 0.6f)
        )
        // Subvalor
        Text(
            text  = subValor,
            style = MaterialTheme.typography.labelSmall,
            color = if (ativo) IndustrialSteel else IndustrialSteel.copy(alpha = 0.6f)
        )
    }
}

// ── Painéis de detalhe ────────────────────────────────────────────────────────

@Composable
private fun PainelDespesas(relatorio: RelatorioDespesas?) {
    if (relatorio == null || relatorio.linhas.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Sem despesas neste dia",
                style = MaterialTheme.typography.bodySmall,
                color = IndustrialSteel
            )
        }
        return
    }

    LazyColumn(
        modifier            = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        contentPadding      = PaddingValues(vertical = 8.dp)
    ) {
        // Cabeçalho
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF111820))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Hora",    Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall, color = IndustrialGlow, fontWeight = FontWeight.Bold)
                Text("Empresa", Modifier.weight(1f),   style = MaterialTheme.typography.labelSmall, color = IndustrialGlow, fontWeight = FontWeight.Bold)
                Text("Obra",    Modifier.width(50.dp), style = MaterialTheme.typography.labelSmall, color = IndustrialGlow, fontWeight = FontWeight.Bold)
                Text("Total",   Modifier.width(60.dp), style = MaterialTheme.typography.labelSmall, color = IndustrialGlow, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            }
        }
        items(relatorio.linhas) { linha ->
            LinhaDespesaItem(linha)
        }
        // Rodapé total
        item {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(IndustrialGlowDim)
                    .border(1.dp, IndustrialGlow.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "TOTAL",
                    style      = MaterialTheme.typography.labelSmall,
                    color      = IndustrialGlow,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "%.2f €".format(relatorio.totalGeral),
                    style      = MaterialTheme.typography.labelSmall,
                    color      = IndustrialGlow,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LinhaDespesaItem(linha: LinhaDespesa) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(IndustrialSurface2.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            linha.hora,
            modifier = Modifier.width(40.dp),
            style    = MaterialTheme.typography.labelSmall,
            color    = IndustrialSteel
        )
        Text(
            linha.empresa,
            modifier = Modifier.weight(1f),
            style    = MaterialTheme.typography.bodySmall,
            color    = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            linha.fref.ifBlank { "—" },
            modifier = Modifier.width(50.dp),
            style    = MaterialTheme.typography.labelSmall,
            color    = IndustrialSteel
        )
        Text(
            "%.2f €".format(linha.total),
            modifier   = Modifier.width(60.dp),
            style      = MaterialTheme.typography.labelSmall,
            color      = Color.White,
            fontWeight = FontWeight.SemiBold,
            textAlign  = TextAlign.End
        )
    }
}

/**
 * Painel de presenças registadas (SUBFUNC_REG).
 * Mostra nome do funcionário em destaque, hora de registo e observações.
 * Botão minimalista de filtro por centro de custo.
 */
@Composable
private fun PainelPresencasReg(
    relatorio:    RelatorioPresencasReg?,
    centrosCusto: List<CentroCusto>,
    ccFiltro:     String?,
    onFiltrarCC:  (String?) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // ── Barra de filtro CC (minimalista) ─────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Label do filtro ativo
            Text(
                text  = if (ccFiltro != null) {
                    val nome = centrosCusto.find { it.fref == ccFiltro }?.nmfref ?: ccFiltro
                    "CC: $ccFiltro — $nome"
                } else "Todos os centros de custo",
                style    = MaterialTheme.typography.labelSmall,
                color    = if (ccFiltro != null) IndustrialGlow else IndustrialSteel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Botão filtro
            Box {
                var menuAberto by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (ccFiltro != null) IndustrialGlowDim else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (ccFiltro != null) IndustrialGlow.copy(alpha = 0.5f) else IndustrialBorder,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { menuAberto = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.FilterList,
                        contentDescription = "Filtrar por CC",
                        tint               = if (ccFiltro != null) IndustrialGlow else IndustrialSteel,
                        modifier           = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded         = menuAberto,
                    onDismissRequest = { menuAberto = false }
                ) {
                    // Opção "Todos"
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Todos",
                                fontWeight = if (ccFiltro == null) FontWeight.Bold else FontWeight.Normal,
                                color      = if (ccFiltro == null) IndustrialGlow else Color.White
                            )
                        },
                        onClick = {
                            onFiltrarCC(null)
                            menuAberto = false
                        }
                    )
                    HorizontalDivider(color = IndustrialBorder)
                    // Lista de CCs
                    centrosCusto.forEach { cc ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "${cc.fref} — ${cc.nmfref}",
                                    fontWeight = if (ccFiltro == cc.fref) FontWeight.Bold else FontWeight.Normal,
                                    color      = if (ccFiltro == cc.fref) IndustrialGlow else Color.White,
                                    maxLines   = 1,
                                    overflow   = TextOverflow.Ellipsis
                                )
                            },
                            onClick = {
                                onFiltrarCC(cc.fref)
                                menuAberto = false
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = IndustrialBorder, thickness = 0.5.dp)

        // ── Lista de presenças ───────────────────────────────────────────────
        if (relatorio == null || relatorio.linhas.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Sem presenças registadas neste dia",
                    style = MaterialTheme.typography.bodySmall,
                    color = IndustrialSteel
                )
            }
            return
        }

        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
            contentPadding      = PaddingValues(vertical = 8.dp)
        ) {
            // Cabeçalho
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF111820))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Nome",  Modifier.weight(1f),    style = MaterialTheme.typography.labelSmall, color = IndustrialGlow, fontWeight = FontWeight.Bold)
                    Text("Função", Modifier.weight(0.6f), style = MaterialTheme.typography.labelSmall, color = IndustrialGlow, fontWeight = FontWeight.Bold)
                    Text("Hora",  Modifier.width(44.dp),  style = MaterialTheme.typography.labelSmall, color = IndustrialGlow, fontWeight = FontWeight.Bold)
                    Text("Obs",   Modifier.weight(0.5f),  style = MaterialTheme.typography.labelSmall, color = IndustrialGlow, fontWeight = FontWeight.Bold)
                }
            }
            items(relatorio.linhas) { linha ->
                LinhaPresencaRegItem(linha)
            }
            // Rodapé
            item {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF111820).copy(alpha = 0.6f))
                        .border(1.dp, IndustrialBorder, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        "${relatorio.linhas.size} funcionário(s) presente(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = IndustrialSteel
                    )
                }
            }
        }
    }
}

@Composable
private fun LinhaPresencaRegItem(linha: LinhaPresencaReg) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(IndustrialSurface2.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Nome — em destaque
        Text(
            linha.nome,
            modifier   = Modifier.weight(1f),
            style      = MaterialTheme.typography.bodyMedium,
            color      = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis
        )
        // Função / Designação
        Text(
            linha.designacao.ifBlank { "—" },
            modifier = Modifier.weight(0.6f),
            style    = MaterialTheme.typography.labelSmall,
            color    = IndustrialSteel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // Hora
        Text(
            linha.hora.ifBlank { "—" },
            modifier = Modifier.width(44.dp),
            style    = MaterialTheme.typography.labelSmall,
            color    = IndustrialGlow
        )
        // Observações
        Text(
            linha.obs.ifBlank { "—" },
            modifier = Modifier.weight(0.5f),
            style    = MaterialTheme.typography.labelSmall,
            color    = IndustrialSteel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── Botão Exportar ────────────────────────────────────────────────────────────

@Composable
private fun BotaoExportar(
    label:    String,
    icone:    androidx.compose.ui.graphics.vector.ImageVector,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick  = onClick,
        modifier = modifier,
        shape    = RoundedCornerShape(8.dp),
        border   = androidx.compose.foundation.BorderStroke(1.dp, IndustrialGlow.copy(alpha = 0.6f)),
        colors   = ButtonDefaults.outlinedButtonColors(contentColor = IndustrialGlow)
    ) {
        Icon(icone, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}
