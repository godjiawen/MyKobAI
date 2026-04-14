// 游戏脚本模块。
import { AcGameObject } from "./AcGameObject";
import { Snake } from "./Snake";
import { Wall } from "./Wall";

export class GameMap extends AcGameObject {
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

    start() {
        this.create_walls();
        
        this.add_listening_events();
    }



    update_size() {
        this.L = parseInt(Math.min(this.parent.clientWidth / this.cols, this.parent.clientHeight / this.rows));
        this.ctx.canvas.width = this.L * this.cols;
        this.ctx.canvas.height = this.L * this.rows;
    }

    check_ready() {  // 判断两条蛇是否都准备好下一回合了
        for (const snake of this.snakes) {
            if (snake.status !== "idle") return false;
            if (snake.direction === -1)  return false;
        }
        return true;
    }

    next_step() {  // 让两条蛇进入下一回合
        for (const snake of this.snakes) {
            snake.next_step();
        }
    }

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

    update() {
        this.update_size();
        if(this.check_ready()) {
            this.next_step();
        }
        this.render();
    }

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
