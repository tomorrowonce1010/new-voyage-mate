-- 为itineraries表添加group_id字段
ALTER TABLE itineraries
ADD COLUMN group_id BIGINT NULL,
ADD CONSTRAINT fk_itineraries_group
FOREIGN KEY (group_id) REFERENCES travel_groups(id)
ON DELETE SET NULL; 