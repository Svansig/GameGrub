package com.winlator.xenvironment.components;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import com.winlator.core.AppUtils;
import com.winlator.core.FileUtils;
import com.winlator.core.ProcessHelper;
import com.winlator.core.envvars.EnvVars;
import com.winlator.xconnector.UnixSocketConfig;
import com.winlator.xenvironment.EnvironmentComponent;
import com.winlator.xenvironment.XEnvironment;

import java.io.File;

import timber.log.Timber;

public class PulseAudioComponent extends EnvironmentComponent {
    private final UnixSocketConfig socketConfig;
    private static int pid = -1;
    private static final Object lock = new Object();
    private float volume = 1.0f;
    private byte performanceMode = 1;
    private boolean isPaused = false;
    private static final long SOCKET_READY_TIMEOUT_MS = 1500;
    private static final long SOCKET_READY_POLL_MS = 50;

    public PulseAudioComponent(UnixSocketConfig socketConfig) {
        this.socketConfig = socketConfig;
    }

    @Override
    public void start() {
        Timber.tag("PulseAudioComponent").d("Starting...");
        synchronized (lock) {
            stop();
            pid = execPulseAudio();
            if (pid <= 0) {
                Timber.tag("PulseAudioComponent").e("PulseAudio failed to start (pid=%d)", pid);
            } else if (!waitForSocketReady()) {
                Timber.tag("PulseAudioComponent").w("PulseAudio pid=%d started but socket did not become ready at %s", pid, socketConfig.path);
            } else {
                Timber.tag("PulseAudioComponent").d("PulseAudio socket is ready at %s", socketConfig.path);
            }
            isPaused = false;
        }
    }

    @Override
    public void stop() {
        Timber.tag("PulseAudioComponent").d("Stopping...");
        synchronized (lock) {
            if (pid != -1) {
                Process.killProcess(pid);
                pid = -1;
            }
            isPaused = false;
        }
    }

    public void pause() {
        Timber.tag("PulseAudioComponent").d("Pausing...");
        synchronized (lock) {
            if (!isPaused && pid != -1) {
                executePactl(true);
                isPaused = true;
                final int capturedPid = pid;
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    synchronized (lock) {
                        if (isPaused && capturedPid == pid) {
                            ProcessHelper.suspendProcess(capturedPid);
                            Timber.tag("PulseAudioComponent").d("Audio paused");
                        }
                    }
                }, 200);
            }
        }
    }

    public void resume() {
        Timber.tag("PulseAudioComponent").d("Resuming...");
        synchronized (lock) {
            if (isPaused && pid != -1) {
                final int capturedPid = pid;
                ProcessHelper.resumeProcess(capturedPid);
                isPaused = false;
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    synchronized (lock) {
                        if (!isPaused && capturedPid == pid) {
                            executePactl(false);
                            Timber.tag("PulseAudioComponent").d("Audio resumed");
                        }
                    }
                }, 200);
            }
        }
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public void setPerformanceMode(int performanceMode) {
        this.performanceMode = (byte) performanceMode;
    }

    private int execPulseAudio() {
        Context context = environment.getContext();
        String nativeLibraryDir = context.getApplicationInfo().nativeLibraryDir;
        // nativeLibraryDir = nativeLibraryDir.replace("arm64", "arm64-v8a");
        File workingDir = new File(context.getFilesDir(), "/pulseaudio");
        if (!workingDir.isDirectory()) {
            workingDir.mkdirs();
            FileUtils.chmod(workingDir, 505);
        }

        File configFile = new File(workingDir, "default.pa");
        FileUtils.writeString(configFile, String.join("\n",
                "load-module module-native-protocol-unix auth-anonymous=1 auth-cookie-enabled=0 socket=\""+socketConfig.path+"\"",
                "load-module module-aaudio-sink volume=" + this.volume + " performance_mode=" + ((int) this.performanceMode),
                "set-default-sink AAudioSink"
        ));

        String archName = AppUtils.getArchName();
        File modulesDir = new File(workingDir, "modules");
        if (!modulesDir.isDirectory()) {
            Timber.tag("PulseAudioComponent").w("PulseAudio modules directory not found: %s", modulesDir.getAbsolutePath());
        }

        EnvVars envVars = new EnvVars();
        envVars.put("LD_LIBRARY_PATH", "/system/lib64:"+nativeLibraryDir+":"+modulesDir);
        envVars.put("HOME", workingDir);
        envVars.put("TMPDIR", XEnvironment.getTmpDir(context));

        Timber.tag("PulseAudioComponent").d("Launching pulseaudio (arch=%s) with socket=%s", archName, socketConfig.path);

        String command = nativeLibraryDir+"/libpulseaudio.so";
        command += " --system=false";
        command += " --disable-shm=true";
        command += " --fail=false";
        command += " -n --file=default.pa";
        command += " --daemonize=false";
        command += " --use-pid-file=false";
        command += " --exit-idle-time=-1";

        return ProcessHelper.exec(command, envVars.toStringArray(), workingDir);
    }

    private boolean waitForSocketReady() {
        File socketFile = new File(socketConfig.path);
        long deadline = System.currentTimeMillis() + SOCKET_READY_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (socketFile.exists()) {
                return true;
            }
            try {
                Thread.sleep(SOCKET_READY_POLL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return socketFile.exists();
    }

    private void executePactl(boolean suspend) {
        Context context = environment.getContext();
        String nativeLibraryDir = context.getApplicationInfo().nativeLibraryDir;

        File workingDir = new File(context.getFilesDir(), "/pulseaudio");
        if (!workingDir.isDirectory()) {
            workingDir.mkdirs();
            FileUtils.chmod(workingDir, 505);
        }

        EnvVars envVars = new EnvVars();
        envVars.put("LD_LIBRARY_PATH", "/system/lib64:"+nativeLibraryDir);
        envVars.put("HOME", workingDir);
        envVars.put("TMPDIR", XEnvironment.getTmpDir(context));
        envVars.put("PULSE_SERVER", socketConfig.path);

        String suspendCommand = workingDir + "/pactl suspend-sink AAudioSink " + (suspend ? "true" : "false");
        ProcessHelper.exec(suspendCommand, envVars.toStringArray(), workingDir);
    }
}
