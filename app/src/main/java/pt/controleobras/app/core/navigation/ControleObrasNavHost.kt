package pt.controleobras.app.core.navigation

import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import pt.controleobras.app.R
import pt.controleobras.app.core.designsystem.theme.IndustrialBorder
import pt.controleobras.app.core.designsystem.theme.IndustrialGlow
import pt.controleobras.app.core.designsystem.theme.IndustrialGlowDim
import pt.controleobras.app.core.designsystem.theme.IndustrialSurface
import pt.controleobras.app.core.designsystem.theme.IndustrialSurface2
import pt.controleobras.app.feature.home.ui.HomeScreen
import pt.controleobras.app.feature.qrscan.ui.QrScanScreen
import pt.controleobras.app.feature.receiptcapture.ui.CameraCaptureScreen
import pt.controleobras.app.feature.receiptdetail.ui.ReceiptDetailScreen
import pt.controleobras.app.feature.receiptflow.viewmodel.ReceiptFlowViewModel
import pt.controleobras.app.feature.receiptlist.ui.ReceiptListScreen
import pt.controleobras.app.feature.receiptreview.ui.ReceiptReviewScreen
import pt.controleobras.app.feature.presencas.ui.PresencasScreen
import pt.controleobras.app.feature.relatorios.ui.RelatoriosScreen
import pt.controleobras.app.feature.workerform.ui.WorkerFormScreen

/**
 * Grafo de navegação raiz.
 *
 * Layout adaptativo:
 *  - Landscape → [NavigationRailIndustrial] lateral esquerdo (como na imagem de referência)
 *  - Portrait  → [BarraNavegacaoIndustrial] na base
 *
 * Os dois modos partilham a mesma lógica de navegação; só muda o contentor visual.
 */
