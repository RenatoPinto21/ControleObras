package pt.controleobras.app.core.export

import pt.controleobras.app.core.model.CaptureMetadata
import pt.controleobras.app.core.model.WorkerFormData
import pt.controleobras.app.core.qr.AtQrData
import java.io.File
import javax.inject.Inject

/**
 * Gera o CSV de registo de captura no formato definido pela empresa.
 *
 * Colunas (separador ';'):
 *   Macadress ; IDREG ; TIPO ; GPS ; FUNCN ; FUNCDESC ; FUNOBS ;
 *   FORNECEDOR ; DATA ; QRCODE ; TIPODOC ; TOTAL
 *
 * Uma linha por captura.
 * O ficheiro é guardado localmente em filesDir/receipts/ com o nome {fileBaseName}.csv.
 * O mesmo ficheiro é depois enviado para o Google Drive.
 */
class CapturaCsvExporter @Inject constructor() {

    fun exportar(
        destDir: File,
        metadata: CaptureMetadata,
        workerData: WorkerFormData,
        atQrData: AtQrData?,
        fornecedor: String,
        tipo: String = "IMG"
    ): File {
        destDir.mkdirs()
        val file = File(destDir, "${metadata.fileBaseName}.csv")

        val header = "Macadress;IDREG;TIPO;GPS;FUNCN;FUNCDESC;FUNOBS;FORNECEDOR;DATA;QRCODE;TIPODOC;TOTAL"

        val qrRaw = metadata.qrCodeRaw?.replace(";", ",") ?: ""
        val data = atQrData?.data?.toString() ?: ""
        val tipoDoc = atQrData?.tipoDocumento ?: ""
        val total = atQrData?.totalComIva ?: ""

        val linha = listOf(
            metadata.macAddress,
            metadata.idReg,
            tipo,
            metadata.gps,
            workerData.funcn,
            workerData.ccnome,      // FUNCDESC = Centro de custo
            workerData.funobs,
            fornecedor.replace(";", ","),
            data,
            qrRaw,
            tipoDoc,
            total
        ).joinToString(";")

        file.writeText("$header\n$linha", Charsets.UTF_8)
        return file
    }
}
