/*
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#version 450

layout (location = 0) in ivec4 VertexPosition;
layout (location = 1) in vec4 uv;
layout (location = 2) in int pickerId;

//layout (location = 3) in ivec4 animInfo;

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

layout(std430, binding = 12) buffer blocksData
{
    int selectedIds[];
};

uniform float brightness;
uniform int drawDistance;

uniform int hoverId;

out ivec3 vPosition;
out vec4 vColor;
out float vHsl;
out vec4 vUv;

flat out int o_pickerId;

#include hsl_to_rgb.glsl

void main()
{
    ivec3 vertex = VertexPosition.xyz;
    int ahsl = VertexPosition.w;
    int hsl = ahsl & 0xffff;
    float a = float(ahsl >> 24 & 0xff) / 255.f;

    vec3 rgb = hslToRgb(hsl);

    vPosition = ivec3(vertex.x, vertex.y, vertex.z);
    vColor = vec4(rgb, 1.f - a);
    vHsl = float(hsl);
    vUv = uv;

    // animation logic
//    int frame = animInfo.x;
//    if (frame > -1) {
//        int frameDuration = animInfo.y;
//        int frameOffset = animInfo.z;
//        int totalFrames = animInfo.w;
//
//        // apparently this multiply and shift is faster than modulo
//        int c = currFrame % totalFrames;
//        if (c < frameOffset || c > frameOffset + frameDuration) {
//            vColor.a = 0;
//            vHsl = 0;
//            return;
//        }
//    }
    // end animation logic

    // picking logic
    o_pickerId = pickerId;

    if (hoverId == pickerId) {
        vColor.a = 0.8;
        vHsl = 11111;
    }

    int x = (pickerId >> 18) & 0x1FFF;
    int y = (pickerId >> 5) & 0x1FFF;
    int type = pickerId & 0x1F;
    int idx = x + 64 * (y + 64 * type);
    if (selectedIds[idx] == 1) {
        vColor.a = 0.8;
        vHsl = 55555;
    }
    // end picking logic
}
