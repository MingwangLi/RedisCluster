package com.hydsoft.redis.test;

import com.hydsoft.redis.redisson.RedissonLockUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @title: RedisClusterTest
 * @Description:
 * @Author Jane
 * @Date: 2022/6/17 14:04
 * @Version 1.0
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class RedisClusterTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonLockUtil redissonLockUtil;

    @Test
    public void test01() {
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        valueOperations.set("sex", "male");
    }

    @Test
    public void test02() {
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        String sex = valueOperations.get("sex");
        System.out.println("sex:" + sex);
    }

    //错误使用 只有当tryLock返回true时，才使用 try finally处理业务，处理完释放锁。
    @Test
    public void test03() {

        String key = "test3";

        new Thread(() -> {
            System.out.println("A线程开始获取分布式锁");
            try {
                boolean flag = redissonLockUtil.tryLock(key);
                if (!flag) {
                    System.out.println("A线程获取分布式锁失败");
                    return;
                }
                System.out.println("A线程获取分布式锁成功");
                //模拟业务
                Thread.sleep(2000);
            } catch (InterruptedException e) {

            }finally {
                redissonLockUtil.unlock(key);
            }
        }).start();

        new Thread(() -> {
            System.out.println("B线程开始获取分布式锁");
            try {
                boolean flag = redissonLockUtil.tryLock(key);
                if (!flag) {
                    System.out.println("B线程获取分布式锁失败");
                    return;
                }
                System.out.println("B线程获取分布式锁成功");
                //模拟业务
                Thread.sleep(2000);
            } catch (InterruptedException e) {

            }finally {
                redissonLockUtil.unlock(key);
            }
        }).start();

        try {
            //防止Test提前结束
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test04() {

        String key = "test4";

        new Thread(() -> {
            System.out.println("A线程开始获取分布式锁");
            boolean flag = redissonLockUtil.tryLock(key);
            if (!flag) {
                System.out.println("A线程获取分布式锁失败");
                return;
            }
            System.out.println("A线程获取分布式锁成功");
            try {
                //模拟业务
                Thread.sleep(2000);
            } catch (InterruptedException e) {

            }finally {
                redissonLockUtil.unlock(key);
            }
        }).start();

        new Thread(() -> {
            System.out.println("B线程开始获取分布式锁");
            boolean flag = redissonLockUtil.tryLock(key);
            if (!flag) {
                System.out.println("B线程获取分布式锁失败");
                return;
            }
            System.out.println("B线程获取分布式锁成功");
            try {

                //模拟业务
                Thread.sleep(2000);
            } catch (InterruptedException e) {

            }finally {
                redissonLockUtil.unlock(key);
            }
        }).start();
        try {
            //防止Test提前结束
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //测试锁有效时间结束后释放锁
    @Test
    public void test05() {
        String key = "test5";
        boolean flag = redissonLockUtil.tryLock(key);
        if (!flag) {
            return;
        }
        try {
            //模拟业务
            Thread.sleep(62000);
        } catch (InterruptedException e) {

        }finally {
            redissonLockUtil.unlock(key);
        }
    }

    //测试看门狗无限续期锁有效期
    @Test
    public void test06() {
        String key = "test6";
        boolean flag = redissonLockUtil.tryLock(key, 60L);
        if (!flag) {
            return;
        }
        try {
            //模拟业务
            Thread.sleep(62000);
        } catch (InterruptedException e) {

        }finally {
            redissonLockUtil.unlock(key);
        }

    }
}
