package br.iots.aqualab.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import br.iots.aqualab.R
import br.iots.aqualab.databinding.ItemSensorStatusBinding
import br.iots.aqualab.model.LeituraSensor

class SensorStatusAdapter(private var leituras: List<LeituraSensor>) : RecyclerView.Adapter<SensorStatusAdapter.SensorStatusViewHolder>() {

    class SensorStatusViewHolder(val binding: ItemSensorStatusBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SensorStatusViewHolder {
        val binding = ItemSensorStatusBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SensorStatusViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SensorStatusViewHolder, position: Int) {
        val leitura = leituras[position]
        val context = holder.itemView.context

        // Formatação de valores para 1 casa decimal
        val valorFormatado = leitura.valor?.let { String.format("%.1f", it) } ?: "--"

        val sensorInfoText = when (leitura.sensorId) {
            "ph" -> "Sensor de PH: $valorFormatado pH"
            "tds" -> "Sensor de TDS: $valorFormatado ppm"
            "amonia" -> "Sensor de Amonia: $valorFormatado mg/L"
            "temperatura" -> "Sensor de Temp.: $valorFormatado °C"
            else -> "Sensor ${leitura.sensorId?.replaceFirstChar { it.uppercase() }}: $valorFormatado"
        }
        holder.binding.tvSensorStatusInfo.text = sensorInfoText

        val statusColorRes = when (leitura.sensorId) {
            "ph" -> if (leitura.valor != null && leitura.valor in 6.5..8.5) {
                R.color.status_verde
            } else {
                R.color.status_vermelho
            }
            else -> R.color.status_verde
        }

        val statusColor = ContextCompat.getColor(context, statusColorRes)
        holder.binding.ivStatusIndicator.setColorFilter(statusColor)
    }

    override fun getItemCount() = leituras.size

    fun updateData(newLeituras: List<LeituraSensor>) {
        this.leituras = newLeituras
        notifyDataSetChanged()
    }
}
