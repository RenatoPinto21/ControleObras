package pt.controleobras.app.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pt.controleobras.app.core.common.AppPreferences
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _mostrarBoasVindas = MutableStateFlow(!appPreferences.jaViuBoasVindas)
    val mostrarBoasVindas: StateFlow<Boolean> = _mostrarBoasVindas.asStateFlow()

    private val _driveConfigurado = MutableStateFlow(!appPreferences.driveFolderUri.isNullOrBlank())
    val driveConfigurado: StateFlow<Boolean> = _driveConfigurado.asStateFlow()

    fun fecharBoasVindas() {
        appPreferences.jaViuBoasVindas = true
        _mostrarBoasVindas.value = false
    }

    fun guardarDriveFolderUri(uri: String) {
        appPreferences.driveFolderUri = uri
        _driveConfigurado.value = true
    }
}
