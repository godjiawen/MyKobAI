package com.kob.backend.controller.friends;

import com.kob.backend.service.friends.FriendRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class FriendRequestController {
    @Autowired
    private FriendRequestService friendRequestService;

    @PostMapping("/api/friends/request/send/")
    public Map<String, Object> send(
            @RequestParam("receiver_id") Integer receiverId,
            @RequestParam(value = "message", required = false) String message) {
        return friendRequestService.send(receiverId, message);
    }

    @GetMapping("/api/friends/request/received/")
    public Map<String, Object> received(
            @RequestParam(value = "status", defaultValue = "pending") String status,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "page_size", defaultValue = "20") Integer pageSize) {
        return friendRequestService.received(status, page, pageSize);
    }

    @GetMapping("/api/friends/request/sent/")
    public Map<String, Object> sent(
            @RequestParam(value = "status", defaultValue = "pending") String status,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "page_size", defaultValue = "20") Integer pageSize) {
        return friendRequestService.sent(status, page, pageSize);
    }

    @PostMapping("/api/friends/request/accept/")
    public Map<String, Object> accept(@RequestParam("request_id") Integer requestId) {
        return friendRequestService.accept(requestId);
    }

    @PostMapping("/api/friends/request/ignore/")
    public Map<String, Object> ignore(@RequestParam("request_id") Integer requestId) {
        return friendRequestService.ignore(requestId);
    }

    @PostMapping("/api/friends/request/cancel/")
    public Map<String, Object> cancel(@RequestParam("request_id") Integer requestId) {
        return friendRequestService.cancel(requestId);
    }
}
