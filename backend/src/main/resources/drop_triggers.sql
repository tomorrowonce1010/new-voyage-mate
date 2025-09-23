-- 删除冲突的触发器，改为完全依赖 Java 代码维护链表
USE voyagemate;

DROP TRIGGER IF EXISTS before_delete_activity;
DROP TRIGGER IF EXISTS after_insert_activity;
 
-- 查看触发器是否已删除
SHOW TRIGGERS WHERE `Table` = 'itinerary_activities'; 