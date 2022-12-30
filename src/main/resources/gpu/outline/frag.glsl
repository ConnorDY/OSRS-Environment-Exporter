#version 330

in vec2 frag_uv;

uniform sampler2D tex;

layout(location = 0) out vec4 out_color;

const int radius = 5;
const float radius_sq = float(radius * radius);

const float color_intensity_threshold = 0.1;
const float color_hue_threshold = 0.1;

bool colors_are_similar(vec3 a, vec3 b) {
    return a == b;
    // TODO: temporary code, will only need to compare equality in the future
    float intensity_a = dot(a, vec3(0.2126, 0.7152, 0.0722));
    float intensity_b = dot(b, vec3(0.2126, 0.7152, 0.0722));
    float intensity_diff = abs(intensity_a - intensity_b);
    if (intensity_diff > color_intensity_threshold) {
        return false;
    }

    if (a == vec3(0.0) || b == vec3(0.0)) {
        return true;
    }

    float hue_similarity = dot(normalize(a), normalize(b));
    return hue_similarity >= 1.0f - color_hue_threshold;
}

void main() {
    vec2 texel_size = 1.0 / textureSize(tex, 0);

    vec3 result = vec3(0.0);
    uint count = 0u;

    vec3 my_color = texture(tex, frag_uv).rgb;

    for (int x = -radius; x <= radius; ++x) {
        for (int y = -radius; y <= radius; ++y) {
            vec2 offset_texels = vec2(x, y);
            // test that the point lies within a circle
            if (dot(offset_texels, offset_texels) > radius_sq) {
                // should be the same across all executions
                // so branching large parts of the code is fine
                continue;
            }
            vec2 offset = offset_texels * texel_size;
            vec3 texel = texture(tex, frag_uv + offset).rgb;
            if (colors_are_similar(texel, my_color)) {
                texel = vec3(0.0);
            }
            result += texel;
            if (texel != vec3(0.0)) {
                ++count;
            }
        }
    }

    float alpha = 1.0;

    if (count == 0u) {
        alpha = 0.0;
    } else {
        result /= float(count);
    }

    out_color = vec4(result, alpha);
}
