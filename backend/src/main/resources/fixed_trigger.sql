-- 修复的触发器：避免在 AFTER INSERT 中更新刚插入的记录
DELIMITER $$
DROP TRIGGER IF EXISTS before_delete_activity;

CREATE TRIGGER before_delete_activity
BEFORE DELETE ON itinerary_activities
FOR EACH ROW
BEGIN
    -- 1. 维护前序活动的next_id
    IF OLD.prev_id IS NOT NULL THEN
        UPDATE itinerary_activities
        SET next_id = OLD.next_id
        WHERE id = OLD.prev_id;
    END IF;

    -- 2. 维护后序活动的prev_id
    IF OLD.next_id IS NOT NULL THEN
        UPDATE itinerary_activities
        SET prev_id = OLD.prev_id
        WHERE id = OLD.next_id;
    END IF;

    -- 3. 如果是头节点，更新itinerary_days的first_activity_id
    IF OLD.prev_id IS NULL THEN
        UPDATE itinerary_days
        SET first_activity_id = OLD.next_id
        WHERE id = OLD.itinerary_day_id;
    END IF;

    -- 4. 如果是尾节点，更新itinerary_days的last_activity_id
    IF OLD.next_id IS NULL THEN
        UPDATE itinerary_days
        SET last_activity_id = OLD.prev_id
        WHERE id = OLD.itinerary_day_id;
    END IF;
END$$

DELIMITER ;

DELIMITER $$
DROP TRIGGER IF EXISTS after_insert_activity;

CREATE TRIGGER after_insert_activity
AFTER INSERT ON itinerary_activities
FOR EACH ROW
BEGIN
    DECLARE old_head_id BIGINT;
    DECLARE old_tail_id BIGINT;
    DECLARE temp_id BIGINT;

    -- 获取当前日程的头结点和尾结点
    SELECT first_activity_id, last_activity_id
    INTO old_head_id, old_tail_id
    FROM itinerary_days
    WHERE id = NEW.itinerary_day_id;

    -- 情况1: 插入到头部 (next_id 指向原头节点)
    IF NEW.next_id = old_head_id AND old_head_id IS NOT NULL THEN
        -- 更新日程的头节点指针
        UPDATE itinerary_days
        SET first_activity_id = NEW.id
        WHERE id = NEW.itinerary_day_id;

        -- 更新原头节点的 prev_id
        UPDATE itinerary_activities
        SET prev_id = NEW.id
        WHERE id = NEW.next_id;

    -- 情况2: 追加到尾部 (next_id IS NULL)
    ELSEIF NEW.next_id IS NULL THEN
        -- 更新日程的尾节点指针
        UPDATE itinerary_days
        SET last_activity_id = NEW.id
        WHERE id = NEW.itinerary_day_id;

        -- 如果有原尾节点，更新其 next_id
        IF old_tail_id IS NOT NULL THEN
            UPDATE itinerary_activities
            SET next_id = NEW.id
            WHERE id = old_tail_id;
        ELSE
            -- 如果是第一个节点，也要更新头节点
            UPDATE itinerary_days
            SET first_activity_id = NEW.id
            WHERE id = NEW.itinerary_day_id;
        END IF;

    -- 情况3: 插入到中间 (next_id 指向某个中间节点)
    ELSE
        -- 获取要插入位置的前一个节点
        SELECT prev_id
        INTO temp_id
        FROM itinerary_activities
        WHERE id = NEW.next_id;

        -- 更新后续节点的 prev_id
        UPDATE itinerary_activities
        SET prev_id = NEW.id
        WHERE id = NEW.next_id;

        -- 如果有前序节点，更新其 next_id
        IF temp_id IS NOT NULL THEN
            UPDATE itinerary_activities
            SET next_id = NEW.id
            WHERE id = temp_id;
        END IF;
    END IF;
END$$

DELIMITER ; 