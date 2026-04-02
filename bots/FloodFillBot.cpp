/*
 * FloodFill Bot (C++)
 * ====================
 * 策略：每步枚举 4 个方向，选走后生存空间（BFS 可达格数）最大的方向。
 *
 * 上传到 KOB 平台时，将本文件全部内容粘贴到代码框中即可。
 * 语言选择：C++
 * 编译：g++ -O2 -std=c++17 -o bot FloodFillBot.cpp
 */

#include <bits/stdc++.h>
using namespace std;

const int ROWS = 13, COLS = 14;
const int DX[] = {-1, 0, 1, 0};
const int DY[] = {0,  1, 0, -1};

bool checkTailIncreasing(int step) {
    if (step <= 10) return true;
    return step % 3 == 1;
}

/** 从起点 + 历史步骤重建蛇身，返回格子列表（front = 蛇尾，back = 蛇头） */
vector<pair<int,int>> getCells(int sx, int sy, const string& steps) {
    deque<pair<int,int>> cells;
    cells.push_back({sx, sy});
    int x = sx, y = sy, step = 0;
    for (char c : steps) {
        int d = c - '0';
        x += DX[d]; y += DY[d];
        cells.push_back({x, y});
        step++;
        if (!checkTailIncreasing(step)) cells.pop_front();
    }
    return {cells.begin(), cells.end()};
}

/** BFS 计算从 (sx, sy) 出发能到达的空格数（含起点） */
int floodFill(int g[ROWS][COLS], int sx, int sy) {
    if (g[sx][sy] != 0) return 0;
    bool visited[ROWS][COLS] = {};
    queue<pair<int,int>> q;
    q.push({sx, sy});
    visited[sx][sy] = true;
    int count = 1;
    while (!q.empty()) {
        auto [cx, cy] = q.front(); q.pop();
        for (int d = 0; d < 4; d++) {
            int nx = cx + DX[d], ny = cy + DY[d];
            if (nx >= 0 && nx < ROWS && ny >= 0 && ny < COLS
                    && g[nx][ny] == 0 && !visited[nx][ny]) {
                visited[nx][ny] = true;
                count++;
                q.push({nx, ny});
            }
        }
    }
    return count;
}

int main() {
    // ── 读取输入 ──────────────────────────────────────────────────────────────
    ifstream fin("input.txt");
    string data; fin >> data; fin.close();

    // 按 '#' 分割
    vector<string> parts;
    {
        stringstream ss(data);
        string token;
        while (getline(ss, token, '#')) parts.push_back(token);
    }

    string mapStr  = parts[0];
    int meSx  = stoi(parts[1]), meSy  = stoi(parts[2]);
    string meSteps  = parts[3].substr(1, parts[3].size() - 2);   // 去掉括号
    int youSx = stoi(parts[4]), youSy = stoi(parts[5]);
    string youSteps = parts[6].substr(1, parts[6].size() - 2);

    // ── 建图 ──────────────────────────────────────────────────────────────────
    int g[ROWS][COLS] = {};
    for (int i = 0; i < ROWS; i++)
        for (int j = 0; j < COLS; j++)
            if (mapStr[i * COLS + j] == '1') g[i][j] = 1;

    auto meCells  = getCells(meSx,  meSy,  meSteps);
    auto youCells = getCells(youSx, youSy, youSteps);
    for (auto& c : meCells)  g[c.first][c.second] = 1;
    for (auto& c : youCells) g[c.first][c.second] = 1;

    // ── 决策 ──────────────────────────────────────────────────────────────────
    auto myHead = meCells.back();
    int bestDir = 0, bestSpace = -1;

    for (int d = 0; d < 4; d++) {
        int nx = myHead.first  + DX[d];
        int ny = myHead.second + DY[d];
        if (nx < 0 || nx >= ROWS || ny < 0 || ny >= COLS) continue;
        if (g[nx][ny] != 0) continue;
        int space = floodFill(g, nx, ny);
        if (space > bestSpace) {
            bestSpace = space;
            bestDir   = d;
        }
    }

    cout << bestDir << endl;
    return 0;
}

