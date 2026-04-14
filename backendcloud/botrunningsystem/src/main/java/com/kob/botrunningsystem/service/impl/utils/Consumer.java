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

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        Consumer.restTemplate = restTemplate;
    }

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

    /** 根据语言选择对应运行器 */
    private static LanguageRunner getRunner(String language) {
        if (language == null) return new JavaRunner();
        return switch (language.toLowerCase()) {
            case "python"     -> new PythonRunner();
            case "cpp"        -> new CppRunner();
            case "javascript" -> new JsRunner();
            default           -> new JavaRunner();
        };
    }

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
