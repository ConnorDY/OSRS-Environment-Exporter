#version 330

in vec2 frag_uv;

uniform sampler2D tex;

layout(location = 0) out vec4 out_color;

void main() {
    out_color = texture(tex, frag_uv);
}
