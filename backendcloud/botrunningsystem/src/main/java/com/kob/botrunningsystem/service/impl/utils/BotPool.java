package com.kob.botrunningsystem.service.impl.utils;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BotPool extends Thread {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private final Queue<Bot> bots = new LinkedList<>();

    /**
     * 创建或保存 addBot 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of addBot with controlled input and output handling.
     *
     * @param userId 标识参数；Identifier value.
     * @param botCode 机器人相关参数；Bot-related parameter.
     * @param input 输入参数；Input parameter.
     * @param language 语言参数；Language parameter.
     */
    public void addBot(Integer userId, String botCode, String input, String language) {
        lock.lock();
        try {
            bots.add(new Bot(userId, botCode, input, language));
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 处理 consume 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of consume with controlled input and output handling.
     *
     * @param bot 机器人相关参数；Bot-related parameter.
     */
    private void consume(Bot bot) {
        Consumer consumer = new Consumer();
        consumer.startTimeout(2000, bot);
    }

    /**
     * 执行或处理 run 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of run with controlled input and output handling.
     *
     */
    @Override
    public void run() {
        while (true) {
            Bot bot;
            lock.lock();
            try {
                // 使用循环判断，避免线程被虚假唤醒
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