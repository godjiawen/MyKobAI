package com.kob.botrunningsystem.service.impl.utils.runners;

/**
 * 各语言机器人运行器的统一接口。
 * run() 同步执行，超时由外层 Consumer.startTimeout() 通过线程中断兜底。
 */
public interface LanguageRunner {
    /**
     * @param code      机器人源代码
     * @param input     本局游戏输入字符串（会写入 input.txt）
     * @param timeoutMs 子进程最大运行毫秒数（仅子进程类 runner 使用）
     * @return 方向 0-3，出错返回 0
     */
    Integer run(String code, String input, long timeoutMs);
}

