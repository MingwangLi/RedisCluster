package com.hydsoft.redis.redisson;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

/**
 * @title: RedissonLockUtil
 * @Description:
 * @Author Jane
 * @Date: 2022/6/17 11:16
 * @Version 1.0
 */
@Component
public class RedissonLockUtil {

    private Logger logger = LoggerFactory.getLogger(RedissonLockUtil.class);

    public static final String LOCK_PREFIX = "redisson_lock_";

    //锁有效时间60s
    public static final long LOCK_TIME = 60;

    //锁等待超时时间1s
    public static final long WAIT_TIME = 1;

    @Autowired
    private RedissonClient redissonClient;

    private String getLockKey(String key) {
        return LOCK_PREFIX + key;
    }

    public boolean tryLock(String key) {
        return tryLock(key, LOCK_TIME, WAIT_TIME);
    }

    /**
     * @Description: 有效期期内 会有一个看门狗线程轮询锁是否释放 如果没有 会不断续租锁的有效期 保证业务处理完再释放锁 不会有并发问题
     * @Author Jane
     * @Date: 2022/6/20 10:30
     * @Version 1.0
     */
    public boolean tryLock(String key, long lockTime) {
        String lockKey = getLockKey(key);
        logger.info("try lock key:{}", lockKey);
        RLock lock = redissonClient.getLock(lockKey);
        if (null == lock) {
            return false;
        }
        try {
            return lock.tryLock(lockTime, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("try lock key :{} error:{}", lockKey, e.getMessage());
        }
        return false;

    }

    /**
     * @Description: 在waitTime时间内尝试获取锁 不会续期 如果有效时间内业务乜有处理完 会导致并发问题
     * @Author Jane
     * @Date: 2022/6/20 10:29
     * @Version 1.0
     */
    public boolean tryLock(String key, long lockTime, long waitTime) {
        String lockKey = getLockKey(key);
        logger.info("try lock key:{}", lockKey);
        RLock lock = redissonClient.getLock(lockKey);
        if (null == lock) {
            return false;
        }
        try {
            return lock.tryLock(waitTime, lockTime, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("try lock key :{} error:{}", lockKey, e.getMessage());
        }
        return false;
    }

    public void lock(String key) {
        lock(key, LOCK_TIME);
    }

    public void lock(String key, long lockTime) {
        String lockKey = getLockKey(key);
        logger.info("lock key:{}", lockKey);
        RLock lock = redissonClient.getLock(lockKey);
        if (null != lock) {
            lock.lock(lockTime, TimeUnit.SECONDS);
        }
    }

    public void unlock(String key) {
        String lockKey = getLockKey(key);
        RLock lock = this.redissonClient.getLock(lockKey);
        //防止锁有效时间结束后执行unlock()抛出异常
        if (null != lock && lock.isLocked()) {
            if (!lock.isHeldByCurrentThread()) {
                String threadName = Thread.currentThread().getName();
                logger.warn("incorrect operation " + threadName + " unlock key:" + lockKey);
                return;
            }
            //注意 为了防止程序员在tryLock失败的情况下 调用unlock方法导致java.lang.IllegalMonitorStateException: attempt to unlock lock,
            // not locked by current thread by node id  此处这里需要加上lock.isHeldByCurrentThread()判断
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                String threadName = Thread.currentThread().getName();
                logger.info(threadName + " unlock key:{}", lockKey);
            }
            // lock.unlock();
            // logger.info("unlock key:{}", lockKey);
        }
    }
}
