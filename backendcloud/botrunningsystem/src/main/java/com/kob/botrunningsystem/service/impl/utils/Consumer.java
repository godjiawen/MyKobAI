package com.kob.botrunningsystem.service.impl.utils;

import com.kob.botrunningsystem.service.impl.utils.runners.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
public class Consumer extends Thread {

    private Bot bot;
    private static RestTemplate restTemplate;
    private static final String RECEIVE_BOT_MOVE_URL = "http://127.0.0.1:3000/pk/receive/bot/move/";

    /**
     * 更新 setRestTemplate 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of setRestTemplate with controlled input and output handling.
     *
     * @param restTemplate 输入参数；Input parameter.
     */
    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        Consumer.restTemplate = restTemplate;
    }

    /**
     * 创建或保存 startTimeout 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of startTimeout with controlled input and output handling.
     *
     * @param timeout 时间参数；Time parameter.
     * @param bot 机器人相关参数；Bot-related parameter.
     */
    public void startTimeout(long timeout, Bot bot) {
        this.bot = bot;
        this.start();
        try {
            this.join(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            this.interrupt();
        }
    }

    /**
     * 查询并返回 getRunner 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getRunner with controlled input and output handling.
     *
     * @param language 语言参数；Language parameter.
     * @return 返回 LanguageRunner 类型结果；Returns a result of type LanguageRunner.
     */
    private static LanguageRunner getRunner(String language) {
        if (language == null) return new JavaRunner();
        return switch (language.toLowerCase()) {
            case "python"     -> new PythonRunner();
            case "cpp"        -> new CppRunner();
            case "javascript" -> new JsRunner();
            default           -> new JavaRunner();
        };
    }

    /**
     * 执行或处理 run 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of run with controlled input and output handling.
     *
     */
    @Override
    public void run() {
        LanguageRunner runner = getRunner(bot.getLanguage());
        Integer direction;
        try {
            direction = runner.run(bot.getBotCode(), bot.getInput(), 2000);
        } catch (Exception e) {
            System.err.println("[Consumer] 运行器异常: " + e.getMessage());
            direction = 0;
        }

        System.out.println("[Consumer] userId=" + bot.getUserId()
                + " lang=" + bot.getLanguage() + " direction=" + direction);

        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add("user_id", bot.getUserId().toString());
        data.add("direction", direction.toString());
        restTemplate.postForObject(RECEIVE_BOT_MOVE_URL, data, String.class);
    }
}