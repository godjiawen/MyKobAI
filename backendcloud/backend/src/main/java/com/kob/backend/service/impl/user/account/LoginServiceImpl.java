package com.kob.backend.service.impl.user.account;

import com.kob.backend.pojo.User;
import com.kob.backend.service.impl.utils.UserDetailsImpl;
import com.kob.backend.service.user.account.LoginService;
import com.kob.backend.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service implementation for user login that authenticates credentials and returns a JWT token.
 * 用户登录服务实现，验证凭据并返回JWT令牌。
 */
@Service
public class LoginServiceImpl implements LoginService {
    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * 查询并返回 getToken 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getToken with controlled input and output handling.
     *
     * @param username 用户相关参数；User-related parameter.
     * @param password 密码参数；Password parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @Override
    public Map<String, String> getToken(String username, String password) {
        Map<String, String> map = new HashMap<>();
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(username, password);

        try {
            Authentication authenticate = authenticationManager.authenticate(authenticationToken);
            UserDetailsImpl loginUser = (UserDetailsImpl) authenticate.getPrincipal();
            User user = loginUser.getUser();
            String jwt = JwtUtil.createJWT(user.getId().toString());

            map.put("error_message", "success");
            map.put("token", jwt);
            return map;
        } catch (AuthenticationException e) {
            map.put("error_message", "username or password is incorrect");
            return map;
        } catch (Exception e) {
            throw new AuthenticationServiceException("Login failed", e);
        }
    }
}