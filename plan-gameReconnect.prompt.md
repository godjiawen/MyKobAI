# Plan: 实现对局断线重连与 Redis 快照功能

基于技术方案文档，通过 Redis 管理全部对局过程快照，后端通过 WebSocket/HTTP 鉴权后读写快照，前端在进入/离开 PK 页面时上报 presence，并利用 `a_steps/b_steps` 恢复蛇身状态，解决切页后局面回退问题。

---

## Steps

### 1. 后端：添加 Redis 依赖并配置连接

在 `backendcloud/backend/pom.xml` 中加入 `spring-boot-starter-data-redis` 依赖；在 `backendcloud/backend/src/main/resources/application.properties` 中配置 `spring.redis.*` 连接属性。

### 2. 后端：新建 `GameSnapshot` POJO 及 `GameSnapshotService`

创建 `pojo/GameSnapshot.java`（对应方案中的快照 JSON 结构），新建 `service/pk/GameSnapshotService.java` 接口及其 `impl/pk/GameSnapshotServiceImpl.java`，使用 `StringRedisTemplate` 实现 `saveInitial / saveStep / savePause / savePresence / deleteGame / getByUserId / getByGameId`，写入时使用 MULTI/EXEC 事务同时更新三条 Key。

### 3. 后端：改造 `Game.java`，加入 `gameId/suspended/awayPlayers` 字段

在 `backendcloud/backend/src/main/java/com/kob/backend/consumer/utils/Game.java` 中新增 `gameId`、`roomId`、`suspended`、`suspendedBy`、`suspendedReason`、`awayPlayers` 字段，在 `nextStep()` 的超时计数逻辑中加入 `if (paused || suspended) { waitedMs 不累积; continue; }`，在 `sendResult()` 调用 MySQL 写入后调用 `gameSnapshotService.deleteGame()`。

### 4. 后端：改造 `WebSocketServer.java`，处理新事件及 presence

- `startGame()` 创建 `Game` 后调用 `saveInitial`，并在下发 `start-matching` 前写入 Redis 快照；
- `onOpen()` 连接建立后自动查询 Redis 并下发 `game-resync` 或 `no-active-game`；
- `onClose()` 不清空 `game`，改为标记暂离并调用 `savePresence`；
- 在 `onMessage()` 路由中新增 `sync-game`、`game-enter`、`game-leave` 三个事件处理分支，实现广播 `player-away / player-back / game-suspended / game-resumed-from-away`。

### 5. 后端：新增 HTTP 兜底接口 `/api/pk/current-game/`

创建 `controller/pk/GetCurrentGameController.java` 和对应 Service，内部调用 `GameSnapshotService.getByUserId(currentUserId)`，需在 Spring Security 配置中放行该路径（GET 需鉴权但需可访问）。

### 6. 前端：扩展 `web/src/store/pk.js` 支持快照状态

新增字段 `gameId / aSteps / bSteps / isAwaySuspended / awayUserIds / suspendedByUserId / suspendedReason`，新增 action `updateGameSnapshot(snapshot)` 按方案完整映射快照字段，新增 `markPlayerAway / markPlayerBack / updateAwaySuspended`。

### 7. 前端：在 `web/src/store/realtime.js` 的 `handleSocketMessage()` 中处理新事件

添加对 `game-resync / no-active-game / player-away / player-back / game-suspended / game-resumed-from-away` 的处理；在 `socket.onopen` 中发送 `{ event: "sync-game" }`。

### 8. 前端：改造 `web/src/assets/scripts/GameMap.js` 支持 steps 恢复蛇身

新增 `restoreSnakeFromSteps(snake, sx, sy, steps)` 方法，逻辑与方案一致（逐字符模拟 `next_step` + `check_tail_increasing`）；在 `start()` 方法中，若 `pkStore.aSteps / bSteps` 非空则调用该方法恢复两条蛇，而不是从初始点新建。

### 9. 前端：改造 `web/src/views/pk/PkIndexView.vue` 上报 presence

在 `onMounted()` 中发送 `game-enter` 和 `sync-game`；新增 `onBeforeUnmount()` 钩子，若对局进行中则发送 `game-leave`（`reason: "route-leave"`）。

### 10. 前端：在 `web/src/components/PlayGround.vue` 中增加暂离遮罩

在现有 `pause-overlay` 之后新增条件为 `pkStore.isAwaySuspended && !pkStore.isPaused` 的暂离遮罩，显示"对手暂时离开对局 / 等待对方返回后继续"文案，无"继续"按钮。

---

## Further Considerations

1. **Redis 可用性降级**：`GameSnapshotService` 实现中所有 Redis 操作应 `try-catch`，异常时记录日志但不抛出，确保 Redis 不可用时主流程（对局、匹配）不崩溃。

2. **`GameSnapshotService` 注入到 `WebSocketServer` 的方式**：`WebSocketServer` 是 `@ServerEndpoint`，Spring 的 `@Autowired` 在其中需使用静态字段 + setter 注入（项目现有模式），`GameSnapshotService` 同样需要遵循此模式，需注意 Bean 注入顺序。

3. **TTL 到期策略（第一版）**：方案建议对局中 TTL 10 分钟，暂离 15 分钟，到期后直接清理 Redis 不写战绩；后续可升级为离开方超时判负，当前版本先按简单策略实现。

