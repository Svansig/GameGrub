package com.winlator.core.envvars

/**
 * Enum representing the type of selection UI for environment variables.
 * Used to determine how environment variable values should be presented
 * and selected in the UI (toggle switch, multi-select dropdown, etc.).
 */

enum class EnvVarSelectionType {
    TOGGLE,
    MULTI_SELECT,
    NONE,
}
