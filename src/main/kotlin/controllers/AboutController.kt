package controllers

import javafx.fxml.FXML
import javafx.scene.control.Hyperlink
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import utils.LinkHandler
import utils.PackageMetadata

class AboutController {
    @FXML
    private lateinit var wrapper: VBox

    @FXML
    private lateinit var txtTitle: Label

    @FXML
    private lateinit var txtVersion: Label

    @FXML
    private lateinit var linkGitHub: Hyperlink

    @FXML
    private lateinit var txtDescription: Label

    @FXML
    private lateinit var linkTrillion: Hyperlink

    @FXML
    private lateinit var linkBasedOn: Hyperlink

    @FXML
    private lateinit var linkPartyVaperFork: Hyperlink

    @FXML
    fun initialize() {
        // Set Title and Version based on package metadata
        val metadata = PackageMetadata()
        txtTitle.text = metadata.NAME
        txtVersion.text = metadata.VERSION

        // bind description text and wrapper width
        txtDescription.prefWidthProperty().bind(wrapper.widthProperty())

        // link handlers
        val links = arrayOf(
            Link(linkGitHub, "https://github.com/ConnorDY/OSRS-Environment-Exporter"),
            Link(linkTrillion, "https://twitter.com/TrillionStudios"),
            Link(linkBasedOn, "https://github.com/tpetrychyn/osrs-map-editor"),
            Link(linkPartyVaperFork, "https://github.com/partyvaper/osrs-map-editor")
        )
        links.forEach { link ->
            link.control.setOnAction {
                LinkHandler(link.link).openInBrowser()
            }
        }
    }

    private class Link(val control: Hyperlink, val link: String)
}
