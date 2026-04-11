package com.winlator.contentdialog;

import android.view.View;

import app.gamegrub.R;

import com.winlator.core.GPUHelper;

/**
 * Dialog for configuring Vortek Vulkan renderer settings.
 * Allows users to specify maximum Vulkan API version for Vortek renderer
 * compatibility with different GPU drivers.
 */
/* loaded from: classes.dex */
public class VortekConfigDialog extends ContentDialog {
    // Computed independently to avoid triggering VortekRendererComponent class initialization
    // (and thus the native library load) when this dialog is opened.
    private static final int DEFAULT_VK_MAX_VERSION_CODE = GPUHelper.vkMakeVersion(1, 3, 128);
    public static final String DEFAULT_VK_MAX_VERSION;

    static {
        int i = DEFAULT_VK_MAX_VERSION_CODE;
        DEFAULT_VK_MAX_VERSION = GPUHelper.vkVersionMajor(i) +
                "." +
                GPUHelper.vkVersionMinor(i);
    }

    public VortekConfigDialog(final View anchor) {
        super(anchor.getContext(), R.layout.content_dialog);
    }
}
