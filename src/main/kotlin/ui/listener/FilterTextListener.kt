package ui.listener

import ui.FilteredListModel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.JTextComponent

class FilterTextListener(
    private val txtField: JTextComponent,
    private val listModel: FilteredListModel<*>
) : DocumentListener {
    override fun insertUpdate(p0: DocumentEvent?) {
        update()
    }

    override fun removeUpdate(p0: DocumentEvent?) {
        update()
    }

    override fun changedUpdate(p0: DocumentEvent?) {
        update()
    }

    private fun update() {
        listModel.filterQuery = txtField.text
    }
}
