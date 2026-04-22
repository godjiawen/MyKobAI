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

    /**
     * 处理 RequestParam 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of RequestParam with controlled input and output handling.
     *
     * @param file 输入参数；Input parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @PostMapping("/api/user/account/avatar/upload/")
    public Map<String, String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return uploadAvatarService.upload(file);
    }
}