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

@Service
public class UploadAvatarServiceImpl implements UploadAvatarService {

    @Autowired
    private UserMapper userMapper;

    @Value("${kob.upload.dir}")
    private String uploadDir;

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

        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
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

        // 构建图片访问URL（依赖于当前的服务器地址和刚才WebConfig映射的/avatars/）
        // 比如：http://127.0.0.1:3000/avatars/550e8400-e29b-41d4-a716-446655440000.jpg
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

