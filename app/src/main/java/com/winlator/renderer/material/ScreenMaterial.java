package com.winlator.renderer.material;

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
