package com.kob.backend.service.user.account;

import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

/**
 * 服务接口定义。
 */
public interface UploadAvatarService {
    /**
     * 处理 upload 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of upload with controlled input and output handling.
     *
     * @param file 输入参数；Input parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    Map<String, String> upload(MultipartFile file);
}