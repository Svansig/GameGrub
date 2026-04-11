package com.winlator.renderer.effects;

import com.winlator.renderer.GLRenderer;
import com.winlator.renderer.material.ShaderMaterial;

/**
 * Base class for post-processing visual effects.
 * Provides lifecycle management for shader-based screen effects
 * like CRT, NTSC, FXAA, color correction, and toon shading.
 */
public abstract class Effect {
    private ShaderMaterial material;

    protected abstract ShaderMaterial createMaterial();

    protected void onUse(ShaderMaterial material, GLRenderer renderer) {
    }

    public final ShaderMaterial getMaterial() {
        if (material == null) {
            material = createMaterial();
        }
        return material;
    }

    public final void use(GLRenderer renderer) {
        ShaderMaterial shaderMaterial = getMaterial();
        shaderMaterial.use();
        onUse(shaderMaterial, renderer);
    }

    public void destroy() {
        if (material != null) {
            material.destroy();
            material = null;
        }
    }
}
