package org.fsj.lock.manager.interceptor;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.fsj.lock.manager.LockAnnotation;
import org.fsj.lock.manager.entity.LockConfigEntity;
import org.fsj.lock.manager.factory.LockFactory;

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
