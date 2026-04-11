package com.winlator.xenvironment.components;

import androidx.annotation.Keep;

import app.gamegrub.BuildConfig;
import com.winlator.renderer.GLRenderer;
import com.winlator.renderer.Texture;
import com.winlator.xconnector.Client;
import com.winlator.xconnector.ConnectionHandler;
import com.winlator.xconnector.RequestHandler;
import com.winlator.xconnector.UnixSocketConfig;
import com.winlator.xconnector.XConnectorEpoll;
import com.winlator.xenvironment.EnvironmentComponent;
import com.winlator.xserver.Drawable;
import com.winlator.xserver.XServer;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import timber.log.Timber;

public class VirGLRendererComponent extends EnvironmentComponent implements ConnectionHandler, RequestHandler {
    private static final String TAG = "VirGLRendererComponent";
    private static final long EGL_CONTEXT_WAIT_TIMEOUT_MS = 250L;
    private static final long REQUEST_TIMING_SAMPLE_MASK = 0x3fL;
    private static final long FLUSH_TIMING_SAMPLE_MASK = 0x1fL;

    private final XServer xServer;
    private final UnixSocketConfig socketConfig;
    private XConnectorEpoll connector;
    private long sharedEGLContextPtr;
    private final PerfStats perfStats = new PerfStats();

    static {
        System.loadLibrary("virglrenderer");
    }

    public VirGLRendererComponent(XServer xServer, UnixSocketConfig socketConfig) {
        this.xServer = xServer;
        this.socketConfig = socketConfig;
    }

    @Override
    public void start() {
        Timber.tag(TAG).d("Starting...");
        if (connector != null) return;
        prefetchSharedEGLContextAsync();
        connector = new XConnectorEpoll(socketConfig, this, this);
        connector.start();
    }

    @Override
    public void stop() {
        Timber.tag(TAG).d("Stopping...");
        if (connector != null) {
            connector.stop();
            connector = null;
        }

        if (BuildConfig.DEBUG) {
            Timber.tag(TAG).d(perfStats.dumpAndReset());
        }
    }

    @Keep
    private void killConnection(int fd) {
        connector.killConnection(connector.getClient(fd));
    }

