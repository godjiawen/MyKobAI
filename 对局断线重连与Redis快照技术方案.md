# 对局断线重连与 Redis 快照技术方案

## 目标

将对局过程中的快照数据全部交给 Redis 管理，但前端不直接访问 Redis。前端只通过后端鉴权后的 WebSocket 或 HTTP 接口读取当前对局快照。

最终目标：

- 玩家离开 PK 页面时，对局自动挂起，不等同于主动暂停。
- 对手能收到“对方暂时离开对局”的提示。
- 玩家返回 PK 页面或 WebSocket 重连后，游戏恢复到离开前的状态。
- 对局过程快照由 Redis 管理，MySQL 只保存最终战绩、回放记录、排行榜等长期数据。
- 前端只负责渲染和输入，不直接接触 Redis key、Redis 地址或 Redis 凭据。

## 当前问题

当前实现里存在两个关键问题：

1. 前端离开 `PK` 页面时，`GameMap.vue` 会销毁本地 `GameMap/Snake` 运行时对象。再次进入 PK 页面时，前端只能根据 `pkStore` 中的开局信息重建棋盘，因此蛇会回到开局状态。
2. 后端 `WebSocketServer.onClose()` 当前会清理连接对象上的 `game` 引用。重连后，新连接无法自动找回原来的进行中对局。

因此，仅依赖前端 Pinia 状态或 WebSocket 连接对象不能可靠支持断线重连。

## 总体架构

推荐架构如下：

```text
Vue 前端
  |
  | WebSocket: move / pause / resume / game-leave / game-enter / sync-game
  | HTTP: GET /api/pk/current-game/
  v
backend 主服务
  |
  | Game 线程：推进、判定、广播
  | GameSnapshotService：读写 Redis 快照
  v
Redis
  |
  | 对局过程快照、用户当前对局索引、暂离状态
  v
MySQL
  |
  | 最终 record / rating / replay
```

职责划分：

- `Game`：实时游戏规则、移动判定、胜负判定。
- `Redis`：开局信息、地图、双方 steps、暂停状态、暂离状态、用户当前对局索引。
- `WebSocketServer`：鉴权、收发事件、连接管理、对局 presence 管理。
- `前端`：消费后端下发的 `game-resync` 快照，并恢复画面。
- `MySQL`：只保存对局结束后的长期记录。

## Redis 数据设计

### Key 设计

```text
kob:pk:user:{userId}:game -> gameId
kob:pk:game:{gameId}:snapshot -> JSON
kob:pk:game:{gameId}:lock -> optional lock
```

第一版可以不单独设计 presence hash，直接把 presence 放进 snapshot，降低实现复杂度。

### Snapshot 示例

```json
{
  "game_id": "game_1_2_1710000000000",
  "status": "playing",
  "a_id": 1,
  "b_id": 2,
  "a_bot_id": -1,
  "b_bot_id": 37,
  "a_sx": 11,
  "a_sy": 1,
  "b_sx": 1,
  "b_sy": 12,
  "map": [[0, 1, 0]],
  "a_steps": "012301",
  "b_steps": "103210",
  "next_step_a": null,
  "next_step_b": null,
  "paused": false,
  "paused_by": "",
  "suspended": true,
  "suspended_by": 1,
  "suspended_reason": "route-leave",
  "away_user_ids": [1],
  "room_id": "1_2",
  "loser": "",
  "updated_at": 1710000000000,
  "expire_at": 1710000300000
}
```

### TTL 策略

建议：

- 正常对局中：每次写快照刷新 TTL，建议 `10 minutes`。
- 暂离状态：TTL 可延长到 `15 minutes`。
- 对局结束：立即删除 Redis key。

写入时需要同时更新：

```text
kob:pk:user:{aId}:game
kob:pk:user:{bId}:game
kob:pk:game:{gameId}:snapshot
```

建议使用 Redis transaction：

```text
MULTI
SET kob:pk:game:{gameId}:snapshot <json> EX 600
SET kob:pk:user:{aId}:game {gameId} EX 600
SET kob:pk:user:{bId}:game {gameId} EX 600
EXEC
```

如果后续 presence 更新逻辑变复杂，可以改用 Lua 保证原子性。

## 后端新增服务

新增 `GameSnapshotService`。

接口建议：

```java
public interface GameSnapshotService {
    void saveInitial(Game game);
    void saveStep(Game game);
    void savePause(Game game);
    void savePresence(Game game);
    void saveResult(Game game);
    GameSnapshot getByUserId(Integer userId);
    GameSnapshot getByGameId(String gameId);
    void bindUserGame(Integer userId, String gameId);
    void deleteGame(Game game);
}
```

实现建议：

- 使用 `StringRedisTemplate`。
- JSON 序列化优先沿用项目已有 FastJSON。
- 所有读取都经过后端鉴权。
- `getByUserId(userId)` 内部先查 `kob:pk:user:{userId}:game`，再查 `kob:pk:game:{gameId}:snapshot`。

鉴权规则：

- 当前登录用户必须是 `snapshot.a_id` 或 `snapshot.b_id`。
- 否则返回无当前对局，或拒绝访问。

