package org.fsj.distribtedlock.core;

import java.util.concurrent.locks.Lock;

/**
 * 锁管理中心
 */
public interface LockManager {

    Lock getDLock(String var1);

    void destroy();
}
