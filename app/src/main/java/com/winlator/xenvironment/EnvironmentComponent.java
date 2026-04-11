package com.winlator.xenvironment;

/**
 * Base class for environment components that can be started and stopped.
 * Each component represents a service or functionality within the Wine environment.
 */
public abstract class EnvironmentComponent {
    protected XEnvironment environment;

    public abstract void start();

    public abstract void stop();
}