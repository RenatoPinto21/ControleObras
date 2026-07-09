package pt.controleobras.app.core.export

import pt.controleobras.app.core.model.CaptureMetadata
import pt.controleobras.app.core.model.TalaoDraft
import pt.controleobras.app.core.model.WorkerFormData
import pt.controleobras.app.core.qr.AtQrData
import java.io.File
import javax.inject.Inject

/**
 * Gera o CSV de registo de captura no formato definido pela empresa.
 *
 * Colunas (separador ';'):
 *   Macadress ; IDREG ; TIPO ; GPS ; FUNCN ; FUNCDESC ; FUNOBS ;
 *   FORNECEDOR ; NIF_FORNECEDOR ; NIF_CLIENTE ; SERIE ;
 *   DATA ; DATA_VENCIMENTO ; METODO_PAGAMENTO ;
 *   QRCODE ; TIPODOC ; TOTAL
 *
 * Uma linha por captura.
 * O ficheiro é guardado localmente em filesDir/receipts/ com o nome {fileBaseName}.csv.
 * O mesmo ficheiro é depois enviado para o Google Drive (silenciosamente, sem interação do utilizador).
 */
class CapturaCsvExporter @Inject constructor() {

    fun exportar(
        destDir: File,
        metadata: CaptureMetadata,
        workerData: WorkerFormData,
        atQrData: AtQrData?,
        draft: TalaoDraft,
        tipo: String = "IMG"
    ): File {
        destDir.mkdirs()
        val file = File(destDir, "${metadata.fileBaseName}.csv")

        val header = listOf(
            "Macadress", "IDREG", "TIPO", "GPS", "FUNCN", "FUNCDESC", "FUNOBS",
            "FORNECEDOR", "NIF_FORNECEDOR", "NIF_CLIENTE", "SERIE",
            "DATA", "DATA_VENCIMENTO", "METODO_PAGAMENTO",
            "QRCODE", "TIPODOC", "TOTAL"
        ).joinToString(";")

        // Preferência para dados do QR AT quando disponíveis (mais fiáveis)
        val data          = atQrData?.data?.toString()        ?: draft.data?.toString()          ?: ""
        val tipoDoc       = atQrData?.tipoDocumento           ?: ""
        val total         = atQrData?.totalComIva             ?: draft.total
        val qrRaw         = metadata.qrCodeRaw?.replace(";", ",") ?: ""

        val linha = listOf(
            metadata.macAddress,
            metadata.idReg,
            tipo,
            metadata.gps,
            workerData.funcn,
            workerData.ccnome,
            workerData.funobs,
            draft.empresa.sanitizar(),
            draft.nif,
            draft.nifCliente,
            draft.serie,
            data,
            draft.dataVencimento?.toString() ?: "",
            draft.metodoPagamento.sanitizar(),
            qrRaw,
            tipoDoc,
            total
        ).joinToString(";")

        file.writeText("$header\n$linha", Charsets.UTF_8)
        return file
    }

    /** Remove ';' dos valores de texto para não quebrar o CSV. */
    private fun String.sanitizar() = this.replace(";", ",")
}
