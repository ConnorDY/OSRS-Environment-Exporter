package controllers

import java.awt.Dimension
import java.awt.Frame
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingConstants


class RuneLiteLicenseController(owner: Frame, title: String) : JDialog(owner, title) {
    init {
        preferredSize = Dimension(680, 500)
        layout = BoxLayout(contentPane, BoxLayout.PAGE_AXIS)
        defaultCloseOperation = DISPOSE_ON_CLOSE

        val panel = JPanel()
        val scrollPane = JScrollPane(panel)

        Box.createGlue().let(panel::add)
        JLabel(
            "<html><pre>BSD 2-Clause License<br />" +
                "<br />" +
                "Copyright (c) 2016-2017, Adam <Adam@sigterm.info><br />" +
                "All rights reserved.<br />" +
                "<br />" +
                "Redistribution and use in source and binary forms, with or without<br />" +
                "modification, are permitted provided that the following conditions are met:<br />" +
                "<br />" +
                "1. Redistributions of source code must retain the above copyright notice, this<br />" +
                "  list of conditions and the following disclaimer.<br />" +
                "<br />" +
                "2. Redistributions in binary form must reproduce the above copyright notice,<br />" +
                "  this list of conditions and the following disclaimer in the documentation<br />" +
                "  and/or other materials provided with the distribution.<br />" +
                "<br />" +
                "THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"<br />" +
                "AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE<br />" +
                "IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE<br />" +
                "DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE<br />" +
                "FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL<br />" +
                "DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR<br />" +
                "SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER<br />" +
                "CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,<br />" +
                "OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE<br />" +
                "OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.</pre></html>",
            SwingConstants.CENTER
        ).apply {
            alignmentX = CENTER_ALIGNMENT
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        }.let(panel::add)
        Box.createGlue().let(panel::add)

        scrollPane.let(::add)

        pack()
    }
}
