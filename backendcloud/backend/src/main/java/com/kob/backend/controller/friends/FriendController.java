package com.kob.backend.controller.friends;

import com.kob.backend.service.friends.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class FriendController {
    @Autowired
    private FriendService friendService;

    @GetMapping("/api/friends/list/")
    public Map<String, Object> list(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "page_size", defaultValue = "20") Integer pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "favorite_only", required = false) Boolean favoriteOnly,
            @RequestParam(value = "sort_by", required = false) String sortBy) {
        return friendService.list(page, pageSize, keyword, status, favoriteOnly, sortBy);
    }

    @GetMapping("/api/friends/stats/")
    public Map<String, Object> stats() {
        return friendService.stats();
    }

    @GetMapping("/api/friends/search/")
    public Map<String, Object> search(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "page_size", defaultValue = "10") Integer pageSize) {
        return friendService.search(keyword, page, pageSize);
    }

    @PostMapping("/api/friends/remove/")
    public Map<String, Object> remove(@RequestParam("friend_id") Integer friendId) {
        return friendService.remove(friendId);
    }

    @PostMapping("/api/friends/favorite/toggle/")
    public Map<String, Object> toggleFavorite(@RequestParam("friend_id") Integer friendId) {
        return friendService.toggleFavorite(friendId);
    }
}
