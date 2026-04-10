package com.winlator.xenvironment;

import android.content.Context;

import androidx.annotation.NonNull;

import com.winlator.core.FileUtils;
import com.winlator.xenvironment.components.ALSAServerComponent;
import com.winlator.xenvironment.components.BionicProgramLauncherComponent;
import com.winlator.xenvironment.components.GlibcProgramLauncherComponent;
import com.winlator.xenvironment.components.GuestProgramLauncherComponent;
import com.winlator.xenvironment.components.PulseAudioComponent;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import timber.log.Timber;

public class XEnvironment implements Iterable<EnvironmentComponent> {
    private final Context context;
    private final ImageFs imageFs;
    private final ArrayList<EnvironmentComponent> components = new ArrayList<>();
    private volatile EnvironmentStopSummary lastStopSummary = EnvironmentStopSummary.empty();

    private boolean winetricksRunning = false;

    public synchronized boolean isWinetricksRunning() {
        return winetricksRunning;
    }

    public synchronized void setWinetricksRunning(boolean running) {
        this.winetricksRunning = running;
    }

    public XEnvironment(Context context, ImageFs imageFs) {
        this.context = context;
        this.imageFs = imageFs;
    }

    public Context getContext() {
        return context;
    }

    public ImageFs getImageFs() {
        return imageFs;
    }

    public void addComponent(EnvironmentComponent environmentComponent) {
        environmentComponent.environment = this;
        components.add(environmentComponent);
    }

    public <T extends EnvironmentComponent> T getComponent(Class<T> componentClass) {
        for (EnvironmentComponent component : components) {
            if (component.getClass() == componentClass) return (T)component;
        }
        return null;
    }

    @NonNull
    @Override
    public Iterator<EnvironmentComponent> iterator() {
        return components.iterator();
    }

    public static File getTmpDir(Context context) {
        File tmpDir = new File(context.getFilesDir(), "tmp");
        if (!tmpDir.isDirectory()) {
            tmpDir.mkdirs();
            FileUtils.chmod(tmpDir, 505);
        }
        return tmpDir;
    }

    public void startEnvironmentComponents() {
        FileUtils.clear(getTmpDir(getContext()));
        for (EnvironmentComponent environmentComponent : this) environmentComponent.start();
    }

    public void stopEnvironmentComponents() {
        stopEnvironmentComponentsWithSummary();
    }

    public EnvironmentStopSummary stopEnvironmentComponentsWithSummary() {
        long startNs = System.nanoTime();
        ArrayList<EnvironmentStopSummary.ComponentStopResult> results = new ArrayList<>();
        for (int i = components.size() - 1; i >= 0; i--) {
            EnvironmentComponent environmentComponent = components.get(i);
            long componentStartNs = System.nanoTime();
            boolean success = true;
            String errorMessage = null;
            try {
                environmentComponent.stop();
            } catch (RuntimeException e) {
                success = false;
                errorMessage = e.getMessage();
                Timber.tag("XEnvironment").e(e, "Failed stopping component: %s", environmentComponent.getClass().getSimpleName());
            }
            long componentDurationMs = (System.nanoTime() - componentStartNs) / 1_000_000L;
            results.add(
                    new EnvironmentStopSummary.ComponentStopResult(
                            environmentComponent.getClass().getSimpleName(),
                            componentDurationMs,
                            success,
                            errorMessage
                    )
            );
        }
        long totalDurationMs = (System.nanoTime() - startNs) / 1_000_000L;
        lastStopSummary = new EnvironmentStopSummary(results, totalDurationMs);
        return lastStopSummary;
    }

    public EnvironmentStopSummary getLastStopSummary() {
        return lastStopSummary;
    }

    public void onPause() {
        GuestProgramLauncherComponent guestProgramLauncherComponent = getComponent(GuestProgramLauncherComponent.class);
        if (guestProgramLauncherComponent != null) guestProgramLauncherComponent.suspendProcess();
        GlibcProgramLauncherComponent glibcProgramLauncherComponent = getComponent(GlibcProgramLauncherComponent.class);
        if (glibcProgramLauncherComponent != null) glibcProgramLauncherComponent.suspendProcess();
        BionicProgramLauncherComponent bionicProgramLauncherComponent = getComponent(BionicProgramLauncherComponent.class);
        if (bionicProgramLauncherComponent != null) bionicProgramLauncherComponent.suspendProcess();

        // Pause audio components
        PulseAudioComponent pulseAudioComponent = getComponent(PulseAudioComponent.class);
        if (pulseAudioComponent != null) pulseAudioComponent.pause();
        ALSAServerComponent alsaServerComponent = getComponent(ALSAServerComponent.class);
        if (alsaServerComponent != null) alsaServerComponent.pause();
    }

    public void onResume() {
        // Resume audio FIRST so it's ready when game processes wake up
        PulseAudioComponent pulseAudioComponent = getComponent(PulseAudioComponent.class);
        if (pulseAudioComponent != null) pulseAudioComponent.resume();
        ALSAServerComponent alsaServerComponent = getComponent(ALSAServerComponent.class);
        if (alsaServerComponent != null) alsaServerComponent.resume();

        // Then resume game processes
        GuestProgramLauncherComponent guestProgramLauncherComponent = getComponent(GuestProgramLauncherComponent.class);
        if (guestProgramLauncherComponent != null) guestProgramLauncherComponent.resumeProcess();
        GlibcProgramLauncherComponent glibcProgramLauncherComponent = getComponent(GlibcProgramLauncherComponent.class);
        if (glibcProgramLauncherComponent != null) glibcProgramLauncherComponent.resumeProcess();
        BionicProgramLauncherComponent bionicProgramLauncherComponent = getComponent(BionicProgramLauncherComponent.class);
        if (bionicProgramLauncherComponent != null) bionicProgramLauncherComponent.resumeProcess();
    }
}
