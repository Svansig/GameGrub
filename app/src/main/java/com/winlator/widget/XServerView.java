package com.winlator.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.winlator.renderer.GLRenderer;
import com.winlator.xserver.XServer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * XServerView - OpenGL view for X11 rendering.
 * 
 * Android GLSurfaceView that hosts the XServer:
 * - Uses OpenGL ES 3.0 for rendering
 * - Hosts the GLRenderer
 * - Handles touch/keyboard input
 * - Renders X11 window contents
 * 
 * This is the main view displayed to users.
 */
@SuppressLint("ViewConstructor")
public class XServerView extends GLSurfaceView {
    private static final int EGL_OPENGL_ES3_BIT_KHR = 0x40;
    private final GLRenderer renderer;
    // private final ArrayList<Callback<MotionEvent>> mouseEventCallbacks = new ArrayList<>();
    private final XServer xServer;

    public XServerView(Context context, XServer xServer) {
        super(context);
        setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setEGLContextClientVersion(3);
        getHolder().setFormat(PixelFormat.OPAQUE);
        setEGLConfigChooser(new XServerEglConfigChooser());
        setPreserveEGLContextOnPause(true);
        this.xServer = xServer;
        renderer = new GLRenderer(this, xServer);
        setRenderer(renderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);

        // setOnFocusChangeListener((view, gainFocus) -> {
        //     Log.d("XServerView", "onFocusChange: " + gainFocus + ", isMe: " + (view == this));
        // });
        //
        // requestFocus();
    }
    public XServer getxServer() {
        return xServer;
    }
    // public void onRelease() {
    //     releasePointerCapture();
    //     clearPointerEventListeners();
    // }

    public GLRenderer getRenderer() {
        return renderer;
    }

    private static final class XServerEglConfigChooser implements EGLConfigChooser {
        private static final int[][] FALLBACKS = new int[][]{
                {8, 8, 8, 0},
                {8, 8, 8, 8},
                {5, 6, 5, 0}
        };

        @Override
        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
            for (int[] fallback : FALLBACKS) {
                EGLConfig config = chooseFirstMatchingConfig(egl, display, fallback[0], fallback[1], fallback[2], fallback[3]);
                if (config != null) {
                    return config;
                }
            }
            throw new IllegalArgumentException("No EGL config available for XServerView");
        }

        private EGLConfig chooseFirstMatchingConfig(EGL10 egl, EGLDisplay display, int r, int g, int b, int a) {
            int[] attribList = new int[]{
                    EGL10.EGL_RED_SIZE, r,
                    EGL10.EGL_GREEN_SIZE, g,
                    EGL10.EGL_BLUE_SIZE, b,
                    EGL10.EGL_ALPHA_SIZE, a,
                    EGL10.EGL_DEPTH_SIZE, 0,
                    EGL10.EGL_STENCIL_SIZE, 0,
                    EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT_KHR,
                    EGL10.EGL_SURFACE_TYPE, EGL10.EGL_WINDOW_BIT,
                    EGL10.EGL_NONE
            };

            int[] numConfigs = new int[1];
            if (!egl.eglChooseConfig(display, attribList, null, 0, numConfigs) || numConfigs[0] <= 0) {
                return null;
            }

            EGLConfig[] configs = new EGLConfig[numConfigs[0]];
            if (!egl.eglChooseConfig(display, attribList, configs, configs.length, numConfigs)) {
                return null;
            }

            return configs[0];
        }
    }

    // public void addPointerEventListener(Callback<MotionEvent> listener) {
    //     mouseEventCallbacks.add(listener);
    // }
    // public void removePointerEventListener(Callback<MotionEvent> listener) {
    //     mouseEventCallbacks.remove(listener);
    // }
    // public void clearPointerEventListeners() {
    //     mouseEventCallbacks.clear();
    // }
    // private void emitPointerEvent(MotionEvent event) {
    //     for (Callback<MotionEvent> listener : mouseEventCallbacks) {
    //         listener.call(event);
    //     }
    // }

    // @Override
    // public boolean onCapturedPointerEvent(MotionEvent event) {
    //     Log.d("XServerView", "onCapturedPointerEvent:\n\t" + event);
    //     emitPointerEvent(event);
    //     return true;
    // }


    // @Override
    // public boolean dispatchGenericMotionEvent(MotionEvent event) {
    //     Log.d("XServerView", "dispatchGenericMotionEvent:\n\t" + event);
    //     return super.dispatchGenericMotionEvent(event);
    // }
    //
    // @Override
    // protected boolean dispatchGenericPointerEvent(MotionEvent event) {
    //     Log.d("XServerView", "dispatchGenericPointerEvent:\n\t" + event);
    //     return super.dispatchGenericPointerEvent(event);
    // }
    //
    // @Override
    // protected void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
    //     super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    //     Log.d("XServerView", "Focus changed: " + gainFocus);
    // }
    //
    // @Override
    // public boolean dispatchCapturedPointerEvent(MotionEvent event) {
    //     Log.d("XServerView", "dispatchCapturedPointerEvent:\n\t" + event);
    //     emitPointerEvent(event);
    //     return super.dispatchCapturedPointerEvent(event);
    // }
    //
    // @Override
    // public void onPointerCaptureChange(boolean hasCapture) {
    //     super.onPointerCaptureChange(hasCapture);
    //     Log.d("XServerView", "onPointerCaptureChange: " + hasCapture);
    // }
    //
    // @Override
    // public void onWindowFocusChanged(boolean hasWindowFocus) {
    //     super.onWindowFocusChanged(hasWindowFocus);
    //     if (hasWindowFocus) {
    //         requestPointerCapture();
    //     } else {
    //         releasePointerCapture();
    //     }
    // }
}
