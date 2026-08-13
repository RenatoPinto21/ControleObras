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
 *   SERIAL ; IDREG ; TIPO ; GPS ; FUNCN ; FREF ; FUNCDESC ; ENCARREGADO ; FUNOBS ;
 *   FORNECEDOR ; NIF_FORNECEDOR ; NIF_CLIENTE ; SERIE ;
 *   DATA ; DATA_VENCIMENTO ; METODO_PAGAMENTO ;
 *   QRCODE ; TIPODOC ; TOTAL
 *
 * Uma linha por captura.
 */
class CapturaCsvExporter @Inject constructor() {

    fun exportar(
        destDir:     File,
        metadata:    CaptureMetadata,
        workerData:  WorkerFormData,
        atQrData:    AtQrData?,
        draft:       TalaoDraft,
        tipo:        String  = "IMG",
        nifManual:   String? = null,
        valorManual: String? = null
    ): File {
        destDir.mkdirs()
        val file = File(destDir, "${metadata.fileBaseName}.csv")

        val colunasBase = listOf(
            "SERIAL", "IDREG", "TIPO", "GPS",
            "FUNCN", "FREF", "FUNCDESC", "ENCARREGADO", "FUNOBS",
            "FORNECEDOR", "NIF_FORNECEDOR", "NIF_CLIENTE", "SERIE",
            "DATA", "DATA_VENCIMENTO", "METODO_PAGAMENTO",
            "QRCODE", "TIPODOC", "TOTAL"
        )

        val semQr        = atQrData == null
        val colunasExtras = if (semQr) listOf("MNIF", "MVALOR") else emptyList()
        val header        = (colunasBase + colunasExtras).joinToString(";")

        val data     = atQrData?.data?.toString()         ?: draft.data?.toString() ?: ""
        val tipoDoc  = atQrData?.tipoDocumento            ?: ""
        val totalStr = atQrData?.totalComIva?.toString()
                       ?: draft.total?.toString()
                       ?: ""
        val qrRaw    = metadata.qrCodeRaw?.replace(";", ",") ?: ""

        val cc = workerData.centroCusto

        val valoresBase = listOf(
            metadata.serialDispositivo,
            metadata.idReg,
            tipo,
            metadata.gps,
            workerData.funcn,
            cc?.fref.orEmpty(),
            cc?.nmfref.orEmpty(),
            cc?.agnome.orEmpty(),
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
            totalStr
        )

        val valoresExtras = if (semQr) {
            listOf(
                nifManual.orEmpty().sanitizar(),
                valorManual.orEmpty().sanitizar()
            )
        } else emptyList()

        val linha = (valoresBase + valoresExtras).joinToString(";")
        file.writeText("$header\n$linha", Charsets.UTF_8)
        return file
    }

    private fun String.sanitizar() = this.replace(";", ",")
}
