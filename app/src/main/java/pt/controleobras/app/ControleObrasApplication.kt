package pt.controleobras.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Ponto de entrada do grafo de injeção de dependências do Hilt.
 * Sem lógica própria — apenas ativa a geração de código do Hilt para toda a app.
 */
@HiltAndroidApp
class ControleObrasApplication : Application()
