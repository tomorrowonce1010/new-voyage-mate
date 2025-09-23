package com.se_07.backend.controller;

import com.se_07.backend.entity.CommunityEntry;
import com.se_07.backend.repository.CommunityEntryRepository;
import com.se_07.backend.service.CommunityService;
import com.se_07.backend.service.SemanticSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link CommunityController}. All endpoints are tested for the happy-path and,
 * where relevant, additional branches such as 404 and 500 responses to make sure we reach
 * >90% statement & branch coverage.
 */
@ExtendWith(MockitoExtension.class)
class CommunityControllerTest {

    @Mock
    private CommunityService communityService;
    @Mock
    private SemanticSearchService semanticSearchService;
    @Mock
    private CommunityEntryRepository communityEntryRepository;

    @InjectMocks
    private CommunityController communityController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(communityController).build();
    }

    @Test
    @DisplayName("GET /community/public ‑ success")
    void getPublicCommunityEntries_success() throws Exception {
        List<Map<String, Object>> mockList = List.of(Map.of("id", 1));
        when(communityService.getPublicCommunityEntries()).thenReturn(mockList);

        mockMvc.perform(get("/community/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        verify(communityService).getPublicCommunityEntries();
    }

    @Test
    @DisplayName("GET /community/public ‑ internal error")
    void getPublicCommunityEntries_error() throws Exception {
        when(communityService.getPublicCommunityEntries()).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/community/public"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("GET /community/share/{shareCode} ‑ found")
    void getCommunityEntryByShareCode_found() throws Exception {
        Map<String, Object> data = Map.of("id", 1, "shareCode", "CODE");
        when(communityService.getCommunityEntryByShareCode("CODE")).thenReturn(data);

        mockMvc.perform(get("/community/share/CODE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shareCode").value("CODE"));
    }

    @Test
    @DisplayName("GET /community/share/{shareCode} ‑ not found")
    void getCommunityEntryByShareCode_notFound() throws Exception {
        when(communityService.getCommunityEntryByShareCode("NONE")).thenReturn(null);

        mockMvc.perform(get("/community/share/NONE"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /community/search?q=xxx ‑ success")
    void searchCommunityEntries_success() throws Exception {
        when(communityService.searchCommunityEntries("test")).thenReturn(List.of());

        mockMvc.perform(get("/community/search").param("q", "test"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /community/search ‑ internal error")
    void searchCommunityEntries_error() throws Exception {
        when(communityService.searchCommunityEntries(anyString())).thenThrow(new RuntimeException());

        mockMvc.perform(get("/community/search").param("q", "err"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("POST /community/{id}/view ‑ success")
    void incrementViewCount_success() throws Exception {
        doNothing().when(communityService).incrementViewCount(1L);

        mockMvc.perform(post("/community/1/view"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /community/itinerary/{id} ‑ found")
    void getCommunityEntryByItineraryId_found() throws Exception {
        CommunityEntry entry = new CommunityEntry();
        entry.setId(2L);
        entry.setShareCode("SHARE2");
        when(communityEntryRepository.findByItineraryId(100L)).thenReturn(Optional.of(entry));

        mockMvc.perform(get("/community/itinerary/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shareCode").value("SHARE2"));
    }

    @Test
    @DisplayName("GET /community/itinerary/{id} ‑ not found")
    void getCommunityEntryByItineraryId_notFound() throws Exception {
        when(communityEntryRepository.findByItineraryId(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/community/itinerary/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /community/popular/tags ‑ success")
    void getPopularTags_success() throws Exception {
        when(communityService.getPopularTags(10)).thenReturn(List.of());

        mockMvc.perform(get("/community/popular/tags"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /community/popular/authors ‑ success")
    void getPopularAuthors_success() throws Exception {
        when(communityService.getPopularAuthors(5)).thenReturn(List.of());

        mockMvc.perform(get("/community/popular/authors"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /community/semantic/search ‑ success")
    void semanticSearchCommunityEntries_success() throws Exception {
        Page<Map<String, Object>> page = new PageImpl<>(List.of(Map.of("id", 1)), PageRequest.of(0, 10), 1);
        when(semanticSearchService.semanticSearchCommunityEntries("hello", 0, 10)).thenReturn(page);

        mockMvc.perform(get("/community/semantic/search").param("q", "hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /community/semantic/search/tags ‑ success")
    void semanticSearchCommunityEntriesByTags_success() throws Exception {
        Page<Map<String, Object>> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(semanticSearchService.semanticSearchCommunityEntriesByTags(eq("hello"), anyList(), eq(0), eq(10))).thenReturn(page);

        mockMvc.perform(get("/community/semantic/search/tags")
                        .param("q", "hello")
                        .param("tags", "t1", "t2"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /community/search/destination ‑ success")
    void searchCommunityEntriesByDestination_success() throws Exception {
        when(communityService.searchCommunityEntriesByDestination(eq("bj"), eq(0), eq(10))).thenReturn(Map.of("total", 0));

        mockMvc.perform(get("/community/search/destination").param("destination", "bj"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /community/semantic/search/authors ‑ success")
    void semanticSearchAuthors_success() throws Exception {
        Page<Map<String, Object>> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(semanticSearchService.semanticSearchAuthors("jack", 0, 10)).thenReturn(page);

        mockMvc.perform(get("/community/semantic/search/authors").param("q", "jack"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /community/search/tags ‑ success")
    void filterCommunityEntriesByTags_success() throws Exception {
        Page<Map<String, Object>> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(semanticSearchService.filterCommunityEntriesByTags(anyList(), eq(0), eq(10))).thenReturn(page);

        mockMvc.perform(get("/community/search/tags").param("tags", "foo", "bar"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /community/public/sorted ‑ success")
    void getPublicCommunityEntriesWithSort_success() throws Exception {
        when(communityService.getPublicCommunityEntriesWithSort("popularity", 0, 10)).thenReturn(Map.of("total", 0));

        mockMvc.perform(get("/community/public/sorted").param("sortBy", "popularity"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /community/semantic/search/tags ‑ no tags param (tags==null) → covers else branch")
    void semanticSearchCommunityEntriesByTags_noTagsParam() throws Exception {
        Page<Map<String, Object>> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        // When tags param is missing, controller should pass an empty list to service
        when(semanticSearchService.semanticSearchCommunityEntriesByTags(eq("hi"), eq(List.of()), eq(0), eq(10))).thenReturn(page);

        mockMvc.perform(get("/community/semantic/search/tags").param("q", "hi"))
                .andExpect(status().isOk());

        verify(semanticSearchService).semanticSearchCommunityEntriesByTags(eq("hi"), eq(List.of()), eq(0), eq(10));
    }

    // ---- ERROR-PATH TESTS ----

    @Test
    @DisplayName("GET /community/share/{shareCode} ‑ internal error")
    void getCommunityEntryByShareCode_error() throws Exception {
        when(communityService.getCommunityEntryByShareCode("ERR")).thenThrow(new RuntimeException());
        mockMvc.perform(get("/community/share/ERR"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("POST /community/{id}/view ‑ internal error")
    void incrementViewCount_error() throws Exception {
        doThrow(new RuntimeException()).when(communityService).incrementViewCount(42L);
        mockMvc.perform(post("/community/42/view"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("GET /community/itinerary/{id} ‑ internal error")
    void getCommunityEntryByItineraryId_error() throws Exception {
        when(communityEntryRepository.findByItineraryId(55L)).thenThrow(new RuntimeException());
        mockMvc.perform(get("/community/itinerary/55"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("GET /community/popular/tags ‑ internal error")
    void getPopularTags_error() throws Exception {
        when(communityService.getPopularTags(10)).thenThrow(new RuntimeException());
        mockMvc.perform(get("/community/popular/tags"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("GET /community/popular/authors ‑ internal error")
    void getPopularAuthors_error() throws Exception {
        when(communityService.getPopularAuthors(5)).thenThrow(new RuntimeException());
        mockMvc.perform(get("/community/popular/authors"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("GET /community/semantic/search ‑ internal error")
    void semanticSearchCommunityEntries_error() throws Exception {
        when(semanticSearchService.semanticSearchCommunityEntries(anyString(), anyInt(), anyInt())).thenThrow(new RuntimeException());
        mockMvc.perform(get("/community/semantic/search").param("q", "err"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("GET /community/semantic/search/tags ‑ internal error")
    void semanticSearchCommunityEntriesByTags_error() throws Exception {
        when(semanticSearchService.semanticSearchCommunityEntriesByTags(anyString(), anyList(), anyInt(), anyInt())).thenThrow(new RuntimeException());
        mockMvc.perform(get("/community/semantic/search/tags").param("q", "err"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("GET /community/search/destination ‑ internal error")
    void searchCommunityEntriesByDestination_error() throws Exception {
        when(communityService.searchCommunityEntriesByDestination(anyString(), anyInt(), anyInt())).thenThrow(new RuntimeException());
        mockMvc.perform(get("/community/search/destination").param("destination", "err"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("GET /community/semantic/search/authors ‑ internal error")
    void semanticSearchAuthors_error() throws Exception {
        when(semanticSearchService.semanticSearchAuthors(anyString(), anyInt(), anyInt())).thenThrow(new RuntimeException());
        mockMvc.perform(get("/community/semantic/search/authors").param("q", "err"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("GET /community/search/tags ‑ internal error")
    void filterCommunityEntriesByTags_error() throws Exception {
        when(semanticSearchService.filterCommunityEntriesByTags(anyList(), anyInt(), anyInt())).thenThrow(new RuntimeException());
        mockMvc.perform(get("/community/search/tags").param("tags", "err"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("GET /community/public/sorted ‑ internal error")
    void getPublicCommunityEntriesWithSort_error() throws Exception {
        when(communityService.getPublicCommunityEntriesWithSort(anyString(), anyInt(), anyInt())).thenThrow(new RuntimeException());
        mockMvc.perform(get("/community/public/sorted").param("sortBy", "time"))
                .andExpect(status().isInternalServerError());
    }
}
