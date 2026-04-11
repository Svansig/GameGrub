/*
 * libltdl_android.c — minimal GNU libltdl shim for Android arm64.
 *
 * Covers all lt_dl* symbols referenced by PulseAudio 13.0:
 *   lt_dlinit, lt_dlexit, lt_dlclose, lt_dlerror, lt_dlgetsearchpath,
 *   lt_dlsetsearchpath, lt_dlopenext, lt_dlsym, lt_dlforeachfile
 *
 * lt_preloaded_symbols is intentionally NOT defined here; it is provided
 * by daemon/main.c when DISABLE_LIBTOOL_PRELOAD is defined.
 *
 * The vtable plugin API (lt_dlvtable, lt_dlloader_*) is omitted; the
 * bind-now loader in ltdl-bind-now.c is disabled for Android via
 * #undef PA_BIND_NOW.
 */

#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef void *lt_dlhandle;
typedef void *lt_ptr;
typedef void *lt_module;

typedef struct {
    const char *name;
    lt_ptr      address;
} lt_dlsymlist;

/* ---- Lifecycle ---- */
int lt_dlinit(void)  { return 0; }
int lt_dlexit(void)  { return 0; }

/* ---- Module operations ---- */
int lt_dlclose(lt_dlhandle handle) {
    if (!handle) return -1;
    return dlclose(handle) == 0 ? 0 : -1;
}

const char *lt_dlerror(void) {
    return dlerror();
}

void *lt_dlsym(lt_dlhandle handle, const char *name) {
    if (!handle || !name) return NULL;
    return dlsym(handle, name);
}

lt_dlhandle lt_dlopenext(const char *name) {
    if (!name) return NULL;

    void *h = dlopen(name, RTLD_NOW | RTLD_GLOBAL);
    if (h) return h;

    size_t nlen = strlen(name);
    char *buf = malloc(nlen + 4);
    if (!buf) return NULL;
    memcpy(buf, name, nlen);
    memcpy(buf + nlen, ".so", 4);
    h = dlopen(buf, RTLD_NOW | RTLD_GLOBAL);
    free(buf);
    return h;
}

/* ---- Search path ---- */
const char *lt_dlgetsearchpath(void) { return NULL; }

int lt_dlsetsearchpath(const char *search_path) {
    (void)search_path;
    return 0;
}

/* ---- Module enumeration ---- */
int lt_dlforeachfile(const char *search_path,
                     int (*func)(const char *filename, void *data),
                     void *data) {
    (void)search_path;
    (void)func;
    (void)data;
    return 0;
}
