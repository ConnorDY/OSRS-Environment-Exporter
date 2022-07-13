package controllers

import javafx.fxml.FXML
import javafx.scene.control.Hyperlink
import javafx.scene.control.Label
import javafx.scene.text.Font
import utils.LinkHandler
import utils.PackageMetadata

class AboutController {
    @FXML
    private lateinit var lblTitle: Label

    @FXML
    private lateinit var lblVersion: Label

    @FXML
    private lateinit var linkGitHub: Hyperlink

    @FXML
    fun initialize() {
        // Set Title and Version based on package metadata
        val metadata = PackageMetadata()
        lblTitle.text = metadata.NAME
        lblVersion.text = metadata.VERSION

        // GitHub Link handler
        linkGitHub.setOnAction {
            LinkHandler(linkGitHub.text).openInBrowser()
        }
    }
}
