package pt.controleobras.app.core.navigation

import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    val onSair: () -> Unit = {
        if (estaEmHome) activity?.finish()
        else navController.popBackStack()
    }
    val onHistorico: () -> Unit = {
        navController.navigate(ControleObrasDestination.ReceiptList.route)
    }
    val onNovoTalao: () -> Unit = {
        navController.navigate(ControleObrasDestination.ReceiptFlowGraph.route)
    }

    val navContent: @Composable (Modifier) -> Unit = { contentModifier ->
        NavHost(
            navController    = navController,
            startDestination = ControleObrasDestination.Home.route,
            modifier         = contentModifier
        ) {
            composable(ControleObrasDestination.Home.route) {
                HomeScreen(
                    onNovoTalao = onNovoTalao,
                    onHistorico = onHistorico
                )
            }

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
                        onContinuar = { navController.navigate(ControleObrasDestination.ReceiptCapture.route) }
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
                        onScanQr = { navController.navigate(ControleObrasDestination.QrScan.route) }
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

            composable(ControleObrasDestination.ReceiptList.route) {
                ReceiptListScreen(
                    onAbrirTalao = { id ->
                        navController.navigate(ControleObrasDestination.ReceiptDetail.buildRoute(id))
                    }
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

    if (emLandscape) {
        // ── LANDSCAPE: Navigation Rail lateral ───────────────────────────────
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRailIndustrial(
                estaEmHome  = estaEmHome,
                rotaAtual   = rotaAtual,
                onInicio    = onInicio,
                onSair      = onSair,
                onHistorico = onHistorico,
                onNovoTalao = onNovoTalao
            )
            navContent(Modifier.weight(1f))
        }
    } else {
        // ── PORTRAIT: Bottom bar ──────────────────────────────────────────────
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                BarraNavegacaoIndustrial(
                    estaEmHome = estaEmHome,
                    onInicio   = onInicio,
                    onSair     = onSair
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
    estaEmHome:  Boolean,
    rotaAtual:   String?,
    onInicio:    () -> Unit,
    onSair:      () -> Unit,
    onHistorico: () -> Unit,
    onNovoTalao: () -> Unit
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
            // Logo: iniciais "CO" em fundo laranja — simples, sem gradiente
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(IndustrialGlow),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text      = "CO",
                    style     = MaterialTheme.typography.labelLarge,
                    color     = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

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
                    .clip(RoundedCornerShape(10.dp))
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
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (ativo) IndustrialGlowDim else Color.Transparent)
                .then(if (!ativo) Modifier else Modifier),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onClick) {
                Icon(
                    imageVector        = icone,
                    contentDescription = label,
                    tint               = if (ativo) IndustrialGlow else Color.White.copy(alpha = 0.45f),
                    modifier           = Modifier.size(22.dp)
                )
            }
        }
        Text(
            text      = label,
            style     = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color     = if (ativo) IndustrialGlow else Color.White.copy(alpha = 0.35f),
            textAlign = TextAlign.Center,
            fontWeight = if (ativo) FontWeight.Bold else FontWeight.Normal
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
 * Barra inferior industrial com dois botões grandes.
 * Fundo escuro com linha laranja no topo.
 */
@Composable
private fun BarraNavegacaoIndustrial(
    estaEmHome: Boolean,
    onInicio:   () -> Unit,
    onSair:     () -> Unit
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
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // VOLTAR / FECHAR
            OutlinedButton(
                onClick  = onSair,
                modifier = Modifier.weight(1f).height(52.dp),
                shape    = RoundedCornerShape(10.dp),
                border   = BorderStroke(1.5.dp, IndustrialBorder)
            ) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint               = if (estaEmHome) Color(0xFFFF5252) else Color.White,
                    modifier           = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text      = if (estaEmHome) "Fechar" else "Voltar",
                    style     = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color     = if (estaEmHome) Color(0xFFFF5252) else Color.White
                )
            }

            // INÍCIO
            Button(
                onClick  = onInicio,
                modifier = Modifier.weight(1f).height(52.dp),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor          = if (estaEmHome) IndustrialSurface2 else IndustrialGlow,
                    contentColor            = Color.White,
                    disabledContainerColor  = IndustrialSurface2,
                    disabledContentColor    = Color.White.copy(alpha = 0.3f)
                ),
                enabled = !estaEmHome
            ) {
                Icon(Icons.Default.Home, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text      = "Início",
                    style     = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

