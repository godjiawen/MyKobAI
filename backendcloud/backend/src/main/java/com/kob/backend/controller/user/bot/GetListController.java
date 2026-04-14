package com.kob.backend.controller.user.bot;

import com.kob.backend.pojo.Bot;
import com.kob.backend.service.user.bot.GetListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 控制器，负责接收请求并调用服务层。
 */
@RestController
public class GetListController {
    @Autowired
    private GetListService getListService;

    @GetMapping("/api/user/bot/getlist/")
    public List<Bot> getList() {
        return getListService.getList();
    }
}
