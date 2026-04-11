package com.winlator.sysvshm;

/**
 * Protocol opcodes for SysV SHM requests: SHMget (create segment), getFd (get file descriptor),
 * and delete (remove segment).
 */
public abstract class RequestCodes {
    public static final byte SHMGET = 0;
    public static final byte GET_FD = 1;
    public static final byte DELETE = 2;
}
