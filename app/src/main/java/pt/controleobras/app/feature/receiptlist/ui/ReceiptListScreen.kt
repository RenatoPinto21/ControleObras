package pt.controleobras.app.feature.receiptlist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pt.controleobras.app.core.export.partilharExportacao
import pt.controleobras.app.core.model.Talao
import pt.controleobras.app.feature.receiptlist.viewmodel.ReceiptListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptListScreen(
    onAbrirTalao: (Long) -> Unit = {},
    viewModel: ReceiptListViewModel = hiltViewModel()
) {
    val talaes by viewModel.talaes.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Histórico de talões") }) }
    ) { innerPadding ->
        if (talaes.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Ainda não há talões guardados.")
                Text("Volta ao ecrã inicial e toca em \"Novo talão\" para criar o primeiro.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("Toca num talão para ver o detalhe. Usa o ícone para partilhar/exportar rapidamente.")
                }
                items(talaes, key = { it.id }) { talao ->
                    TalaoCard(talao = talao, onAbrir = { onAbrirTalao(talao.id) })
                }
            }
        }
    }
}

@Composable
private fun TalaoCard(talao: Talao, onAbrir: () -> Unit) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onAbrir)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = talao.empresa, style = MaterialTheme.typography.titleMedium)
                talao.data?.let { Text(text = it.toString()) }
                talao.total?.let { Text(text = "Total: ${it.toPlainString()} €") }
            }
            IconButton(onClick = { partilharExportacao(context, talao.id, "xml") }) {
                Icon(Icons.Filled.Share, contentDescription = "Exportar talão")
            }
        }
    }
}
