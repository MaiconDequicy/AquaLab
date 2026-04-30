package br.iots.aqualab.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import br.iots.aqualab.model.FotoLocal
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Repository para gerenciar fotos armazenadas localmente no dispositivo
 */
class FotoLocalRepository(private val context: Context) {

    private val gson = Gson()
    private val metadataFile = File(context.filesDir, "fotos_metadata.json")

    companion object {
        private const val PHOTOS_DIR = "collection_point_photos"
    }

    /**
     * Diretório onde as fotos são armazenadas
     */
    private fun getPhotosDirectory(): File {
        val dir = File(context.getExternalFilesDir(null), PHOTOS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Carrega todos os metadados de fotos do arquivo JSON
     */
    private suspend fun loadAllMetadata(): MutableList<FotoLocal> = withContext(Dispatchers.IO) {
        try {
            if (!metadataFile.exists()) {
                return@withContext mutableListOf()
            }

            val json = metadataFile.readText()
            val type = object : TypeToken<MutableList<FotoLocal>>() {}.type
            gson.fromJson<MutableList<FotoLocal>>(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            e.printStackTrace()
            mutableListOf()
        }
    }

    /**
     * Salva todos os metadados no arquivo JSON
     */
    private suspend fun saveAllMetadata(fotos: List<FotoLocal>) = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(fotos)
            metadataFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Cria um arquivo temporário para captura de foto
     */
    suspend fun createPhotoFile(): File = withContext(Dispatchers.IO) {
        val photoDir = getPhotosDirectory()
        val fileName = FotoLocal.generateFileName()
        File(photoDir, fileName)
    }

    /**
     * Obtém URI do arquivo para usar com camera intent
     */
    fun getUriForFile(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Salva metadados da foto
     */
    suspend fun salvarFoto(
        pontoColetaId: String,
        caminhoArquivo: String,
        comentario: String
    ): Result<FotoLocal> = withContext(Dispatchers.IO) {
        try {
            val foto = FotoLocal(
                pontoColetaId = pontoColetaId,
                caminhoArquivo = caminhoArquivo,
                comentario = comentario
            )

            val fotos = loadAllMetadata()
            fotos.add(foto)
            saveAllMetadata(fotos)

            Result.success(foto)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Obtém todas as fotos de um ponto de coleta específico
     */
    suspend fun getFotosPorPonto(pontoColetaId: String): List<FotoLocal> = withContext(Dispatchers.IO) {
        val todasFotos = loadAllMetadata()
        todasFotos.filter { it.pontoColetaId == pontoColetaId && it.exists() }
    }

    /**
     * Obtém uma foto específica por ID
     */
    suspend fun getFotoPorId(fotoId: String): FotoLocal? = withContext(Dispatchers.IO) {
        val todasFotos = loadAllMetadata()
        todasFotos.find { it.id == fotoId }
    }

    /**
     * Atualiza o comentário de uma foto
     */
    suspend fun atualizarComentario(fotoId: String, novoComentario: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val fotos = loadAllMetadata()
                val index = fotos.indexOfFirst { it.id == fotoId }

                if (index != -1) {
                    val fotoAtualizada = fotos[index].copy(comentario = novoComentario)
                    fotos[index] = fotoAtualizada
                    saveAllMetadata(fotos)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Foto não encontrada"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }

    /**
     * Deleta uma foto (arquivo e metadados)
     */
    suspend fun deletarFoto(fotoId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val fotos = loadAllMetadata()
            val foto = fotos.find { it.id == fotoId }

            if (foto != null) {
                // Deletar arquivo físico
                foto.delete()

                // Remover metadados
                fotos.removeIf { it.id == fotoId }
                saveAllMetadata(fotos)

                Result.success(Unit)
            } else {
                Result.failure(Exception("Foto não encontrada"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Deleta todas as fotos de um ponto de coleta
     */
    suspend fun deletarFotosDoPonto(pontoColetaId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val fotos = loadAllMetadata()
            val fotosDoPonto = fotos.filter { it.pontoColetaId == pontoColetaId }

            // Deletar arquivos físicos
            fotosDoPonto.forEach { it.delete() }

            // Remover metadados
            fotos.removeIf { it.pontoColetaId == pontoColetaId }
            saveAllMetadata(fotos)

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Conta o número de fotos de um ponto de coleta
     */
    suspend fun contarFotosDoPonto(pontoColetaId: String): Int = withContext(Dispatchers.IO) {
        val fotos = getFotosPorPonto(pontoColetaId)
        fotos.size
    }

    /**
     * Copia uma foto para um destino específico
     */
    suspend fun copiarFoto(fotoId: String, destino: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val foto = getFotoPorId(fotoId)
            if (foto == null || !foto.exists()) {
                return@withContext Result.failure(Exception("Foto não encontrada"))
            }

            FileInputStream(foto.getFile()).use { input ->
                FileOutputStream(destino).use { output ->
                    input.copyTo(output)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Limpa fotos órfãs (metadados sem arquivo ou arquivos sem metadados)
     */
    suspend fun limparFotosOrfas(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val fotos = loadAllMetadata()
            var removidas = 0

            // Remover metadados sem arquivo
            fotos.removeIf {
                if (!it.exists()) {
                    removidas++
                    true
                } else {
                    false
                }
            }

            // Remover arquivos sem metadados
            val photoDir = getPhotosDirectory()
            val arquivosNoSistema = photoDir.listFiles() ?: emptyArray()
            val caminhosCadastrados = fotos.map { it.caminhoArquivo }.toSet()

            arquivosNoSistema.forEach { arquivo ->
                if (arquivo.absolutePath !in caminhosCadastrados) {
                    arquivo.delete()
                    removidas++
                }
            }

            saveAllMetadata(fotos)
            Result.success(removidas)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
