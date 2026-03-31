package com.kob.botrunningsystem.service.impl.utils.runners;

import java.io.File;

/**
 * JavaScript Runner —— 使用 Node.js 子进程执行。
 *
 * Bot 代码模板：
 *   const fs = require('fs');
 *   const input = fs.readFileSync('input.txt', 'utf8').trim();
 *   // ... 解析 input，计算方向 direction（0-3）...
 *   console.log(direction);
 *
 * 依赖：服务器已安装 Node.js（命令 node 可用）
 */
public class JsRunner extends SubprocessRunner {

    @Override
    protected String codeFileExtension() {
        return ".js";
    }

    @Override
    protected ProcessBuilder buildProcess(File tmpDir) {
        return new ProcessBuilder("node", "bot.js");
    }
}

