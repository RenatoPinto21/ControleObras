package pt.controleobras.app.core.location

import android.content.Context

/**
 * Abstrai a obtenção de localização GPS.
 * Devolve "lat,lon" ou string vazia se não disponível.
 */
interface LocationProvider {
    suspend fun getCurrentLocation(context: Context): String
}
