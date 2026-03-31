package com.kob.botrunningsystem.service.impl.utils.runners;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * C++ Runner —— 先用 g++ 编译，再运行生成的可执行文件。
 *
 * Bot 代码模板：
 *   #include <bits/stdc++.h>
 *   using namespace std;
 *   int main() {
 *       ifstream fin("input.txt");
 *       string s; fin >> s; fin.close();
 *       // ... 解析 s，计算方向 direction ...
 *       cout << direction << endl;
 *   }
 *
 * 依赖：服务器已安装 g++（Windows 上需要 MinGW/MSYS2/WSL）
 */
public class CppRunner extends SubprocessRunner {

    private static final boolean IS_WINDOWS =
            System.getProperty("os.name", "").toLowerCase().contains("win");

    @Override
    protected String codeFileExtension() {
        return ".cpp";
    }

    @Override
    protected ProcessBuilder buildProcess(File tmpDir) throws Exception {
        // Step 1: 编译（最多 10 秒）
        String outputBinary = IS_WINDOWS ? "bot.exe" : "bot";
        ProcessBuilder compileBuilder = new ProcessBuilder("g++", "-O2", "-o", outputBinary, "bot.cpp");
        compileBuilder.directory(tmpDir);
        compileBuilder.redirectErrorStream(true);

        Process compileProcess = compileBuilder.start();
        boolean compiled = compileProcess.waitFor(10, TimeUnit.SECONDS);

        if (!compiled) {
            compileProcess.destroyForcibly();
            throw new RuntimeException("C++ 编译超时");
        }
        if (compileProcess.exitValue() != 0) {
            String err = new String(compileProcess.getInputStream().readAllBytes());
            throw new RuntimeException("C++ 编译失败:\n" + err);
        }

        // Step 2: 返回运行 Builder（SubprocessRunner 会 start 它）
        return new ProcessBuilder(IS_WINDOWS ? "bot.exe" : "./bot");
    }
}

