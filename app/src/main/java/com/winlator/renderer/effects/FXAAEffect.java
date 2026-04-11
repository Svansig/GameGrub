package com.winlator.renderer.effects;

import com.winlator.renderer.material.ScreenMaterial;
import com.winlator.renderer.material.ShaderMaterial;

/**
 * Post-processing anti-aliasing effect using the FXAA algorithm.
 * Detects edges based on luminance and applies efficient smoothing
 * to reduce jagged aliasing artifacts without multi-sampling overhead.
 */
public class FXAAEffect extends Effect {
    @Override
    protected ShaderMaterial createMaterial() {
        return new FXAAMaterial();
    }

    private static class FXAAMaterial extends ScreenMaterial {
        @Override
        protected String getFragmentShader() {
            return
                    """
                            precision highp float;
                            #define FXAA_MIN_REDUCE (1.0 / 128.0)
                            #define FXAA_MUL_REDUCE (1.0 / 8.0)
                            #define MAX_SPAN 8.0
                            uniform sampler2D screenTexture;
                            uniform vec2 resolution;
                            const vec3 luma = vec3(0.299, 0.587, 0.114);
                            void main() {
                                vec2 invResolution = 1.0 / resolution;
                                vec3 rgbNW = texture2D(screenTexture, (gl_FragCoord.xy + vec2(-1.0, -1.0)) * invResolution).rgb;
                                vec3 rgbNE = texture2D(screenTexture, (gl_FragCoord.xy + vec2( 1.0, -1.0)) * invResolution).rgb;
                                vec3 rgbSW = texture2D(screenTexture, (gl_FragCoord.xy + vec2(-1.0,  1.0)) * invResolution).rgb;
                                vec3 rgbSE = texture2D(screenTexture, (gl_FragCoord.xy + vec2( 1.0,  1.0)) * invResolution).rgb;
                                vec3 rgbM  = texture2D(screenTexture,  gl_FragCoord.xy * invResolution).rgb;
                                float lumaNW = dot(rgbNW, luma);
                                float lumaNE = dot(rgbNE, luma);
                                float lumaSW = dot(rgbSW, luma);
                                float lumaSE = dot(rgbSE, luma);
                                float lumaM  = dot(rgbM,  luma);
                                float lumaMin = min(lumaM, min(min(lumaNW, lumaNE), min(lumaSW, lumaSE)));
                                float lumaMax = max(lumaM, max(max(lumaNW, lumaNE), max(lumaSW, lumaSE)));
                                vec2 dir;
                                dir.x = -((lumaNW + lumaNE) - (lumaSW + lumaSE));
                                dir.y =  ((lumaNW + lumaSW) - (lumaNE + lumaSE));
                                float dirReduce = max((lumaNW + lumaNE + lumaSW + lumaSE) * 0.25 * FXAA_MUL_REDUCE, FXAA_MIN_REDUCE);
                                float minDirFactor = 1.0 / (min(abs(dir.x), abs(dir.y)) + dirReduce);
                                dir = clamp(dir * minDirFactor, vec2(-MAX_SPAN), vec2(MAX_SPAN)) * invResolution;
                                vec4 rgbA = 0.5 * (
                                    texture2D(screenTexture, gl_FragCoord.xy * invResolution + dir * (1.0 / 3.0 - 0.5)) +
                                    texture2D(screenTexture, gl_FragCoord.xy * invResolution + dir * (2.0 / 3.0 - 0.5)));
                                vec4 rgbB = rgbA * 0.5 + 0.25 * (
                                    texture2D(screenTexture, gl_FragCoord.xy * invResolution + dir * -0.5) +
                                    texture2D(screenTexture, gl_FragCoord.xy * invResolution + dir *  0.5));
                                float lumaB = dot(rgbB, vec4(luma, 0.0));
                                gl_FragColor = lumaB < lumaMin || lumaB > lumaMax ? rgbA : rgbB;
                            }""";
        }
    }
}
