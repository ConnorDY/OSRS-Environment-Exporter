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

#version 330

layout(triangles) in;
layout(triangle_strip, max_vertices = 3) out;

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

uniform mat4 projectionMatrix;
uniform int drawDistance;

in vec4 vColor[];
in float vHsl[];
in vec4 vUv[];

out vec4 Color;
centroid out float fHsl;
out vec4 fUv;

flat in int o_pickerId[];
flat out int frag_pickerId;

#include to_screen.glsl

void main() {
  if (-gl_in[0].gl_Position.z < 0 || -gl_in[1].gl_Position.z < 0 || -gl_in[2].gl_Position.z < 0
    || -gl_in[0].gl_Position.z > drawDistance || -gl_in[1].gl_Position.z > drawDistance|| -gl_in[2].gl_Position.z > drawDistance) {
    return;
  }

  Color = vColor[0];
  fHsl = vHsl[0];
  fUv = vUv[0];
  gl_Position = projectionMatrix * gl_in[0].gl_Position;
  frag_pickerId = o_pickerId[0];
  EmitVertex();

  Color = vColor[1];
  fHsl = vHsl[1];
  fUv = vUv[1];
  gl_Position  = projectionMatrix * gl_in[1].gl_Position;
  frag_pickerId = o_pickerId[1];
  EmitVertex();

  Color = vColor[2];
  fHsl = vHsl[2];
  fUv = vUv[2];
  gl_Position  = projectionMatrix * gl_in[2].gl_Position;
  frag_pickerId = o_pickerId[2];
  EmitVertex();

  EndPrimitive();
}
