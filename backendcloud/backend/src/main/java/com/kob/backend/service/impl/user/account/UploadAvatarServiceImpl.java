package com.kob.backend.service.impl.user.account;

import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.User;
import com.kob.backend.service.impl.utils.UserDetailsImpl;
import com.kob.backend.service.user.account.UploadAvatarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service implementation for uploading and updating the current user's avatar image.
 * 上传并更新当前用户头像图片的服务实现。
 */
@Service
public class UploadAvatarServiceImpl implements UploadAvatarService {

    @Autowired
    private UserMapper userMapper;

    @Value("${kob.upload.dir}")
    private String uploadDir;

    /**
     * 处理 upload 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of upload with controlled input and output handling.
     *
     * @param file 输入参数；Input parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @Override
    public Map<String, String> upload(MultipartFile file) {
        Map<String, String> map = new HashMap<>();

        if (file == null || file.isEmpty()) {
            map.put("error_message", "文件不能为空");
            return map;
        }

        UsernamePasswordAuthenticationToken authenticationToken =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl loginUser = (UserDetailsImpl) authenticationToken.getPrincipal();
        User user = loginUser.getUser();

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            map.put("error_message", "文件名异常");
            return map;
        }

        int suffixIndex = originalFilename.lastIndexOf(".");
        if (suffixIndex < 0 || suffixIndex == originalFilename.length() - 1) {
            map.put("error_message", "Invalid file format");
            return map;
        }

        String suffix = originalFilename.substring(suffixIndex);
        if (!suffix.equalsIgnoreCase(".jpg") && !suffix.equalsIgnoreCase(".png") && !suffix.equalsIgnoreCase(".jpeg")) {
            map.put("error_message", "只支持上传jpg/png格式图片");
            return map;
        }

        String newFileName = UUID.randomUUID().toString() + suffix;
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File dest = new File(dir, newFileName);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            e.printStackTrace();
            map.put("error_message", "文件上传失败");
            return map;
        }

        // 基于当前服务地址拼接头像访问路径
        String photoUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/avatars/")
                .path(newFileName)
                .toUriString();

        User dbUser = userMapper.selectById(user.getId());
        dbUser.setPhoto(photoUrl);
        userMapper.updateById(dbUser); // 更新数据库

        map.put("error_message", "success");
        map.put("photo", photoUrl); // 将新头像地址返回去

        return map;
    }
}