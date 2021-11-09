package org.fsj.distribtedlock.core;


import java.lang.annotation.*;

/**
 * 用于处理锁，只要在方法上添加该注解 <br />
 * 系统会自动通过AOP对该方法加锁、释放锁
 *
 * keys的规则如下：<br />
 * 基础类型：String,Int,Long <br />
 * 引用类型：以.开头，格式为.field.field...
 *
 * @author fushoujiang -- 2021/08/12
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LockAnnotation {

    /**
     * 要加锁的key，有特定规则，和${@code keyIndexes}对应，用于解析keyIndex对应的值
     */
    String[] keys();

    /**
     * 要加锁的key的index，标识key所在的参数位置
     */
    int[] keyIndexes();

    /**
     * 获取锁超时等待时间，单位为毫秒。
     *
     * <p>
     * 依赖具体锁实现是否支持timeout
     * </p>
     */
    int timeout() default 0;

    /**
     * lock 前缀
     */
    String lockPrefix() default "lock_";

    /**
     * lock失败之后降级方法，需要加锁方法返回值一致
     */
    String lockFailMethod() default "";

}
