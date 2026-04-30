package br.iots.aqualab.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import br.iots.aqualab.constants.WaterQualityConstants
import br.iots.aqualab.model.LeituraSensor
import br.iots.aqualab.model.PontoColeta
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utilitário para exportação de dados de sensores em formato CSV
 */
object CsvExporter {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    private val fileNameDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /**
     * Exporta leituras de sensores para arquivo CSV
     *
     * @param context Contexto da aplicação
     * @param leituras Lista de leituras a exportar
     * @param pontoColeta Ponto de coleta (opcional, para incluir no nome do arquivo)
     * @param sensorType Tipo de sensor (opcional, para filtrar)
     * @return Uri do arquivo criado ou null em caso de erro
     */
    fun exportToCsv(
        context: Context,
        leituras: List<LeituraSensor>,
        pontoColeta: PontoColeta? = null,
        sensorType: WaterQualityConstants.SensorType? = null
    ): Uri? {
        try {
            // Filtrar por tipo de sensor se especificado
            val leiturasParaExportar = if (sensorType != null) {
                leituras.filter { it.sensorType == sensorType }
            } else {
                leituras
            }

            if (leiturasParaExportar.isEmpty()) {
                Toast.makeText(context, "Nenhuma leitura para exportar", Toast.LENGTH_SHORT).show()
                return null
            }

            // Criar nome do arquivo
            val timestamp = fileNameDateFormat.format(Date())
            val sensorName = sensorType?.displayName ?: "Todos_Sensores"
            val pontoNome = pontoColeta?.nome?.replace(" ", "_") ?: "Ponto"
            val fileName = "AquaLab_${pontoNome}_${sensorName}_${timestamp}.csv"

            // Criar arquivo no diretório de cache
            val cacheDir = File(context.cacheDir, "exports")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val file = File(cacheDir, fileName)
            val writer = FileWriter(file)

            // Escrever cabeçalho do CSV
            writer.append("# AquaLab - Exportação de Dados de Sensores\n")
            writer.append("# Data de Exportação: ${dateFormat.format(Date())}\n")
            pontoColeta?.let {
                writer.append("# Ponto de Coleta: ${it.nome}\n")
                writer.append("# Localização: ${it.endereco}\n")
                writer.append("# Tipo: ${it.tipo}\n")
                writer.append("# Coordenadas: ${it.latitude}, ${it.longitude}\n")
            }
            sensorType?.let {
                writer.append("# Sensor: ${it.displayName} (${it.unit})\n")
                writer.append("# Descrição: ${it.description}\n")
            }
            writer.append("# Total de Leituras: ${leiturasParaExportar.size}\n")
            writer.append("#\n")

            // Cabeçalho das colunas
            writer.append("Data/Hora,Sensor,Valor,Unidade,Qualidade,Observação\n")

            // Escrever dados
            leiturasParaExportar.sortedBy { it.getTimestampMillis() }.forEach { leitura ->
                val dataHora = leitura.getFormattedTimestamp("dd/MM/yyyy HH:mm:ss")
                val sensor = leitura.sensorType?.displayName ?: leitura.sensorId ?: "Desconhecido"
                val valor = leitura.valor?.let { String.format(Locale.US, "%.2f", it) } ?: "N/A"
                val unidade = leitura.sensorType?.unit ?: ""
                val qualidade = leitura.qualityLevel.label
                val observacao = getObservacao(leitura)

                writer.append("$dataHora,$sensor,$valor,$unidade,$qualidade,\"$observacao\"\n")
            }

            writer.flush()
            writer.close()

            // Obter URI do arquivo usando FileProvider
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "Erro ao exportar dados: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            return null
        }
    }

