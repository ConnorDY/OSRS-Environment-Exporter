package ui

import java.awt.Color
import java.text.ParseException
import javax.swing.JFormattedTextField
import javax.swing.UIManager
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

object NumericTextField {
    fun create(value: Int, min: Int, max: Int): JFormattedTextField =
        createWithFormatter(value, RestrictedNumberFormat(min, max))

    private fun createWithFormatter(value: Any?, formatter: JFormattedTextField.AbstractFormatter): JFormattedTextField {
        val field = JFormattedTextField(formatter)
        field.value = value
        field.document.addDocumentListener(
            RestrictedNumberDocumentChangeListener(field)
        )
        return field
    }

    fun createNullable(value: Int?, min: Int, max: Int): JFormattedTextField =
        createWithFormatter(value, NullableFormatter(RestrictedNumberFormat(min, max)))

    class RestrictedNumberFormat(private val min: Int, private val max: Int) :
        JFormattedTextField.AbstractFormatter() {

        override fun stringToValue(string: String): Int {
            val parsed = string.toIntOrNull()
            if (parsed == null || parsed < min || parsed > max) {
                throw ParseException(string, 0)
            }
            return parsed
        }

        override fun valueToString(value: Any?): String = value.toString()
    }

    class NullableFormatter(private val backing: JFormattedTextField.AbstractFormatter) :
        JFormattedTextField.AbstractFormatter() {
        override fun stringToValue(string: String): Any? =
            if (string.isEmpty()) null
            else backing.stringToValue(string)

        override fun valueToString(value: Any?): String =
            if (value == null) ""
            else backing.valueToString(value)
    }

    class RestrictedNumberDocumentChangeListener(private val control: JFormattedTextField) :
        DocumentListener {
        override fun insertUpdate(event: DocumentEvent?) {
            event?.let(::textChange)
        }

        override fun removeUpdate(event: DocumentEvent?) {
            event?.let(::textChange)
        }

        private fun textChange(event: DocumentEvent) {
            if (event.length == 0) return
            val doc = event.document
            val txt = doc.getText(0, doc.length)
            val valid =
                try {
                    control.formatter.stringToValue(txt)
                    true
                } catch (e: ParseException) {
                    false
                }
            control.foreground =
                if (valid) UIManager.getColor("TextField.foreground") as Color
                else Color.RED
        }

        override fun changedUpdate(event: DocumentEvent?) {}
    }
}
