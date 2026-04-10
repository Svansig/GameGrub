package com.winlator.xenvironment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Structured result for one environment stop pass.
 */
public final class EnvironmentStopSummary {
    public static final class ComponentStopResult {
        private final String componentName;
        private final long durationMs;
        private final boolean success;
        private final String errorMessage;

        public ComponentStopResult(String componentName, long durationMs, boolean success, String errorMessage) {
            this.componentName = componentName;
            this.durationMs = durationMs;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public String getComponentName() {
            return componentName;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    private static final EnvironmentStopSummary EMPTY = new EnvironmentStopSummary(Collections.emptyList(), 0L);

    private final List<ComponentStopResult> componentResults;
    private final long totalDurationMs;

    public EnvironmentStopSummary(List<ComponentStopResult> componentResults, long totalDurationMs) {
        this.componentResults = Collections.unmodifiableList(new ArrayList<>(componentResults));
        this.totalDurationMs = totalDurationMs;
    }

    public static EnvironmentStopSummary empty() {
        return EMPTY;
    }

    public List<ComponentStopResult> getComponentResults() {
        return componentResults;
    }

    public long getTotalDurationMs() {
        return totalDurationMs;
    }

    public int getFailedCount() {
        int failed = 0;
        for (ComponentStopResult result : componentResults) {
            if (!result.isSuccess()) {
                failed++;
            }
        }
        return failed;
    }
}

