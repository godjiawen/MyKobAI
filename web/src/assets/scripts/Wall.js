/**
 * Wall game object representing a single impassable cell on the game map.
 * 表示游戏地图上单个不可通行格子的墙体游戏对象。
 */
// 游戏脚本模块。
import { AcGameObject } from "./AcGameObject";

export class Wall extends AcGameObject {
    /**
     * Initializes the wall at the given row/column on the game map.
     * 在游戏地图的指定行列初始化墙体。
     */
    constructor(r, c, gamemap) {
        super();

        this.r = r;
        this.c = c;
        this.gamemap = gamemap;
        this.color = "#B37226";
    }

    /**
     * Per-frame update: renders the wall tile.
     * 每帧更新：渲染墙体格子。
     */
    update() {
        this.render();
    }

    /**
     * Draws the wall as a filled rectangle on the canvas.
     * 在画布上将墙体绘制为填充矩形。
     */
    render() {
        const L = this.gamemap.L;
        const ctx = this.gamemap.ctx;

        ctx.fillStyle = this.color;
        ctx.fillRect(this.c * L, this.r * L, L, L);
    }
}
