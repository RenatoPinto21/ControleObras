package pt.controleobras.app.feature.receiptlist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import pt.controleobras.app.core.model.Talao
import pt.controleobras.app.data.repository.TalaoRepository
import javax.inject.Inject

@HiltViewModel
class ReceiptListViewModel @Inject constructor(
    talaoRepository: TalaoRepository
) : ViewModel() {

    val talaes: StateFlow<List<Talao>> = talaoRepository.observarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
