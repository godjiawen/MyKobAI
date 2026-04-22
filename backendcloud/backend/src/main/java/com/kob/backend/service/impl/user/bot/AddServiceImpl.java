package com.kob.backend.service.impl.user.bot;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kob.backend.mapper.BotMapper;
import com.kob.backend.pojo.Bot;
import com.kob.backend.pojo.User;
import com.kob.backend.service.impl.utils.UserDetailsImpl;
import com.kob.backend.service.user.bot.AddService;
import com.kob.backend.utils.BotLanguageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Service implementation for adding a new bot to the current user's bot list.
 * 向当前用户的机器人列表中添加新机器人的服务实现。
 */
@Service
public class AddServiceImpl implements AddService {

    @Autowired
    private BotMapper botMapper;

    /**
     * 创建或保存 add 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of add with controlled input and output handling.
     *
     * @param data 输入参数；Input parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @Override
    public Map<String, String> add(Map<String, String> data) {
        UsernamePasswordAuthenticationToken authenticationToken =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl loginuser = (UserDetailsImpl) authenticationToken.getPrincipal();
        User user = loginuser.getUser();

        String title = data.get("title");
        String description = data.get("description");
        String content = data.get("content");
        String language = BotLanguageUtil.normalize(data.getOrDefault("language", "java"));

        Map<String, String> map = new HashMap<>();

        if (title == null || title.length() == 0) {
            map.put("error_message", "标题不能为空");
            return map;
        }

        if (title.length() > 100) {
            map.put("error_message", "标题的长度不能大于100");
            return map;
        }

        if (description == null || description.length() == 0) {
            description = "这个用户很懒，什么也没留下~";
        }

        if (description != null && description.length() > 300) {
            map.put("error_message", "Bot描述的长度不能大于300");
            return map;
        }

        if (content == null || content.length() == 0) {
            map.put("error_message", "代码不能为空");
            return map;
        }

        if (content.length() > 10000) {
            map.put("error_message", "代码的长度不能超过10000");
            return map;
        }

        if (!BotLanguageUtil.isSupported(language)) {
            map.put("error_message", "涓嶆敮鎸佺殑 Bot 璇█");
            return map;
        }

        QueryWrapper<Bot> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", user.getId());
        if (botMapper.selectCount(queryWrapper) >= 10) {
            map.put("error_message", "每名玩家最多创建10个Bot!");
            return map;
        }

        Date now = new Date();
        Bot bot = new Bot(null, user.getId(), title, description, content, language, now, now);

        botMapper.insert(bot);
        map.put("error_message", "success");

        return map;
    }
}