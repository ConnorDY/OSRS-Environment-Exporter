#version 330

layout (location = 0) in vec3 pos;
layout (location = 1) in vec2 uv;

out vec2 frag_uv;

void main()
{
    // No transformation needed, as we are using normalized device coordinates
    gl_Position = vec4(pos, 1.0);
    frag_uv = uv;
}
