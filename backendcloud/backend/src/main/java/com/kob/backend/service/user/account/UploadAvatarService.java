package com.kob.backend.service.user.account;

import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

/**
 * 服务接口定义。
 */
public interface UploadAvatarService {
    Map<String, String> upload(MultipartFile file);
}

