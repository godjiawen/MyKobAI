package com.kob.botrunningsystem.service.impl.utils.runners;

import org.joor.Reflect;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Java Runner —— 使用 jOOR 在 JVM 内动态编译并执行。
 * Bot 代码须实现 java.util.function.Supplier<Integer>，
 * 通过读取当前目录下的 input.txt 获得游戏输入。
 */
public class JavaRunner implements LanguageRunner {

    private static String addUid(String code, String uid) {
        int k = code.indexOf(" implements java.util.function.Supplier<Integer>");
        if (k < 0) throw new IllegalArgumentException("Bot 代码必须包含 'implements java.util.function.Supplier<Integer>'");
        return code.substring(0, k) + uid + code.substring(k);
    }

    @Override
    public Integer run(String code, String input, long timeoutMs) {
        UUID uuid = UUID.randomUUID();
        String uid = uuid.toString().substring(0, 8);

        // 编译 Bot 类
        Supplier<Integer> bot = Reflect.compile(
                "com.kob.botrunningsystem.utils.Bot" + uid,
                addUid(code, uid)
        ).create().get();

        // 写入输入文件（Bot 代码读 input.txt）
        File inputFile = new File("input.txt");
        try (PrintWriter pw = new PrintWriter(inputFile)) {
            pw.println(input);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("无法写入 input.txt", e);
        }

        try {
            return bot.get();
        } finally {
            inputFile.delete();
        }
    }
}

