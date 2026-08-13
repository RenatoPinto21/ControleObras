package pt.controleobras.app.feature.presencas.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import pt.controleobras.app.core.designsystem.components.IndustrialHeader
import pt.controleobras.app.core.designsystem.theme.IndustrialBorder
import pt.controleobras.app.core.designsystem.theme.IndustrialGlow
import pt.controleobras.app.core.designsystem.theme.IndustrialGlowDim
import pt.controleobras.app.core.designsystem.theme.IndustrialSteel
import pt.controleobras.app.core.designsystem.theme.IndustrialSurface2
import pt.controleobras.app.core.model.CentroCusto
import pt.controleobras.app.core.model.Funcionario
import pt.controleobras.app.feature.presencas.viewmodel.PresencasViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PresencasScreen(
    viewModel: PresencasViewModel = hiltViewModel()
) {
    val centrosCusto by viewModel.centrosCusto.collectAsState()
    val funcionarios by viewModel.funcionarios.collectAsState()
    val presentes    by viewModel.presentes.collectAsState()
    val carregando   by viewModel.carregando.collectAsState()
    val enviando     by viewModel.enviando.collectAsState()
    val mensagem     by viewModel.mensagem.collectAsState()
    val observacoes    by viewModel.observacoes.collectAsState()
    val obsPorEmpresa  by viewModel.obsPorEmpresa.collectAsState()

    var ccExpandido        by remember { mutableStateOf<String?>(null) }
    var empresasExpandidas by remember { mutableStateOf(emptySet<String>()) }
    var termoPesquisa      by remember { mutableStateOf("") }
    var mostrarResumo      by remember { mutableStateOf(false) }

    // Funcionários filtrados pela pesquisa
    val funcionariosFiltrados by remember(funcionarios, termoPesquisa) {
        derivedStateOf {
            if (termoPesquisa.isBlank()) funcionarios
            else {
                val termo = termoPesquisa.lowercase()
                funcionarios.filter {
                    it.nome.lowercase().contains(termo) ||
                    it.designacao.lowercase().contains(termo)
                }
            }
        }
    }

    // Agrupados por empresa (NOME = entidade/empresa no PHC SUBFUNC)
    // Ordenação: empresas com funcionários selecionados primeiro, depois alfabético
    val grupos by remember(funcionariosFiltrados, presentes) {
        derivedStateOf {
            funcionariosFiltrados.groupBy {
                it.nome.ifBlank { "Sem empresa" }
            }.entries.sortedWith(
                compareByDescending<Map.Entry<String, List<Funcionario>>> { (_, membros) ->
                    membros.count { presentes.contains(it.bistamp) }
                }.thenBy { it.key }
            ).associate { it.toPair() }
        }
    }

    // Pesquisa activa → auto-expandir empresas com resultados
    LaunchedEffect(termoPesquisa, grupos) {
        if (termoPesquisa.isNotBlank()) {
            empresasExpandidas = grupos.keys.toSet()
        }
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor   = IndustrialGlow,
        unfocusedBorderColor = IndustrialBorder,
        cursorColor          = IndustrialGlow,
        focusedTextColor     = Color.White,
        unfocusedTextColor   = Color.White,
        focusedContainerColor   = IndustrialSurface2,
        unfocusedContainerColor = IndustrialSurface2,
        focusedLabelColor    = IndustrialGlow,
        unfocusedLabelColor  = IndustrialSteel
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            IndustrialHeader(
                titulo    = "Presenças",
                subtitulo = "Selecione a obra para registar presenças",
                icone     = Icons.Default.People
            )

            // ── Lista de centros de custo (accordion) ────────────────────
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (centrosCusto.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color       = IndustrialGlow,
                                    modifier    = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text  = "A carregar centros de custo…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = IndustrialSteel
                                )
                            }
                        }
                    }
                }

                items(
                    items = centrosCusto,
                    key   = { it.fref }
                ) { cc ->
                    val isExpandido = ccExpandido == cc.fref.trim()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(IndustrialSurface2)
                    ) {
                        // ── Cabeçalho do CC (sempre visível) ────────────
                        CentroCustoHeader(
                            cc             = cc,
                            isExpandido    = isExpandido,
                            presentes      = if (isExpandido) presentes.size else 0,
                            total          = if (isExpandido) funcionarios.size else 0,
                            totalEmpresas  = if (isExpandido) grupos.size else 0,
                            onClick     = {
                                val fref = cc.fref.trim()
                                if (isExpandido) {
                                    ccExpandido = null
                                } else {
                                    ccExpandido        = fref
                                    empresasExpandidas = emptySet()
                                    termoPesquisa      = ""
                                    viewModel.selecionarCentroCusto(fref)
                                }
                            }
                        )

                        // ── Conteúdo expandido ──────────────────────────
                        AnimatedVisibility(
                            visible = isExpandido,
                            enter   = expandVertically() + fadeIn(),
                            exit    = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.padding(
                                    start  = 12.dp,
                                    end    = 12.dp,
                                    bottom = 12.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (carregando) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color       = IndustrialGlow,
                                            modifier    = Modifier.size(22.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                } else if (funcionarios.isEmpty()) {
                                    Text(
                                        text     = "Sem funcionários nesta obra",
                                        style    = MaterialTheme.typography.bodySmall,
                                        color    = IndustrialSteel,
                                        modifier = Modifier.padding(vertical = 16.dp)
                                    )
                                } else {
                                    // Barra pesquisa + contador
                                    BarraPesquisa(
                                        termo       = termoPesquisa,
                                        onTermoChange = { termoPesquisa = it },
                                        presentes   = presentes.size,
                                        total       = funcionarios.size,
                                        onTodos     = { viewModel.selecionarTodos(funcionariosFiltrados) },
                                        onLimpar    = { viewModel.limparSelecao() }
                                    )

                                    // Expandir / Colapsar todas
                                    val todasExpandidas = empresasExpandidas.size == grupos.size
                                            && grupos.isNotEmpty()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text(
                                            text  = if (todasExpandidas) "Colapsar todas"
                                                    else "Expandir todas",
                                            style = MaterialTheme.typography.labelSmall
                                                .copy(fontSize = 10.sp),
                                            color = IndustrialGlow.copy(alpha = 0.7f),
                                            modifier = Modifier
                                                .clickable {
                                                    empresasExpandidas = if (todasExpandidas)
                                                        emptySet() else grupos.keys.toSet()
                                                }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }

                                    // Funcionários agrupados por empresa (accordion)
                                    grupos.forEach { (empresa, membros) ->
                                        val isEmpresaAberta = empresa in empresasExpandidas
                                        val presentesGrupo  = membros.count {
                                            presentes.contains(it.bistamp)
                                        }
                                        val isCompleta = presentesGrupo == membros.size
                                                && membros.isNotEmpty()

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isEmpresaAberta)
                                                        IndustrialGlow.copy(alpha = 0.05f)
                                                    else
                                                        MaterialTheme.colorScheme.background
                                                            .copy(alpha = 0.3f)
                                                )
                                        ) {
                                            EmpresaHeader(
                                                designacao     = empresa,
                                                presentesGrupo = presentesGrupo,
                                                totalGrupo     = membros.size,
                                                isExpandida    = isEmpresaAberta,
                                                isCompleta     = isCompleta,
                                                onClick        = {
                                                    empresasExpandidas = if (isEmpresaAberta)
                                                        empresasExpandidas - empresa
                                                    else
                                                        empresasExpandidas + empresa
                                                },
                                                onToggleGrupo  = {
                                                    if (isCompleta) {
                                                        membros.forEach {
                                                            viewModel.alternarPresenca(it.bistamp)
                                                        }
                                                    } else {
                                                        membros
                                                            .filter {
                                                                !presentes.contains(it.bistamp)
                                                            }
                                                            .forEach {
                                                                viewModel.alternarPresenca(
                                                                    it.bistamp
                                                                )
                                                            }
                                                    }
                                                }
                                            )

                                            AnimatedVisibility(
                                                visible = isEmpresaAberta,
                                                enter   = expandVertically() + fadeIn(),
                                                exit    = shrinkVertically() + fadeOut()
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(
                                                        start  = 8.dp,
                                                        end    = 8.dp,
                                                        bottom = 6.dp
                                                    )
                                                ) {
                                                    membros.forEach { func ->
                                                        FuncionarioItem(
                                                            funcionario = func,
                                                            isPresente  = presentes.contains(
                                                                func.bistamp
                                                            ),
                                                            onClick     = {
                                                                viewModel.alternarPresenca(
                                                                    func.bistamp
                                                                )
                                                            }
                                                        )
                                                    }

                                                    // Observações por empresa
                                                    OutlinedTextField(
                                                        value = obsPorEmpresa[empresa] ?: "",
                                                        onValueChange = {
                                                            viewModel.atualizarObsEmpresa(
                                                                empresa, it
                                                            )
                                                        },
                                                        placeholder = {
                                                            Text(
                                                                "Obs. desta empresa",
                                                                fontSize = 11.sp
                                                            )
                                                        },
                                                        shape      = RoundedCornerShape(4.dp),
                                                        colors     = textFieldColors,
                                                        singleLine = true,
                                                        modifier   = Modifier
                                                            .fillMaxWidth()
                                                            .height(38.dp)
                                                            .padding(top = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Observações globais
                                    OutlinedTextField(
                                        value         = observacoes,
                                        onValueChange = { viewModel.atualizarObservacoes(it) },
                                        placeholder   = {
                                            Text("Observações gerais (opcional)",
                                                fontSize = 12.sp)
                                        },
                                        shape      = RoundedCornerShape(6.dp),
                                        colors     = textFieldColors,
                                        singleLine = true,
                                        modifier   = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp)
                                    )

                                    // Botão registar → abre resumo
                                    Button(
                                        onClick = { mostrarResumo = true },
                                        enabled = presentes.isNotEmpty() && !enviando,
                                        shape   = RoundedCornerShape(6.dp),
                                        colors  = ButtonDefaults.buttonColors(
                                            containerColor         = IndustrialGlow,
                                            contentColor           = Color.Black,
                                            disabledContainerColor = IndustrialBorder,
                                            disabledContentColor   = IndustrialSteel
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                    ) {
                                        if (enviando) {
                                            CircularProgressIndicator(
                                                color       = Color.Black,
                                                modifier    = Modifier.size(14.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Icon(
                                            imageVector        = Icons.Default.Save,
                                            contentDescription = null,
                                            modifier           = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text       = if (enviando) "A registar…"
                                                         else "Registar (${presentes.size})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize   = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Espaço final para não ficar colado ao fundo
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }

        // ── Diálogo de resumo antes de registar ──────────────────────────
        if (mostrarResumo) {
            ResumoRegistoDialog(
                funcionarios  = funcionarios,
                presentes     = presentes,
                observacoes   = observacoes,
                obsPorEmpresa = obsPorEmpresa,
                onConfirmar   = {
                    mostrarResumo = false
                    viewModel.registarPresencas(funcionarios)
                },
                onCancelar    = { mostrarResumo = false }
            )
        }

        // ── Snackbar ─────────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = mensagem != null,
            enter    = fadeIn(),
            exit     = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            mensagem?.let { msg ->
                Snackbar(
                    containerColor = IndustrialSurface2,
                    contentColor   = Color.White,
                    action = {
                        TextButton(onClick = { viewModel.limparMensagem() }) {
                            Text("OK", color = IndustrialGlow)
                        }
                    }
                ) { Text(msg) }
            }
        }
    }
}

// ─── Composables privados ────────────────────────────────────────────────────

/** Cabeçalho de um centro de custo — sempre visível, clicável para expandir. */
@Composable
private fun CentroCustoHeader(
    cc: CentroCusto,
    isExpandido: Boolean,
    presentes: Int,
    total: Int,
    totalEmpresas: Int = 0,
    onClick: () -> Unit
) {
    val rotacao by animateFloatAsState(
        targetValue = if (isExpandido) 180f else 0f,
        label       = "arrow"
    )
    val corFundo by animateColorAsState(
        targetValue = if (isExpandido) IndustrialGlow.copy(alpha = 0.08f)
                      else Color.Transparent,
        label       = "bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(corFundo)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Info da obra
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = cc.nmfref,
                style      = MaterialTheme.typography.bodyMedium,
                color      = if (isExpandido) Color.White else Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text  = cc.fref.trim(),
                    style = MaterialTheme.typography.labelSmall,
                    color = IndustrialSteel
                )
                if (cc.agnome.isNotBlank()) {
                    Text(
                        text     = cc.agnome,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = IndustrialSteel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Badge empresas + presentes (só quando expandido e com dados)
        if (isExpandido && total > 0) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text       = "$presentes/$total",
                    style      = MaterialTheme.typography.labelMedium,
                    color      = IndustrialGlow,
                    fontWeight = FontWeight.Bold
                )
                if (totalEmpresas > 0) {
                    Text(
                        text  = "$totalEmpresas emp.",
                        style = MaterialTheme.typography.labelSmall
                            .copy(fontSize = 9.sp),
                        color = IndustrialSteel
                    )
                }
            }
        }

        // Seta
        Icon(
            imageVector        = Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpandido) "Fechar" else "Abrir",
            tint               = IndustrialSteel,
            modifier           = Modifier
                .size(20.dp)
                .rotate(rotacao)
        )
    }
}

/** Barra compacta: pesquisa + contador + ações rápidas. */
@Composable
private fun BarraPesquisa(
    termo: String,
    onTermoChange: (String) -> Unit,
    presentes: Int,
    total: Int,
    onTodos: () -> Unit,
    onLimpar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint     = IndustrialSteel,
            modifier = Modifier.size(14.dp)
        )

        BasicTextField(
            value         = termo,
            onValueChange = onTermoChange,
            singleLine    = true,
            textStyle     = TextStyle(color = Color.White, fontSize = 12.sp),
            cursorBrush   = SolidColor(IndustrialGlow),
            decorationBox = { inner ->
                Box(modifier = Modifier.weight(1f)) {
                    if (termo.isEmpty()) {
                        Text(
                            text  = "Pesquisar…",
                            style = MaterialTheme.typography.labelSmall,
                            color = IndustrialSteel
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier.weight(1f)
        )

        if (termo.isNotEmpty()) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Limpar",
                tint     = IndustrialSteel,
                modifier = Modifier
                    .size(12.dp)
                    .clickable { onTermoChange("") }
            )
        }

        // Separador
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(16.dp)
                .background(IndustrialBorder)
        )

        // Ações
        Text(
            text     = "Todos",
            style    = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color    = IndustrialGlow,
            modifier = Modifier
                .clickable(onClick = onTodos)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Text(
            text     = "Limpar",
            style    = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color    = IndustrialSteel,
            modifier = Modifier
                .clickable(onClick = onLimpar)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

/** Cabeçalho de empresa — accordion expandível com seta e toggle Todos/Limpar. */
@Composable
private fun EmpresaHeader(
    designacao: String,
    presentesGrupo: Int,
    totalGrupo: Int,
    isExpandida: Boolean,
    isCompleta: Boolean = false,
    onClick: () -> Unit,
    onToggleGrupo: () -> Unit
) {
    // Verde quando empresa completa (todos marcados)
    val corCompleta = Color(0xFF4CAF50)

    val rotacao by animateFloatAsState(
        targetValue = if (isExpandida) 180f else 0f,
        label       = "empresaArrow"
    )
    val corFundo by animateColorAsState(
        targetValue = when {
            isCompleta  -> corCompleta.copy(alpha = 0.10f)
            isExpandida -> IndustrialGlow.copy(alpha = 0.06f)
            else        -> Color.Transparent
        },
        label       = "empresaBg"
    )
    val corAccent = if (isCompleta) corCompleta else IndustrialGlow

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(corFundo)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Ícone: check se completa, seta se não
        Icon(
            imageVector = if (isCompleta) Icons.Default.CheckCircle
                          else Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpandida) "Fechar" else "Abrir",
            tint               = if (isCompleta) corCompleta
                                 else if (isExpandida) IndustrialGlow
                                 else IndustrialSteel,
            modifier           = Modifier
                .size(18.dp)
                .then(if (!isCompleta) Modifier.rotate(rotacao) else Modifier)
        )

        // Nome da empresa
        Text(
            text       = designacao,
            style      = MaterialTheme.typography.bodySmall,
            color      = if (isExpandida || isCompleta) Color.White
                         else Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.SemiBold,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
            modifier   = Modifier.weight(1f)
        )

        // Badge presentes/total
        Text(
            text       = "$presentesGrupo/$totalGrupo",
            style      = MaterialTheme.typography.labelSmall,
            color      = if (presentesGrupo > 0) corAccent else IndustrialSteel,
            fontWeight = FontWeight.Bold
        )

        // Toggle Todos/Limpar
        Text(
            text     = if (isCompleta) "Limpar" else "Todos",
            style    = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color    = corAccent.copy(alpha = 0.7f),
            modifier = Modifier
                .clickable(onClick = onToggleGrupo)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

/** Item de funcionário — compacto, click para toggle. */
@Composable
private fun FuncionarioItem(
    funcionario: Funcionario,
    isPresente: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .background(
                if (isPresente) IndustrialGlow.copy(alpha = 0.08f)
                else Color.Transparent
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isPresente) Icons.Default.CheckCircle
                          else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint     = if (isPresente) IndustrialGlow else IndustrialBorder,
            modifier = Modifier.size(18.dp)
        )

        Text(
            text       = funcionario.designacao.ifBlank { funcionario.nome },
            style      = MaterialTheme.typography.bodySmall,
            color      = if (isPresente) Color.White else Color.White.copy(alpha = 0.7f),
            fontWeight = if (isPresente) FontWeight.Medium else FontWeight.Normal,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
            modifier   = Modifier.weight(1f)
        )
    }
}

/** Diálogo de confirmação — resumo dos selecionados agrupados por empresa. */
@Composable
private fun ResumoRegistoDialog(
    funcionarios: List<Funcionario>,
    presentes: Set<String>,
    observacoes: String,
    obsPorEmpresa: Map<String, String>,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    val selecionados = funcionarios.filter { presentes.contains(it.bistamp) }
    val porEmpresa   = selecionados.groupBy { it.nome.ifBlank { "Sem empresa" } }
        .toSortedMap()

    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor   = IndustrialSurface2,
        titleContentColor = Color.White,
        textContentColor  = Color.White,
        title = {
            Text(
                text       = "Confirmar registo (${selecionados.size})",
                fontWeight = FontWeight.Bold,
                fontSize   = 14.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                porEmpresa.forEach { (empresa, membros) ->
                    // Nome da empresa
                    Text(
                        text       = empresa,
                        style      = MaterialTheme.typography.labelSmall,
                        color      = IndustrialGlow,
                        fontWeight = FontWeight.Bold
                    )
                    // Funcionários
                    membros.forEach { func ->
                        Text(
                            text     = "  • ${func.designacao.ifBlank { func.nome }}",
                            style    = MaterialTheme.typography.bodySmall,
                            color    = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // Obs da empresa (se existir)
                    val obsEmp = obsPorEmpresa[empresa]
                    if (!obsEmp.isNullOrBlank()) {
                        Text(
                            text  = "  Obs: $obsEmp",
                            style = MaterialTheme.typography.labelSmall,
                            color = IndustrialSteel
                        )
                    }
                }

                // Obs globais
                if (observacoes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text       = "Obs. gerais: $observacoes",
                        style      = MaterialTheme.typography.bodySmall,
                        color      = IndustrialSteel,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text("Registar", color = IndustrialGlow, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar", color = IndustrialSteel)
            }
        }
    )
}
