package org.fsj.lock.manager.interceptor;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.fsj.lock.manager.LockFailException;
import org.fsj.lock.manager.entity.LockConfigEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

public abstract class AbsLockInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbsLockInterceptor.class);

    public Object lockAround(ProceedingJoinPoint joinPoint, Annotation annotation) throws Throwable{
        final LockConfigEntity lockConfigEntity = lockAnnotation2LockConfig(annotation);
        String lockKey = getLockKey(joinPoint, lockConfigEntity);
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Lock lock = getLock(lockKey);
        if (!lock( lock,lockKey,lockConfigEntity.getTimeout())) {
            LOGGER.info("{}-{}, get lock fail:{}", joinPoint.getTarget().getClass().getName(), methodSignature.getName(), lockKey);
            String failMethod = lockConfigEntity.getLockFailMethod();
            if (StringUtils.isBlank(failMethod)) {
                throw new LockFailException(joinPoint.getTarget().getClass().getName() + "--" + methodSignature.getName() + "...key=" + lockKey);
            }
            return invokeFallbackMethod(joinPoint, failMethod);
        }
        LOGGER.info("{}-{}, get lock success:{}", joinPoint.getTarget().getClass().getName(), methodSignature.getName(), lockKey);
        try {
            return joinPoint.proceed();
        }finally {
            unlock(lock);
            LOGGER.info("{}-{}, release lock:{}", joinPoint.getTarget().getClass().getName(), methodSignature.getName(), lockKey);
        }
    }

    /**
     * ??????????????????LockConfigEntity
     * @param annotation
     * @return
     */
    public abstract LockConfigEntity lockAnnotation2LockConfig(Annotation annotation);
    /**
     * ???????????????
     * @param lockKey
     * @return
     */
    public abstract Lock getLock(String lockKey);


    /**
     * ??????????????????
     *
     * @return ??????????????????
     */
    public boolean lock(Lock lock,String lockKey ,int timeout) {
        Preconditions.checkArgument(Objects.nonNull(lock), "??????????????????lock???null");
        try {
            return lock.tryLock(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void unlock(Lock lock) {
        Preconditions.checkArgument(Objects.nonNull(lock), "??????????????????lock???null");
        lock.unlock();
    }



    /**
     * ??????????????????
     *
     * @param joinPoint            ?????????????????????
     * @param lockConfigEntity ????????????????????????
     * @return ????????????key
     */
    public String getLockKey(ProceedingJoinPoint joinPoint, LockConfigEntity lockConfigEntity) {
        final Object[] args = joinPoint.getArgs();
        StringBuilder lockKey = new StringBuilder(lockConfigEntity.getLockPrefix());
        String[] keys = lockConfigEntity.getKeys();
        int index = 0;
        for (int keyIndex : lockConfigEntity.getKeyIndexes()) {
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
     * ??????????????????????????????
     *
     * @param name ?????????????????????
     * @return true/false
     */
    private boolean isBasicType(String name) {
        return StringUtils.equalsIgnoreCase(name, "LONG") || StringUtils.equalsIgnoreCase(name, "INT")
                || StringUtils.equalsIgnoreCase(name, "STRING");
    }

    /**
     * ???????????????????????????????????????
     *
     * @param obj ?????????????????????
     * @param key ???????????????
     * @return ??????????????????
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
            //??????????????????????????????????????????????????????fallback??????
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


}
