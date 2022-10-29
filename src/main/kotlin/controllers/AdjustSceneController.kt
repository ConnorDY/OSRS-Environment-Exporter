package controllers

import cache.definitions.RegionDefinition.Companion.X
import cache.definitions.RegionDefinition.Companion.Y
import cache.definitions.RegionDefinition.Companion.Z
import controllers.worldRenderer.ClickHandler
import controllers.worldRenderer.ClickHandler.ClickListener
import controllers.worldRenderer.entities.Entity
import models.scene.Scene
import models.scene.SceneRegion
import models.scene.SceneTile
import utils.Utils.blockAxisToRegionAxis
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Frame
import java.awt.GridBagLayout
import java.awt.event.ActionListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.JTree
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import javax.swing.tree.TreeModel
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

class AdjustSceneController(parent: Frame, title: String, val scene: Scene, clickHandler: ClickHandler) : JDialog(parent, title) {
    private val treeEntities: JTree

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        layout = BorderLayout()

        val treeModel = SceneTreeModel(scene)
        treeEntities = object : JTree(treeModel) {
            override fun convertValueToText(
                value: Any?,
                selected: Boolean,
                expanded: Boolean,
                leaf: Boolean,
                row: Int,
                hasFocus: Boolean
            ): String = treeModel.convertValueToText(value)
        }
        add(JScrollPane(treeEntities))

        val cardLayout = CardLayout()
        val cardPane = JPanel(cardLayout)

        cardPane.add(JLabel("No entity selected", SwingConstants.CENTER), CARD_EMPTY)
        cardPane.add(JLabel("Multiple items selected", SwingConstants.CENTER), CARD_MULTI)

        val entityPane = JPanel(GridBagLayout())
        entityPane.add(JLabel("placeholder"))
        val table = JTable()
        cardPane.add(entityPane, CARD_ENTITY)

        add(cardPane, BorderLayout.EAST)

        pack()

        val sceneChangeListener = ActionListener { treeModel.onSceneChange() }
        scene.sceneChangeListeners.add(sceneChangeListener)
        addWindowListener(object : WindowAdapter() {
            override fun windowClosed(e: WindowEvent?) {
                scene.sceneChangeListeners.remove(sceneChangeListener)
            }
        })

        val clickListener = ClickListener { location ->
            SwingUtilities.invokeLater {
                selectEntity(location.z, location.x, location.y, location.entity)
            }
        }
        clickHandler.clickListeners.add(clickListener)
        addWindowListener(object : WindowAdapter() {
            override fun windowClosed(e: WindowEvent?) {
                clickHandler.clickListeners.remove(clickListener)
            }
        })