@Composable
fun ControleObrasNavHost(
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val rotaAtual  = backStackEntry?.destination?.route
    val estaEmHome = rotaAtual == ControleObrasDestination.Home.route
    val activity   = LocalContext.current as? Activity
    val configuration = LocalConfiguration.current
    val emLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Lambdas de navegação partilhados
    val onInicio: () -> Unit = {
        navController.navigate(ControleObrasDestination.Home.route) {
            popUpTo(ControleObrasDestination.Home.route) { inclusive = false }
            launchSingleTop = true
        }
    }
    // ── Estado do diálogo de confirmação de saída ─────────────────────────
    // Quando o utilizador está no Home e carrega em "SAIR", mostramos um
    // AlertDialog a pedir confirmação. Sem isto, um toque acidental fecha a app.
    // NOTA: Removemos o android.os.Process.killProcess() que existia aqui.
    //       O killProcess impedia o Android de fazer cleanup correto (ex: uploads
    //       em curso para o Drive eram cortados). O activity?.finish() é suficiente.
    var mostrarDialogoSair by remember { mutableStateOf(false) }

    val onSair: () -> Unit = {
        if (estaEmHome) {
            // No ecrã principal, pedir confirmação antes de fechar
            mostrarDialogoSair = true
        } else {
            // Nos outros ecrãs, apenas recua na pilha de navegação
            navController.popBackStack()
        }
    }
    val onHistorico: () -> Unit = {
        navController.navigate(ControleObrasDestination.ReceiptList.route)
    }
    val onRelatorios: () -> Unit = {
        navController.navigate(ControleObrasDestination.Relatorios.route) {
            launchSingleTop = true
        }
    }
    val onNovoTalao: () -> Unit = {
        navController.navigate(ControleObrasDestination.ReceiptFlowGraph.route)
    }
    val onPresencas: () -> Unit = {
        navController.navigate(ControleObrasDestination.Presencas.route) {
            launchSingleTop = true
        }
    }

    // ── Conteúdo de navegação (partilhado entre landscape e portrait) ────
    // Definido como lambda para ser reutilizado nos dois modos de layout.
    // O NavHost gere a pilha de ecrãs e as transições entre eles.
    val navContent: @Composable (Modifier) -> Unit = { contentModifier ->
        NavHost(
            navController    = navController,
            startDestination = ControleObrasDestination.Home.route,
            modifier         = contentModifier,
            // Transições globais — apenas fade, rápido e leve.
            // Sem slide horizontal: evita jank em tablets com ecrãs pesados
            // (imagens, listas, OCR). O crossfade curto (150ms) é imperceptível
            // mas elimina o flash branco entre ecrãs.
            enterTransition     = { fadeIn(animationSpec = tween(150, easing = LinearEasing)) },
            exitTransition      = { fadeOut(animationSpec = tween(150, easing = LinearEasing)) },
            popEnterTransition  = { fadeIn(animationSpec = tween(150, easing = LinearEasing)) },
            popExitTransition   = { fadeOut(animationSpec = tween(150, easing = LinearEasing)) }
        ) {
            // ── Ecrã Home ────────────────────────────────────────────────────
            composable(ControleObrasDestination.Home.route) {
                HomeScreen(
                    onNovoTalao = onNovoTalao,
                    onHistorico = onHistorico
                )
            }

            // ── Grafo aninhado: Fluxo de digitalização de fatura ─────────────
            // Este grafo partilha o mesmo ReceiptFlowViewModel entre 4 ecrãs:
            //   1. WorkerForm    — formulário do funcionário (funcn + centro de custo)
            //   2. ReceiptCapture — câmara para fotografar o talão
            //   3. ReceiptReview — revisão dos dados extraídos por OCR
            //   4. QrScan        — scanner de QR code AT (opcional)
            // O ViewModel é obtido via hiltViewModel(parentEntry) para garantir
            // que todos os ecrãs acedem à mesma instância (dados partilhados).
            navigation(
                startDestination = ControleObrasDestination.WorkerForm.route,
                route            = ControleObrasDestination.ReceiptFlowGraph.route
            ) {
                composable(ControleObrasDestination.WorkerForm.route) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(ControleObrasDestination.ReceiptFlowGraph.route)
                    }
                    val viewModel: ReceiptFlowViewModel = hiltViewModel(parentEntry)
                    WorkerFormScreen(
                        viewModel   = viewModel,
                        onContinuar = { navController.navigate(ControleObrasDestination.ReceiptCapture.route) },
                        onVoltar    = { navController.popBackStack() }
                    )
                }

                composable(ControleObrasDestination.ReceiptCapture.route) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(ControleObrasDestination.ReceiptFlowGraph.route)
                    }
                    val viewModel: ReceiptFlowViewModel = hiltViewModel(parentEntry)
                    CameraCaptureScreen(
                        viewModel          = viewModel,
                        onImagemProcessada = { navController.navigate(ControleObrasDestination.ReceiptReview.route) }
                    )
                }

                composable(ControleObrasDestination.ReceiptReview.route) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(ControleObrasDestination.ReceiptFlowGraph.route)
                    }
                    val viewModel: ReceiptFlowViewModel = hiltViewModel(parentEntry)
                    ReceiptReviewScreen(
                        viewModel  = viewModel,
                        onGuardado = {
                            navController.navigate(ControleObrasDestination.Home.route) {
                                popUpTo(ControleObrasDestination.Home.route) { inclusive = false }
                            }
                        },
                        onScanQr = { navController.navigate(ControleObrasDestination.QrScan.route) },
                        onVoltar = { navController.popBackStack() }
                    )
                }

                composable(ControleObrasDestination.QrScan.route) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(ControleObrasDestination.ReceiptFlowGraph.route)
                    }
                    val viewModel: ReceiptFlowViewModel = hiltViewModel(parentEntry)
                    QrScanScreen(
                        viewModel     = viewModel,
                        onQrDetectado = { navController.popBackStack() },
                        onVoltar      = { navController.popBackStack() }
                    )
                }
            }

            composable(ControleObrasDestination.Relatorios.route) {
                RelatoriosScreen()
            }

            composable(ControleObrasDestination.Presencas.route) {
                PresencasScreen()
            }

            composable(ControleObrasDestination.ReceiptList.route) {
                ReceiptListScreen(
                    onAbrirTalao = { id ->
                        navController.navigate(ControleObrasDestination.ReceiptDetail.buildRoute(id))
                    },
                    // Botão "Digitalizar primeiro talão" no estado vazio
                    onNovoTalao = onNovoTalao
                )
            }

            composable(
                route     = ControleObrasDestination.ReceiptDetail.route,
                arguments = listOf(
                    navArgument(ControleObrasDestination.ARG_TALAO_ID) { type = NavType.LongType }
                )
            ) {
                ReceiptDetailScreen(onVoltar = { navController.popBackStack() })
            }
        }
    }

    // ── Diálogo de confirmação de saída ─────────────────────────────────────
    // Aparece quando o utilizador carrega em "SAIR" estando no ecrã Home.
    // Dois botões: "Cancelar" (fecha o diálogo) e "Sair" (fecha a app).
    if (mostrarDialogoSair) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoSair = false },
            title = { Text("Sair da aplicação") },
            text  = { Text("Tem a certeza que pretende fechar o Controle Obras?") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarDialogoSair = false
                    // finish() encerra a Activity de forma limpa.
                    // O Android faz o cleanup correto (salva estado, liberta recursos).
                    activity?.finish()
                }) {
                    Text("Sair", color = Color(0xFFFF5252))
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoSair = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (emLandscape) {
        // ── LANDSCAPE: Navigation Rail lateral ───────────────────────────────
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRailIndustrial(
                estaEmHome   = estaEmHome,
                rotaAtual    = rotaAtual,
                onInicio     = onInicio,
                onSair       = onSair,
                onHistorico  = onHistorico,
                onNovoTalao  = onNovoTalao,
                onRelatorios = onRelatorios,
                onPresencas  = onPresencas
            )
            navContent(Modifier.weight(1f))
        }
    } else {
        // ── PORTRAIT: Bottom bar ──────────────────────────────────────────────
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                BarraNavegacaoIndustrial(
                    estaEmHome   = estaEmHome,
                    rotaAtual    = rotaAtual,
                    onInicio     = onInicio,
                    onSair       = onSair,
                    onNovoTalao  = onNovoTalao,
                    onHistorico  = onHistorico,
                    onRelatorios = onRelatorios,
                    onPresencas  = onPresencas
                )
            }
        ) { innerPadding ->
            navContent(Modifier.padding(innerPadding))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Navigation Rail Industrial — landscape
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Rail lateral com tema industrial.
 * Logo no topo, itens de navegação ao centro, botões VOLTAR/FECHAR no fundo.
 * Linha laranja na borda direita — traço de obra.
 */
@Composable
private fun NavigationRailIndustrial(
    estaEmHome:   Boolean,
    rotaAtual:    String?,
    onInicio:     () -> Unit,
    onSair:       () -> Unit,
    onHistorico:  () -> Unit,
    onNovoTalao:  () -> Unit,
    onRelatorios: () -> Unit,
    onPresencas:  () -> Unit
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .fillMaxHeight()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A2028), IndustrialSurface)
                )
            )
            .drawBehind {
                // Linha laranja na borda direita
                drawLine(
                    color       = IndustrialGlow,
                    start       = Offset(size.width, 0f),
                    end         = Offset(size.width, size.height),
                    strokeWidth = 2.dp.toPx()
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // ── Logo / topo ───────────────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Marca da app — logótipo oficial
            Image(
                painter            = painterResource(R.drawable.ic_logo),
                contentDescription = "Controle Obras",
                // Logo com cantos arredondados (8dp para consistência visual)
                modifier           = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            RailDivider()

            // ── Itens de navegação ────────────────────────────────────────────
            RailItem(
                icone     = Icons.Default.Home,
                label     = "INÍCIO",
                ativo     = estaEmHome,
                onClick   = onInicio
            )
            RailItem(
                icone   = Icons.Default.CameraAlt,
                label   = "SCAN",
                ativo   = rotaAtual?.startsWith("receiptFlow") == true,
                onClick = onNovoTalao
            )
            RailItem(
                icone   = Icons.Default.History,
                label   = "REGISTO",
                ativo   = rotaAtual == ControleObrasDestination.ReceiptList.route ||
                          rotaAtual?.startsWith("receiptDetail") == true,
                onClick = onHistorico
            )
            RailItem(
                icone   = Icons.Default.CalendarMonth,
                label   = "RELAT.",
                ativo   = rotaAtual == ControleObrasDestination.Relatorios.route,
                onClick = onRelatorios
            )
            RailItem(
                icone   = Icons.Default.People,
                label   = "PRESENÇ.",
                ativo   = rotaAtual == ControleObrasDestination.Presencas.route,
                onClick = onPresencas
            )
        }

        // ── Botão VOLTAR no fundo ─────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RailDivider()
            IconButton(
                onClick  = onSair,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(IndustrialSurface2)
            ) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (estaEmHome) "Fechar app" else "Voltar",
                    tint               = if (estaEmHome) Color(0xFFFF5252) else IndustrialGlow,
                    modifier           = Modifier.size(20.dp)
                )
            }
            Text(
                text      = if (estaEmHome) "SAIR" else "VOLTAR",
                style     = MaterialTheme.typography.labelSmall,
                color     = if (estaEmHome) Color(0xFFFF5252) else IndustrialGlow.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RailItem(
    icone:   androidx.compose.ui.graphics.vector.ImageVector,
    label:   String,
    ativo:   Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        IconButton(
            onClick  = onClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector        = icone,
                contentDescription = label,
                // Ativo = branco puro; inativo = aço transparente
                tint               = if (ativo) Color.White else Color.White.copy(alpha = 0.35f),
                modifier           = Modifier.size(22.dp)
            )
        }

        // Label
        Text(
            text       = label,
            style      = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color      = if (ativo) Color.White else Color.White.copy(alpha = 0.28f),
            textAlign  = TextAlign.Center,
            fontWeight = if (ativo) FontWeight.SemiBold else FontWeight.Normal
        )

        // Dot indicator — único toque de laranja, discreto
        Box(
            modifier = Modifier
                .size(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (ativo) IndustrialGlow else Color.Transparent)
        )
    }
}

