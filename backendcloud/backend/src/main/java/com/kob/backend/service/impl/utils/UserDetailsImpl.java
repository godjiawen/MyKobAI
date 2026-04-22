package com.kob.backend.service.impl.utils;


import com.kob.backend.pojo.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Spring Security UserDetails implementation that wraps the application's User entity.
 * 封装应用User实体的Spring Security UserDetails实现类。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsImpl implements UserDetails {


    private User user;  //User是pojo中自己定义的user

    /**
     * 查询并返回 getAuthorities 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getAuthorities with controlled input and output handling.
     *
     * @return 返回集合结果；Returns a collection result.
     */
    @Override //重写
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    /**
     * 查询并返回 getPassword 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getPassword with controlled input and output handling.
     *
     * @return 返回字符串结果；Returns a string result.
     */
    @Override
    public String getPassword() {

        return user.getPassword();
    }

    /**
     * 查询并返回 getUsername 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getUsername with controlled input and output handling.
     *
     * @return 返回字符串结果；Returns a string result.
     */
    @Override
    public String getUsername() {
        return user.getUsername();
    }

    /**
     * 校验或判断 isAccountNonExpired 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of isAccountNonExpired with controlled input and output handling.
     *
     * @return 返回判断结果；Returns a boolean decision result.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 校验或判断 isAccountNonLocked 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of isAccountNonLocked with controlled input and output handling.
     *
     * @return 返回判断结果；Returns a boolean decision result.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 校验或判断 isCredentialsNonExpired 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of isCredentialsNonExpired with controlled input and output handling.
     *
     * @return 返回判断结果；Returns a boolean decision result.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 校验或判断 isEnabled 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of isEnabled with controlled input and output handling.
     *
     * @return 返回判断结果；Returns a boolean decision result.
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}