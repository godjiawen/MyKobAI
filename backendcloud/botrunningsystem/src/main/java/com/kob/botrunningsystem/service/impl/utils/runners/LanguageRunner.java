package com.kob.botrunningsystem.service.impl.utils.runners;

/**
 * 各语言机器人运行器的统一接口。
 * run() 同步执行，超时由外层 Consumer.startTimeout() 通过线程中断兜底。
 */
public interface LanguageRunner {
    /**
     * 执行或处理 run 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of run with controlled input and output handling.
     *
     * @param code 代码内容参数；Code content parameter.
     * @param input 输入参数；Input parameter.
     * @param timeoutMs 时间参数；Time parameter.
     * @return 返回数值结果；Returns a numeric result.
     */
    Integer run(String code, String input, long timeoutMs);
}
