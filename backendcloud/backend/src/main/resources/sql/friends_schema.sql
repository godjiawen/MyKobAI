CREATE TABLE IF NOT EXISTS friend_relation (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    friend_id INT NOT NULL,
    is_favorite TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_friend_relation_user_friend (user_id, friend_id),
    KEY idx_friend_relation_user_id (user_id),
    KEY idx_friend_relation_friend_id (friend_id)
);

CREATE TABLE IF NOT EXISTS friend_request (
    id INT PRIMARY KEY AUTO_INCREMENT,
    sender_id INT NOT NULL,
    receiver_id INT NOT NULL,
    message VARCHAR(100) NOT NULL DEFAULT '',
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    handled_at DATETIME NULL,
    KEY idx_friend_request_sender (sender_id),
    KEY idx_friend_request_receiver (receiver_id),
    KEY idx_friend_request_status (status)
);

CREATE TABLE IF NOT EXISTS friend_invite (
    id INT PRIMARY KEY AUTO_INCREMENT,
    sender_id INT NOT NULL,
    receiver_id INT NOT NULL,
    game_mode VARCHAR(20) NOT NULL DEFAULT 'pk',
    sender_bot_id INT NOT NULL DEFAULT -1,
    receiver_bot_id INT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expired_at DATETIME NOT NULL,
    handled_at DATETIME NULL,
    KEY idx_friend_invite_sender (sender_id),
    KEY idx_friend_invite_receiver (receiver_id),
    KEY idx_friend_invite_status (status),
    KEY idx_friend_invite_expired_at (expired_at)
);

CREATE TABLE IF NOT EXISTS friend_chat_message (
    id INT PRIMARY KEY AUTO_INCREMENT,
    sender_id INT NOT NULL,
    receiver_id INT NOT NULL,
    content VARCHAR(500) NOT NULL,
    is_read TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_friend_chat_sender (sender_id),
    KEY idx_friend_chat_receiver (receiver_id),
    KEY idx_friend_chat_created_at (created_at)
);
