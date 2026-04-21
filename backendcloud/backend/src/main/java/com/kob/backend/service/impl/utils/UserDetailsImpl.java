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
     * Returns the user's granted authorities (none configured by default).
     * 返回用户的授权权限（默认未配置）。
     */
    @Override //重写
    /**
     * Handles getAuthorities.
     * ??getAuthorities?
     */
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    /**
     * Returns the user's encoded password.
     * 返回用户的加密密码。
     */
    @Override
    /**
     * Handles getPassword.
     * ??getPassword?
     */
    public String getPassword() {

        return user.getPassword();
    }

    /**
     * Returns the user's username.
     * 返回用户名。
     */
    @Override
    /**
     * Handles getUsername.
     * ??getUsername?
     */
    public String getUsername() {
        return user.getUsername();
    }

    /**
     * Returns true, indicating the account is never expired.
     * 返回true，表示账户永不过期。
     */
    @Override
    /**
     * Handles isAccountNonExpired.
     * ??isAccountNonExpired?
     */
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Returns true, indicating the account is never locked.
     * 返回true，表示账户永不锁定。
     */
    @Override
    /**
     * Handles isAccountNonLocked.
     * ??isAccountNonLocked?
     */
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Returns true, indicating credentials are never expired.
     * 返回true，表示凭据永不过期。
     */
    @Override
    /**
     * Handles isCredentialsNonExpired.
     * ??isCredentialsNonExpired?
     */
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Returns true, indicating the account is always enabled.
     * 返回true，表示账户始终启用。
     */
    @Override
    /**
     * Handles isEnabled.
     * ??isEnabled?
     */
    public boolean isEnabled() {
        return true;
    }
}
