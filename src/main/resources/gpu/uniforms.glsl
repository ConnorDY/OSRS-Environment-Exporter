layout(std140) uniform uniforms {
    int cameraYaw;
    int cameraPitch;
    int centerX;
    int centerY;
    int zoom;
    int cameraX;
    int cameraY;
    int cameraZ;
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
