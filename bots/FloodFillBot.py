"""
FloodFill Bot (Python)
======================
策略：每步枚举 4 个方向，选走后生存空间（BFS 可达格数）最大的方向。

上传到 KOB 平台时，将本文件全部内容粘贴到代码框中即可。
语言选择：Python
"""
from collections import deque

ROWS, COLS = 13, 14
DX = [-1, 0, 1, 0]
DY = [0, 1, 0, -1]


# 判断在给定步数时蛇尾是否应增长。
# Determines whether the snake tail should grow at the given step.
def check_tail_increasing(step: int) -> bool:
    if step <= 10:
        return True
    return step % 3 == 1


# 根据起点和历史步骤重建蛇身格子。
# Rebuilds snake body cells from start position and step history.
def get_cells(sx: int, sy: int, steps: str):
    cells = [[sx, sy]]
    x, y, step = sx, sy, 0
    for c in steps:
        d = int(c)
        x += DX[d]
        y += DY[d]
        cells.append([x, y])
        step += 1
        if not check_tail_increasing(step):
            cells.pop(0)
    return cells


# 运行BFS并返回从给定起点可到达的空白格数量。
# Runs BFS and returns reachable free-cell count from the given start cell.
def flood_fill(g, sx: int, sy: int) -> int:
    if g[sx][sy] != 0:
        return 0
    queue = deque([[sx, sy]])
    visited = [[False] * COLS for _ in range(ROWS)]
    visited[sx][sy] = True
    count = 1
    while queue:
        cx, cy = queue.popleft()
        for d in range(4):
            nx, ny = cx + DX[d], cy + DY[d]
            if 0 <= nx < ROWS and 0 <= ny < COLS and g[nx][ny] == 0 and not visited[nx][ny]:
                visited[nx][ny] = True
                count += 1
                queue.append([nx, ny])
    return count


# ── 读取输入 ──────────────────────────────────────────────────────────────────
with open("input.txt") as f:
    data = f.read().strip()

parts = data.split("#")
map_str  = parts[0]
me_sx,  me_sy  = int(parts[1]), int(parts[2])
me_steps       = parts[3][1:-1]   # 去掉首尾括号
you_sx, you_sy = int(parts[4]), int(parts[5])
you_steps      = parts[6][1:-1]

# ── 建图 ──────────────────────────────────────────────────────────────────────
g = [[0] * COLS for _ in range(ROWS)]
for i in range(ROWS):
    for j in range(COLS):
        if map_str[i * COLS + j] == "1":
            g[i][j] = 1

me_cells  = get_cells(me_sx,  me_sy,  me_steps)
you_cells = get_cells(you_sx, you_sy, you_steps)
for c in me_cells:  g[c[0]][c[1]] = 1
for c in you_cells: g[c[0]][c[1]] = 1

# ── 决策 ──────────────────────────────────────────────────────────────────────
my_head = me_cells[-1]
best_dir, best_space = 0, -1

for d in range(4):
    nx, ny = my_head[0] + DX[d], my_head[1] + DY[d]
    if nx < 0 or nx >= ROWS or ny < 0 or ny >= COLS:
        continue
    if g[nx][ny] != 0:
        continue
    space = flood_fill(g, nx, ny)
    if space > best_space:
        best_space = space
        best_dir = d

print(best_dir)
