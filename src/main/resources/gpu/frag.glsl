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

#define ALPHA_MODE_BLEND 0
#define ALPHA_MODE_CLIP 1
#define ALPHA_MODE_HASH 2
#define ALPHA_MODE_IGNORE 3
#define ALPHA_MODE_ORDERED_DITHER 4
#define ALPHA_MODE_HEX_DOTS 5

#define HEX_DOTS_RADIUS 6

#include uniforms.glsl

in vec4 Color;
noperspective centroid in float fHsl;
flat in int textureId;
in vec2 fUv;

in vec3 vPosition;

layout(location = 0) out vec4 fragColor;

#include hsl_to_rgb.glsl
#include pcg_hash.glsl
#include bit_twiddle.glsl
#include hex_dots.glsl

void main() {
  vec4 c;

  if (textureId > 0) {
    int textureIdx = textureId - 1;

    vec4 textureColor = texture(textures, vec3(fUv, float(textureIdx)));
    vec4 textureColorBrightness = pow(textureColor, vec4(brightness, brightness, brightness, 1.0f));

    // textured triangles hsl is a 7 bit lightness 2-126
    float light = fHsl / 127.f;
    c = textureColorBrightness * vec4(light, light, light, 1.f);
  } else {
    // pick interpolated hsl or rgb depending on smooth banding setting
    vec3 rgb = hslToRgb(int(fHsl)) * smoothBanding + Color.rgb * (1.f - smoothBanding);
    c = vec4(rgb, Color.a);
  }

  switch (alphaMode) {
    case ALPHA_MODE_CLIP:
      if (c.a < 0.1f) discard;
      break;
    case ALPHA_MODE_HASH:
      uint uhash = pcg32_random_r(floatBitsToUint(gl_FragCoord.x), floatBitsToUint(gl_FragCoord.y), hashSeed << 1);
      uhash = pcg32_random_r(uhash, uint(floatBitsToUint(gl_FragCoord.z / gl_FragCoord.w)), 4u);
      float hash = float(uhash) / 4294967296.0f;
      if (c.a <= hash) discard;
      break;
    case ALPHA_MODE_ORDERED_DITHER:
      uint threshold = bit_reverse8(bit_interleave8(uint(gl_FragCoord.x) ^ uint(gl_FragCoord.y), uint(gl_FragCoord.x)));
      if (c.a <= float(threshold) / 256.0f) discard;
      break;
    case ALPHA_MODE_HEX_DOTS:
      // Dots bounded by a hexagonal grid
      vec2 centre = findHexDotCentre(gl_FragCoord.xy, float(HEX_DOTS_RADIUS));

      // Find the distance from the centre of the hexagon to the fragment coords
      float distSq = distanceSq(centre, gl_FragCoord.xy);

      // If the point is farther from the centre than the radius of a circle
      // scaled by the alpha whose centre is declared above, discard. The
      // radius of the circle is proportional to the square root of the alpha
      // value, so that the alpha value corresponds to the area of the circle
      // rather than the radius.
      // This could easily be rearranged to be in terms of an upper limit on
      // the alpha value, but this is more intuitive.
      if (distSq > c.a * HEX_DOTS_RADIUS * HEX_DOTS_RADIUS) discard;
      break;
    // else, blend or ignore means we don't do anything here
    default:
      break;
  }

  fragColor = c;
}
