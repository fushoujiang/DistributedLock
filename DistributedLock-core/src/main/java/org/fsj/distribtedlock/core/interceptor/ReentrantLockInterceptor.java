package org.fsj.distribtedlock.core.interceptor;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.fsj.distribtedlock.core.LockAnnotation;
import org.fsj.distribtedlock.core.entity.LockConfigEntity;
import org.fsj.distribtedlock.core.factory.LockFactory;

import java.lang.annotation.Annotation;
import java.util.concurrent.locks.Lock;

@Aspect
public class ReentrantLockInterceptor extends AbsLockInterceptor {


    private LockFactory lockFactory;

    public ReentrantLockInterceptor(LockFactory lockFactory) {
        this.lockFactory = lockFactory;
    }

    @Around("@annotation(lockAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint, LockAnnotation lockAnnotation) throws Throwable {
       return lockAround(joinPoint,lockAnnotation);
    }

    @Override
    public LockConfigEntity lockAnnotation2LockConfig(Annotation annotation) {
        LockAnnotation distributedLock = (LockAnnotation) annotation;
        return new LockConfigEntity()
                .setLockPrefix(distributedLock.lockPrefix())
                .setKeyIndexes(distributedLock.keyIndexes())
                .setKeys(distributedLock.keys())
                .setLockFailMethod(distributedLock.lockFailMethod())
                .setTimeout(distributedLock.timeout());
    }

    @Override
    public Lock getLock(String lockKey) {
        return lockFactory.getLock(lockKey);
    }

    @Override
    public String getLockKey(ProceedingJoinPoint joinPoint, LockConfigEntity lockConfigEntity) {
        return joinPoint.getSignature().toString();
    }
}
