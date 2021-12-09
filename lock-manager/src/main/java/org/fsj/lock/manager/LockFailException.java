package org.fsj.lock.manager;

/**
 * 加锁失败异常
 */
public class LockFailException extends RuntimeException{

    public LockFailException() {
    }

    public LockFailException(String message) {
        super(message);
    }

    public LockFailException(String message, Throwable cause) {
        super(message, cause);
    }

    public LockFailException(Throwable cause) {
        super(cause);
    }

    public LockFailException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
