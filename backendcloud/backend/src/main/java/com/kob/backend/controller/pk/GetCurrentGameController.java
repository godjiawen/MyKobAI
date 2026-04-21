package com.kob.backend.controller.pk;

import com.alibaba.fastjson.JSONObject;
import com.kob.backend.pojo.GameSnapshot;
import com.kob.backend.pojo.User;
import com.kob.backend.service.pk.GameSnapshotService;
import com.kob.backend.utils.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP 兜底接口：查询当前用户的进行中对局快照。
 */
@RestController
public class GetCurrentGameController {

    @Autowired
    private GameSnapshotService gameSnapshotService;

    @GetMapping("/api/pk/current-game/")
    public String getCurrentGame() {
        JSONObject resp = new JSONObject();
        try {
            User user = AuthUtil.getCurrentUser();
            GameSnapshot snap = gameSnapshotService.getByUserId(user.getId());
            if (snap != null && (snap.getAId().equals(user.getId()) || snap.getBId().equals(user.getId()))) {
                resp.put("error_message", "success");
                resp.put("has_game", true);
                // 手动构建 snake_case JSON
                JSONObject snapJson = new JSONObject();
                snapJson.put("game_id", snap.getGameId());
                snapJson.put("status", snap.getStatus());
                snapJson.put("a_id", snap.getAId());
                snapJson.put("b_id", snap.getBId());
                snapJson.put("a_sx", snap.getASx());
                snapJson.put("a_sy", snap.getASy());
                snapJson.put("b_sx", snap.getBSx());
                snapJson.put("b_sy", snap.getBSy());
                snapJson.put("map", snap.getMap());
                snapJson.put("a_steps", snap.getASteps());
                snapJson.put("b_steps", snap.getBSteps());
                snapJson.put("paused", snap.isPaused());
                snapJson.put("paused_by", snap.getPausedBy());
                snapJson.put("suspended", snap.isSuspended());
                snapJson.put("suspended_by", snap.getSuspendedBy());
                snapJson.put("suspended_reason", snap.getSuspendedReason());
                snapJson.put("away_user_ids", snap.getAwayUserIds());
                snapJson.put("room_id", snap.getRoomId());
                snapJson.put("updated_at", snap.getUpdatedAt());
                resp.put("snapshot", snapJson);
            } else {
                resp.put("error_message", "success");
                resp.put("has_game", false);
            }
        } catch (Exception e) {
            resp.put("error_message", "error: " + e.getMessage());
        }
        return resp.toJSONString();
    }
}