    /**
     * Compartilha arquivo CSV via intent (WhatsApp, Email, etc)
     */
    fun shareCsv(
        context: Context,
        fileUri: Uri,
        sensorType: WaterQualityConstants.SensorType? = null,
        pontoColeta: PontoColeta? = null
    ) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                // Texto para compartilhamento
                val titulo = "Dados de Monitoramento - AquaLab"
                val mensagem = buildString {
                    append("📊 *Dados de Monitoramento AquaLab*\n\n")
                    pontoColeta?.let {
                        append("📍 Ponto: ${it.nome}\n")
                        append("🗺️ Local: ${it.endereco}\n")
                    }
                    sensorType?.let {
                        append("🔬 Sensor: ${it.displayName}\n")
                    }
                    append("\n📅 Exportado em: ${dateFormat.format(Date())}\n")
                    append("\nArquivo CSV anexado com os dados completos.")
                }

                putExtra(Intent.EXTRA_SUBJECT, titulo)
                putExtra(Intent.EXTRA_TEXT, mensagem)
            }

            // Criar chooser para selecionar app
            val chooserIntent = Intent.createChooser(shareIntent, "Compartilhar dados via")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "Erro ao compartilhar: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Compartilha diretamente via WhatsApp (se instalado)
     */
    fun shareViaWhatsApp(
        context: Context,
        fileUri: Uri,
        sensorType: WaterQualityConstants.SensorType? = null,
        pontoColeta: PontoColeta? = null
    ) {
        try {
            val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                val mensagem = buildString {
                    append("📊 *Dados AquaLab*\n\n")
                    pontoColeta?.let { append("📍 ${it.nome}\n") }
                    sensorType?.let { append("🔬 ${it.displayName}\n") }
                    append("\n📅 ${dateFormat.format(Date())}")
                }

                putExtra(Intent.EXTRA_TEXT, mensagem)
            }

            context.startActivity(whatsappIntent)

        } catch (e: Exception) {
            // Se WhatsApp não estiver instalado, usar compartilhamento genérico
            Toast.makeText(context, "WhatsApp não encontrado, abrindo outras opções...", Toast.LENGTH_SHORT).show()
            shareCsv(context, fileUri, sensorType, pontoColeta)
        }
    }

    /**
     * Gera observação baseada no valor da leitura
     */
    private fun getObservacao(leitura: LeituraSensor): String {
        return when (leitura.qualityLevel) {
            WaterQualityConstants.QualityLevel.OTIMA -> "Dentro dos parâmetros ideais"
            WaterQualityConstants.QualityLevel.BOA -> "Boa qualidade"
            WaterQualityConstants.QualityLevel.REGULAR -> "Requer atenção"
            WaterQualityConstants.QualityLevel.RUIM -> "Fora dos padrões recomendados"
            WaterQualityConstants.QualityLevel.PESSIMA -> "Qualidade crítica"
            WaterQualityConstants.QualityLevel.INDISPONIVEL -> "Dados não disponíveis"
        }
    }

    /**
     * Gera estatísticas resumidas em texto
     */
    fun generateSummaryText(
        leituras: List<LeituraSensor>,
        sensorType: WaterQualityConstants.SensorType
    ): String {
        val stats = LeituraSensor.calculateStats(leituras)

        return buildString {
            append("Resumo Estatístico de ${sensorType.displayName}\n\n")

            stats?.let {
                append("📈 Valor Máximo: ${WaterQualityConstants.formatSensorValue(sensorType, it.max)}\n")
                append("📉 Valor Mínimo: ${WaterQualityConstants.formatSensorValue(sensorType, it.min)}\n")
                append("📊 Média: ${WaterQualityConstants.formatSensorValue(sensorType, it.average)}\n")
                append("🔢 Total de Leituras: ${it.count}\n\n")

                // Distribuição de qualidade
                val distribuicao = leituras.groupingBy { it.qualityLevel }.eachCount()
                append("📋 Distribuição de Qualidade:\n")
                distribuicao.forEach { (nivel, count) ->
                    val percentual = (count * 100.0 / leituras.size).toInt()
                    append("   ${nivel.label}: $count ($percentual%)\n")
                }
            } ?: append("Sem dados estatísticos disponíveis")
        }
    }
}
