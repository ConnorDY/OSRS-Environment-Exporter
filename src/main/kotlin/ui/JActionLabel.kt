package ui

import java.awt.Color
import java.awt.Cursor
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.font.TextAttribute
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.SwingConstants
import javax.swing.UIManager

open class JActionLabel(text: String, align: Int = SwingConstants.LEADING) : JLabel(text, align) {
    private val focusBorderColor get() = UIManager.getColor("Label.foreground") ?: Color.BLACK
    private val actionListeners = mutableListOf<ActionListener>()

    init {
        cursor = Cursor(Cursor.HAND_CURSOR)
        addMouseListener(
            object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    requestFocusInWindow(FocusEvent.Cause.MOUSE_EVENT)
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
        updateBorder(false)

        addFocusListener(
            object : FocusListener {
                override fun focusGained(e: FocusEvent?) {
                    updateBorder(true)
                }

                override fun focusLost(e: FocusEvent?) {
                    updateBorder(false)
                }
            }
        )

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

    open fun updateBorder(focused: Boolean) {
        border = if (focused) {
            BorderFactory.createDashedBorder(focusBorderColor, 1f, 1f, 1f, true)
        } else {
            BorderFactory.createEmptyBorder(1, 1, 1, 1)
        }
    }
}
