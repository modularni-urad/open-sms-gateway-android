package cz.sazel.android.opensmsgw.adapter.diff_callbacks

import androidx.recyclerview.widget.DiffUtil
import cz.sazel.android.opensmsgw.model.LogItem

/**
 * Created on 1/17/20.
 */
class LogItemDiffCallback(private val oldList: List<LogItem>, private val newList: List<LogItem>) :
    DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        areContentsTheSame(oldItemPosition, newItemPosition)

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition] == newList[newItemPosition]
}