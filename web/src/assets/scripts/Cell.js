/**
 * Represents a single grid cell with pixel-center coordinates derived from row and column.
 * 表示一个网格格子，包含由行列计算得出的像素中心坐标。
 */
// 游戏脚本模块。
export class Cell {
    /**
     * Creates a cell at the given row and column, computing center pixel coordinates.
     * 在给定行列创建一个格子，并计算像素中心坐标。
     */
    constructor(r, c) {
        this.r = r;
        this.c = c;
        this.x = c + 0.5;
        this.y = r + 0.5;
    }
}