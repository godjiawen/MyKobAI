package com.kob.botrunningsystem.service.impl.utils.runners;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java 运行器通过独立的 javac/java 子进程执行机器人代码，避免与服务进程共享 JVM。
 */
public class JavaRunner implements LanguageRunner {

    private static final String BOT_PACKAGE = "com.kob.botrunningsystem.utils";
    private static final Pattern BOT_CLASS_PATTERN = Pattern.compile("\\bpublic\\s+class\\s+Bot\\b");

    /**
     * 构建或转换 buildBotSource 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of buildBotSource with controlled input and output handling.
     *
     * @param code 代码内容参数；Code content parameter.
     * @param uid 标识参数；Identifier value.
     * @return 返回字符串结果；Returns a string result.
     */
    private static String buildBotSource(String code, String uid) {
        Matcher matcher = BOT_CLASS_PATTERN.matcher(code);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Bot 代码必须包含 `public class Bot`");
        }

        String normalized = code;
        if (normalized.contains("package ")) {
            throw new IllegalArgumentException("Bot 代码不需要 package 声明");
        }

        normalized = matcher.replaceFirst("public class Bot" + uid);
        return "package " + BOT_PACKAGE + ";\n" + normalized;
    }

    /**
     * 构建或转换 buildWrapperSource 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of buildWrapperSource with controlled input and output handling.
     *
     * @param uid 标识参数；Identifier value.
     * @return 返回字符串结果；Returns a string result.
     */
    private static String buildWrapperSource(String uid) {
        return """
                package %s;

                public class RunnerMain {
                    public static void main(String[] args) {
                        System.out.println(new Bot%s().get());
                    }
                }
                """.formatted(BOT_PACKAGE, uid);
    }

    /**
     * 处理 readStream 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of readStream with controlled input and output handling.
     *
     * @param process 输入参数；Input parameter.
     * @param buffer 输入参数；Input parameter.
     * @param errorStream 输入参数；Input parameter.
     */
    private static void readStream(Process process, StringBuilder buffer, boolean errorStream) {
        Thread reader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    errorStream ? process.getErrorStream() : process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    buffer.append(line).append("\n");
                }
            } catch (IOException ignored) {
            }
        });
        reader.setDaemon(true);
        reader.start();
        try {
            reader.join(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 构建或转换 parseDirection 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of parseDirection with controlled input and output handling.
     *
     * @param output 输入参数；Input parameter.
     * @param stderr 输入参数；Input parameter.
     * @return 返回数值结果；Returns a numeric result.
     */
    private static Integer parseDirection(String output, String stderr) {
        String trimmed = output.trim();
        if (trimmed.isEmpty()) {
            System.err.println("[JavaRunner] 子进程无 stdout 输出"
                    + (stderr.isBlank() ? "" : "\n[stderr] " + stderr.trim()));
            return 0;
        }

        try {
            int direction = Integer.parseInt(trimmed.split("\\r?\\n")[0].trim());
            if (direction < 0 || direction > 3) {
                System.err.println("[JavaRunner] 输出值超出范围 [0,3]: " + direction);
                return 0;
            }
            return direction;
        } catch (NumberFormatException e) {
            System.err.println("[JavaRunner] 输出不是合法整数");
            return 0;
        }
    }

    /**
     * 执行或处理 run 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of run with controlled input and output handling.
     *
     * @param code 代码内容参数；Code content parameter.
     * @param input 输入参数；Input parameter.
     * @param timeoutMs 时间参数；Time parameter.
     * @return 返回数值结果；Returns a numeric result.
     */
    @Override
    public Integer run(String code, String input, long timeoutMs) {
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        File tmpDir = new File(System.getProperty("java.io.tmpdir"), "kob_java_bot_" + UUID.randomUUID());
        File packageDir = new File(tmpDir, BOT_PACKAGE.replace('.', File.separatorChar));
        if (!packageDir.mkdirs()) {
            System.err.println("[JavaRunner] 无法创建临时目录");
            return 0;
        }

        try {
            long startedAt = System.nanoTime();
            Files.writeString(new File(tmpDir, "input.txt").toPath(), input + "\n");

            Path botSource = new File(packageDir, "Bot" + uid + ".java").toPath();
            Path wrapperSource = new File(packageDir, "RunnerMain.java").toPath();
            Files.writeString(botSource, buildBotSource(code, uid));
            Files.writeString(wrapperSource, buildWrapperSource(uid));

            ProcessBuilder compileBuilder = new ProcessBuilder("javac", botSource.toString(), wrapperSource.toString());
            compileBuilder.directory(tmpDir);
            compileBuilder.redirectErrorStream(true);
            Process compileProcess = compileBuilder.start();
            long compileTimeoutMs = Math.max(1, Math.min(1000, timeoutMs));
            boolean compiled = compileProcess.waitFor(compileTimeoutMs, TimeUnit.MILLISECONDS);
            if (!compiled) {
                compileProcess.destroyForcibly();
                System.err.println("[JavaRunner] 编译超时");
                return 0;
            }
            if (compileProcess.exitValue() != 0) {
                String err = new String(compileProcess.getInputStream().readAllBytes()).trim();
                System.err.println("[JavaRunner] 编译失败" + (err.isBlank() ? "" : ":\n" + err));
                return 0;
            }

            ProcessBuilder runBuilder = new ProcessBuilder("java", "-cp", tmpDir.getAbsolutePath(), BOT_PACKAGE + ".RunnerMain");
            runBuilder.directory(tmpDir);
            runBuilder.redirectErrorStream(false);
            Process process = runBuilder.start();

            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            long remainingMs = Math.max(1, timeoutMs - elapsedMs);
            boolean finished = process.waitFor(remainingMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                System.err.println("[JavaRunner] 子进程超时（" + timeoutMs + "ms），已强制终止");
                return 0;
            }

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();
            readStream(process, stdout, false);
            readStream(process, stderr, true);
            return parseDirection(stdout.toString(), stderr.toString());
        } catch (IllegalArgumentException e) {
            System.err.println("[JavaRunner] " + e.getMessage());
            return 0;
        } catch (Exception e) {
            System.err.println("[JavaRunner] 运行出错: " + e.getMessage());
            return 0;
        } finally {
            deleteDir(tmpDir);
        }
    }

    /**
     * 删除或清理 deleteDir 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of deleteDir with controlled input and output handling.
     *
     * @param dir 输入参数；Input parameter.
     */
    private void deleteDir(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                deleteDir(file);
            }
        }
        dir.delete();
    }
}