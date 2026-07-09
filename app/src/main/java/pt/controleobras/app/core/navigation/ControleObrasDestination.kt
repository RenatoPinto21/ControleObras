package pt.controleobras.app.core.navigation

/**
 * Destinos de navegação da aplicação. Cada novo ecrã principal deve ser
 * adicionado aqui, nunca com strings de rota soltas espalhadas pelo código.
 */
sealed class ControleObrasDestination(val route: String) {
    data object Home : ControleObrasDestination(route = "home")
    data object ReceiptFlowGraph : ControleObrasDestination(route = "receiptFlow")
    data object WorkerForm : ControleObrasDestination(route = "receiptFlow/workerForm")
    data object ReceiptCapture : ControleObrasDestination(route = "receiptFlow/capture")
    data object ReceiptReview : ControleObrasDestination(route = "receiptFlow/review")
    data object ReceiptList : ControleObrasDestination(route = "receiptList")

    data object ReceiptDetail : ControleObrasDestination(route = "receiptDetail/{$ARG_TALAO_ID}") {
        fun buildRoute(talaoId: Long): String = "receiptDetail/$talaoId"
    }

    companion object {
        const val ARG_TALAO_ID = "talaoId"
    }
}
