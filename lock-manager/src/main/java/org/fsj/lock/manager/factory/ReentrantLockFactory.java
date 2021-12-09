package org.fsj.lock.manager.factory;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockFactory implements LockFactory{
    private boolean fair;
    private static final ConcurrentHashMap<String ,Lock> cache = new ConcurrentHashMap<>();

    public ReentrantLockFactory(boolean fair) {
        this.fair = fair;
    }

    @Override
    public Lock getLock(String lockKey) {
        Lock lock = cache.get(lockKey);
        if (Objects.isNull(lock)){
            lock = new ReentrantLock(fair);
            cache.put(lockKey,lock);
        }
        return lock;
    }
}
