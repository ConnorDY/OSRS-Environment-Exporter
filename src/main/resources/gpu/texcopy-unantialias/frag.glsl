#version 330

in vec2 frag_uv;

uniform sampler2DMS tex;

layout(location = 0) out vec4 out_color;

void main() {
    out_color = texelFetch(tex, ivec2(frag_uv * textureSize(tex)), 0);
}