        treeEntities.selectionModel.selectionMode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
        treeEntities.addTreeSelectionListener {
            val paths = treeEntities.selectionPaths
            if (paths == null || paths.isEmpty() || !paths.any { it.lastPathComponent is Entity })
                cardLayout.show(cardPane, CARD_EMPTY)
            else if (paths.size > 1)
                cardLayout.show(cardPane, CARD_MULTI)
            else {
                val entity = paths.first().lastPathComponent as Entity
                // TODO.
                cardLayout.show(cardPane, CARD_ENTITY)
            }
        }
    }

    class WrappedRegion(val x: Int, val y: Int, val region: SceneRegion?)
    class WrappedTile(val z: Int, val x: Int, val y: Int, val tile: SceneTile?)
    class XCoord(val region: SceneRegion, val x: Int)
    class YCoord(val region: SceneRegion, val x: Int, val y: Int)

    class SceneTreeModel(val scene: Scene) : TreeModel {
        private val listeners = mutableListOf<TreeModelListener>()

        override fun getRoot(): Any = scene

        override fun getChild(parent: Any, index: Int): Any? = when (parent) {
            is Scene -> {
                val cols = parent.cols
                val rows = parent.rows
                if (index >= 0 && index < rows * cols) {
                    val indexX = index % cols
                    val indexY = index / cols
                    WrappedRegion(indexX, indexY, parent.getRegion(indexX, indexY))
                } else {
                    null
                }
            }
            is WrappedRegion ->
                if (index in 0 until X) XCoord(parent.region!!, index)
                else null
            is XCoord ->
                if (index in 0 until Y) YCoord(parent.region, parent.x, index)
                else null
            is YCoord ->
                if (index in 0 until Z)
                    WrappedTile(index, parent.x, parent.y, parent.region.tiles[index][parent.x][parent.y])
                else null
            is WrappedTile -> {
                if (parent.tile != null && index >= 0 && index < parent.tile.allEntities.size) {
                    parent.tile.allEntities[index]
                } else {
                    null
                }
            }
            else -> throw IllegalArgumentException("Invalid parent type")
        }

        override fun getChildCount(parent: Any): Int = when (parent) {
            is Scene -> parent.cols * parent.rows
            is WrappedRegion -> if (parent.region == null) 0 else X
            is XCoord -> Y
            is YCoord -> Z
            is WrappedTile -> parent.tile?.allEntities?.size ?: 0
            is Entity -> 0
            else -> throw IllegalArgumentException("Invalid parent type")
        }

        override fun isLeaf(node: Any): Boolean =
            (node is WrappedTile && node.tile == null) ||
                (node is WrappedRegion && node.region == null) ||
                node is Entity

        override fun valueForPathChanged(path: TreePath, newValue: Any) {
            // This is only editable through the underlying scene
            throw UnsupportedOperationException()
        }

        override fun getIndexOfChild(parent: Any, child: Any): Int = when (parent) {
            is Scene ->
                if (child is WrappedRegion) child.y * parent.cols + child.x
                else -1
            is WrappedRegion ->
                if (child is XCoord) child.x
                else -1
            is XCoord ->
                if (child is YCoord) child.y
                else -1
            is YCoord ->
                if (child is WrappedTile) child.z
                else -1
            is WrappedTile ->
                if (child is Entity && parent.tile != null) parent.tile.allEntities.indexOf(child)
                else -1
            else -> -1
        }

        override fun addTreeModelListener(l: TreeModelListener?) {
            if (l != null) listeners.add(l)
        }

        override fun removeTreeModelListener(l: TreeModelListener?) {
            if (l != null) listeners.remove(l)
        }

        internal fun convertValueToText(value: Any?): String {
            return when (value) {
                is Scene -> "Scene"
                is WrappedRegion -> {
                    val region = value.region
                    if (region != null)
                        "Region ${region.locationsDefinition.regionId} @ ${value.x}, ${value.y}"
                    else
                        "Empty region @ ${value.x}, ${value.y}"
                }
                is XCoord -> "X = ${value.x}"
                is YCoord -> "Y = ${value.y}"
                is WrappedTile -> "Z = ${value.z}"
                is Entity -> {
                    val def = value.objectDefinition
                    if (def.name == "null") "Object ${def.id}"
                    else "${def.name} (${def.id})"
                }
                else -> value.toString()
            }
        }

        internal fun onSceneChange() {
            listeners.forEach {
                it.treeStructureChanged(TreeModelEvent(this, arrayOf(scene)))
            }
        }
    }

    private fun selectEntity(z: Int, x: Int, y: Int, entity: Entity) {
        val regionX = blockAxisToRegionAxis(x)
        val regionY = blockAxisToRegionAxis(y)
        val blockX = x % X
        val blockY = y % Y

        val region = scene.getRegion(regionX, regionY) ?: return

        val regionNode = WrappedRegion(regionX, regionY, region)
        val xNode = XCoord(region, blockX)
        val yNode = YCoord(region, blockX, blockY)
        val zNode = WrappedTile(z, blockX, blockY, region.tiles[z][blockX][blockY])

        val path = TreePath(arrayOf(scene, regionNode, xNode, yNode, zNode, entity))

        treeEntities.selectionPath = path
        treeEntities.scrollPathToVisible(path)
    }

    companion object {
        private const val CARD_EMPTY = "EMPTY"
        private const val CARD_MULTI = "MULTI"
        private const val CARD_ENTITY = "ENTITY"
    }
}
