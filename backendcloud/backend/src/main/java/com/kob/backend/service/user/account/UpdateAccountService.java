package com.kob.backend.service.user.account;

import java.util.Map;

public interface UpdateAccountService {
    Map<String, String> updateUsername(String newUsername);
    Map<String, String> updatePassword(String oldPassword, String newPassword, String confirmedPassword);
}

