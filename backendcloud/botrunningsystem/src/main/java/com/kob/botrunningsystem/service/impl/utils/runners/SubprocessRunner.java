package com.kob.botrunningsystem.service.impl.utils.runners;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 子进程运行器基类，封装“写临时目录 -> 启动子进程 -> 异步读取标准输出 -> 清理”的通用流程。
 * 子类只需实现 buildProcess(File tmpDir) 并返回配置好的 ProcessBuilder。
 *
 * 每次执行都在独立临时目录中进行，机器人代码读取 input.txt（当前目录）即可。
 * 使用异步读取，防止标准输出缓冲区写满导致子进程阻塞。
 */
public abstract class SubprocessRunner implements LanguageRunner {

    /** 子类返回在 tmpDir 内运行的 ProcessBuilder（工作目录已设置） */
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

            // 3. 构建并启动子进程（标准错误分离，便于诊断）
            ProcessBuilder pb = buildProcess(tmpDir);
            pb.directory(tmpDir);
            // 不合并标准错误，改为独立线程读取，避免缓冲区死锁
            pb.redirectErrorStream(false);

            Process process = pb.start();

            // 4. 异步读取标准输出（关键：防止缓冲区写满导致进程挂起）
            StringBuilder stdoutBuf = new StringBuilder();
            StringBuilder stderrBuf = new StringBuilder();
            Thread stdoutReader = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        stdoutBuf.append(line).append("\n");
                    }
                } catch (IOException ignored) {}
            });
            Thread stderrReader = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        stderrBuf.append(line).append("\n");
                    }
                } catch (IOException ignored) {}
            });
            stdoutReader.setDaemon(true);
            stderrReader.setDaemon(true);
            stdoutReader.start();
            stderrReader.start();

            // 5. 限时等待进程结束
            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                System.err.println("[BotRunner] 子进程超时（" + timeoutMs + "ms），已强制终止");
                return 0;
            }

            // 等待读取线程完成
            stdoutReader.join(500);
            stderrReader.join(200);

            // 6. 解析输出（应为 0-3 的单行整数）
            String output = stdoutBuf.toString().trim();
            if (output.isEmpty()) {
                String errOut = stderrBuf.toString().trim();
                System.err.println("[BotRunner] 子进程无标准输出"
                        + (errOut.isEmpty() ? "" : "\n[标准错误] " + errOut));
                return 0;
            }
            // 只取第一行，防止机器人输出额外调试信息
            String firstLine = output.split("\\r?\\n")[0].trim();
            int direction = Integer.parseInt(firstLine);
            if (direction < 0 || direction > 3) {
                System.err.println("[BotRunner] 输出值超出范围 [0,3]: " + direction);
                return 0;
            }
            return direction;

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

