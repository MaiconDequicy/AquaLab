package br.iots.aqualab.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.iots.aqualab.R
import br.iots.aqualab.model.LeituraSensor
import java.text.SimpleDateFormat
import java.util.*

class LeiturasAdapter(
    private var leituras: List<LeituraSensor>
) : RecyclerView.Adapter<LeiturasAdapter.LeituraViewHolder>() {

    class LeituraViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSensorValor: TextView = view.findViewById(R.id.tvSensorValor)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeituraViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leitura_sensor, parent, false)
        return LeituraViewHolder(view)
    }

    override fun onBindViewHolder(holder: LeituraViewHolder, position: Int) {
        val leitura = leituras[position]

        val sensorNome = leitura.sensorId?.replaceFirstChar { it.titlecase(Locale.getDefault()) } ?: "N/A"
        holder.tvSensorValor.text = "$sensorNome: ${leitura.valor ?: "N/A"}"

        leitura.timestamp?.toDate()?.let { date ->
            val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            holder.tvTimestamp.text = format.format(date)
        } ?: run {
            holder.tvTimestamp.text = ""
        }
    }

    override fun getItemCount() = leituras.size

    fun updateData(newLeituras: List<LeituraSensor>) {
        this.leituras = newLeituras
        notifyDataSetChanged()
    }
}