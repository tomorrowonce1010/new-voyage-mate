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

    -- 以指定日程id及后序活动id的方式插入

    -- 获取当前日程的头结点和尾结点
    SELECT first_activity_id, last_activity_id
    INTO old_head_id, old_tail_id
    FROM itinerary_days
    WHERE id = NEW.itinerary_day_id;

    -- 1. 如果next_id是头结点，则新活动为新的头结点，prev_id设为NULL
    IF NEW.next_id = old_head_id THEN
        UPDATE itinerary_days
        SET first_activity_id = NEW.id
        WHERE id = NEW.itinerary_day_id;

        UPDATE itinerary_activities
        SET prev_id = NEW.id
        WHERE id = NEW.next_id;

        UPDATE itinerary_activities
        SET prev_id = NULL
        WHERE id = NEW.id;

    -- 2. 如果next_id IS NULL，则新活动为新的尾结点，prev_id设为原尾结点id
    ELSEIF NEW.next_id IS NULL THEN
        UPDATE itinerary_days
        SET last_activity_id = NEW.id
        WHERE id = NEW.itinerary_day_id;

        UPDATE itinerary_activities
        SET next_id = NEW.id
        WHERE id = old_tail_id;

        UPDATE itinerary_activities
        SET prev_id = old_tail_id
        WHERE id = NEW.id;

    -- 3. 否则插入到某活动前面，将原next_id的prev_id设为新活动的prev_id
    ELSE
        -- 获取新活动的后序活动的前序活动id
        SELECT prev_id
        INTO temp_id
        FROM itinerary_activities
        WHERE id = NEW.next_id;

        UPDATE itinerary_activities
        SET prev_id = temp_id
        WHERE id = NEW.id;

        UPDATE itinerary_activities
        SET prev_id = NEW.id
        WHERE id = NEW.next_id;

        IF temp_id IS NOT NULL THEN
            UPDATE itinerary_activities
            SET next_id = NEW.id
            WHERE id = temp_id;
        END IF;
    END IF;
END$$

DELIMITER ;