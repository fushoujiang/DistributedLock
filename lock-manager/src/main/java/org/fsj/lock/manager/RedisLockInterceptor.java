package org.fsj.lock.manager;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.fsj.lock.manager.factory.LockFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.core.Ordered;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * 处理分布式锁的加锁&释放
 *
 *
 * @see LockAnnotation
 * @author fushoujiang -- 2017/12/15
 */
@Aspect
public class RedisLockInterceptor implements Ordered{

    @Resource
    LockFactory lockFactory;

    @Around("@annotation(distributedLock)")
    public Object lockAround(ProceedingJoinPoint joinPoint, LockAnnotation distributedLock) {
        final String lockKey = getLockKey(joinPoint.getArgs(), distributedLock);
        final  Lock lock = lockFactory.getLock(lockKey);
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        if (!lock(lock, distributedLock.timeout())) {
            String failMethod = distributedLock.lockFailMethod();
            if (ObjectUtils.isEmpty(failMethod)){
                throw  new LockFailException(joinPoint.getTarget().getClass().getName()+"--"+methodSignature.getName()+"...key="+lockKey);
            }
            return invokeFallbackMethod(joinPoint,failMethod);
        }
        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取分布式锁
     *
     * @param lock lock
     * @param timeout 超时
     * @return 是否获取成功
     */
    private boolean lock(Lock lock, int timeout) {
        try {
            return lock.tryLock(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 解析加锁内容
     *
     * @param args 要加锁方法入参
     * @param distributedLock 加锁配置（注解）
     * @return 要加锁的key
     */
    private String getLockKey(Object[] args, LockAnnotation distributedLock) {
        StringBuilder lockKey = new StringBuilder(distributedLock.lockPrefix());
        String[] keys = distributedLock.keys();
        int index = 0;
        for (int keyIndex : distributedLock.keyIndexes()) {
            Object arg = args[keyIndex];
            String key = keys[index++];
            if (isBasicType(key)) {
                lockKey.append("_").append(arg);
            } else {
                lockKey.append("_").append(parseKey(arg, key));
            }
        }
        return lockKey.toString();
    }

    /**
     * 判断是否基础数据类型
     *
     * @param name 要判断的占位符
     * @return true/false
     */
    private boolean isBasicType(String name) {
        return "LONG".equalsIgnoreCase(name) || "INT".equalsIgnoreCase(name) || "STRING".equalsIgnoreCase(name);
    }

    /**
     * 解析非基础数据类型的占位符
     *
     * @param obj 占位符对应参数
     * @param key 占位符内容
     * @return 解析后的内容
     */
    private String parseKey(Object obj, String key) {
        String[] stirs = key.substring(1, key.length()).split("\\.");
        Object currObj = obj;
        for (String fieldName : stirs) {
            try {
                PropertyDescriptor propertyDescriptor = BeanUtils.getPropertyDescriptor(obj.getClass(), fieldName);
                Method readMethod = propertyDescriptor.getReadMethod();
                if (!Modifier.isPublic(readMethod.getDeclaringClass().getModifiers())) {
                    readMethod.setAccessible(true);
                }
                currObj = readMethod.invoke(obj);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return String.valueOf(currObj);
    }

    private Method findFallbackMethod(ProceedingJoinPoint joinPoint, String fallbackMethodName) {
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        Method method = methodSignature.getMethod();
        Class<?>[] parameterTypes = method.getParameterTypes();
        Method fallbackMethod = null;
        try {
            //这里通过判断必须取和原方法一样参数的fallback方法
            fallbackMethod = joinPoint.getTarget().getClass().getMethod(fallbackMethodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new LockFailException(e);
        }
        return fallbackMethod;
    }


    private Object invokeFallbackMethod(ProceedingJoinPoint joinPoint, String fallback) {
        Method method = findFallbackMethod(joinPoint, fallback);
        method.setAccessible(true);
        try {
            Object invoke = method.invoke(joinPoint.getTarget(), joinPoint.getArgs());
            return invoke;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new LockFailException(e);
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
