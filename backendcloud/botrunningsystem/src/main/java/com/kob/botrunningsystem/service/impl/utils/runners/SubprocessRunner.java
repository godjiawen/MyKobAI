package com.kob.botrunningsystem.service.impl.utils.runners;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 子进程 Runner 基类，封装"写临时目录 → 启动子进程 → 读 stdout → 清理"的通用流程。
 * 子类只需实现 buildProcess(File tmpDir) 返回配置好的 ProcessBuilder。
 *
 * 每次执行都在独立的临时目录里，Bot 代码读 input.txt（当前目录）即可。
 */
public abstract class SubprocessRunner implements LanguageRunner {

    /** 子类返回在 tmpDir 内运行的 ProcessBuilder（working directory 已设好） */
    protected abstract ProcessBuilder buildProcess(File tmpDir) throws Exception;

    /** 代码文件的扩展名，如 ".py" ".js" ".cpp" */
    protected abstract String codeFileExtension();

    @Override
    public Integer run(String code, String input, long timeoutMs) {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"),
                "kob_bot_" + UUID.randomUUID());
        tmpDir.mkdirs();

        try {
            // 1. 写 input.txt
            Files.writeString(new File(tmpDir, "input.txt").toPath(), input + "\n");

            // 2. 写代码文件
            Files.writeString(new File(tmpDir, "bot" + codeFileExtension()).toPath(), code);

            // 3. 构建并启动子进程
            ProcessBuilder pb = buildProcess(tmpDir);
            pb.directory(tmpDir);
            pb.redirectErrorStream(true);   // stderr 并入 stdout，方便调试

            Process process = pb.start();

            // 4. 限时等待
            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                System.err.println("[BotRunner] 子进程超时，已强制终止");
                return 0;
            }

            // 5. 读取输出（应为 0-3 的单行整数）
            String output = new String(process.getInputStream().readAllBytes()).trim();
            if (output.isEmpty()) {
                System.err.println("[BotRunner] 子进程无输出");
                return 0;
            }
            // 取第一行，防止 Bot 多打了换行
            String firstLine = output.split("\\r?\\n")[0].trim();
            return Integer.parseInt(firstLine);

        } catch (NumberFormatException e) {
            System.err.println("[BotRunner] 输出不是合法整数");
            return 0;
        } catch (Exception e) {
            System.err.println("[BotRunner] 运行出错: " + e.getMessage());
            return 0;
        } finally {
            deleteDir(tmpDir);
        }
    }

    private void deleteDir(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) deleteDir(f);
        dir.delete();
    }
}

