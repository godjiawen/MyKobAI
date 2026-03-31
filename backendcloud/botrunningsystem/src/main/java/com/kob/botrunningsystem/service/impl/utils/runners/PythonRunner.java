package com.kob.botrunningsystem.service.impl.utils.runners;

import java.io.File;

/**
 * Python Runner
 *
 * Bot 代码模板：
 *   with open('input.txt') as f:
 *       data = f.read().strip()
 *   # ... 解析 data，计算方向 ...
 *   print(direction)   # 输出 0/1/2/3
 *
 * 依赖：服务器已安装 python3（Windows 上可能是 python）
 * 如需修改命令，更改 PYTHON_CMD 常量。
 */
public class PythonRunner extends SubprocessRunner {

    private static final String PYTHON_CMD =
            System.getProperty("os.name", "").toLowerCase().contains("win") ? "python" : "python3";

    @Override
    protected String codeFileExtension() {
        return ".py";
    }

    @Override
    protected ProcessBuilder buildProcess(File tmpDir) {
        return new ProcessBuilder(PYTHON_CMD, "bot.py");
    }
}

