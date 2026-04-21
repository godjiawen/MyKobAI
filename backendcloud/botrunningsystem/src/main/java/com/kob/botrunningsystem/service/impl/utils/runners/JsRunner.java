package com.kob.botrunningsystem.service.impl.utils.runners;

import java.io.File;

/**
 * JavaScript 运行器，使用 Node.js 子进程执行。
 *
 * 机器人代码模板：
 *   const fs = require('fs');
 *   const input = fs.readFileSync('input.txt', 'utf8').trim();
 *   // ... 解析 input，计算方向 direction（0-3）...
 *   console.log(direction);
 *
 * 依赖：服务器已安装 Node.js（命令 node 可用）
 */
public class JsRunner extends SubprocessRunner {

    @Override
    /**
     * Handles codeFileExtension.
     * ??codeFileExtension?
     */
    protected String codeFileExtension() {
        return ".js";
    }

    @Override
    /**
     * Handles buildProcess.
     * ??buildProcess?
     */
    protected ProcessBuilder buildProcess(File tmpDir) {
        return new ProcessBuilder("node", "bot.js");
    }
}

