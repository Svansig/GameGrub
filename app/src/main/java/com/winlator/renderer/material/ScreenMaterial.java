package com.winlator.renderer.material;

/**
 * OpenGL shader material for rendering the full-screen display output.
 * Uses a simple vertex shader that maps normalized coordinates directly
 * to screen space, displaying the final composed frame to the user.
 */
public class ScreenMaterial extends ShaderMaterial {
    public ScreenMaterial() {
        setUniformNames("screenTexture", "resolution");
    }

    @Override
    protected String getVertexShader() {
        return
                """
                        attribute vec2 position;
                        varying vec2 vUV;
                        void main() {
                        vUV = position;
                        gl_Position = vec4(2.0 * position.x - 1.0, 2.0 * position.y - 1.0, 0.0, 1.0);
                        }""";
    }
}
