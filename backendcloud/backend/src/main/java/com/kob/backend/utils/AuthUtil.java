package com.kob.backend.utils;

import com.kob.backend.pojo.User;
import com.kob.backend.service.impl.utils.UserDetailsImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility class for retrieving the currently authenticated user from the security context.
 * 从Spring安全上下文中获取当前已认证用户的工具类。
 */
public class AuthUtil {
    /**
     * 处理 AuthUtil 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of AuthUtil with controlled input and output handling.
     *
     */
    private AuthUtil() {
    }

    /**
     * 查询并返回 getCurrentUser 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getCurrentUser with controlled input and output handling.
     *
     * @return 返回 User 类型结果；Returns a result of type User.
     */
    public static User getCurrentUser() {
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl loginUser = (UserDetailsImpl) authentication.getPrincipal();
        return loginUser.getUser();
    }
}