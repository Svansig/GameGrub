#include <jni.h>
#include <sys/epoll.h>
#include <sys/poll.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/eventfd.h>
#include <sys/un.h>
#include <unistd.h>
#include <string.h>
#include <malloc.h>
#include <jni.h>
#include <android/log.h>
#include <android/fdsan.h>
#include <sys/resource.h>
#include <errno.h>
#include <inttypes.h>
#include <stdio.h>
#include <time.h>

#if defined(JNI_BOUNDARY_TRACE)
#define XCONNECTOR_TRACE(...) __android_log_print(ANDROID_LOG_DEBUG, "XConnectorEpoll", __VA_ARGS__)
#else
#define XCONNECTOR_TRACE(...) ((void)0)
#endif

#define XCONNECTOR_ERROR(...) __android_log_print(ANDROID_LOG_ERROR, "XConnectorEpoll", __VA_ARGS__)
#define MAX_EVENTS 10
#define MAX_FDS 32

#define MAX_TRACKED_FDS 1024  // Adjust based on your needs

typedef struct {
    int fd;
    bool is_owned;
} FdTracker;

static FdTracker fd_tracking[MAX_TRACKED_FDS] = {0};

typedef struct {
    uint64_t do_epoll_calls;
    uint64_t do_epoll_total_ns;
    uint64_t do_epoll_total_events;
    uint64_t do_epoll_callbacks;
    uint64_t wait_read_calls;
    uint64_t wait_read_total_ns;
    uint64_t recv_ancillary_calls;
    uint64_t recv_ancillary_total_ns;
    uint64_t recv_ancillary_fds;
    uint64_t send_ancillary_calls;
    uint64_t send_ancillary_total_ns;
    uint64_t lookup_handle_new_misses;
    uint64_t lookup_handle_existing_misses;
    uint64_t lookup_add_ancillary_misses;
} XConnectorPerfStats;

static XConnectorPerfStats g_stats = {0};
static jmethodID g_mid_handle_new_connection = NULL;
static jmethodID g_mid_handle_existing_connection = NULL;
static jmethodID g_mid_add_ancillary_fd = NULL;

static uint64_t now_monotonic_ns(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (uint64_t) ts.tv_sec * 1000000000ULL + (uint64_t) ts.tv_nsec;
}

static jmethodID cache_method_id(JNIEnv *env,
                                 jobject obj,
                                 jmethodID *slot,
                                 const char *name,
                                 const char *sig,
                                 uint64_t *miss_counter) {
    if (*slot != NULL) {
        return *slot;
    }

    jclass cls = (*env)->GetObjectClass(env, obj);
    if (cls == NULL) {
        return NULL;
    }

    jmethodID mid = (*env)->GetMethodID(env, cls, name, sig);
    if (mid != NULL) {
        *slot = mid;
        __sync_fetch_and_add(miss_counter, 1);
    }
    return mid;
}

struct epoll_event events[MAX_EVENTS];

// Call this when you first obtain/create a file descriptor
void trackFd(jint fd) {
    for (int i = 0; i < MAX_TRACKED_FDS; i++) {
        if (fd_tracking[i].fd == 0) {
            fd_tracking[i].fd = fd;
            fd_tracking[i].is_owned = true;
            break;
        }
    }
}
void closeFd(jint fd) {
    bool can_close = false;

    // Find and check ownership
    for (int i = 0; i < MAX_TRACKED_FDS; i++) {
        if (fd_tracking[i].fd == fd) {
            if (fd_tracking[i].is_owned) {
                can_close = true;
                // Mark as no longer owned
                fd_tracking[i].fd = 0;
                fd_tracking[i].is_owned = false;
            }
            break;
        }
    }

    if (can_close) {
        close(fd);
        XCONNECTOR_TRACE("close owned fd=%d", fd);
    } else {
        XCONNECTOR_TRACE("attempted to close unowned fd=%d", fd);
    }
}

