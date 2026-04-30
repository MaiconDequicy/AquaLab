package br.iots.aqualab.ui.components

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import br.iots.aqualab.R
import br.iots.aqualab.constants.WaterQualityConstants
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog para configuração de filtros e opções de visualização de gráficos de sensores
 */
class ChartConfigDialog(
    private val context: Context,
    private val availableSensors: List<WaterQualityConstants.SensorType>,
    private val currentFilter: ChartDataFilter = ChartDataFilter.default(),
    private val onApply: (ChartDataFilter, WaterQualityConstants.SensorType?) -> Unit
) {

    private var dialog: Dialog? = null
    private var selectedSensorType: WaterQualityConstants.SensorType? = null
    private var selectedStartDate: Date? = currentFilter.startDate
    private var selectedEndDate: Date? = currentFilter.endDate

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    fun show() {
        dialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)

            val view = LayoutInflater.from(context).inflate(
                R.layout.dialog_chart_config,
                null
            )

            setContentView(view)
            window?.setBackgroundDrawableResource(android.R.color.transparent)

            setupViews(view)

            show()
        }
    }

    private fun setupViews(view: View) {
        // Configurar seletor de tipo de sensor
        setupSensorTypeSelector(view)

        // Configurar campo de número máximo de amostras
        val etMaxSamples = view.findViewById<TextInputEditText>(R.id.et_max_samples)
        etMaxSamples.setText(currentFilter.maxSamples.toString())

        // Configurar switch de filtro de data
        val switchDateFilter = view.findViewById<SwitchMaterial>(R.id.switch_enable_date_filter)
        val containerDateFilters = view.findViewById<LinearLayout>(R.id.container_date_filters)

        switchDateFilter.isChecked = currentFilter.hasDateFilter()
        containerDateFilters.visibility = if (switchDateFilter.isChecked) View.VISIBLE else View.GONE

        switchDateFilter.setOnCheckedChangeListener { _, isChecked ->
            containerDateFilters.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                selectedStartDate = null
                selectedEndDate = null
            }
        }

        // Configurar campos de data
        setupDatePickers(view)

        // Configurar chips de atalho
        setupShortcutChips(view)

        // Configurar botões
        view.findViewById<MaterialButton>(R.id.btn_cancel).setOnClickListener {
            dialog?.dismiss()
        }

        view.findViewById<MaterialButton>(R.id.btn_apply).setOnClickListener {
            applyFilters(view)
        }
    }

    private fun setupSensorTypeSelector(view: View) {
        val tilSensorType = view.findViewById<TextInputLayout>(R.id.til_sensor_type)
        val actvSensorType = view.findViewById<AutoCompleteTextView>(R.id.actv_sensor_type)

        // Criar lista de opções incluindo "Todos os Sensores"
        val options = mutableListOf("Todos os Sensores")
        options.addAll(availableSensors.map { it.displayName })

        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, options)
        actvSensorType.setAdapter(adapter)

        // Definir seleção inicial
        val currentSensorType = currentFilter.sensorTypes.firstOrNull()?.let { sensorId ->
            WaterQualityConstants.SensorType.fromId(sensorId)
        }

        actvSensorType.setText(currentSensorType?.displayName ?: "Todos os Sensores", false)
        selectedSensorType = currentSensorType

        // Listener de seleção
        actvSensorType.setOnItemClickListener { _, _, position, _ ->
            selectedSensorType = if (position == 0) {
                null // "Todos os Sensores"
            } else {
                availableSensors[position - 1]
            }
        }
    }

    private fun setupDatePickers(view: View) {
        val etStartDate = view.findViewById<TextInputEditText>(R.id.et_start_date)
        val etEndDate = view.findViewById<TextInputEditText>(R.id.et_end_date)

        // Definir valores iniciais se existirem
        selectedStartDate?.let { etStartDate.setText(dateFormat.format(it)) }
        selectedEndDate?.let { etEndDate.setText(dateFormat.format(it)) }

        // Configurar clique para data inicial
        etStartDate.setOnClickListener {
            showDateTimePicker(selectedStartDate ?: Date()) { date ->
                selectedStartDate = date
                etStartDate.setText(dateFormat.format(date))
            }
        }

        // Configurar clique para data final
        etEndDate.setOnClickListener {
            showDateTimePicker(selectedEndDate ?: Date()) { date ->
                selectedEndDate = date
                etEndDate.setText(dateFormat.format(date))
            }
        }
    }

    private fun setupShortcutChips(view: View) {
        view.findViewById<Chip>(R.id.chip_last_24h).setOnClickListener {
            setLast24Hours(view)
        }

        view.findViewById<Chip>(R.id.chip_last_week).setOnClickListener {
            setLastWeek(view)
        }

        view.findViewById<Chip>(R.id.chip_last_month).setOnClickListener {
            setLastMonth(view)
        }
    }

    private fun setLast24Hours(view: View) {
        val now = Date()
        val yesterday = Date(now.time - 24 * 60 * 60 * 1000)

        selectedStartDate = yesterday
        selectedEndDate = now

        updateDateFields(view)
        enableDateFilter(view)
    }

    private fun setLastWeek(view: View) {
        val now = Date()
        val lastWeek = Date(now.time - 7 * 24 * 60 * 60 * 1000)

        selectedStartDate = lastWeek
        selectedEndDate = now

        updateDateFields(view)
        enableDateFilter(view)
    }

    private fun setLastMonth(view: View) {
        val now = Date()
        val lastMonth = Date(now.time - 30L * 24 * 60 * 60 * 1000)

        selectedStartDate = lastMonth
        selectedEndDate = now

        updateDateFields(view)
        enableDateFilter(view)
    }

    private fun updateDateFields(view: View) {
        val etStartDate = view.findViewById<TextInputEditText>(R.id.et_start_date)
        val etEndDate = view.findViewById<TextInputEditText>(R.id.et_end_date)

        selectedStartDate?.let { etStartDate.setText(dateFormat.format(it)) }
        selectedEndDate?.let { etEndDate.setText(dateFormat.format(it)) }
    }

    private fun enableDateFilter(view: View) {
        val switchDateFilter = view.findViewById<SwitchMaterial>(R.id.switch_enable_date_filter)
        val containerDateFilters = view.findViewById<LinearLayout>(R.id.container_date_filters)

        if (!switchDateFilter.isChecked) {
            switchDateFilter.isChecked = true
            containerDateFilters.visibility = View.VISIBLE
        }
    }

    private fun showDateTimePicker(initialDate: Date, onDateTimeSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        calendar.time = initialDate

        // Primeiro, mostrar DatePicker
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                // Depois, mostrar TimePicker
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)

                        onDateTimeSelected(calendar.time)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun applyFilters(view: View) {
        val etMaxSamples = view.findViewById<TextInputEditText>(R.id.et_max_samples)
        val switchDateFilter = view.findViewById<SwitchMaterial>(R.id.switch_enable_date_filter)

        // Obter número máximo de amostras
        val maxSamples = etMaxSamples.text.toString().toIntOrNull() ?: 50

        // Criar filtro
        val filter = ChartDataFilter(
            startDate = if (switchDateFilter.isChecked) selectedStartDate else null,
            endDate = if (switchDateFilter.isChecked) selectedEndDate else null,
            maxSamples = maxSamples,
            sensorTypes = selectedSensorType?.let { listOf(it.id) } ?: emptyList()
        )

        // Chamar callback
        onApply(filter, selectedSensorType)

        // Fechar dialog
        dialog?.dismiss()
    }

    fun dismiss() {
        dialog?.dismiss()
    }

    companion object {
        /**
         * Cria e exibe o dialog de configuração
         */
        fun show(
            context: Context,
            availableSensors: List<WaterQualityConstants.SensorType>,
            currentFilter: ChartDataFilter = ChartDataFilter.default(),
            onApply: (ChartDataFilter, WaterQualityConstants.SensorType?) -> Unit
        ) {
            ChartConfigDialog(context, availableSensors, currentFilter, onApply).show()
        }
    }
}
