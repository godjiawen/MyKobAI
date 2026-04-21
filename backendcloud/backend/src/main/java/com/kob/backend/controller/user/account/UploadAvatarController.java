package com.kob.backend.controller.user.account;

import com.kob.backend.service.user.account.UploadAvatarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 控制器，负责接收请求并调用服务层。
 */
@RestController
public class UploadAvatarController {
    
    @Autowired
    private UploadAvatarService uploadAvatarService;

    @PostMapping("/api/user/account/avatar/upload/")
    /**
     * Handles uploadAvatar.
     * ??uploadAvatar?
     */
    public Map<String, String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return uploadAvatarService.upload(file);
    }
}

