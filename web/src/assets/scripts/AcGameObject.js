// Global registry of all active game objects used by the animation loop.
// 全局游戏对象注册表，由动画循环使用。
const AC_GAME_OBJECTS = [];

/**
 * Base class for all game objects; manages lifecycle hooks (start, update, destroy).
 * 所有游戏对象的基类；管理生命周期钩子（start、update、destroy）。
 */
export class AcGameObject {
    /**
     * Registers this object in the global game object list and initializes state.
     * 将此对象注册到全局游戏对象列表并初始化状态。
     */
    constructor() {
        AC_GAME_OBJECTS.push(this);
        this.timedelta = 0;
        this.has_called_start = false;
    }

    /**
     * Called once before the first update; override to add initialization logic.
     * 在首次更新之前调用一次；覆写以添加初始化逻辑。
     */
    start() {  // 只执行一次
    }

    /**
     * Called once per frame after the first frame; override to add per-frame logic.
     * 从第二帧开始每帧调用一次；覆写以添加逐帧逻辑。
     */
    update() {  // 每一帧执行一次，除了第一帧之外

    }

    /**
     * Called just before this object is removed; override for cleanup logic.
     * 对象被移除前调用；覆写以添加清理逻辑。
     */
    on_destroy() {  // 删除之前执行

    }

    /**
     * Invokes on_destroy and removes this object from the global game object list.
     * 调用on_destroy并将此对象从全局游戏对象列表中移除。
     */
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

/**
 * The main animation step: calls start or update on all registered game objects.
 * 主动画步骤：对所有注册的游戏对象调用start或update。
 */
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

/**
 * Starts the global game loop if it is not already running.
 * 如果全局游戏循环未在运行，则启动它。
 */
// 启动全局游戏循环
export const startGameLoop = () => {
    if (animFrameId !== null) return; // 已在运行，不重复启动
    animFrameId = requestAnimationFrame(step);
};

/**
 * Stops the global game loop; call when leaving game-related pages.
 * 停止全局游戏循环；离开游戏相关页面时调用。
 */
// 停止全局游戏循环（离开游戏相关页面时调用）
export const stopGameLoop = () => {
    if (animFrameId !== null) {
        cancelAnimationFrame(animFrameId);
        animFrameId = null;
    }
};
