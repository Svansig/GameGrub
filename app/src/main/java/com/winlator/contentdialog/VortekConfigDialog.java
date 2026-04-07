package com.winlator.contentdialog;

import android.view.View;

import app.gamegrub.R;

import com.winlator.core.GPUHelper;
import com.winlator.xenvironment.components.VortekRendererComponent;

/* loaded from: classes.dex */
public class VortekConfigDialog extends ContentDialog {
    public static final String DEFAULT_VK_MAX_VERSION;

    static {
        int i = VortekRendererComponent.VK_MAX_VERSION;
        DEFAULT_VK_MAX_VERSION = GPUHelper.vkVersionMajor(i) +
                "." +
                GPUHelper.vkVersionMinor(i);
    }

    public VortekConfigDialog(final View anchor) {
        super(anchor.getContext(), R.layout.content_dialog);
    }
}
