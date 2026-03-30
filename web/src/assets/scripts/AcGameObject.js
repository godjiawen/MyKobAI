const AC_GAME_OBJECTS = [];

export class AcGameObject {
    constructor() {
        AC_GAME_OBJECTS.push(this);
        this.timedelta = 0;
        this.has_called_start = false;
    }

    start() {  // 只执行一次
    }

    update() {  // 每一帧执行一次，除了第一帧之外

    }

    on_destroy() {  // 删除之前执行

    }

    destroy() {
        this.on_destroy();

        for (let i in AC_GAME_OBJECTS) {
            const obj = AC_GAME_OBJECTS[i];
            if (obj === this) {
                AC_GAME_OBJECTS.splice(i, 1);
                break;
            }
        }
    }
}

let last_timestamp;  // 上一次执行的时刻
let animFrameId = null;

const step = timestamp => {
    for (let obj of AC_GAME_OBJECTS) {
        if (!obj.has_called_start) {
            obj.has_called_start = true;
            obj.start();
        } else {
            obj.timedelta = timestamp - last_timestamp;
            obj.update();
        }
    }

    last_timestamp = timestamp;
    animFrameId = requestAnimationFrame(step);
}

// 启动全局游戏循环
export const startGameLoop = () => {
    if (animFrameId !== null) return; // 已在运行，不重复启动
    animFrameId = requestAnimationFrame(step);
};

// 停止全局游戏循环（离开游戏相关页面时调用）
export const stopGameLoop = () => {
    if (animFrameId !== null) {
        cancelAnimationFrame(animFrameId);
        animFrameId = null;
    }
};