@Composable
private fun RailDivider() {
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(1.dp)
            .background(IndustrialBorder)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom Bar Industrial — portrait
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Barra inferior industrial — portrait.
 * 5 itens: VOLTAR/FECHAR · INÍCIO · SCAN · REGISTO · RELAT.
 * Linha laranja no topo, fundo escuro, ícones com estado ativo.
 */
@Composable
private fun BarraNavegacaoIndustrial(
    estaEmHome:   Boolean,
    rotaAtual:    String?,
    onInicio:     () -> Unit,
    onSair:       () -> Unit,
    onNovoTalao:  () -> Unit,
    onHistorico:  () -> Unit,
    onRelatorios: () -> Unit,
    onPresencas:  () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(IndustrialSurface)
            .drawBehind {
                drawLine(
                    color       = IndustrialGlow,
                    start       = Offset(0f, 0f),
                    end         = Offset(size.width, 0f),
                    strokeWidth = 2.dp.toPx()
                )
            }
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().height(56.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // VOLTAR / FECHAR
            BottomNavItem(
                icone    = Icons.AutoMirrored.Filled.ArrowBack,
                label    = if (estaEmHome) "SAIR" else "VOLTAR",
                ativo    = false,
                corAtiva = if (estaEmHome) Color(0xFFFF5252) else IndustrialGlow,
                onClick  = onSair
            )

            BottomNavItem(
                icone   = Icons.Default.Home,
                label   = "INÍCIO",
                ativo   = estaEmHome,
                onClick = onInicio
            )

            // SCAN — botão central destacado
            Box(
                modifier = Modifier
                    .size(52.dp)
                    // Raio consistente com o resto da app (8dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (rotaAtual?.startsWith("receiptFlow") == true) IndustrialGlow
                        else IndustrialSurface2
                    ),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onNovoTalao) {
                    Icon(
                        imageVector        = Icons.Default.CameraAlt,
                        contentDescription = "SCAN",
                        tint               = Color.White,
                        modifier           = Modifier.size(24.dp)
                    )
                }
            }

            BottomNavItem(
                icone   = Icons.Default.History,
                label   = "REGISTO",
                ativo   = rotaAtual == ControleObrasDestination.ReceiptList.route ||
                          rotaAtual?.startsWith("receiptDetail") == true,
                onClick = onHistorico
            )

            BottomNavItem(
                icone   = Icons.Default.CalendarMonth,
                label   = "RELAT.",
                ativo   = rotaAtual == ControleObrasDestination.Relatorios.route,
                onClick = onRelatorios
            )

            BottomNavItem(
                icone   = Icons.Default.People,
                label   = "PRESENÇ.",
                ativo   = rotaAtual == ControleObrasDestination.Presencas.route,
                onClick = onPresencas
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icone:    androidx.compose.ui.graphics.vector.ImageVector,
    label:    String,
    ativo:    Boolean,
    corAtiva: Color = IndustrialGlow,
    onClick:  () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector        = icone,
                contentDescription = label,
                tint               = if (ativo) Color.White else Color.White.copy(alpha = 0.35f),
                modifier           = Modifier.size(20.dp)
            )
        }
        Text(
            text       = label,
            style      = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
            color      = if (ativo) Color.White else Color.White.copy(alpha = 0.28f),
            textAlign  = TextAlign.Center,
            fontWeight = if (ativo) FontWeight.SemiBold else FontWeight.Normal
        )
        // Dot indicator
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (ativo) corAtiva else Color.Transparent)
        )
    }
}

