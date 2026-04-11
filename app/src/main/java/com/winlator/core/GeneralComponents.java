package com.winlator.core;

import android.content.Context;

import com.winlator.xenvironment.ImageFs;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Manages downloadable Wine runtime components such as DXVK, VKD3D, Turnip drivers,
 * and Box64. Handles component versioning, download, and installation for
 * supporting various graphics APIs and emulation layers.
 */
public abstract class GeneralComponents {

    public enum Type {
        BOX64,
        TURNIP,
        DXVK,
        VKD3D,
        WINED3D,
        SOUNDFONT,
        ADRENOTOOLS_DRIVER;

        public String lowerName() {
            return name().toLowerCase(Locale.ENGLISH);
        }

        public String title() {
            return switch (this) {
                case BOX64 -> "Box64";
                case TURNIP -> "Turnip";
                case DXVK -> "DXVK";
                case VKD3D -> "VKD3D";
                case WINED3D -> "WineD3D";
                case SOUNDFONT -> "SoundFont";
                case ADRENOTOOLS_DRIVER -> "Adrenotools Driver";
                default -> "";
            };
        }

        private String assetFolder() {
            return switch (this) {
                case BOX64 -> "box64";
                case TURNIP -> "graphics_driver";
                case DXVK, VKD3D, WINED3D -> "dxwrapper";
                case SOUNDFONT -> "soundfont";
                default -> "";
            };
        }

        private File getSource(Context context, String identifier) {
            File componentDir = GeneralComponents.getComponentDir(this, context);
            return switch (this) {
                case SOUNDFONT -> new File(componentDir, identifier + ".sf2");
                case ADRENOTOOLS_DRIVER -> new File(componentDir, identifier);
                default -> new File(componentDir, lowerName() + "-" + identifier + ".tzst");
            };
        }

        public File getDestination(Context context) {
            File rootDir = ImageFs.find(context).getRootDir();
            switch (this) {
                case DXVK:
                case VKD3D:
                case WINED3D:
                    return new File(rootDir, "/home/xuser/.wine/drive_c/windows");
                case SOUNDFONT:
                    File destination = new File(context.getCacheDir(), "soundfont");
                    if (!destination.isDirectory()) {
                        destination.mkdirs();
                    }
                    return destination;
                default:
                    return rootDir;
            }
        }

        private int getInstallModes() {
            if (this == SOUNDFONT || this == ADRENOTOOLS_DRIVER) {
                return 2; // File install mode
            }
            return 1; // Download install mode
        }

        private boolean isVersioned() {
            return this == BOX64 || this == TURNIP || this == DXVK || this == VKD3D || this == WINED3D;
        }
    }

    public static ArrayList<String> getBuiltinComponentNames(Type type) {
        String[] items = switch (type) {
            case BOX64 -> new String[]{"0.3.4", "0.3.6"};
            case TURNIP -> new String[]{"25.1.0"};
            case DXVK -> new String[]{"1.10.3", "2.4.1"};
            case VKD3D -> new String[]{"2.13"};
            case WINED3D -> new String[]{"9.2"};
            case SOUNDFONT -> new String[]{"SONiVOX-EAS-GM-Wavetable"};
            case ADRENOTOOLS_DRIVER -> new String[]{"System"};
        };
        return new ArrayList<>(Arrays.asList(items));
    }

    public static File getComponentDir(Type type, Context context) {
        File file = new File(context.getFilesDir(), "/installed_components/" + type.lowerName());
        if (!file.isDirectory()) {
            file.mkdirs();
        }
        return file;
    }

    public static boolean isBuiltinComponent(Type type, String identifier) {
        for (String builtinComponentName : getBuiltinComponentNames(type)) {
            if (builtinComponentName.equalsIgnoreCase(identifier)) {
                return true;
            }
        }
        return false;
    }

    public static String getDefinitivePath(Type type, Context context, String identifier) {
        if (identifier.isEmpty()) {
            return null;
        }

        if (type == Type.SOUNDFONT && isBuiltinComponent(type, identifier)) {
            File destination = type.getDestination(context);
            FileUtils.clear(destination);
            String filename = identifier + ".sf2";
            File destinationFile = new File(destination, filename);
            FileUtils.copy(context, type.assetFolder() + "/" + filename, destinationFile);
            return destinationFile.getPath();
        }

        if (type == Type.ADRENOTOOLS_DRIVER) {
            if (isBuiltinComponent(type, identifier)) {
                return null;
            }
            File source = type.getSource(context, identifier);
            File[] manifestFiles = source.listFiles((file, name) -> name.endsWith(".json"));
            if (manifestFiles != null && manifestFiles.length > 0) {
                try {
                    JSONObject manifestJSONObject = new JSONObject(FileUtils.readString(manifestFiles[0]));
                    String libraryName = manifestJSONObject.optString("libraryName", "");
                    File libraryFile = new File(source, libraryName);
                    if (libraryFile.isFile()) {
                        return libraryFile.getPath();
                    }
                    return null;
                } catch (JSONException e) {
                    return null;
                }
            }
        }
        return type.getSource(context, identifier).getPath();
    }
}
