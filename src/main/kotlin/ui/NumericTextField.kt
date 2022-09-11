package ui

import java.awt.Color
import java.text.ParseException
import javax.swing.JFormattedTextField
import javax.swing.UIManager
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

object NumericTextField {
    fun create(value: Int, min: Int, max: Int): JFormattedTextField =
        create(value, min, max, String::toIntOrNull)

    fun <T : Comparable<T>> create(value: T, min: T, max: T, conversion: (String) -> T?): JFormattedTextField =
        createWithFormatter(value, RestrictedNumberFormat(min, max, conversion))

    private fun createWithFormatter(value: Any?, formatter: JFormattedTextField.AbstractFormatter): JFormattedTextField {
        val field = JFormattedTextField(formatter)
        field.value = value
        field.document.addDocumentListener(
            RestrictedNumberDocumentChangeListener(field)
        )
        return field
    }

    fun createNullable(value: Int?, min: Int, max: Int): JFormattedTextField =
        createNullable(value, min, max, String::toIntOrNull)

    fun <T : Comparable<T>> createNullable(value: T?, min: T, max: T, conversion: (String) -> T?): JFormattedTextField =
        createWithFormatter(value, NullableFormatter(RestrictedNumberFormat(min, max, conversion)))

    class RestrictedNumberFormat<T : Comparable<T>>(private val min: T, private val max: T, private val conversion: (String) -> T?) :
        JFormattedTextField.AbstractFormatter() {

        override fun stringToValue(string: String): T {
            val parsed = conversion(string)
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
            val valid =
                try {
                    control.formatter.stringToValue(control.text)
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
