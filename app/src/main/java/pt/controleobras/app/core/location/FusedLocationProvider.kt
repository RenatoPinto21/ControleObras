package pt.controleobras.app.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Implementação com FusedLocationProviderClient (Google Play Services).
 * Pede a localização atual com alta precisão; tem timeout implícito do sistema.
 * Devolve "lat,lon" com 6 casas decimais, ou string vazia em caso de erro/sem permissão.
 */
class FusedLocationProvider @Inject constructor() : LocationProvider {

    override suspend fun getCurrentLocation(context: Context): String {
        val temPermissao = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (!temPermissao) return ""

        return suspendCancellableCoroutine { continuation ->
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()

            continuation.invokeOnCancellation { cts.cancel() }

            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location ->
                    val resultado = if (location != null) {
                        "%.6f,%.6f".format(location.latitude, location.longitude)
                    } else {
                        ""
                    }
                    continuation.resume(resultado)
                }
                .addOnFailureListener {
                    continuation.resume("")
                }
        }
    }
}
