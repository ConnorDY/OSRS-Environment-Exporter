package controllers

import javafx.fxml.FXML
import javafx.scene.control.Hyperlink
import javafx.scene.control.Label
import utils.LinkHandler
import utils.PackageMetadata
import java.awt.Desktop
import java.net.URI

class AboutController {
    @FXML
    private lateinit var lblTitle: Label

    @FXML
    private lateinit var lblVersion: Label

    @FXML
    private lateinit var lblAbout: Label

    @FXML
    private lateinit var linkGithub: Hyperlink

    @FXML
    fun initialize() {
        val metadata = PackageMetadata()
        lblTitle.text = metadata.NAME
        lblVersion.text = metadata.VERSION

        linkGithub.setOnAction {
            LinkHandler(linkGithub.text).openInBrowser()
        }

        lblAbout.text = "This application enables exporting Old School RuneScape environments so they can be used in 3D modeling programs like Blender.\n\n" +
                "Credits:\n" +
                "Original idea by Trillion (twitter.com/TrillionStudios).\n" +
                "Based on @tpetrychyn's OSRS Map Editor (github.com/tpetrychyn/osrs-map-editor).\n" +
                "Using changes from @partyvaper's fork (github.com/partyvaper/osrs-map-editor)."
    }
}
