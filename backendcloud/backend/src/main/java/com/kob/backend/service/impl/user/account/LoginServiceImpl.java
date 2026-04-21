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
     * Authenticates the user with the given credentials and returns a JWT token on success.
     * 使用给定凭据对用户进行身份验证，成功时返回JWT令牌。
     */
    @Override
    /**
     * Handles getToken.
     * ??getToken?
     */
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