## 后端 Game 改造

`Game` 需要新增字段：

```java
private final String gameId;
private final String roomId;
private volatile boolean suspended = false;
private volatile Integer suspendedBy = null;
private volatile String suspendedReason = "";
private final Set<Integer> awayPlayers = ConcurrentHashMap.newKeySet();
```

状态变化写 Redis：

- `startGame()` 创建并 `createMap()` 后调用 `gameSnapshotService.saveInitial(game)`。
- 每次双方方向都确认并推进一回合后调用 `gameSnapshotService.saveStep(this)`。
- `pause()` 和 `resume()` 后调用 `gameSnapshotService.savePause(game)`。
- `game-leave` 或 `onClose()` 后调用 `gameSnapshotService.savePresence(game)`。
- `game-enter` 或 `onOpen()` 后调用 `gameSnapshotService.savePresence(game)`。
- `sendResult()` 中先写 MySQL，再调用 `gameSnapshotService.deleteGame(this)`。

`nextStep()` 要区分主动暂停和暂离挂起：

```java
if (paused || suspended) {
    continue;
}
```

关键点：暂离挂起期间不累计走子超时，否则玩家切页面超过 `STEP_TIMEOUT_MS` 会被误判负。

## WebSocket 协议

### 同步当前对局

前端发送：

```json
{ "event": "sync-game" }
```

后端行为：

- 根据当前 WebSocket 鉴权用户查 Redis。
- 如果有进行中对局，返回 `game-resync`。
- 如果没有，返回 `no-active-game`。

后端响应：

```json
{
  "event": "game-resync",
  "snapshot": {}
}
```

```json
{
  "event": "no-active-game"
}
```

### 离开对局页面

前端发送：

```json
{
  "event": "game-leave",
  "reason": "route-leave"
}
```

后端行为：

- 查询当前用户的 active game。
- 标记 `away_user_ids`。
- 设置 `suspended = true`。
- 写 Redis。
- 广播给双方。

后端广播：

```json
{
  "event": "player-away",
  "user_id": 1,
  "reason": "route-leave"
}
```

```json
{
  "event": "game-suspended",
  "suspended_by": 1,
  "reason": "route-leave"
}
```

### 返回对局页面

前端发送：

```json
{ "event": "game-enter" }
```

后端行为：

- 标记当前用户回来。
- 写 Redis。
- 给当前用户发送 `game-resync`。
- 给对手发送 `player-back`。
- 如果双方都在对局页，并且不是主动暂停，则解除 `suspended`。

后端广播：

```json
{
  "event": "player-back",
  "user_id": 1
}
```

```json
{
  "event": "game-resumed-from-away"
}
```

### 断线重连

断线场景使用同一套状态：

- `onClose()` 等价于 `game-leave`，`reason = "disconnect"`。
- `onOpen()` 后自动执行一次 `sync-game`。
- 如果用户当前就在 PK 页面，前端再发送 `game-enter`。

## HTTP 兜底接口

新增接口：

```http
GET /api/pk/current-game/
Authorization: Bearer <token>
```

有当前对局：

```json
{
  "error_message": "success",
  "has_game": true,
  "snapshot": {}
}
```

无当前对局：

```json
{
  "error_message": "success",
  "has_game": false
}
```

用途：

- WebSocket 重连前后的兜底恢复。
- 用户刷新页面后恢复 UI。
- 前端初始化时判断是否需要显示“返回对局”状态。

实现位置建议：

- `controller/pk/GetCurrentGameController.java`
- `service/pk/GetCurrentGameService.java`
- 内部调用 `GameSnapshotService.getByUserId(currentUserId)`

## 前端状态改造

`pkStore` 新增状态：

```js
gameId: "",
aSteps: "",
bSteps: "",
isAwaySuspended: false,
awayUserIds: [],
suspendedByUserId: null,
suspendedReason: "",
lastSnapshotAt: 0,
```

新增 `updateGameSnapshot(snapshot)`：

```js
updateGameSnapshot(snapshot) {
  this.gameId = snapshot.game_id;
  this.gamemap = snapshot.map;
  this.a_id = snapshot.a_id;
  this.b_id = snapshot.b_id;
  this.a_sx = snapshot.a_sx;
  this.a_sy = snapshot.a_sy;
  this.b_sx = snapshot.b_sx;
  this.b_sy = snapshot.b_sy;
  this.aSteps = snapshot.a_steps || "";
  this.bSteps = snapshot.b_steps || "";
  this.roomId = snapshot.room_id || "";
  this.isPaused = Boolean(snapshot.paused);
  this.pausedByUserId = snapshot.paused_by || null;
  this.isAwaySuspended = Boolean(snapshot.suspended);
  this.awayUserIds = snapshot.away_user_ids || [];
  this.suspendedByUserId = snapshot.suspended_by || null;
  this.suspendedReason = snapshot.suspended_reason || "";
  this.status = snapshot.status === "finished" ? "matching" : "playing";
}
```

`realtimeStore.handleSocketMessage()` 新增事件处理：

