/**
 * FloodFill Bot (JavaScript / Node.js)
 * =====================================
 * 策略：每步枚举 4 个方向，选走后生存空间（BFS 可达格数）最大的方向。
 *
 * 上传到 KOB 平台时，将本文件全部内容粘贴到代码框中即可。
 * 语言选择：JavaScript
 */

"use strict";
const fs = require("fs");

const ROWS = 13, COLS = 14;
const DX = [-1, 0, 1, 0];
const DY = [0,  1, 0, -1];

/**
 * Determines whether the snake tail should grow at the given step.
 * 判断在给定步数时蛇尾是否应增长。
 */
function checkTailIncreasing(step) {
    if (step <= 10) return true;
    return step % 3 === 1;
}

/**
 * Rebuilds snake body cells from start position and step history.
 * 根据起点和历史步骤重建蛇身格子。
 */
function getCells(sx, sy, steps) {
    const cells = [[sx, sy]];
    let x = sx, y = sy, step = 0;
    for (const c of steps) {
        const d = parseInt(c);
        x += DX[d]; y += DY[d];
        cells.push([x, y]);
        step++;
        if (!checkTailIncreasing(step)) cells.shift();
    }
    return cells;
}

/**
 * Runs BFS and returns reachable free-cell count from the given start cell.
 * 运行BFS并返回从给定起点可到达的空白格数量。
 */
function floodFill(g, sx, sy) {
    if (g[sx][sy] !== 0) return 0;
    const visited = Array.from({ length: ROWS }, () => new Array(COLS).fill(false));
    const queue = [[sx, sy]];
    visited[sx][sy] = true;
    let count = 1;
    let head = 0;
    while (head < queue.length) {
        const [cx, cy] = queue[head++];
        for (let d = 0; d < 4; d++) {
            const nx = cx + DX[d], ny = cy + DY[d];
            if (nx >= 0 && nx < ROWS && ny >= 0 && ny < COLS
                    && g[nx][ny] === 0 && !visited[nx][ny]) {
                visited[nx][ny] = true;
                count++;
                queue.push([nx, ny]);
            }
        }
    }
    return count;
}

// ── 读取输入 ──────────────────────────────────────────────────────────────────
const data   = fs.readFileSync("input.txt", "utf8").trim();
const parts  = data.split("#");
const mapStr = parts[0];
const meSx   = parseInt(parts[1]), meSy   = parseInt(parts[2]);
const meSteps  = parts[3].slice(1, -1);   // 去掉括号
const youSx  = parseInt(parts[4]), youSy  = parseInt(parts[5]);
const youSteps = parts[6].slice(1, -1);

// ── 建图 ──────────────────────────────────────────────────────────────────────
const g = Array.from({ length: ROWS }, () => new Array(COLS).fill(0));
for (let i = 0; i < ROWS; i++)
    for (let j = 0; j < COLS; j++)
        if (mapStr[i * COLS + j] === "1") g[i][j] = 1;

const meCells  = getCells(meSx,  meSy,  meSteps);
const youCells = getCells(youSx, youSy, youSteps);
for (const c of meCells)  g[c[0]][c[1]] = 1;
for (const c of youCells) g[c[0]][c[1]] = 1;

// ── 决策 ──────────────────────────────────────────────────────────────────────
const myHead = meCells[meCells.length - 1];
let bestDir = 0, bestSpace = -1;

for (let d = 0; d < 4; d++) {
    const nx = myHead[0] + DX[d], ny = myHead[1] + DY[d];
    if (nx < 0 || nx >= ROWS || ny < 0 || ny >= COLS) continue;
    if (g[nx][ny] !== 0) continue;
    const space = floodFill(g, nx, ny);
    if (space > bestSpace) {
        bestSpace = space;
        bestDir   = d;
    }
}

console.log(bestDir);
