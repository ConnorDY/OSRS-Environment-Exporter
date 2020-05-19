import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.web.HTMLEditor
import javafx.stage.Stage
import org.dockfx.DockNode
import org.dockfx.DockPane
import org.dockfx.DockPos
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class DockFX : Application() {
    override fun start(primaryStage: Stage) {
        primaryStage.title = "DockFX"

        // create a dock pane that will manage our dock nodes and handle the layout
        val dockPane = DockPane()

        // create a default test node for the center of the dock area
        val tabs = TabPane()
        val htmlEditor = HTMLEditor()
        try {
            htmlEditor.htmlText = String(Files.readAllBytes(Paths.get("readme.html")))
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // empty tabs ensure that dock node has its own background color when floating
        tabs.tabs.addAll(
            Tab("Tab 1", htmlEditor),
            Tab("Tab 2"),
            Tab("Tab 3")
        )
        val tableView = TableView<String>()
        // this is why @SupressWarnings is used above
        // we don't care about the warnings because this is just a demonstration
        // for docks not the table view
        tableView.columns.addAll(
            TableColumn<String, String>("A"),
            TableColumn<String, String>("B"),
            TableColumn<String, String>("C")
        )

        // load an image to caption the dock nodes
        val dockImage =
            Image(DockFX::class.java.getResource("docknode.png").toExternalForm())

        // create and dock some prototype dock nodes to the middle of the dock pane
        // the preferred sizes are used to specify the relative size of the node
        // to the other nodes

        // we can use this to give our central content a larger area where
        // the top and bottom nodes have a preferred width of 300 which means that
        // when a node is docked relative to them such as the left or right dock below
        // they will have 300 / 100 + 300 (400) or 75% of their previous width
        // after both the left and right node's are docked the center docks end up with 50% of the width
        val tabsDock = DockNode(tabs, "Tabs Dock", ImageView(dockImage))
        tabsDock.setPrefSize(300.0, 100.0)
        tabsDock.dock(dockPane, DockPos.TOP)
        val tableDock = DockNode(tableView)
        // let's disable our table from being undocked
        tableDock.dockTitleBar = null
        tableDock.setPrefSize(300.0, 100.0)
        tableDock.dock(dockPane, DockPos.BOTTOM)
        val menu1 = Menu("File")
        val menu2 = Menu("Options")
        val menu3 = Menu("Help")
        val menuBar = MenuBar()
        menuBar.menus.addAll(menu1, menu2, menu3)
        val toolBar = ToolBar(
            Button("New"),
            Button("Open"),
            Button("Save"),
            Separator(),
            Button("Clean"),
            Button("Compile"),
            Button("Run"),
            Separator(),
            Button("Debug"),
            Button("Profile")
        )
        val vbox = VBox()
        vbox.children.addAll(menuBar, toolBar, dockPane)
        VBox.setVgrow(dockPane, Priority.ALWAYS)
        primaryStage.scene = Scene(vbox, 800.0, 500.0)
        primaryStage.sizeToScene()
        primaryStage.show()

        // can be created and docked before or after the scene is created
        // and the stage is shown
        var treeDock = DockNode(generateRandomTree(), "Tree Dock", ImageView(dockImage))
        treeDock.setPrefSize(100.0, 100.0)
        treeDock.dock(dockPane, DockPos.LEFT)
        treeDock = DockNode(generateRandomTree(), "Tree Dock", ImageView(dockImage))
        treeDock.setPrefSize(100.0, 100.0)
        treeDock.dock(dockPane, DockPos.RIGHT)

        // test the look and feel with both Caspian and Modena
        setUserAgentStylesheet(STYLESHEET_MODENA)
        // initialize the default styles for the dock pane and undocked nodes using the DockFX
        // library's internal Default.css stylesheet
        // unlike other custom control libraries this allows the user to override them globally
        // using the style manager just as they can with internal JavaFX controls
        // this must be called after the primary stage is shown
        // https://bugs.openjdk.java.net/browse/JDK-8132900
        DockPane.initializeDefaultUserAgentStylesheet()

        // TODO: after this feel free to apply your own global stylesheet using the StyleManager class
    }

    private fun generateRandomTree(): TreeView<String> {
        // create a demonstration tree view to use as the contents for a dock node
        val root = TreeItem("Root")
        val treeView = TreeView(root)
        treeView.isShowRoot = false

        // populate the prototype tree with some random nodes
        val rand = Random()
        for (i in 4 + rand.nextInt(8) downTo 1) {
            val treeItem = TreeItem("Item $i")
            root.children.add(treeItem)
            for (j in 2 + rand.nextInt(4) downTo 1) {
                val childItem = TreeItem("Child $j")
                treeItem.children.add(childItem)
            }
        }
        return treeView
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(*args)
        }
    }
}