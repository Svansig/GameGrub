/* config_android.h
 * Minimal config.h stub for GNU libltdl compiled with Android NDK.
 * Replaces the autoconf-generated config.h for cross-builds.
 *
 * Referenced via -DLT_CONFIG_H=\"config_android.h\" so lt__private.h
 * uses this file instead of the missing <config.h>.
 */

#ifndef LIBLTDL_CONFIG_ANDROID_H
#define LIBLTDL_CONFIG_ANDROID_H 1

/* Android/bionic always has dlopen/dlsym/dlclose from libdl. */
#define HAVE_DLFCN_H    1
#define HAVE_DLOPEN     1
#define HAVE_LIBDL      1

/* POSIX headers available in bionic. */
#define HAVE_UNISTD_H   1
#define HAVE_DIRENT_H   1
#define HAVE_STDIO_H    1
#define HAVE_STDLIB_H   1
#define HAVE_STRING_H   1
#define HAVE_STRINGS_H  1
#define HAVE_SYS_STAT_H 1
#define HAVE_SYS_TYPES_H 1
#define HAVE_ERRNO_H    1

/* Function availability. */
#define HAVE_CLOSEDIR   1
#define HAVE_OPENDIR    1
#define HAVE_READDIR    1
#define HAVE_STRCHR     1
#define HAVE_STRRCHR    1
#define HAVE_STRERROR   1
#define HAVE_MEMCPY     1
#define HAVE_MEMMOVE    1

/* No argz support on bionic. */
#undef HAVE_ARGZ_H
#undef HAVE_ARGZ_APPEND
#undef HAVE_ARGZ_CREATE_SEP
#undef HAVE_ARGZ_INSERT
#undef HAVE_ARGZ_NEXT
#undef HAVE_ARGZ_STRINGIFY

/* Shared library extension. */
#define LTDL_SHLIB_EXT  ".so"

/* ltdl search path (empty — use LD_LIBRARY_PATH at runtime). */
#define LT_DLSEARCH_PATH ""

/* Package info. */
#define PACKAGE         "ltdl"
#define PACKAGE_VERSION "2.5.4"
#define VERSION         "2.5.4"

#endif /* LIBLTDL_CONFIG_ANDROID_H */
