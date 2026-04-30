package br.iots.aqualab.model

import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Modelo para fotos armazenadas localmente no dispositivo
 */
data class FotoLocal(
    val id: String = UUID.randomUUID().toString(),
    val pontoColetaId: String,
    val caminhoArquivo: String,
    val comentario: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Retorna o arquivo da foto
     */
    fun getFile(): File = File(caminhoArquivo)

    /**
     * Verifica se o arquivo da foto existe
     */
    fun exists(): Boolean = getFile().exists()

    /**
     * Retorna timestamp formatado
     */
    fun getFormattedTimestamp(pattern: String = "dd/MM/yyyy HH:mm"): String {
        val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    /**
     * Retorna o tamanho do arquivo em MB
     */
    fun getFileSizeInMB(): Double {
        return if (exists()) {
            getFile().length() / (1024.0 * 1024.0)
        } else {
            0.0
        }
    }

    /**
     * Deleta o arquivo de foto do sistema
     */
    fun delete(): Boolean {
        return try {
            getFile().delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    companion object {
        /**
         * Cria nome de arquivo único para nova foto
         */
        fun generateFileName(): String {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            return "AQUALAB_PHOTO_${timestamp}.jpg"
        }
    }
}
