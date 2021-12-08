package org.fsj.distribtedlock.core.factory;


import org.redisson.Redisson;
import org.redisson.config.Config;

import java.util.concurrent.locks.Lock;


public class RedissonLockFactory implements LockFactory {

    private Config config;

    public RedissonLockFactory(Config config) {
        this.config = config;
    }

    @Override
    public Lock getLock(String lockKey) {
        Redisson redisson = (Redisson) Redisson.create(config);
        return redisson.getLock(lockKey);
    }

}
