/* third_party/libltdl/lt_system_android.h
 *
 * Android arm64-v8a override for GNU libltdl's lt_system.h.
 * Replace the autoconf-generated lt_system.h with this file after
 * running fetch-libltdl.sh (the script installs it as src/lt_system.h).
 *
 * Android (bionic) provides dlopen/dlsym/dlclose/dlerror natively.
 * We do not have a working autoconf environment at Android CMake build time,
 * so configure-generated defines are supplied here instead.
 */

#ifndef __LTDL_LT_SYSTEM_H__
#define __LTDL_LT_SYSTEM_H__ 1

/* Android bionic is a POSIX-like environment. */
#define LT_PATHSEP_CHAR ':'
#define LT_DIRSEP_CHAR  '/'

/* ltdl uses LIBEXT to know the extension to search for. */
#define LTDL_SHLIB_EXT ".so"

/* ltdl object directory inside the build tree (unused at runtime). */
#define LTDL_OBJDIR ".libs"

/* ltdl search path (empty — Android uses LD_LIBRARY_PATH instead). */
#define LT_DLSEARCH_PATH ""

/* Standard size/type defines available from bionic. */
#include <stddef.h>
#include <sys/types.h>

/* Tell ltdl that we have a working dirent + closedir + opendir. */
#define HAVE_DIRENT_H   1
#define HAVE_CLOSEDIR   1
#define HAVE_OPENDIR    1
#define HAVE_READDIR    1

/* dlopen family is available from libdl. */
#define HAVE_DLFCN_H    1
#define HAVE_LIBDL      1
#define HAVE_DLOPEN     1

#endif /* __LTDL_LT_SYSTEM_H__ */
