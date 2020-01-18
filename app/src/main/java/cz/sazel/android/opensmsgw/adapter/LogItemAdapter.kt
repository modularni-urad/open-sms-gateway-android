package cz.sazel.android.opensmsgw.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cz.sazel.android.opensmsgw.R
import cz.sazel.android.opensmsgw.model.LogItem
import cz.sazel.android.opensmsgw.model.LogLevel
import cz.sazel.android.opensmsgw.util.Logger
import kotlinx.android.synthetic.main.item_log.view.*


/**
 * View holder for log line UI.
 */
class LogItemVH(view: View) : RecyclerView.ViewHolder(view) {
    fun bind(logItem: LogItem) {
        itemView.tvText.text = logItem.uiText
        itemView.tvText.setTextColor(
            when (logItem.logLevel) {
                LogLevel.ERROR -> Color.RED
                LogLevel.WARNING -> Color.rgb(255, 184, 30)
                LogLevel.INFO -> Color.BLUE
                LogLevel.DEBUG -> Color.LTGRAY
                else -> Color.BLACK
            }
        )
    }
}

/**
 * RecyclerView adapter for log lines.
 */
class LogItemAdapter(private val log: Logger) : RecyclerView.Adapter<LogItemVH>() {

    init {
        setHasStableIds(false)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogItemVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return LogItemVH(view)
    }

    override fun getItemCount(): Int = log.contents.size

    override fun getItemId(position: Int): Long {
        return log.contents[position].timestamp
    }

    override fun onBindViewHolder(holder: LogItemVH, position: Int) {
        holder.bind(log.contents[position])
    }
}