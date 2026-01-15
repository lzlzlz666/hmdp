package com.hmdp.utils;

public interface Ilock {

    /**
     * 尝试获取锁
     * @param timeoutSec
     * @return
     */
    boolean trylock(long timeoutSec);


    /**
     * 释放锁
     */
    void unlock();
}
