package ui.listener

import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class DocumentTextListener(private val onChange: (event: DocumentEvent) -> Unit) : DocumentListener {
    override fun insertUpdate(event: DocumentEvent?) {
        event?.let(onChange)
    }

    override fun removeUpdate(event: DocumentEvent?) {
        event?.let(onChange)
    }

    override fun changedUpdate(event: DocumentEvent?) {}
}
