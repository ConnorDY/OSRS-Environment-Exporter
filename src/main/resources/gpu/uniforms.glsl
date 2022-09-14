layout(std140) uniform uniforms {
    float cameraYaw;
    float cameraPitch;
    float cameraX;
    float cameraY;
    float cameraZ;
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
