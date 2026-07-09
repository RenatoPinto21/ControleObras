package pt.controleobras.app.core.drive

import android.content.Context
import java.io.File

/**
 * Abstrai o envio de ficheiros para o Google Drive.
 * A implementação usa o Storage Access Framework (SAF) — o utilizador seleciona
 * uma pasta do Drive uma única vez; daí em diante o upload é automático e
 * não requer OAuth, chaves de API, nem interação adicional.
 */
interface DriveUploader {
    /** True se a pasta Drive já foi selecionada e o URI persiste. */
    fun isConfigurado(context: Context): Boolean

    /**
     * Envia [file] para a pasta Drive configurada.
     * Devolve true em caso de sucesso, false caso contrário.
     */
    suspend fun upload(context: Context, file: File, mimeType: String): Boolean
}
