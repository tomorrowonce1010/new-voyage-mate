package com.se_07.backend.service.impl;

import com.se_07.backend.dto.ItineraryDTO;
import com.se_07.backend.dto.converter.ItineraryConverter;
import com.se_07.backend.entity.*;
import com.se_07.backend.repository.CommunityEntryRepository;
import com.se_07.backend.repository.CommunityEntryTagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CommunityServiceImpl} focusing on achieving high statement & branch coverage.
 */
@ExtendWith(MockitoExtension.class)
class CommunityServiceImplTest {

    @Mock
    private CommunityEntryRepository communityEntryRepository;
    @Mock
    private CommunityEntryTagRepository communityEntryTagRepository;
    @Mock
    private ItineraryConverter itineraryConverter;

    @InjectMocks
    private CommunityServiceImpl communityService;

    private CommunityEntry sampleEntryWithDest;
    private CommunityEntry sampleEntryNoDest;

    private List<CommunityEntryTag> tagLinks;

    @BeforeEach
    void setup() {
        // Build sample user
        User user = new User();
        user.setId(1L);
        user.setUsername("tester");
        user.setEmail("t@e.com");

        // Build itinerary with destinations (for sampleEntryWithDest)
        Itinerary itineraryWithDest = new Itinerary();
        itineraryWithDest.setId(11L);
        itineraryWithDest.setUser(user);
        itineraryWithDest.setTitle("Trip A");
        itineraryWithDest.setImageUrl("img.png");
        itineraryWithDest.setStartDate(LocalDate.now());
        itineraryWithDest.setEndDate(LocalDate.now().plusDays(2));
        itineraryWithDest.setBudget(BigDecimal.valueOf(99));
        itineraryWithDest.setTravelerCount(2);
        itineraryWithDest.setPermissionStatus(Itinerary.PermissionStatus.所有人可见);
        itineraryWithDest.setCreatedAt(LocalDateTime.now());
        itineraryWithDest.setUpdatedAt(LocalDateTime.now());

        // Build itinerary with NO destinations (for else branch)
        Itinerary itineraryNoDest = new Itinerary();
        itineraryNoDest.setId(22L);
        itineraryNoDest.setUser(user);
        itineraryNoDest.setTitle("Trip B");
        itineraryNoDest.setImageUrl(null);
        itineraryNoDest.setStartDate(LocalDate.now());
        itineraryNoDest.setEndDate(LocalDate.now().plusDays(1));
        itineraryNoDest.setBudget(BigDecimal.valueOf(50));
        itineraryNoDest.setTravelerCount(1);
        itineraryNoDest.setPermissionStatus(Itinerary.PermissionStatus.所有人可见);
        itineraryNoDest.setCreatedAt(LocalDateTime.now());
        itineraryNoDest.setUpdatedAt(LocalDateTime.now());

        // Build entries
        sampleEntryWithDest = new CommunityEntry();
        sampleEntryWithDest.setId(101L);
        sampleEntryWithDest.setShareCode("CODE101");
        sampleEntryWithDest.setDescription("desc");
        sampleEntryWithDest.setViewCount(0);
        sampleEntryWithDest.setCreatedAt(LocalDateTime.now());
        sampleEntryWithDest.setItinerary(itineraryWithDest);

        sampleEntryNoDest = new CommunityEntry();
        sampleEntryNoDest.setId(202L);
        sampleEntryNoDest.setShareCode("CODE202");
        sampleEntryNoDest.setDescription("desc2");
        sampleEntryNoDest.setViewCount(1);
        sampleEntryNoDest.setCreatedAt(LocalDateTime.now());
        sampleEntryNoDest.setItinerary(itineraryNoDest);

        // Build tags (covers tag present & null)
        Tag tag = new Tag();
        tag.setId(1L);
        tag.setTag("美食");
        CommunityEntryTag linkWithTag = new CommunityEntryTag();
        linkWithTag.setCommunityEntry(sampleEntryWithDest);
        linkWithTag.setTag(tag);

        CommunityEntryTag linkNoTag = new CommunityEntryTag();
        linkNoTag.setCommunityEntry(sampleEntryWithDest);
        linkNoTag.setTag(null); // triggers else path in code

        tagLinks = List.of(linkWithTag, linkNoTag);

        // Stub itineraryConverter for both itineraries
        Mockito.lenient().when(itineraryConverter.toDTO(itineraryWithDest)).thenReturn(buildDTO(itineraryWithDest, List.of("北京")));
        Mockito.lenient().when(itineraryConverter.toDTO(itineraryNoDest)).thenReturn(buildDTO(itineraryNoDest, Collections.emptyList()));
    }

