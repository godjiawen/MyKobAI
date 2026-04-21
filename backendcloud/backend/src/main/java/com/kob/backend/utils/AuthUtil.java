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
    private AuthUtil() {
    }

    /**
     * Returns the User entity of the currently authenticated principal from the security context.
     * 从安全上下文中返回当前已认证主体对应的User实体。
     */
    public static User getCurrentUser() {
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl loginUser = (UserDetailsImpl) authentication.getPrincipal();
        return loginUser.getUser();
    }
}
