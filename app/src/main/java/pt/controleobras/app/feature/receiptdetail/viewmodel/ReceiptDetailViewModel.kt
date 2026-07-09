package pt.controleobras.app.feature.receiptdetail.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.controleobras.app.core.model.Talao
import pt.controleobras.app.core.navigation.ControleObrasDestination
import pt.controleobras.app.data.repository.TalaoRepository
import javax.inject.Inject

@HiltViewModel
class ReceiptDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val talaoRepository: TalaoRepository
) : ViewModel() {

    private val talaoId: Long =
        checkNotNull(savedStateHandle[ControleObrasDestination.ARG_TALAO_ID])

    private val _talao = MutableStateFlow<Talao?>(null)
    val talao: StateFlow<Talao?> = _talao.asStateFlow()

    init {
        viewModelScope.launch {
            _talao.value = talaoRepository.obterPorId(talaoId)
        }
    }
}
