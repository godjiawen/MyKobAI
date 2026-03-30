package com.kob.backend.service.impl.user.account;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.User;
import com.kob.backend.service.impl.utils.UserDetailsImpl;
import com.kob.backend.service.user.account.UpdateAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class UpdateAccountServiceImpl implements UpdateAccountService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{6,12}$");

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Map<String, String> updateUsername(String newUsername) {
        Map<String, String> map = new HashMap<>();

        UsernamePasswordAuthenticationToken authenticationToken =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl loginUser = (UserDetailsImpl) authenticationToken.getPrincipal();
        User user = loginUser.getUser();

        if (newUsername == null) {
            map.put("error_message", "用户名不能为空");
            return map;
        }

        newUsername = newUsername.trim();
        if (newUsername.isEmpty()) {
            map.put("error_message", "用户名不能为空");
            return map;
        }

        if (!USERNAME_PATTERN.matcher(newUsername).matches()) {
            map.put("error_message", "用户名需为6-12位，只能包含字母、数字和下划线");
            return map;
        }

        if (newUsername.equals(user.getUsername())) {
            map.put("error_message", "新用户名不能与原用户名相同");
            return map;
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", newUsername);
        List<User> users = userMapper.selectList(queryWrapper);
        if (!users.isEmpty()) {
            map.put("error_message", "用户名已存在");
            return map;
        }

        User dbUser = userMapper.selectById(user.getId());
        dbUser.setUsername(newUsername);
        userMapper.updateById(dbUser);

        map.put("error_message", "success");
        return map;
    }

    @Override
    public Map<String, String> updatePassword(String oldPassword, String newPassword, String confirmedPassword) {
        Map<String, String> map = new HashMap<>();

        UsernamePasswordAuthenticationToken authenticationToken =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl loginUser = (UserDetailsImpl) authenticationToken.getPrincipal();
        User user = loginUser.getUser();

        if (oldPassword == null || oldPassword.isEmpty()) {
            map.put("error_message", "原密码不能为空");
            return map;
        }

        if (newPassword == null || confirmedPassword == null || newPassword.isEmpty() || confirmedPassword.isEmpty()) {
            map.put("error_message", "新密码不能为空");
            return map;
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            map.put("error_message", "原密码不正确");
            return map;
        }

        if (newPassword.length() < 8 || newPassword.length() > 16) {
            map.put("error_message", "新密码长度需为8-16位");
            return map;
        }

        if (containsWhitespace(newPassword)) {
            map.put("error_message", "新密码不能包含空白字符");
            return map;
        }

        if (passwordComplexityScore(newPassword) < 3) {
            map.put("error_message", "新密码至少包含大写字母、小写字母、数字、特殊字符中的三项");
            return map;
        }

        if (!newPassword.equals(confirmedPassword)) {
            map.put("error_message", "两次输入的新密码不一致");
            return map;
        }

        User dbUser = userMapper.selectById(user.getId());
        dbUser.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(dbUser);

        map.put("error_message", "success");
        return map;
    }

    private int passwordComplexityScore(String password) {
        int score = 0;
        if (password.chars().anyMatch(Character::isUpperCase)) score++;
        if (password.chars().anyMatch(Character::isLowerCase)) score++;
        if (password.chars().anyMatch(Character::isDigit)) score++;
        if (password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch))) score++;
        return score;
    }

    private boolean containsWhitespace(String text) {
        return text.chars().anyMatch(Character::isWhitespace);
    }
}

