package com.winlator.renderer.material;

public class WindowMaterial extends ShaderMaterial {
    public WindowMaterial() {
        setUniformNames("xform", "viewSize", "texture");
    }

    @Override
    protected String getVertexShader() {
        return
                """
                        uniform float xform[6];
                        uniform vec2 viewSize;
                        attribute vec2 position;
                        varying vec2 vUV;
                        void main() {
                        vUV = position;
                        vec2 transformedPos = applyXForm(position, xform);
                        gl_Position = vec4(2.0 * transformedPos.x / viewSize.x - 1.0, 1.0 - 2.0 * transformedPos.y / viewSize.y, 0.0, 1.0);
                        }"""
                ;
    }

    @Override
    protected String getFragmentShader() {
        return
                """
                        precision mediump float;
                        uniform sampler2D texture;
                        varying vec2 vUV;
                        void main() {
                        gl_FragColor = vec4(texture2D(texture, vUV).rgb, 1.0);
                        }"""
                ;
    }
}
