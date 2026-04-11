package com.winlator.renderer.effects;

import com.winlator.renderer.material.ScreenMaterial;
import com.winlator.renderer.material.ShaderMaterial;

/**
 * Post-processing edge detection effect for cel-shaded toon rendering.
 * Applies simple Sobel-based edge detection to create outline effects,
 * giving a stylized cartoon appearance to the displayed content.
 */
public class ToonEffect extends Effect {
    @Override
    protected ShaderMaterial createMaterial() {
        return new ToonMaterial();
    }

    private static class ToonMaterial extends ScreenMaterial {
        public ToonMaterial() {
            setUniformNames("screenTexture", "resolution");
        }

        @Override
        protected String getFragmentShader() {
            return
                    """
                            precision highp float;
                            uniform sampler2D screenTexture;
                            uniform vec2 resolution;
                            void main() {
                                vec2 uv = gl_FragCoord.xy / resolution;
                                float edgeThreshold = 0.2;
                                vec2 offset = vec2(1.0) / resolution;
                                vec3 colorCenter = texture2D(screenTexture, uv).rgb;
                                vec3 colorLeft = texture2D(screenTexture, uv - vec2(offset.x, 0.0)).rgb;
                                vec3 colorRight = texture2D(screenTexture, uv + vec2(offset.x, 0.0)).rgb;
                                vec3 colorUp = texture2D(screenTexture, uv - vec2(0.0, offset.y)).rgb;
                                vec3 colorDown = texture2D(screenTexture, uv + vec2(0.0, offset.y)).rgb;
                                float diffHorizontal = length(colorRight - colorLeft);
                                float diffVertical = length(colorUp - colorDown);
                                float edgeFactor = step(edgeThreshold, diffHorizontal + diffVertical);
                                vec3 outlineColor = mix(colorCenter, vec3(0.0), edgeFactor);
                                gl_FragColor = vec4(outlineColor, 1.0);
                            }""";
        }
    }
}
