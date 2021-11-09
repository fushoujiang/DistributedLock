package org.fsj.distribtedlock.core;

import org.springframework.stereotype.Component;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 默认实现
 */
@Component
public class DefaultLockManger implements LockManager{
    @Override
    public Lock getDLock(String var1) {
        return new ReentrantLock();
    }

    @Override
    public void destroy() {

    }
}
