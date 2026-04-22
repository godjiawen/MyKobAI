package com.kob.matchingsystem.service.impl.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class MatchingPool extends Thread{
    private static List<Player> players = new ArrayList<>();
    private ReentrantLock lock = new ReentrantLock();
    private static RestTemplate restTemplate;
    private final static String startGameUrl = "http://127.0.0.1:3000/pk/start/game/";

    /**
     * 更新 setRestTemplate 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of setRestTemplate with controlled input and output handling.
     *
     * @param restTemplate 输入参数；Input parameter.
     */
    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        MatchingPool.restTemplate = restTemplate;
    }

    /**
     * 创建或保存 addPlayer 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of addPlayer with controlled input and output handling.
     *
     * @param userId 标识参数；Identifier value.
     * @param rating 输入参数；Input parameter.
     * @param botId 标识参数；Identifier value.
     */
    public void addPlayer(Integer userId, Integer rating, Integer botId) {
        lock.lock();
        try {
            players.add(new Player(userId, rating, botId,0));
        } finally {
            lock.unlock();
        }
    }

    /**
     * 删除或清理 removePlayer 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of removePlayer with controlled input and output handling.
     *
     * @param userId 标识参数；Identifier value.
     */
    public void removePlayer(Integer userId) {
        lock.lock();
        try {
            List<Player> newPlayers = new ArrayList<>();
            for (Player player : players) {
                if(!player.getUserId().equals(userId)) {
                    newPlayers.add(player);
                }
            }
            players = newPlayers;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 处理 for 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of for with controlled input and output handling.
     *
     * @param players 输入参数；Input parameter.
     */
    private void increaseWaitingTime() { // 将所有当前玩家等待时间加1
        for (Player player : players) {
            player.setWaitingTime(player.getWaitingTime() + 1);
        }
    }

    private boolean checkMatched(Player a, Player b) { // 判断两名玩家是否匹配
        int ratingDelta = Math.abs(a.getRating() - b.getRating());
        int waitingTime = Math.min(a.getWaitingTime(), b.getWaitingTime());
        return ratingDelta <= waitingTime * 10;
    }

    /**
     * 处理 println 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of println with controlled input and output handling.
     *
     * @param a 输入参数；Input parameter.
     * @param b 输入参数；Input parameter.
     * @return 返回 void sendResult(Player a, Player b) { // 返回匹配结果 System.out. 类型结果；Returns a result of type void sendResult(Player a, Player b) { // 返回匹配结果 System.out..
     */
    private void sendResult(Player a, Player b) {  // 返回匹配结果
        System.out.println("send result: " + a + " " + b);
        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add("a_id", a.getUserId().toString());
        data.add("a_bot_id", a.getBotId().toString());
        data.add("b_id", b.getUserId().toString());
        data.add("b_bot_id", b.getBotId().toString());
        restTemplate.postForObject(startGameUrl, data, String.class);
    }

    private void matchPlayers() {  // 尝试匹配所有玩家
        // System.out.println("打印匹配队列: " + players);
        boolean[] used = new boolean[players.size()];
        for (int i = 0; i < players.size(); i ++ ) {
            if (used[i]) continue;
            for (int j = i + 1; j < players.size(); j ++ ) {
                if (used[j]) continue;
                Player a = players.get(i), b = players.get(j);
                if (checkMatched(a, b)) {
                    used[i] = used[j] = true;
                    sendResult(a, b);
                    break;
                }
            }
        }

        List<Player> newPlayers = new ArrayList<>();
        for (int i = 0; i < players.size(); i ++ ) {
            if (!used[i]) {
                newPlayers.add(players.get(i));
            }
        }
        players = newPlayers;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(1000);
                lock.lock();
                try {
                    increaseWaitingTime();
                    matchPlayers();
                } finally {
                    lock.unlock();
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }

}