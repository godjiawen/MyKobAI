/**
 * Game map object that renders the grid, walls, handles input and orchestrates snake turns.
 * 游戏地图对象，负责渲染网格与墙体、处理输入并协调蛇的回合。
 */
// 游戏脚本模块。
import { AcGameObject } from "./AcGameObject";
import { Snake } from "./Snake";
import { Wall } from "./Wall";
import { Cell } from "./Cell";

export class GameMap extends AcGameObject {
    /**
     * Initializes the map with canvas context, parent element, and Pinia stores.
     * 使用画布上下文、父元素和Pinia存储初始化地图。
     */
    constructor(ctx, parent, { pkStore, recordStore }) {
        super();

        this.ctx = ctx;
        this.parent = parent;
        this.pkStore = pkStore;
        this.recordStore = recordStore;
        this.L = 0;

        this.rows = 13;
        this.cols = 14;
        
        this.inner_walls_count = 20;
        this.walls = [];
        this.keydownHandler = null;
        this.replayIntervalId = null;

        this.snakes = [
            new Snake({id: 0, color: "#4876EC", r: this.rows - 2, c: 1}, this),
            new Snake({id: 1, color: "#F94848", r: 1, c: this.cols - 2}, this),
        ];
    }

    /**
     * Reads the map data from the store and creates Wall objects for each blocked cell.
     * 从存储中读取地图数据并为每个阻塞格子创建Wall对象。
     */
    create_walls() {
        const g = this.pkStore.gamemap;

        for (let r = 0; r < this.rows; r ++ ) {
            for (let c = 0; c < this.cols; c ++ ) {
                if (g[r][c]) {
                    this.walls.push(new Wall(r, c, this));
                }
            }
        }
    }

    /**
     * In record mode: replays steps via interval; in live mode: binds keyboard input to WebSocket moves.
     * 录像模式：通过定时器回放步骤；实时模式：将键盘输入绑定为WebSocket移动消息。
     */
    add_listening_events() {
        if (this.recordStore.is_record) {
            let k = 0;
            const a_steps = this.recordStore.a_steps;
            const b_steps = this.recordStore.b_steps;
            const loser = this.recordStore.record_loser;
            const [snake0, snake1] = this.snakes;
            this.replayIntervalId = setInterval(() => {
                if (k >= a_steps.length - 1) {
                    if (loser === "all" || loser === "A") {
                        snake0.status = "die";
                    }
                    if (loser === "all" || loser === "B") {
                        snake1.status = "die";
                    }
                    clearInterval(this.replayIntervalId);
                    this.replayIntervalId = null;
                } else {
                    snake0.set_direction(parseInt(a_steps[k]));
                    snake1.set_direction(parseInt(b_steps[k]));
                }
                k ++ ;
            }, 300) 
        } else {
            this.keydownHandler = e => {
                // 当焦点在输入框（如聊天框）时，不处理游戏按键
                const tag = document.activeElement?.tagName?.toUpperCase();
                if (tag === 'INPUT' || tag === 'TEXTAREA') return;
                // 暂停中不处理移动
                if (this.pkStore.isPaused) return;

                let d = -1;
                if (e.key === 'w') d = 0;
                else if (e.key === 'd') d = 1;
                else if (e.key === 's') d = 2;
                else if (e.key === 'a') d = 3;
    
                if (d >= 0) {
                    e.preventDefault(); // 阻止浏览器默认行为（例如页面滚动等）
                    const socket = this.pkStore.socket;
                    if (socket && socket.readyState === WebSocket.OPEN) {
                        socket.send(JSON.stringify({
                            event: "move",
                            direction: d,
                        }));
                    }
                }
            };
            // 键盘事件绑定在页面对象上，不依赖画布焦点，点击聊天框后依然有效
            document.addEventListener("keydown", this.keydownHandler);
        }
       
    }

    /**
     * 根据 steps 字符串从起始位置重建蛇身（用于断线重连恢复）。
     */
    restoreSnakeFromSteps(snake, sx, sy, steps) {
        snake.cells = [new Cell(sx, sy)];
        snake.step = 0;

        for (const ch of steps) {
            const d = Number.parseInt(ch, 10);
            snake.direction = d;
            snake.next_step();
            // next_step 会把 cells[0] 拷贝一份到 cells[1]，头部指向 next_cell
            snake.cells[0] = snake.next_cell;
            snake.next_cell = null;
            snake.status = "idle";
            if (!snake.check_tail_increasing()) {
                snake.cells.pop();
            }
        }

        snake.direction = -1;
        snake.status = "idle";
    }

