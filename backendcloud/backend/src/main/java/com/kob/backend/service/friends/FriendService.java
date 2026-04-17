package com.kob.backend.service.friends;

import java.util.Map;

public interface FriendService {
    Map<String, Object> list(Integer page, Integer pageSize, String keyword, String status, Boolean favoriteOnly, String sortBy);

    Map<String, Object> stats();

    Map<String, Object> search(String keyword, Integer page, Integer pageSize);

    Map<String, Object> remove(Integer friendId);

    Map<String, Object> toggleFavorite(Integer friendId);
}
