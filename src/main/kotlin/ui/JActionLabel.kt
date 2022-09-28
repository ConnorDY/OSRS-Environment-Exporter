package ui

import java.awt.Color
import java.awt.Cursor
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.font.TextAttribute
import javax.swing.JLabel
import javax.swing.SwingConstants

open class JActionLabel(text: String, align: Int = SwingConstants.LEADING) : JLabel(text, align) {
    private val actionListeners = mutableListOf<ActionListener>()

    init {
        cursor = Cursor(Cursor.HAND_CURSOR)
        addMouseListener(
            object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    if (e?.button == MouseEvent.BUTTON1) {
                        fireClickEvent()
                    }
                }
            }
        )
        addKeyListener(
            object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent?) {
                    if (e?.keyCode == KeyEvent.VK_ENTER) {
                        fireClickEvent()
                    }
                }
            }
        )
        font = font.let {
            it.deriveFont(it.attributes + (TextAttribute.UNDERLINE to TextAttribute.UNDERLINE_ON))
        }
        foreground = Color.BLUE
        isFocusable = true
    }

    fun addActionListener(listener: ActionListener) {
        actionListeners.add(listener)
    }

    fun removeActionListener(listener: ActionListener) {
        actionListeners.remove(listener)
    }

    private fun fireClickEvent() {
        actionListeners.forEach {
            val event = ActionEvent(this, ActionEvent.ACTION_PERFORMED, "click")
            it.actionPerformed(event)
        }
    }
}