    /**
     * 当 game-resync 快照在 GameMap 已创建之后到达时，重新恢复蛇身位置。
     * 由 realtimeStore 的 game-resync 处理器调用。
     */
    restoreFromSnapshot() {
        const aSteps = this.pkStore.aSteps;
        const bSteps = this.pkStore.bSteps;
        if (!aSteps && !bSteps) return;

        const [snake0, snake1] = this.snakes;
        this.restoreSnakeFromSteps(snake0, this.pkStore.a_sx, this.pkStore.a_sy, aSteps || "");
        this.restoreSnakeFromSteps(snake1, this.pkStore.b_sx, this.pkStore.b_sy, bSteps || "");
    }

    /**
     * Initializes walls and starts input event listening.
     * 初始化墙体并开始监听输入事件。
     */
    start() {
        this.create_walls();

        // 若有 steps 快照，则恢复蛇身，否则蛇处于初始位置
        const aSteps = this.pkStore.aSteps;
        const bSteps = this.pkStore.bSteps;
        if (!this.recordStore.is_record && aSteps && bSteps) {
            const [snake0, snake1] = this.snakes;
            this.restoreSnakeFromSteps(snake0, this.pkStore.a_sx, this.pkStore.a_sy, aSteps);
            this.restoreSnakeFromSteps(snake1, this.pkStore.b_sx, this.pkStore.b_sy, bSteps);
        }

        this.add_listening_events();
    }

    /**
     * Recalculates the cell size L to fit the canvas inside the parent element.
     * 重新计算格子尺寸L以使画布适应父元素。
     */
    update_size() {
        this.L = parseInt(Math.min(this.parent.clientWidth / this.cols, this.parent.clientHeight / this.rows));
        this.ctx.canvas.width = this.L * this.cols;
        this.ctx.canvas.height = this.L * this.rows;
    }

    /**
     * Returns true if both snakes are idle and have a queued direction for the next round.
     * 若两条蛇均处于空闲状态且已有下一回合方向则返回true。
     */
    check_ready() {  // 判断两条蛇是否都准备好下一回合了
        for (const snake of this.snakes) {
            if (snake.status !== "idle") return false;
            if (snake.direction === -1)  return false;
        }
        return true;
    }

    /**
     * Advances both snakes to the next step/round.
     * 将两条蛇推进到下一步/回合。
     */
    next_step() {  // 让两条蛇进入下一回合
        for (const snake of this.snakes) {
            snake.next_step();
        }
    }

    /**
     * Checks whether a target cell is free of walls and snake bodies.
     * 检测目标格子是否不含墙体和蛇身。
     */
    check_valid(cell) { // 检测目标位置是否合法： 没有撞墙和互撞
        for (const wall of this.walls) {
            if(wall.r === cell.r && wall.c === cell.c)
                return false;
        }

        for (const snake of this.snakes) {
            let k = snake.cells.length;
            if (!snake.check_tail_increasing()) { // 当蛇尾会前进的时候，蛇尾不要判断
                k --;
            }
            for (let i = 0;  i < k; i ++ ) {
                if (snake.cells[i].r === cell.r && snake.cells[i].c === cell.c)
                    return false;  
            }
        }

        return true;
    }

    /**
     * Per-frame update: resizes the canvas, advances snakes if ready, then renders.
     * 每帧更新：调整画布大小，如就绪则推进蛇，然后渲染。
     */
    update() {
        this.update_size();
        if(this.check_ready()) {
            this.next_step();
        }
        this.render();
    }

    /**
     * Draws the alternating-color checkerboard background on the canvas.
     * 在画布上绘制交替颜色的棋盘格背景。
     */
    render() {
        const color_even = "#AAD751", color_odd = "#A2D149";
        for (let r = 0; r < this.rows; r ++ ) {
            for (let c = 0; c < this.cols; c ++) {
                if((r + c) % 2 == 0) {
                    this.ctx.fillStyle = color_even;
                } else {
                    this.ctx.fillStyle = color_odd;
                }
                this.ctx.fillRect(c * this.L, r * this.L, this.L, this.L);
            }
        }
    }

    /**
     * Cleans up intervals, event listeners, snakes and walls when the map is destroyed.
     * 地图销毁时清理定时器、事件监听器、蛇和墙体。
     */
    on_destroy() {
        if (this.replayIntervalId) {
            clearInterval(this.replayIntervalId);
            this.replayIntervalId = null;
        }

        if (this.keydownHandler) {
            document.removeEventListener("keydown", this.keydownHandler);
            this.keydownHandler = null;
        }

        this.snakes.forEach((snake) => snake.destroy());
        this.walls.forEach((wall) => wall.destroy());
        this.snakes = [];
        this.walls = [];
    }
}
