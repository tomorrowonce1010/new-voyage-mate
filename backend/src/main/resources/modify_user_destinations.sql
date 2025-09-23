-- 修改user_destinations表，添加auto_add字段
USE voyagemate;

ALTER TABLE user_destinations
    ADD COLUMN auto_add BOOLEAN DEFAULT FALSE COMMENT '是否是根据用户的已出行行程自动添加的历史目的地';

alter table itinerary_activities
drop column transport_notes;

alter table user_preferences
drop column budget_range;

alter table user_preferences
drop column visited_destinations;

alter table user_preferences
drop column wishlist_destinations;