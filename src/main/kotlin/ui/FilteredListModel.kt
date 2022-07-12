package ui

import java.util.Collections
import java.util.Locale
import javax.swing.ListModel
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener

class FilteredListModel<T>(val stringify: (T) -> String) : ListModel<T> {
    var backingList: List<T> = emptyList()
        set(value) {
            field = Collections.unmodifiableList(ArrayList(value))
            reapplyFilter()
        }
    var filteredList: List<T> = emptyList()
        private set
    var filterQuery: String = ""
        set(value) {
            field = value.lowercase(Locale.getDefault())
            reapplyFilter()
        }

    private val listeners = ArrayList<ListDataListener>()

    private fun reapplyFilter() {
        val prevSize = size
        val prevFilteredList = filteredList
        filteredList =
            if (filterQuery.isEmpty()) backingList
            else backingList.filter {
                stringify(it).lowercase(Locale.getDefault()).contains(filterQuery)
            }
        if (filteredList != prevFilteredList) {
            val removed =
                ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, 0, prevSize)
            val added =
                ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, 0, size)
            listeners.forEach {
                it.intervalRemoved(removed)
                it.intervalAdded(added)
            }
        }
    }

    override fun getSize(): Int = filteredList.size

    override fun getElementAt(p0: Int): T = filteredList[p0]

    override fun addListDataListener(p0: ListDataListener?) {
        p0?.let { listeners.add(it) }
    }

    override fun removeListDataListener(p0: ListDataListener?) {
        listeners.remove(p0)
    }
}