```js
if (data.event === "game-resync") {
  pkStore.updateGameSnapshot(data.snapshot);
  return;
}

if (data.event === "player-away") {
  pkStore.markPlayerAway(data.user_id, data.reason);
  return;
}

if (data.event === "player-back") {
  pkStore.markPlayerBack(data.user_id);
  return;
}

if (data.event === "game-suspended") {
  pkStore.updateAwaySuspended(true, data.suspended_by, data.reason);
  return;
}

if (data.event === "game-resumed-from-away") {
  pkStore.updateAwaySuspended(false, null, "");
  return;
}
```

WebSocket `onopen()` 后发送：

```js
socket.send(JSON.stringify({ event: "sync-game" }));
```

`PkIndexView.vue`：

- `onMounted()` 时发送 `game-enter` 和 `sync-game`。
- `onBeforeUnmount()` 时，如果对局未结束，发送 `game-leave`。

示例：

```js
onMounted(() => {
  send({ event: "game-enter" });
  send({ event: "sync-game" });
});

onBeforeUnmount(() => {
  if (pkStore.status === "playing" && pkStore.loser === "none") {
    send({ event: "game-leave", reason: "route-leave" });
  }
});
```

刷新或关闭页面时 `onBeforeUnmount` 不一定可靠，因此后端 `onClose()` 仍然必须兜底。

## 前端 GameMap 恢复

当前 `GameMap` 只按初始点创建蛇。要支持 Redis 快照恢复，需要根据 `a_steps/b_steps` 重建蛇身。

新增恢复方法：

```js
restoreSnakeFromSteps(snake, sx, sy, steps) {
  snake.cells = [new Cell(sx, sy)];
  snake.step = 0;

  for (const ch of steps) {
    const d = Number.parseInt(ch, 10);
    snake.direction = d;
    snake.next_step();
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
```

`GameMap.start()` 执行顺序：

1. 创建墙体。
2. 如果 `pkStore.aSteps/bSteps` 存在，则恢复蛇身。
3. 绑定键盘事件。

这样玩家返回页面时，画面会从 Redis 快照里的 steps 恢复，而不是回到开局。

## 暂离 UI

`PlayGround.vue` 保留原有主动暂停遮罩，再增加“暂离遮罩”。

显示条件：

```js
pkStore.isAwaySuspended && !pkStore.isPaused
```

文案建议：

```text
对手暂时离开对局
等待对方返回后继续
```

如果当前用户是暂离者并刚返回：

```text
正在恢复对局状态
```

规则：

- 主动暂停优先展示暂停遮罩。
- 暂离只表示系统挂起，不显示“继续”按钮。
- 只有玩家回来触发 `game-enter`，后端才解除暂离挂起。

## 边界规则

主动暂停：

- `paused = true`
- 只能由暂停发起者 `resume`
- 玩家离开又回来，不自动解除 `paused`

暂离挂起：

- `suspended = true`
- 任一玩家离开 PK 页面或断线触发
- 双方都 present 后自动解除
- 不计入走子超时

双方都离开：

- `away_user_ids = [aId, bId]`
- 游戏线程保持挂起
- TTL 到期后按策略处理

TTL 到期策略建议：

- 第一版：直接清理 Redis，对局标记异常结束，不写战绩。
- 更严格版本：离开方超时判负；双方都离开则平局。

## 实施顺序

1. 加 Redis 依赖和配置。
2. 新增 `GameSnapshotService`，完成 `save/get/delete`。
3. 改 `Game`，增加 `gameId/suspended/awayPlayers/snapshot`。
4. 改 `WebSocketServer`：
   - `startGame()` 写 Redis
   - `onOpen()` 自动查 Redis 并发送 `game-resync`
   - `onClose()` 不清空对局，改为暂离
   - 新增 `sync-game/game-enter/game-leave`
5. 新增 HTTP `/api/pk/current-game/`。
6. 改 `pkStore` 支持 snapshot。
7. 改 `realtimeStore` 支持新事件。
8. 改 `GameMap` 支持 steps 恢复蛇身。
9. 改 `PkIndexView` 进入/离开上报 presence。
10. 改 `PlayGround` 增加暂离提示。
11. 做联调测试和 TTL 清理。

## 测试清单

必须覆盖：

- A/B 正常开局，Redis 出现 user/game/snapshot key。
- A 切到好友页，B 收到对手暂离提示，游戏不继续判负。
- A 回到 PK 页，收到 `game-resync`，蛇身位置与离开前一致。
- A 刷新页面后重连，仍能恢复对局。
- A 主动暂停后离开，回来后仍是暂停状态，不自动继续。
- B 是 Bot 时，A 暂离后 Bot 不继续导致 A 被判负。
- 对局结束后 Redis key 被删除，MySQL 有最终 record。
- Redis 不可用时后端要降级，至少不能让服务崩溃；可以提示“当前对局恢复失败”。

## 结论

采用该方案后：

```text
Redis 管理全部对局过程快照
前端不直连 Redis
后端通过 WebSocket/HTTP 鉴权后读取 Redis 并下发快照
Game 线程仍负责实时判定
MySQL 只保存最终战绩和回放
```

这能解决返回游戏后状态回到新开局的问题，也能让对手明确感知“暂离”和“主动暂停”的区别。
