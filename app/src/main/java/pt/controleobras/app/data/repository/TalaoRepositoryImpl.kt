package pt.controleobras.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pt.controleobras.app.core.database.dao.TalaoDao
import pt.controleobras.app.core.database.mapper.toDomain
import pt.controleobras.app.core.database.mapper.toEntity
import pt.controleobras.app.core.export.CsvExporter
import pt.controleobras.app.core.export.ExportFileLocator
import pt.controleobras.app.core.export.TalaoJsonDto
import pt.controleobras.app.core.export.XmlExporter
import pt.controleobras.app.core.model.Talao
import javax.inject.Inject

/**
 * Guarda o talão na Room Database e escreve, ao lado da imagem original,
 * as exportações JSON, XML e CSV exigidas pelo requisito do projeto.
 */
class TalaoRepositoryImpl @Inject constructor(
    private val dao: TalaoDao,
    @ApplicationContext private val context: Context
) : TalaoRepository {

    private val json = Json { prettyPrint = true }

    override suspend fun guardar(talao: Talao): Long = withContext(Dispatchers.IO) {
        val id = dao.insert(talao.toEntity())
        escreverExportacoes(talao.copy(id = id))
        id
    }

    override fun observarTodos(): Flow<List<Talao>> =
        dao.observeAll().map { entidades -> entidades.map { it.toDomain() } }

    override suspend fun obterPorId(id: Long): Talao? =
        withContext(Dispatchers.IO) { dao.getById(id)?.toDomain() }

    private fun escreverExportacoes(talao: Talao) {
        ExportFileLocator.ficheiroJson(context, talao.id)
            .writeText(json.encodeToString(TalaoJsonDto.fromDomain(talao)))
        ExportFileLocator.ficheiroXml(context, talao.id)
            .writeText(XmlExporter.toXml(talao))
        ExportFileLocator.ficheiroCsv(context, talao.id)
            .writeText(CsvExporter.toCsv(talao))
    }
}
