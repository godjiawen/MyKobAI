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

    /**
     * 查询并返回 getList 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getList with controlled input and output handling.
     *
     * @return 返回集合结果；Returns a collection result.
     */
    @GetMapping("/api/user/bot/getlist/")
    public List<Bot> getList() {
        return getListService.getList();
    }
}