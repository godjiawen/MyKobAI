package com.kob.botrunningsystem.service.impl.utils;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BotPool extends Thread {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private final Queue<Bot> bots = new LinkedList<>();

    public void addBot(Integer userId, String botCode, String input, String language) {
        lock.lock();
        try {
            bots.add(new Bot(userId, botCode, input, language));
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private void consume(Bot bot) {
        Consumer consumer = new Consumer();
        consumer.startTimeout(2000, bot);
    }

    @Override
    public void run() {
        while (true) {
            Bot bot;
            lock.lock();
            try {
                // 用 while 防止虚假唤醒
                while (bots.isEmpty()) {
                    condition.await();
                }
                bot = bots.remove();
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            } finally {
                lock.unlock(); // 无论正常还是异常，都保证锁被完整释放
            }
            consume(bot);  // 比较耗时，可能会执行几秒钟，在锁外执行
        }
    }
}
