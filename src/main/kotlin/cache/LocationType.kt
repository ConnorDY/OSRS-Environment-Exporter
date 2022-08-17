package cache

enum class LocationType(val id: Int) {
    /**
     * A wall that is presented lengthwise with respect to the tile.
     */
    LENGTHWISE_WALL(0),

    /**
     * A triangular object positioned in the corner of the tile.
     */
    TRIANGULAR_CORNER(1),

    /**
     * A corner for a wall, where the model is placed on two perpendicular edges of a single tile.
     */
    WALL_CORNER(2),

    /**
     * A rectangular object positioned in the corner of the tile.
     */
    RECTANGULAR_CORNER(3),

    /**
     * A decoration, such as a torch, placed on the inside of a wall.
     */
    INSIDE_WALL_DECORATION(4),

    /**
     * A decoration, such as a torch, placed on the outside of a wall.
     */
    OUTSIDE_WALL_DECORATION(5),

    /**
     * A decoration, such as a torch, placed on the outside of a diagonal wall.
     */
    DIAGONAL_OUTSIDE_WALL_DECORATION(6),

    /**
     * A decoration, such as a torch, placed on the inside of a diagonal wall.
     */
    DIAGONAL_INSIDE_WALL_DECORATION(7),

    /**
     * A decoration, such as a window, placed on both sides of a diagonal wall.
     */
    DIAGONAL_WALL_DECORATION(8),

    /**
     * A wall joint that is presented diagonally with respect to the tile.
     */
    DIAGONAL_WALL(9),

    /**
     * An object that can be interacted with by a player.
     */
    INTERACTABLE(10),

    /**
     * An [INTERACTABLE] object, rotated `pi / 2` radians.
     */
    DIAGONAL_INTERACTABLE(11),

    /**
     * A roof tile with a vertical slope.
     */
    SLOPED_ROOF(12),

    /**
     * As [SLOPED_ROOF], but a diagonal outer corner.
     */
    SLOPED_ROOF_OUTER_CORNER(13),

    /**
     * As [SLOPED_ROOF], but a diagonal inner corner.
     * i.e. concave roof shapes, or tessellation with the other corner tile
     * to create diagonal roofs.
     */
    SLOPED_ROOF_INNER_CORNER(14),

    /**
     * As [SLOPED_ROOF_INNER_CORNER], but with a seam running vertically.
     */
    SLOPED_ROOF_HARD_INNER_CORNER(15),

    /**
     * As [SLOPED_ROOF_OUTER_CORNER], but with a seam running vertically.
     */
    SLOPED_ROOF_HARD_OUTER_CORNER(16),

    /**
     * A flat rooftop tile.
     */
    FLAT_ROOF(17),

    /**
     * A sloped overhanging roof tile.
     */
    SLOPED_ROOF_OVERHANG(18),

    /**
     * As [SLOPED_ROOF_OVERHANG], but a diagonal outer corner.
     */
    SLOPED_ROOF_OVERHANG_OUTER_CORNER(19),

    /**
     * As [SLOPED_ROOF_OVERHANG], but a diagonal inner corner.
     */
    SLOPED_ROOF_OVERHANG_INNER_CORNER(20),

    /**
     * As [SLOPED_ROOF_OVERHANG_OUTER_CORNER], but with a seam running vertically.
     */
    SLOPED_ROOF_OVERHANG_HARD_OUTER_CORNER(21),

    /**
     * A decoration positioned on the floor.
     */
    FLOOR_DECORATION(22),

    TILE_PAINT(30),
    TILE_MODEL(31);

    companion object {
        fun fromId(id: Int) = values().firstOrNull { it.id == id }
    }
}
