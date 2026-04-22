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

    /**
     * 处理 codeFileExtension 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of codeFileExtension with controlled input and output handling.
     *
     * @return 返回字符串结果；Returns a string result.
     */
    @Override
    protected String codeFileExtension() {
        return ".js";
    }

    /**
     * 构建或转换 buildProcess 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of buildProcess with controlled input and output handling.
     *
     * @param tmpDir 输入参数；Input parameter.
     * @return 返回 ProcessBuilder 类型结果；Returns a result of type ProcessBuilder.
     */
    @Override
    protected ProcessBuilder buildProcess(File tmpDir) {
        return new ProcessBuilder("node", "bot.js");
    }
}
