package org.fsj.distribtedlock.core.factory;

import java.util.concurrent.locks.Lock;

/**
 * 锁管理中心
 */
public interface LockFactory {

    Lock getLock(String lockKey);

}