    private ItineraryDTO buildDTO(Itinerary itinerary, List<String> destNames) {
        ItineraryDTO dto = new ItineraryDTO();
        dto.setId(itinerary.getId());
        dto.setUserId(itinerary.getUser().getId());
        dto.setUsername(itinerary.getUser().getUsername());
        dto.setTitle(itinerary.getTitle());
        dto.setImageUrl(itinerary.getImageUrl());
        dto.setStartDate(itinerary.getStartDate());
        dto.setEndDate(itinerary.getEndDate());
        dto.setPermissionStatus(itinerary.getPermissionStatus());
        dto.setDestinationNames(destNames);
        return dto;
    }

    // ---------- Tests ----------

    @Test
    @DisplayName("getPublicCommunityEntries ‑ happy path")
    void getPublicCommunityEntries_success() {
        when(communityEntryRepository.findPublicEntries()).thenReturn(List.of(sampleEntryWithDest));
        when(communityEntryTagRepository.findByCommunityEntry(sampleEntryWithDest)).thenReturn(tagLinks);

        List<Map<String, Object>> result = communityService.getPublicCommunityEntries();

        assertEquals(1, result.size());
        Map<String, Object> entryMap = result.get(0);
        assertEquals("CODE101", entryMap.get("shareCode"));
        // Tag list should contain only non-null tag names
        List<?> tags = (List<?>) entryMap.get("tags");
        assertEquals(List.of("美食"), tags);
        // Destination should be joined string
        Map<?, ?> itineraryMap = (Map<?, ?>) entryMap.get("itinerary");
        assertEquals("北京", itineraryMap.get("destination"));
    }

