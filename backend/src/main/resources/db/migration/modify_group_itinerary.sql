-- 修改group_itineraries表，移除itinerary_id的非空约束
ALTER TABLE group_itineraries MODIFY COLUMN itinerary_id bigint NULL; 