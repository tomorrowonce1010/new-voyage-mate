package com.se_07.backend.controller;

import com.se_07.backend.entity.Tag;
import com.se_07.backend.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagControllerTest {
    @Mock
    private TagRepository tagRepository;
    @InjectMocks
    private TagController tagController;

    @Test
    void getAllTags_normal() {
        Tag tag1 = new Tag(); tag1.setId(1L); tag1.setTag("A");
        Tag tag2 = new Tag(); tag2.setId(2L); tag2.setTag("B");
        when(tagRepository.findAll()).thenReturn(Arrays.asList(tag1, tag2));
        ResponseEntity<Map<String, Object>> resp = tagController.getAllTags();
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.hasBody());
        Map<String, Object> body = resp.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        List<?> data = (List<?>) body.get("data");
        assertEquals(2, data.size());
        assertEquals("A", ((Tag)data.get(0)).getTag());
        assertEquals("B", ((Tag)data.get(1)).getTag());
        verify(tagRepository, times(1)).findAll();
    }

    @Test
    void getAllTags_empty() {
        when(tagRepository.findAll()).thenReturn(Collections.emptyList());
        ResponseEntity<Map<String, Object>> resp = tagController.getAllTags();
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        Map<String, Object> body = resp.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        List<?> data = (List<?>) body.get("data");
        assertTrue(data.isEmpty());
    }

    @Test
    void getAllTags_exception() {
        when(tagRepository.findAll()).thenThrow(new RuntimeException("db error"));
        ResponseEntity<Map<String, Object>> resp = tagController.getAllTags();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        Map<String, Object> body = resp.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertTrue(((String) body.get("message")).contains("db error"));
    }

    @Test
    void createSuccessResponse_reflection() throws Exception {
        List<Tag> tags = new ArrayList<>();
        Method m = TagController.class.getDeclaredMethod("createSuccessResponse", Object.class);
        m.setAccessible(true);
        Map<String, Object> resp = (Map<String, Object>) m.invoke(tagController, tags);
        assertTrue((Boolean) resp.get("success"));
        assertEquals(tags, resp.get("data"));
        assertFalse(resp.containsKey("message"));
    }

    @Test
    void createErrorResponse_reflection() throws Exception {
        String msg = "err";
        Method m = TagController.class.getDeclaredMethod("createErrorResponse", String.class);
        m.setAccessible(true);
        Map<String, Object> resp = (Map<String, Object>) m.invoke(tagController, msg);
        assertFalse((Boolean) resp.get("success"));
        assertEquals(msg, resp.get("message"));
        assertFalse(resp.containsKey("data"));
    }

    @Test
    void tagCanEqual_reflection() throws Exception {
        Tag tag = new Tag();
        java.lang.reflect.Method m = Tag.class.getDeclaredMethod("canEqual", Object.class);
        m.setAccessible(true);
        // 自身类型
        assertTrue((Boolean) m.invoke(tag, tag));
        // 其它类型
        assertFalse((Boolean) m.invoke(tag, "string"));
        // 子类
        class SubTag extends Tag {}
        assertTrue((Boolean) m.invoke(tag, new SubTag()));
    }
}