    @Test
    @DisplayName("getPublicCommunityEntries ‑ repository throws exception → wrapped RuntimeException")
    void getPublicCommunityEntries_error() {
        when(communityEntryRepository.findPublicEntries()).thenThrow(new RuntimeException("db down"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> communityService.getPublicCommunityEntries());
        assertTrue(ex.getMessage().contains("获取公共社区条目失败"));
    }

    @Test
    @DisplayName("getCommunityEntryByShareCode ‑ found and viewCount incremented")
    void getCommunityEntryByShareCode_found() {
        when(communityEntryRepository.findByShareCode("CODE101"))
                .thenReturn(Optional.of(sampleEntryWithDest));
        when(communityEntryTagRepository.findByCommunityEntry(sampleEntryWithDest)).thenReturn(tagLinks);

        Map<String, Object> result = communityService.getCommunityEntryByShareCode("CODE101");
        assertNotNull(result);
        assertEquals(1, result.get("viewCount")); // started at 0 → incremented to 1
        verify(communityEntryRepository).save(sampleEntryWithDest);
    }

    @Test
    @DisplayName("getCommunityEntryByShareCode ‑ not found returns null")
    void getCommunityEntryByShareCode_notFound() {
        when(communityEntryRepository.findByShareCode("NONE")).thenReturn(Optional.empty());
        assertNull(communityService.getCommunityEntryByShareCode("NONE"));
    }

    @Test
    @DisplayName("searchCommunityEntries ‑ returns list and handles dest==empty (else branch)")
    void searchCommunityEntries_success() {
        when(communityEntryRepository.searchPublicEntries("foo"))
                .thenReturn(List.of(sampleEntryNoDest));
        when(communityEntryTagRepository.findByCommunityEntry(sampleEntryNoDest)).thenReturn(Collections.emptyList());

        List<Map<String, Object>> res = communityService.searchCommunityEntries("foo");
        assertEquals(1, res.size());
        Map<String, Object> itineraryMap = (Map<String, Object>) res.get(0).get("itinerary");
        assertEquals("待规划目的地", itineraryMap.get("destination")); // else branch covered
    }

    @Test
    @DisplayName("searchCommunityEntries ‑ destination names non-empty branch")
    void searchCommunityEntries_destNonEmpty() {
        when(communityEntryRepository.searchPublicEntries("bar")).thenReturn(List.of(sampleEntryWithDest));
        when(communityEntryTagRepository.findByCommunityEntry(sampleEntryWithDest)).thenReturn(tagLinks);

        List<Map<String, Object>> res = communityService.searchCommunityEntries("bar");
        Map<String, Object> itineraryMap = (Map<String, Object>) res.get(0).get("itinerary");
        assertEquals("北京", itineraryMap.get("destination"));
    }

    @Test
    @DisplayName("getPublicCommunityEntries ‑ entry without itinerary branch")
    void getPublicCommunityEntries_noItinerary() {
        CommunityEntry entry = new CommunityEntry();
        entry.setId(303L);
        entry.setShareCode("CODE303");
        entry.setDescription("desc");
        entry.setViewCount(0);
        entry.setCreatedAt(LocalDateTime.now());

        when(communityEntryRepository.findPublicEntries()).thenReturn(List.of(entry));
        when(communityEntryTagRepository.findByCommunityEntry(entry)).thenReturn(Collections.emptyList());

        List<Map<String, Object>> list = communityService.getPublicCommunityEntries();
        Map<String, Object> map = list.get(0);
        assertFalse(map.containsKey("itinerary")); // itinerary branch not executed
    }

    @Test
    @DisplayName("getCommunityEntryByShareCode ‑ itinerary null branch")
    void getCommunityEntryByShareCode_noItinerary() {
        CommunityEntry entry = new CommunityEntry();
        entry.setId(404L);
        entry.setShareCode("NOITIN");
        entry.setDescription("d");
        entry.setViewCount(0);
        entry.setCreatedAt(LocalDateTime.now());

        when(communityEntryRepository.findByShareCode("NOITIN"))
                .thenReturn(Optional.of(entry));
        when(communityEntryTagRepository.findByCommunityEntry(entry)).thenReturn(Collections.emptyList());

        Map<String, Object> res = communityService.getCommunityEntryByShareCode("NOITIN");
        assertFalse(res.containsKey("itinerary"));
    }

    @Test
    @DisplayName("getPublicCommunityEntries ‑ itinerary but user null branch")
    void getPublicCommunityEntries_itineraryUserNull() {
        Itinerary itin = new Itinerary();
        itin.setId(33L);
        itin.setUser(null); // user null
        itin.setTitle("X");
        itin.setStartDate(LocalDate.now());
        itin.setEndDate(LocalDate.now());

        CommunityEntry entry = new CommunityEntry();
        entry.setId(505L);
        entry.setShareCode("CODE505");
        entry.setDescription("d");
        entry.setViewCount(0);
        entry.setCreatedAt(LocalDateTime.now());
        entry.setItinerary(itin);

        ItineraryDTO dto = new ItineraryDTO();
        dto.setId(itin.getId());
        dto.setTitle("X");
        dto.setDestinationNames(Collections.emptyList());
        dto.setPermissionStatus(Itinerary.PermissionStatus.所有人可见);

        when(itineraryConverter.toDTO(itin)).thenReturn(dto);
        when(communityEntryRepository.findPublicEntries()).thenReturn(Collections.singletonList(entry));
        when(communityEntryTagRepository.findByCommunityEntry(entry)).thenReturn(Collections.emptyList());

        List<Map<String, Object>> list = communityService.getPublicCommunityEntries();
        Map<String, Object> itineraryMap = (Map<String, Object>) list.get(0).get("itinerary");
        assertFalse(itineraryMap.containsKey("user"));
    }

    @Test
    @DisplayName("getCommunityEntryByShareCode ‑ dest empty branch")
    void getCommunityEntryByShareCode_destEmpty() {
        when(communityEntryRepository.findByShareCode("CODE202")).thenReturn(Optional.of(sampleEntryNoDest));
        when(communityEntryTagRepository.findByCommunityEntry(sampleEntryNoDest)).thenReturn(Collections.emptyList());

        Map<String, Object> res = communityService.getCommunityEntryByShareCode("CODE202");
        Map<String, Object> itineraryMap = (Map<String, Object>) res.get("itinerary");
        assertEquals("待规划目的地", itineraryMap.get("destination"));
    }

    // ---- Additional coverage & error-path tests ----

    @Test
    @DisplayName("getCommunityEntryByShareCode ‑ repository throws")
    void getCommunityEntryByShareCode_error() {
        when(communityEntryRepository.findByShareCode("BOOM")).thenThrow(new RuntimeException("db"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> communityService.getCommunityEntryByShareCode("BOOM"));
        assertTrue(ex.getMessage().contains("根据分享码获取社区条目失败"));
    }

    @Test
    @DisplayName("searchCommunityEntries ‑ repository throws")
    void searchCommunityEntries_error() {
        when(communityEntryRepository.searchPublicEntries(anyString())).thenThrow(new RuntimeException("db"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> communityService.searchCommunityEntries("oops"));
        assertTrue(ex.getMessage().contains("搜索社区条目失败"));
    }

    @Test
    @DisplayName("searchCommunityEntriesByDestination ‑ repository throws")
    void searchCommunityEntriesByDestination_error() {
        when(communityEntryRepository.countByDestination(anyString())).thenThrow(new RuntimeException("db"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> communityService.searchCommunityEntriesByDestination("sh", 0, 10));
        assertTrue(ex.getMessage().contains("按目的地搜索社区条目失败"));
    }

    @Test
    @DisplayName("getPublicCommunityEntriesWithSort ‑ repository throws")
    void getPublicCommunityEntriesWithSort_error() {
        when(communityEntryRepository.findAllPublicByPopularity(PageRequest.of(0, 10))).thenThrow(new RuntimeException("db"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> communityService.getPublicCommunityEntriesWithSort("popularity", 0, 10));
        assertTrue(ex.getMessage().contains("获取排序社区条目失败"));
    }

    @Test
    @DisplayName("getPopularTags ‑ limit greater than rows (cover second row)")
    void getPopularTags_largeLimit() {
        Object[] row1 = new Object[]{"美食", 5L};
        Object[] row2 = new Object[]{"徒步", 2L};
        List<Object[]> rows = Arrays.asList(row1, row2);
        when(communityEntryTagRepository.findTagPopularity()).thenReturn(rows);

        List<Map<String, Object>> tags = communityService.getPopularTags(10);
        assertEquals(2, tags.size());
        assertEquals("徒步", tags.get(1).get("tag"));
    }

    @Test
    @DisplayName("getPopularAuthors ‑ large limit")
    void getPopularAuthors_largeLimit() {
        Object[] row1 = new Object[]{1L, "tester", 10L};
        Object[] row2 = new Object[]{2L, "other", 5L};
        List<Object[]> rows = Arrays.asList(row1, row2);
        when(communityEntryRepository.findAuthorPopularity()).thenReturn(rows);
        List<Map<String, Object>> authors = communityService.getPopularAuthors(10);
        assertEquals(2, authors.size());
        assertEquals("other", authors.get(1).get("username"));
    }

    @Test
    @DisplayName("getPopularTags limit 0 returns empty list")
    void getPopularTags_zeroLimit() {
        Object[] row1 = new Object[]{"美食", 5L};
        when(communityEntryTagRepository.findTagPopularity()).thenReturn(Collections.singletonList(row1));
        List<Map<String, Object>> tags = communityService.getPopularTags(0);
        assertTrue(tags.isEmpty());
    }

    @Test
    @DisplayName("getPopularAuthors limit 0 returns empty list")
    void getPopularAuthors_zeroLimit() {
        Object[] row = new Object[]{1L, "tester", 10L};
        when(communityEntryRepository.findAuthorPopularity()).thenReturn(Collections.singletonList(row));
        List<Map<String, Object>> authors = communityService.getPopularAuthors(0);
        assertTrue(authors.isEmpty());
    }

    @Test
    @DisplayName("searchCommunityEntriesByDestination ‑ destination list empty branch")
    void searchCommunityEntriesByDestination_emptyDestNames() {
        when(communityEntryRepository.countByDestination("sh")).thenReturn(1L);
        when(communityEntryRepository.findByDestination("sh", 0, 10)).thenReturn(List.of(sampleEntryNoDest));
        when(communityEntryTagRepository.findByCommunityEntry(sampleEntryNoDest)).thenReturn(Collections.emptyList());

        Map<String, Object> res = communityService.searchCommunityEntriesByDestination("sh", 0, 10);
        List<?> content = (List<?>) res.get("content");
        Map<String, Object> entry = (Map<String, Object>) content.get(0);
        Map<String, Object> itineraryMap = (Map<String, Object>) entry.get("itinerary");
        assertEquals("待规划目的地", itineraryMap.get("destination"));
    }

    @Test
    @DisplayName("incrementViewCount ‑ entry exists")
    void incrementViewCount_exists() {
        when(communityEntryRepository.findById(101L)).thenReturn(Optional.of(sampleEntryWithDest));
        communityService.incrementViewCount(101L);
        assertEquals(1, sampleEntryWithDest.getViewCount());
        verify(communityEntryRepository).save(sampleEntryWithDest);
    }

    @Test
    @DisplayName("incrementViewCount ‑ entry missing (warn path)")
    void incrementViewCount_missing() {
        when(communityEntryRepository.findById(999L)).thenReturn(Optional.empty());
        communityService.incrementViewCount(999L);
        verify(communityEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("incrementViewCount ‑ repository throws -> wraps exception")
    void incrementViewCount_error() {
        when(communityEntryRepository.findById(101L)).thenThrow(new RuntimeException("db"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> communityService.incrementViewCount(101L));
        assertTrue(ex.getMessage().contains("增加查看次数失败"));
    }

    @Test
    @DisplayName("getPublicCommunityEntries ‑ empty list branch")
    void getPublicCommunityEntries_empty() {
        when(communityEntryRepository.findPublicEntries()).thenReturn(Collections.emptyList());
        List<Map<String, Object>> list = communityService.getPublicCommunityEntries();
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("getPublicCommunityEntriesWithSort ‑ empty page")
    void getPublicCommunityEntriesWithSort_emptyPage() {
        when(communityEntryRepository.findAllPublicByTime(PageRequest.of(0, 10)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0));
        Map<String, Object> res = communityService.getPublicCommunityEntriesWithSort("unknown", 0, 10);
        assertEquals(0L, res.get("totalElements"));
        List<?> content = (List<?>) res.get("content");
        assertTrue(content.isEmpty());
    }

    @Test
    @DisplayName("searchCommunityEntriesByDestination ‑ zero results branch")
    void searchCommunityEntriesByDestination_zero() {
        when(communityEntryRepository.countByDestination("none")).thenReturn(0L);
        when(communityEntryRepository.findByDestination("none", 0, 10)).thenReturn(Collections.emptyList());
        Map<String, Object> res = communityService.searchCommunityEntriesByDestination("none", 0, 10);
        assertEquals(0L, ((Number) res.get("totalElements")).longValue());
        List<?> content = (List<?>) res.get("content");
        assertTrue(content.isEmpty());
    }

    @Test
    @DisplayName("searchCommunityEntriesByDestination ‑ entry itinerary null")
    void searchCommunityEntriesByDestination_noItinerary() {
        CommunityEntry entry = new CommunityEntry();
        entry.setId(606L);
        entry.setShareCode("SC606");
        entry.setDescription("d");
        entry.setViewCount(0);
        entry.setCreatedAt(LocalDateTime.now());

        when(communityEntryRepository.countByDestination("x")).thenReturn(1L);
        when(communityEntryRepository.findByDestination("x", 0, 10)).thenReturn(Collections.singletonList(entry));
        when(communityEntryTagRepository.findByCommunityEntry(entry)).thenReturn(Collections.emptyList());

        Map<String, Object> res = communityService.searchCommunityEntriesByDestination("x", 0, 10);
        Map<String, Object> first = (Map<String, Object>) ((List<?>) res.get("content")).get(0);
        assertFalse(first.containsKey("itinerary"));
    }

    @Test
    @DisplayName("getPopularTags respects limit")
    void getPopularTags_limit() {
        Object[] row1 = new Object[]{"美食", 5L};
        Object[] row2 = new Object[]{"徒步", 2L};
        List<Object[]> tagRows = Arrays.asList(row1, row2);
        when(communityEntryTagRepository.findTagPopularity()).thenReturn(tagRows);

        List<Map<String, Object>> tags = communityService.getPopularTags(1);
        assertEquals(1, tags.size());
        assertEquals("美食", tags.get(0).get("tag"));
    }

    @Test
    @DisplayName("getPopularAuthors respects limit")
    void getPopularAuthors_limit() {
        Object[] row = new Object[]{1L, "tester", 10L};
        List<Object[]> authorRows = Collections.singletonList(row);
        when(communityEntryRepository.findAuthorPopularity()).thenReturn(authorRows);
        List<Map<String, Object>> authors = communityService.getPopularAuthors(5);
        assertEquals(1, authors.size());
        assertEquals("tester", authors.get(0).get("username"));
    }

    @Test
    @DisplayName("searchCommunityEntriesByDestination aggregates paging info")
    void searchCommunityEntriesByDestination_success() {
        when(communityEntryRepository.countByDestination("bj")).thenReturn(1L);
        when(communityEntryRepository.findByDestination("bj", 0, 10)).thenReturn(List.of(sampleEntryWithDest));
        when(communityEntryTagRepository.findByCommunityEntry(sampleEntryWithDest)).thenReturn(tagLinks);

        Map<String, Object> res = communityService.searchCommunityEntriesByDestination("bj", 0, 10);
        assertEquals(1L, ((Number) res.get("totalElements")).longValue());
        assertEquals(1, ((List<?>) res.get("content")).size());
    }

    @Test
    @DisplayName("getPublicCommunityEntriesWithSort ‑ popularity branch")
    void getPublicCommunityEntriesWithSort_popularity() {
        List<CommunityEntry> list = List.of(sampleEntryWithDest);
        when(communityEntryRepository.findAllPublicByPopularity(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(list, PageRequest.of(0, 10), 1));
        when(communityEntryTagRepository.findByCommunityEntry(sampleEntryWithDest)).thenReturn(tagLinks);

        Map<String, Object> res = communityService.getPublicCommunityEntriesWithSort("popularity", 0, 10);
        assertEquals("popularity", res.get("sortBy"));
        assertEquals(1L, res.get("totalElements"));
    }

    @Test
    @DisplayName("getPublicCommunityEntriesWithSort ‑ default time branch")
    void getPublicCommunityEntriesWithSort_time() {
        List<CommunityEntry> list = List.of(sampleEntryNoDest);
        when(communityEntryRepository.findAllPublicByTime(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(list, PageRequest.of(0, 10), 1));
        when(communityEntryTagRepository.findByCommunityEntry(sampleEntryNoDest)).thenReturn(Collections.emptyList());

        Map<String, Object> res = communityService.getPublicCommunityEntriesWithSort("time", 0, 10);
        assertEquals("time", res.get("sortBy"));
        // destination else branch still hit due to sampleEntryNoDest
        List<?> contentList = (List<?>) res.get("content");
        Map<String, Object> firstEntry = (Map<String, Object>) contentList.get(0);
        Map<String, Object> itineraryMap = (Map<String, Object>) firstEntry.get("itinerary");
        assertEquals("待规划目的地", itineraryMap.get("destination"));
    }

    @Test
    @DisplayName("getPublicCommunityEntriesWithSort ‑ entry without itinerary")
    void getPublicCommunityEntriesWithSort_noItinerary() {
        CommunityEntry entry = new CommunityEntry();
        entry.setId(707L);
        entry.setShareCode("SC707");
        entry.setDescription("d");
        entry.setViewCount(0);
        entry.setCreatedAt(LocalDateTime.now());

        when(communityEntryRepository.findAllPublicByPopularity(PageRequest.of(0, 5)))
                .thenReturn(new PageImpl<>(Collections.singletonList(entry), PageRequest.of(0, 5), 1));
        when(communityEntryTagRepository.findByCommunityEntry(entry)).thenReturn(Collections.emptyList());

        Map<String, Object> res = communityService.getPublicCommunityEntriesWithSort("popularity", 0, 5);
        Map<String, Object> first = (Map<String, Object>) ((List<?>) res.get("content")).get(0);
        assertFalse(first.containsKey("itinerary"));
    }

    @Test
    @DisplayName("getCommunityEntryByShareCode ‑ user null branch")
    void getCommunityEntryByShareCode_userNull() {
        // Build itinerary without user
        Itinerary itin = new Itinerary();
        itin.setId(909L);
        itin.setUser(null);
        itin.setTitle("T");
        itin.setStartDate(LocalDate.now());
        itin.setEndDate(LocalDate.now());

        CommunityEntry entry = new CommunityEntry();
        entry.setId(909L);
        entry.setShareCode("SC909");
        entry.setDescription("d");
        entry.setViewCount(0);
        entry.setCreatedAt(LocalDateTime.now());
        entry.setItinerary(itin);

        ItineraryDTO dto = new ItineraryDTO();
        dto.setId(itin.getId());
        dto.setTitle("T");
        dto.setDestinationNames(Collections.emptyList());
        dto.setPermissionStatus(Itinerary.PermissionStatus.所有人可见);
        when(itineraryConverter.toDTO(itin)).thenReturn(dto);

        CommunityEntryTag linkNull = new CommunityEntryTag();
        linkNull.setCommunityEntry(entry);
        linkNull.setTag(null);

        when(communityEntryRepository.findByShareCode("SC909"))
                .thenReturn(Optional.of(entry));
        when(communityEntryTagRepository.findByCommunityEntry(entry)).thenReturn(Collections.singletonList(linkNull));

        Map<String, Object> res = communityService.getCommunityEntryByShareCode("SC909");
        Map<String, Object> itineraryMap = (Map<String, Object>) res.get("itinerary");
        assertFalse(itineraryMap.containsKey("user"));
        List<?> tags = (List<?>) res.get("tags");
        assertTrue(tags.isEmpty());
    }

    @Test
    @DisplayName("searchCommunityEntries ‑ user null branch")
    void searchCommunityEntries_userNull() {
        Itinerary itin = new Itinerary();
        itin.setId(808L);
        itin.setUser(null);
        itin.setTitle("S");
        itin.setStartDate(LocalDate.now());
        itin.setEndDate(LocalDate.now());

        CommunityEntry entry = new CommunityEntry();
        entry.setId(808L);
        entry.setShareCode("SC808");
        entry.setDescription("d");
        entry.setViewCount(0);
        entry.setCreatedAt(LocalDateTime.now());
        entry.setItinerary(itin);

        ItineraryDTO dto = new ItineraryDTO();
        dto.setId(itin.getId());
        dto.setTitle("S");
        dto.setDestinationNames(Collections.emptyList());
        dto.setPermissionStatus(Itinerary.PermissionStatus.所有人可见);
        when(itineraryConverter.toDTO(itin)).thenReturn(dto);

        CommunityEntryTag linkNull = new CommunityEntryTag();
        linkNull.setCommunityEntry(entry);
        linkNull.setTag(null);

        when(communityEntryRepository.searchPublicEntries("nu"))
                .thenReturn(Collections.singletonList(entry));
        when(communityEntryTagRepository.findByCommunityEntry(entry))
                .thenReturn(Collections.singletonList(linkNull));

        List<Map<String, Object>> list = communityService.searchCommunityEntries("nu");
        Map<String, Object> itineraryMap = (Map<String, Object>) list.get(0).get("itinerary");
        assertFalse(itineraryMap.containsKey("user"));
        List<?> tags = (List<?>) list.get(0).get("tags");
        assertTrue(tags.isEmpty());
    }

    @Test
    @DisplayName("searchCommunityEntriesByDestination ‑ user null branch")
    void searchCommunityEntriesByDestination_userNull() {
        Itinerary itin = new Itinerary();
        itin.setId(818L);
        itin.setUser(null);
        itin.setTitle("D");
        itin.setStartDate(LocalDate.now());
        itin.setEndDate(LocalDate.now());

        CommunityEntry entry = new CommunityEntry();
        entry.setId(818L);
        entry.setShareCode("SC818");
        entry.setDescription("d");
        entry.setViewCount(0);
        entry.setCreatedAt(LocalDateTime.now());
        entry.setItinerary(itin);

        ItineraryDTO dto = new ItineraryDTO();
        dto.setId(itin.getId());
        dto.setTitle("D");
        dto.setDestinationNames(Collections.emptyList());
        dto.setPermissionStatus(Itinerary.PermissionStatus.所有人可见);
        when(itineraryConverter.toDTO(itin)).thenReturn(dto);

        CommunityEntryTag linkNull = new CommunityEntryTag();
        linkNull.setCommunityEntry(entry);
        linkNull.setTag(null);

        when(communityEntryRepository.countByDestination("y")).thenReturn(1L);
        when(communityEntryRepository.findByDestination("y", 0, 10)).thenReturn(Collections.singletonList(entry));
        when(communityEntryTagRepository.findByCommunityEntry(entry)).thenReturn(Collections.singletonList(linkNull));

        Map<String, Object> res = communityService.searchCommunityEntriesByDestination("y", 0, 10);
        Map<String, Object> first = (Map<String, Object>) ((List<?>) res.get("content")).get(0);
        Map<String, Object> itineraryMap = (Map<String, Object>) first.get("itinerary");
        assertFalse(itineraryMap.containsKey("user"));
        List<?> tags = (List<?>) first.get("tags");
        assertTrue(tags.isEmpty());
    }

    @Test
    @DisplayName("getPublicCommunityEntriesWithSort ‑ user null branch")
    void getPublicCommunityEntriesWithSort_userNull() {
        Itinerary itin = new Itinerary();
        itin.setId(828L);
        itin.setUser(null);
        itin.setTitle("G");
        itin.setStartDate(LocalDate.now());
        itin.setEndDate(LocalDate.now());

        CommunityEntry entry = new CommunityEntry();
        entry.setId(828L);
        entry.setShareCode("SC828");
        entry.setDescription("d");
        entry.setViewCount(0);
        entry.setCreatedAt(LocalDateTime.now());
        entry.setItinerary(itin);

        ItineraryDTO dto = new ItineraryDTO();
        dto.setId(itin.getId());
        dto.setTitle("G");
        dto.setDestinationNames(Collections.emptyList());
        dto.setPermissionStatus(Itinerary.PermissionStatus.所有人可见);
        when(itineraryConverter.toDTO(itin)).thenReturn(dto);

        CommunityEntryTag linkNull = new CommunityEntryTag();
        linkNull.setCommunityEntry(entry);
        linkNull.setTag(null);

        when(communityEntryRepository.findAllPublicByTime(PageRequest.of(0, 5)))
                .thenReturn(new PageImpl<>(Collections.singletonList(entry), PageRequest.of(0, 5), 1));
        when(communityEntryTagRepository.findByCommunityEntry(entry)).thenReturn(Collections.singletonList(linkNull));

        Map<String, Object> res = communityService.getPublicCommunityEntriesWithSort("time", 0, 5);
        Map<String, Object> first = (Map<String, Object>) ((List<?>) res.get("content")).get(0);
        Map<String, Object> itineraryMap = (Map<String, Object>) first.get("itinerary");
        assertFalse(itineraryMap.containsKey("user"));
        List<?> tags = (List<?>) first.get("tags");
        assertTrue(tags.isEmpty());
    }
} 