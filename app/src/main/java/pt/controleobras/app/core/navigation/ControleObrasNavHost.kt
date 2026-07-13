package pt.controleobras.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pt.controleobras.app.feature.home.ui.HomeScreen
import pt.controleobras.app.feature.qrscan.ui.QrScanScreen
import pt.controleobras.app.feature.receiptcapture.ui.CameraCaptureScreen
import pt.controleobras.app.feature.receiptdetail.ui.ReceiptDetailScreen
import pt.controleobras.app.feature.receiptflow.viewmodel.ReceiptFlowViewModel
import pt.controleobras.app.feature.receiptlist.ui.ReceiptListScreen
import pt.controleobras.app.feature.receiptreview.ui.ReceiptReviewScreen
import pt.controleobras.app.feature.workerform.ui.WorkerFormScreen

/**
 * Grafo de navegação raiz da aplicação.
 *
 * Fluxo de talão (grafo aninhado partilhado):
 *   WorkerForm → ReceiptCapture → ReceiptReview
 *
 * O [ReceiptFlowViewModel] é partilhado pelos três ecrãs do grafo aninhado,
 * garantindo que o formulário do funcionário, os metadados da captura e o
 * draft OCR/QR sobrevivem à navegação entre ecrãs.
 */
@Composable
fun ControleObrasNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = ControleObrasDestination.Home.route
    ) {
        composable(ControleObrasDestination.Home.route) {
            HomeScreen(
                onNovoTalao = {
                    navController.navigate(ControleObrasDestination.ReceiptFlowGraph.route)
                },
                onHistorico = { navController.navigate(ControleObrasDestination.ReceiptList.route) }
            )
        }

        navigation(
            startDestination = ControleObrasDestination.WorkerForm.route,
            route = ControleObrasDestination.ReceiptFlowGraph.route
        ) {
            // 1. Formulário do funcionário (obrigatório)
            composable(ControleObrasDestination.WorkerForm.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(ControleObrasDestination.ReceiptFlowGraph.route)
                }
                val viewModel: ReceiptFlowViewModel = hiltViewModel(parentEntry)
                WorkerFormScreen(
                    viewModel = viewModel,
                    onContinuar = {
                        navController.navigate(ControleObrasDestination.ReceiptCapture.route)
                    }
                )
            }

            // 2. Captura de imagem
            composable(ControleObrasDestination.ReceiptCapture.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(ControleObrasDestination.ReceiptFlowGraph.route)
                }
                val viewModel: ReceiptFlowViewModel = hiltViewModel(parentEntry)
                CameraCaptureScreen(
                    viewModel = viewModel,
                    onImagemProcessada = {
                        navController.navigate(ControleObrasDestination.ReceiptReview.route)
                    }
                )
            }

            // 3. Revisão dos dados extraídos
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
                    onScanQr   = {
                        navController.navigate(ControleObrasDestination.QrScan.route)
                    }
                )
            }

            // 4. Scan de QR code AT (sem tirar nova fotografia)
            composable(ControleObrasDestination.QrScan.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(ControleObrasDestination.ReceiptFlowGraph.route)
                }
                val viewModel: ReceiptFlowViewModel = hiltViewModel(parentEntry)
                QrScanScreen(
                    viewModel      = viewModel,
                    onQrDetectado  = { navController.popBackStack() },
                    onVoltar       = { navController.popBackStack() }
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
            route = ControleObrasDestination.ReceiptDetail.route,
            arguments = listOf(
                navArgument(ControleObrasDestination.ARG_TALAO_ID) { type = NavType.LongType }
            )
        ) {
            ReceiptDetailScreen(onVoltar = { navController.popBackStack() })
        }
    }
}
