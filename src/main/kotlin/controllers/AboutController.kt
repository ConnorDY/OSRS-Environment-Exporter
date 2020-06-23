package controllers

import javafx.fxml.FXML
import javafx.scene.control.Hyperlink
import javafx.scene.control.Label
import java.awt.Desktop
import java.net.URI

class AboutController {

    @FXML
    private lateinit var lblVersion: Label

    @FXML
    private lateinit var lblAbout: Label

    @FXML
    private lateinit var linkGithub: Hyperlink

    @FXML
    fun initialize() {
        linkGithub.setOnAction {
            Desktop.getDesktop().browse(URI("https://github.com/tpetrychyn/osrs-map-editor"))
        }
        lblAbout.text = "This application serves as a personal learning project for the purpose of becoming familiar with the OSRS cache and discovering how Runescape is built.\n" +
                "\n" +
                "You are currently using a very very early development build so expect many bugs.\n" +
                "\n" +
                "Special thanks to:\n" +
                "Displee - for their cache tool (github.com/Displee/rs-cache-library)\n" +
                "Explv - for their interactive OSRS map (explv.github.io)\n" +
                "Runestats - for their OSRS cache archive (archive.runestats.com/osrs)\n" +
                "OpenOSRS - for their cache definitions and loaders (openosrs.com)"
    }
}