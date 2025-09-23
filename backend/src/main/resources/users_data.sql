-- 用户数据
use voyagemate;

INSERT INTO user (id, username, email, created_at, updated_at) VALUES
(1, 'coconutrice', 'tomorrowonce1010@gmail.com', NOW(), NOW()),
(2, 'aiki', 'y4935857@gmail.com', NOW(), NOW());

INSERT INTO user_auth (id, user_id, password_hash, created_at, last_login) VALUES
(1, 1, '$2a$10$R/wX.zVFY3Dy08eTgQzliOl25t.v1eQgZ6vHK3.4fJIPaUIxWYJK6', NOW(), NOW()),
(2, 2, '$2a$10$GgJ/LpnQu997oD0gAQ9QeeGq4sMrqHPMHEsD30CT5AsYqt1qOtL1i', NOW(), NOW());

INSERT INTO user_profiles (id, user_id, avatar_url, birthday, signature,
                           bio, created_at, updated_at) VALUES
(1, 1, 'avatars/snowman-win.png', '2005-10-10', '这个入没有添加签名', '绝望的文盲', NOW(), NOW()),
(2, 2, 'avatars/snowman-win.png', '2004-12-30', '文介入殳月忝咖佥各', '色塑旳丈育', NOW(), NOW());

INSERT INTO user_preferences (id, user_id, special_requirements, travel_preferences) VALUES
(1, 1, '不擅长登山', '{"1": 0,"2": 1,"3": 1,"4": 1,"5": 1,"6": 1,"7": 1,"8": 1,
"9": 1,"10": 0,"11": 1,"12": 1,"13": 1,"14": 0,"15": 0,"16": 0,"17": 0,"18": 1,
"19": 1,"20": 1,"21": 0,"22": 0,"23": 0,"24": 1,"25": 1,"26": 0,"27": 1,"28": 0,
"29": 0,"30": 1}'),
(2, 2, '残疾', '{"1": 0,"2": 1,"3": 1,"4": 1,"5": 1,"6": 1,"7": 1,"8": 1,
"9": 1,"10": 0,"11": 1,"12": 1,"13": 1,"14": 0,"15": 0,"16": 0,"17": 0,"18": 1,
"19": 1,"20": 1,"21": 0,"22": 0,"23": 0,"24": 1,"25": 1,"26": 0,"27": 1,"28": 0,
"29": 0,"30": 1}');

INSERT INTO user_destinations (id, user_preferences_id, destination_id,
                               type, visit_year_month, days, notes, created_at) VALUES
(1, 1, 3, '期望目的地', '0000000', -1, '', NOW()),
(2, 1, 2, '历史目的地', '2025-07', 200, '上大学', NOW()),
(3, 2, 6, '历史目的地', '2024-01', 4, '特别无聊', NOW());