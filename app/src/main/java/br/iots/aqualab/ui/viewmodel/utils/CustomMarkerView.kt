package br.iots.aqualab.ui.components

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import br.iots.aqualab.R

class CustomMarkerView(
    context: Context
) : MarkerView(context, R.layout.marker_chart) {

    private val tvContent: TextView = findViewById(R.id.tvMarkerContent)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        tvContent.text = e?.data?.toString() ?: ""
        super.refreshContent(e, highlight)
    }

    fun getXOffset(xpos: Float): Int = -(width / 2)

    fun getYOffset(ypos: Float): Int = -height
}
