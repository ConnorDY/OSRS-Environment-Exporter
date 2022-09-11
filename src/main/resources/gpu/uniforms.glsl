#extension GL_ARB_gpu_shader_fp64 : enable

layout(std140) uniform uniforms {
    double cameraYaw;
    double cameraPitch;
    double cameraX;
    double cameraY;
    double cameraZ;
    int centerX;
    int centerY;
    int zoom;
    int currFrame;
};

uniform float brightness;
uniform int drawDistance;
uniform mat4 viewProjectionMatrix;

uniform sampler2DArray textures;
uniform vec2 textureOffsets[128];
uniform float smoothBanding;
uniform ivec2 mouseCoords;
uniform int alphaMode;
uniform uint hashSeed;
