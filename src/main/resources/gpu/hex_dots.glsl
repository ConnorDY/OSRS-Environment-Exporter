float distanceSq(vec2 a, vec2 b) {
    vec2 d = a - b;
    return dot(d, d);
}

vec2 findHexDotCentre(vec2 pos, float radius) {
    // Find the nearest hexagon centre (each column is offset by half a hexagon)
    // Not based on any well-known algorithm. I scribbled this into a notebook
    // and pulled from my secondary-school-level geometry knowledge.

    // hexagons are oriented horizontally, so height is the shortest distance
    // between opposite edges.
    // You can calculate this by splitting the hexagon into equilateral
    // triangles, splitting one of those into two right-angled triangles, and
    // then using Pythagoras' theorem to find the length of the new side, then
    // doubling that length to span the whole shape.
    float hexHeight = radius * sqrt(3.0f);

    // The width is the corner-to-corner distance, which is much easier to
    // calculate. One full radius away from the circle gets us to the
    // bottom-left corner of another hexagon, and to get to the centre of that
    // we just need to go half a radius more (because the side lengths are the
    // same as the radius). So that leaves the width at 1.5x the radius.
    float hexWidth = radius * 3.0f / 2.0f;

    // Transform the coordinates such that different cases to be considered
    // are all at different integer values.
    vec2 hexSection = gl_FragCoord.xy * vec2(3.0f / hexWidth, 2.0f / hexHeight);

    // x,y coordinates of the hexagon centre.
    // scaled such that 1.0 is the size of a hexagon in each dimension.
    float x, y;

    // Splitting the hexagons into 6 columns allows us to take the trivial
    // cases of back-to-back square strips separately, and then treat the
    // remaining two "border" columns (which are made of triangles and have
    // trickier calculations) as a special case.
    switch (int(hexSection.x) % 6) {
        case 1:
        case 4:
            // Border case
            int offset = int(hexSection.x / 3) % 2;
            bool isInverted = (int(hexSection.y) + offset) % 2 == 0;
            float limit = isInverted ? 1 - fract(hexSection.y) : fract(hexSection.y);

            // Start with a sensible guess: the centre of the hexagon at the
            // floor() of our coordinates.
            x = floor(hexSection.x / 3.0f);
            y = floor(hexSection.y / 2.0f);

            if (fract(hexSection.x) < limit) {
                // We're in the "left" triangle, so our guess is valid unless
                // it was a bottom-left to top-right diagonal, in which case
                // we need to move up one because the hexagon is the one at
                // the top left of our coordinate.
                if (!isInverted) y += 1.0f;
            } else {
                // Otherwise in the "right" triangle, it goes into the
                // "offset" column and the y-value is easier to calculate
                // from the floor of the coordinate, but we also need to move
                // right by one because floor() took us left.
                x += 1.0f;
                y += 0.5f;
            }
            if (offset == 1) {
                // Every other border column is just the previous one but
                // mirrored, so if we get here our assumptions about which
                // side had the offset column were inverted.
                // This compensates for that.
                y += int(hexSection.y) % 2 == 0 ? -0.5f : 0.5f;
            }
            break;
        case 2:
        case 3:
            // Offset hexagon column case
            // These are the columns that are shifted by 0.5 on the y-axis.
            x = floor(hexSection.x / 3.0f + 0.5f);
            y = floor(hexSection.y / 2.0f) + 0.5f;
            break;
        default:
            // Normal hexagon column case
            x = floor(hexSection.x / 3.0f + 0.5f);
            y = floor(hexSection.y / 2.0f + 0.5f);
            break;
    }

    // Transform the coordinates back to the original scale.
    return vec2(x * hexWidth, y * hexHeight);
}