JNIEXPORT void JNICALL
Java_com_winlator_xconnector_XConnectorEpoll_setRLimitToMax(JNIEnv *env, jobject obj) {
    struct rlimit rlm;
    if (getrlimit(RLIMIT_NOFILE, &rlm) < 0) {
        XCONNECTOR_ERROR("failed to getrlimit: %s", strerror(errno));
    } else {
        XCONNECTOR_TRACE("setting current limit (%lu) to max limit (%lu)", rlm.rlim_cur, rlm.rlim_max);
        rlm.rlim_cur = rlm.rlim_max;
        if (setrlimit(RLIMIT_NOFILE, &rlm) < 0) {
            XCONNECTOR_ERROR("failed to setrlimit: %s", strerror(errno));
        }
    }
}

JNIEXPORT jint JNICALL
Java_com_winlator_xconnector_XConnectorEpoll_createAFUnixSocket(JNIEnv *env, jobject obj,
                                                                jstring path) {
    int fd = socket(AF_UNIX, SOCK_STREAM, 0);
    XCONNECTOR_TRACE("socket fd=%d", fd);
    if (fd < 0) return -1;
    trackFd(fd);

    struct sockaddr_un serverAddr;
    memset(&serverAddr, 0, sizeof(serverAddr));
    serverAddr.sun_family = AF_LOCAL;

    const char *pathPtr = (*env)->GetStringUTFChars(env, path, 0);

    int addrLength = sizeof(sa_family_t) + strlen(pathPtr);
    strncpy(serverAddr.sun_path, pathPtr, sizeof(serverAddr.sun_path) - 1);

    (*env)->ReleaseStringUTFChars(env, path, pathPtr);

    unlink(serverAddr.sun_path);
    if (bind(fd, (struct sockaddr*) &serverAddr, addrLength) < 0) goto error;
    if (listen(fd, MAX_EVENTS) < 0) goto error;

    return fd;
    error:
    closeFd(fd);
    return -1;
}

JNIEXPORT jint JNICALL
Java_com_winlator_xconnector_XConnectorEpoll_createEpollFd(JNIEnv *env, jobject obj) {
    int fd = epoll_create(MAX_EVENTS);
    XCONNECTOR_TRACE("epoll_create fd=%d", fd);
    trackFd(fd);
    return fd;
}


JNIEXPORT void JNICALL
Java_com_winlator_xconnector_XConnectorEpoll_closeFd(JNIEnv *env, jobject obj, jint fd) {
    closeFd(fd);
//    printf("XConnectorEpoll2 close %d", fd);
//    close(fd);
//    printf("XConnectorEpoll2 close %d done", fd);
}

JNIEXPORT jboolean JNICALL
Java_com_winlator_xconnector_XConnectorEpoll_doEpollIndefinitely(JNIEnv *env, jobject obj,
                                                                 jint epollFd,
                                                                 jint serverFd,
                                                                 jboolean addClientToEpoll) {
    uint64_t start_ns = now_monotonic_ns();

    jmethodID handleNewConnection = cache_method_id(
            env,
            obj,
            &g_mid_handle_new_connection,
            "handleNewConnection",
            "(I)V",
            &g_stats.lookup_handle_new_misses);
    jmethodID handleExistingConnection = cache_method_id(
            env,
            obj,
            &g_mid_handle_existing_connection,
            "handleExistingConnection",
            "(I)V",
            &g_stats.lookup_handle_existing_misses);
    if (handleNewConnection == NULL || handleExistingConnection == NULL) {
        return JNI_FALSE;
    }

    int numFds = epoll_wait(epollFd, events, MAX_EVENTS, -1);
    uint64_t callback_count = 0;
    for (int i = 0; i < numFds; i++) {
        if (events[i].data.fd == serverFd) {
            int clientFd = accept(serverFd, NULL, NULL);
            if (clientFd >= 0) {
                trackFd(clientFd);
                if (addClientToEpoll) {
                    struct epoll_event ev = {.data.fd = clientFd, .events = EPOLLIN};
                    if (epoll_ctl(epollFd, EPOLL_CTL_ADD, clientFd, &ev) >= 0) {
                        (*env)->CallVoidMethod(env, obj, handleNewConnection, clientFd);
                        callback_count++;
                    }
                } else {
                    (*env)->CallVoidMethod(env, obj, handleNewConnection, clientFd);
                    callback_count++;
                }
            }
        } else if (events[i].events & EPOLLIN) {
            (*env)->CallVoidMethod(env, obj, handleExistingConnection, events[i].data.fd);
            callback_count++;
        }
    }

    uint64_t elapsed_ns = now_monotonic_ns() - start_ns;
    __sync_fetch_and_add(&g_stats.do_epoll_calls, 1);
    __sync_fetch_and_add(&g_stats.do_epoll_total_ns, elapsed_ns);
    if (numFds > 0) {
        __sync_fetch_and_add(&g_stats.do_epoll_total_events, (uint64_t) numFds);
    }
    __sync_fetch_and_add(&g_stats.do_epoll_callbacks, callback_count);
    return numFds >= 0;
}