    @Keep
    private long getSharedEGLContext() {
        if (sharedEGLContextPtr != 0) {
            return sharedEGLContextPtr;
        }

        long waitStartNs = System.nanoTime();
        CountDownLatch latch = new CountDownLatch(1);
        try {
            GLRenderer renderer = xServer.getRenderer();
            renderer.xServerView.queueEvent(() -> {
                sharedEGLContextPtr = getCurrentEGLContextPtr();
                latch.countDown();
            });
            if (!latch.await(EGL_CONTEXT_WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                if (BuildConfig.DEBUG) {
                    Timber.tag(TAG).w("Timed out waiting for shared EGL context");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (BuildConfig.DEBUG) {
                Timber.tag(TAG).w(e, "Interrupted while waiting for shared EGL context");
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                Timber.tag(TAG).w(e, "Failed to acquire shared EGL context");
            }
        }

        perfStats.sharedContextWaitCalls.incrementAndGet();
        perfStats.sharedContextWaitNs.addAndGet(System.nanoTime() - waitStartNs);
        return sharedEGLContextPtr;
    }

    @Override
    public void handleConnectionShutdown(Client client) {
        long clientPtr = (long)client.getTag();
        destroyClient(clientPtr);
    }

    @Override
    public void handleNewConnection(Client client) {
        getSharedEGLContext();
        long clientPtr = handleNewConnection(client.clientSocket.fd);
        client.setTag(clientPtr);
    }

    @Override
    public boolean handleRequest(Client client) throws IOException {
        long callCount = perfStats.requestCalls.incrementAndGet();
        long clientPtr = (long)client.getTag();
        if ((callCount & REQUEST_TIMING_SAMPLE_MASK) == 0) {
            long startNs = System.nanoTime();
            handleRequest(clientPtr);
            perfStats.requestSampledCalls.incrementAndGet();
            perfStats.requestSampledNs.addAndGet(System.nanoTime() - startNs);
        } else {
            handleRequest(clientPtr);
        }
        return true;
    }

    @Keep
    private void flushFrontbuffer(int drawableId, int framebuffer) {
        long callCount = perfStats.flushCalls.incrementAndGet();
        long startNs = 0L;
        if ((callCount & FLUSH_TIMING_SAMPLE_MASK) == 0) {
            startNs = System.nanoTime();
        }

        Drawable drawable = xServer.drawableManager.getDrawable(drawableId);
        if (drawable == null) return;

        synchronized (drawable.renderLock) {
            drawable.setData(null);
            Texture texture = drawable.getTexture();
            texture.copyFromFramebuffer(framebuffer, drawable.width, drawable.height);
        }

        Runnable onDrawListener = drawable.getOnDrawListener();
        if (onDrawListener != null) onDrawListener.run();

        if (startNs != 0L) {
            perfStats.flushSampledCalls.incrementAndGet();
            perfStats.flushSampledNs.addAndGet(System.nanoTime() - startNs);
        }
    }

    private void prefetchSharedEGLContextAsync() {
        if (sharedEGLContextPtr != 0) {
            return;
        }

        try {
            GLRenderer renderer = xServer.getRenderer();
            renderer.xServerView.queueEvent(() -> sharedEGLContextPtr = getCurrentEGLContextPtr());
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                Timber.tag(TAG).w(e, "Unable to prefetch shared EGL context");
            }
        }
    }

    public String dumpPerfStats(boolean reset) {
        return perfStats.dump(reset);
    }

    private static final class PerfStats {
        private final AtomicLong requestCalls = new AtomicLong();
        private final AtomicLong requestSampledCalls = new AtomicLong();
        private final AtomicLong requestSampledNs = new AtomicLong();
        private final AtomicLong flushCalls = new AtomicLong();
        private final AtomicLong flushSampledCalls = new AtomicLong();
        private final AtomicLong flushSampledNs = new AtomicLong();
        private final AtomicLong sharedContextWaitCalls = new AtomicLong();
        private final AtomicLong sharedContextWaitNs = new AtomicLong();

        private String dumpAndReset() {
            return dump(true);
        }

        private String dump(boolean reset) {
            long requestCallsValue = reset ? requestCalls.getAndSet(0) : requestCalls.get();
            long requestSampledCallsValue = reset ? requestSampledCalls.getAndSet(0) : requestSampledCalls.get();
            long requestSampledNsValue = reset ? requestSampledNs.getAndSet(0) : requestSampledNs.get();
            long flushCallsValue = reset ? flushCalls.getAndSet(0) : flushCalls.get();
            long flushSampledCallsValue = reset ? flushSampledCalls.getAndSet(0) : flushSampledCalls.get();
            long flushSampledNsValue = reset ? flushSampledNs.getAndSet(0) : flushSampledNs.get();
            long sharedWaitCallsValue = reset ? sharedContextWaitCalls.getAndSet(0) : sharedContextWaitCalls.get();
            long sharedWaitNsValue = reset ? sharedContextWaitNs.getAndSet(0) : sharedContextWaitNs.get();

            long requestAvgUs = requestSampledCallsValue == 0 ? 0 : (requestSampledNsValue / requestSampledCallsValue) / 1000;
            long flushAvgUs = flushSampledCallsValue == 0 ? 0 : (flushSampledNsValue / flushSampledCallsValue) / 1000;
            long sharedWaitAvgUs = sharedWaitCallsValue == 0 ? 0 : (sharedWaitNsValue / sharedWaitCallsValue) / 1000;

            return "VirGL JNI perf: requests{calls=" + requestCallsValue + ",sampled=" + requestSampledCallsValue + ",avgUs=" + requestAvgUs + "} " +
                    "flush{calls=" + flushCallsValue + ",sampled=" + flushSampledCallsValue + ",avgUs=" + flushAvgUs + "} " +
                    "sharedEglWait{calls=" + sharedWaitCallsValue + ",avgUs=" + sharedWaitAvgUs + "}";
        }
    }

    private native long handleNewConnection(int fd);

    private native void handleRequest(long clientPtr);

    private native long getCurrentEGLContextPtr();

    private native void destroyClient(long clientPtr);

    private native void destroyRenderer(long clientPtr);
}
