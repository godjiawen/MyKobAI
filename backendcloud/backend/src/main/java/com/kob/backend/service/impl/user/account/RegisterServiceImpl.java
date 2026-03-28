package com.kob.backend.service.impl.user.account;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.User;
import com.kob.backend.service.user.account.RegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class RegisterServiceImpl implements RegisterService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{6,12}$");

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Map<String, String> register(String username, String password, String confirmePassword) {
        Map<String, String> map = new HashMap<>();

        if (username == null) {
            map.put("error_message", "用户名不能为空");
            return map;
        }
        if (password == null || confirmePassword == null) {
            map.put("error_message", "密码不能为空");
            return map;
        }

        username = username.trim();
        if (username.isEmpty()) {
            map.put("error_message", "用户名不能为空");
            return map;
        }
        if (password.isEmpty() || confirmePassword.isEmpty()) {
            map.put("error_message", "密码不能为空");
            return map;
        }

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            map.put("error_message", "用户名需为6-12位，只能包含字母、数字和下划线");
            return map;
        }

        if (password.length() < 8 || password.length() > 16) {
            map.put("error_message", "密码长度需为8-16位");
            return map;
        }

        if (containsWhitespace(password)) {
            map.put("error_message", "密码不能包含空白字符");
            return map;
        }

        if (passwordComplexityScore(password) < 3) {
            map.put("error_message", "密码至少包含大写字母、小写字母、数字、特殊字符中的三项");
            return map;
        }

        if (!password.equals(confirmePassword)) {
            map.put("error_message", "两次输入的密码不一致");
            return map;
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        List<User> users = userMapper.selectList(queryWrapper);
        if (!users.isEmpty()) {
            map.put("error_message", "用户名已存在");
            return map;
        }

        String encodedPassword = passwordEncoder.encode(password);
        String photo = "https://cdn.acwing.com/media/user/profile/photo/84941_lg_8a5730ded2.jpg";
        User user = new User(null, username, encodedPassword, photo, 1500);
        userMapper.insert(user);

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