JNIEXPORT jboolean JNICALL
Java_com_winlator_xconnector_XConnectorEpoll_addFdToEpoll(JNIEnv *env, jobject obj,
                                                          jint epollFd,
                                                          jint fd) {
    struct epoll_event event;
    event.data.fd = fd;
    event.events = EPOLLIN;
    if (epoll_ctl(epollFd, EPOLL_CTL_ADD, fd, &event) < 0) return JNI_FALSE;
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_winlator_xconnector_XConnectorEpoll_removeFdFromEpoll(JNIEnv *env, jobject obj,
                                                               jint epollFd, jint fd) {
    epoll_ctl(epollFd, EPOLL_CTL_DEL, fd, NULL);
}

JNIEXPORT jint JNICALL
Java_com_winlator_xconnector_ClientSocket_read(JNIEnv *env, jobject obj, jint fd, jobject data,
                                               jint offset, jint length) {
    char *dataAddr = (*env)->GetDirectBufferAddress(env, data);
    return read(fd, dataAddr + offset, length);
}

JNIEXPORT jint JNICALL
Java_com_winlator_xconnector_ClientSocket_write(JNIEnv *env, jobject obj, jint fd, jobject data,
                                                jint length) {
//    printf("Writing to %d", fd);
    char *dataAddr = (*env)->GetDirectBufferAddress(env, data);
    return write(fd, dataAddr, length);
}

JNIEXPORT jint JNICALL
Java_com_winlator_xconnector_XConnectorEpoll_createEventFd(JNIEnv *env, jobject obj) {
    int fd = eventfd(0, EFD_NONBLOCK);
    XCONNECTOR_TRACE("eventfd fd=%d", fd);
    trackFd(fd);
    return fd;
}

JNIEXPORT jint JNICALL
Java_com_winlator_xconnector_ClientSocket_recvAncillaryMsg(JNIEnv *env, jobject obj, jint clientFd, jobject data,
                                                           jint offset, jint length) {
    uint64_t start_ns = now_monotonic_ns();
    char *dataAddr = (*env)->GetDirectBufferAddress(env, data);

    struct iovec iovmsg = {.iov_base = dataAddr + offset, .iov_len = length};
    struct {
        struct cmsghdr align;
        int fds[MAX_FDS];
    } ctrlmsg;

    struct msghdr msg = {
        .msg_name = NULL,
        .msg_namelen = 0,
        .msg_iov = &iovmsg,
        .msg_iovlen = 1,
        .msg_control = &ctrlmsg,
        .msg_controllen = sizeof(struct cmsghdr) + MAX_FDS * sizeof(int)
    };

    int size = recvmsg(clientFd, &msg, 0);
    uint64_t ancillary_count = 0;

    if (size >= 0) {
        jmethodID addAncillaryFd = cache_method_id(
                env,
                obj,
                &g_mid_add_ancillary_fd,
                "addAncillaryFd",
                "(I)V",
                &g_stats.lookup_add_ancillary_misses);
        if (addAncillaryFd == NULL) {
            return size;
        }
        struct cmsghdr *cmsg;
        for (cmsg = CMSG_FIRSTHDR(&msg); cmsg; cmsg = CMSG_NXTHDR(&msg, cmsg)) {
            if (cmsg->cmsg_level == SOL_SOCKET && cmsg->cmsg_type == SCM_RIGHTS) {
                int numFds = (cmsg->cmsg_len - CMSG_LEN(0)) / sizeof(int);
                if (numFds > 0) {
                    for (int i = 0; i < numFds; i++) {
                        int ancillaryFd = ((int*)CMSG_DATA(cmsg))[i];
                        trackFd(ancillaryFd);
                        (*env)->CallVoidMethod(env, obj, addAncillaryFd, ancillaryFd);
                        ancillary_count++;
                    }
                }
            }
        }
    }

    uint64_t elapsed_ns = now_monotonic_ns() - start_ns;
    __sync_fetch_and_add(&g_stats.recv_ancillary_calls, 1);
    __sync_fetch_and_add(&g_stats.recv_ancillary_total_ns, elapsed_ns);
    if (ancillary_count > 0) {
        __sync_fetch_and_add(&g_stats.recv_ancillary_fds, ancillary_count);
    }
    return size;
}

JNIEXPORT jint JNICALL
Java_com_winlator_xconnector_ClientSocket_sendAncillaryMsg(JNIEnv *env, jobject obj, jint clientFd,
                                                           jobject data, jint length, jint ancillaryFd) {
    uint64_t start_ns = now_monotonic_ns();
    char *dataAddr = (*env)->GetDirectBufferAddress(env, data);

    struct iovec iovmsg = {.iov_base = dataAddr, .iov_len = length};
    struct {
        struct cmsghdr align;
        int fds[1];
    } ctrlmsg;

    struct msghdr msg = {
        .msg_name = NULL,
        .msg_namelen = 0,
        .msg_iov = &iovmsg,
        .msg_iovlen = 1,
        .msg_flags = 0,
        .msg_control = &ctrlmsg,
        .msg_controllen = sizeof(struct cmsghdr) + sizeof(int)
    };

    struct cmsghdr *cmsg = CMSG_FIRSTHDR(&msg);
    cmsg->cmsg_level = SOL_SOCKET;
    cmsg->cmsg_type = SCM_RIGHTS;
    cmsg->cmsg_len = msg.msg_controllen;
    ((int*)CMSG_DATA(cmsg))[0] = ancillaryFd;

    jint size = sendmsg(clientFd, &msg, 0);
    uint64_t elapsed_ns = now_monotonic_ns() - start_ns;
    __sync_fetch_and_add(&g_stats.send_ancillary_calls, 1);
    __sync_fetch_and_add(&g_stats.send_ancillary_total_ns, elapsed_ns);
    return size;
}

JNIEXPORT jboolean JNICALL
Java_com_winlator_xconnector_XConnectorEpoll_waitForSocketRead(JNIEnv *env, jobject obj, jint clientFd, jint shutdownFd) {
    uint64_t start_ns = now_monotonic_ns();
    struct pollfd pfds[2];
    pfds[0].fd = clientFd;
    pfds[0].events = POLLIN;

    pfds[1].fd = shutdownFd;
    pfds[1].events = POLLIN;

    int res = poll(pfds, 2, -1);
    if (res < 0 || (pfds[1].revents & POLLIN)) {
        uint64_t elapsed_ns = now_monotonic_ns() - start_ns;
        __sync_fetch_and_add(&g_stats.wait_read_calls, 1);
        __sync_fetch_and_add(&g_stats.wait_read_total_ns, elapsed_ns);
        return JNI_FALSE;
    }

    if (pfds[0].revents & POLLIN) {
        jmethodID handleExistingConnection = cache_method_id(
                env,
                obj,
                &g_mid_handle_existing_connection,
                "handleExistingConnection",
                "(I)V",
                &g_stats.lookup_handle_existing_misses);
        if (handleExistingConnection == NULL) {
            return JNI_FALSE;
        }
        (*env)->CallVoidMethod(env, obj, handleExistingConnection, clientFd);
    }

    uint64_t elapsed_ns = now_monotonic_ns() - start_ns;
    __sync_fetch_and_add(&g_stats.wait_read_calls, 1);
    __sync_fetch_and_add(&g_stats.wait_read_total_ns, elapsed_ns);
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL
Java_com_winlator_xconnector_XConnectorEpoll_getNativePerfStats(JNIEnv *env, jclass cls, jboolean reset) {
    (void) cls;
    uint64_t do_epoll_calls = g_stats.do_epoll_calls;
    uint64_t do_epoll_total_ns = g_stats.do_epoll_total_ns;
    uint64_t wait_read_calls = g_stats.wait_read_calls;
    uint64_t wait_read_total_ns = g_stats.wait_read_total_ns;
    uint64_t recv_calls = g_stats.recv_ancillary_calls;
    uint64_t recv_total_ns = g_stats.recv_ancillary_total_ns;
    uint64_t send_calls = g_stats.send_ancillary_calls;
    uint64_t send_total_ns = g_stats.send_ancillary_total_ns;

    uint64_t do_epoll_avg_ns = do_epoll_calls ? do_epoll_total_ns / do_epoll_calls : 0;
    uint64_t wait_read_avg_ns = wait_read_calls ? wait_read_total_ns / wait_read_calls : 0;
    uint64_t recv_avg_ns = recv_calls ? recv_total_ns / recv_calls : 0;
    uint64_t send_avg_ns = send_calls ? send_total_ns / send_calls : 0;
    uint64_t do_epoll_avg_us = do_epoll_avg_ns / 1000;
    uint64_t wait_read_avg_us = wait_read_avg_ns / 1000;
    uint64_t recv_avg_us = recv_avg_ns / 1000;
    uint64_t send_avg_us = send_avg_ns / 1000;

    char buffer[640];
    snprintf(buffer, sizeof(buffer),
             "epoll{calls=%" PRIu64 ",avg_us=%" PRIu64 ",events=%" PRIu64 ",callbacks=%" PRIu64 "} "
             "waitRead{calls=%" PRIu64 ",avg_us=%" PRIu64 "} "
             "recvAnc{calls=%" PRIu64 ",avg_us=%" PRIu64 ",fds=%" PRIu64 "} "
             "sendAnc{calls=%" PRIu64 ",avg_us=%" PRIu64 "} "
             "lookupMiss{new=%" PRIu64 ",existing=%" PRIu64 ",ancillary=%" PRIu64 "}",
             do_epoll_calls,
             do_epoll_avg_us,
             g_stats.do_epoll_total_events,
             g_stats.do_epoll_callbacks,
             wait_read_calls,
             wait_read_avg_us,
             recv_calls,
             recv_avg_us,
             g_stats.recv_ancillary_fds,
             send_calls,
             send_avg_us,
             g_stats.lookup_handle_new_misses,
             g_stats.lookup_handle_existing_misses,
             g_stats.lookup_add_ancillary_misses);

    if (reset) {
        memset(&g_stats, 0, sizeof(g_stats));
    }

    return (*env)->NewStringUTF(env, buffer);
}

JNIEXPORT jintArray JNICALL
Java_com_winlator_xconnector_XConnectorEpoll_pollEpollEvents(JNIEnv *env, jobject obj,
                                                             jint epollFd, jint maxEvents) {
    struct epoll_event events[maxEvents];
    int numFds = epoll_wait(epollFd, events, maxEvents, -1); // Wait indefinitely

    if (numFds < 0) return NULL;

    jintArray result = (*env)->NewIntArray(env, numFds);
    if (result == NULL) return NULL;

    jint *r = (*env)->GetIntArrayElements(env, result, 0);

    for (int i = 0; i < numFds; i++) {
        r[i] = events[i].data.fd; // Store file descriptor
    }

    (*env)->ReleaseIntArrayElements(env, result, r, 0);
    return result;
}
