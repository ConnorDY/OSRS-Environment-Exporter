package controllers

import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.control.ContentDisplay
import javafx.scene.control.Hyperlink
import javafx.scene.control.Label
import javafx.scene.control.Labeled
import javafx.scene.text.Font
import utils.LinkHandler
import utils.PackageMetadata

class AboutController {
    private val fontTitle = Font(18.0)
    private val fontDefault = Font(14.0)

    @FXML
    private lateinit var lblTitle: Label

    @FXML
    private lateinit var lblVersion: Label

    @FXML
    private lateinit var linkGithub: Hyperlink

    @FXML
    private lateinit var lblDescription: Label

    @FXML
    private lateinit var lblCredits: Label

    @FXML
    private lateinit var lblCreditsLine1: Label

    @FXML
    private lateinit var lblCreditsLine2: Label

    @FXML
    private lateinit var lblCreditsLine3: Label

    private fun centerControl(control: Labeled) {
        control.alignment = Pos.CENTER
        control.contentDisplay = ContentDisplay.CENTER
        control.maxWidth = 1.7976931348623157E308
    }

    @FXML
    fun initialize() {
        // Set fonts and alignment
        lblTitle.font = fontTitle
        centerControl(lblTitle)

        linkGithub.font = fontDefault

        arrayOf(lblVersion, lblDescription, lblCredits, lblCreditsLine1, lblCreditsLine2, lblCreditsLine3).forEach {
            it.font = fontDefault
            centerControl(it)
        }

        // Set Title and Version based on package metadata
        val metadata = PackageMetadata()
        lblTitle.text = metadata.NAME
        lblVersion.text = metadata.VERSION

        // GitHub Link handler
        linkGithub.setOnAction {
            LinkHandler(linkGithub.text).openInBrowser()
        }
    }
}
