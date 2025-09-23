package com.se_07.backend.service;

import com.se_07.backend.dto.CreateTravelGroupRequest;
import com.se_07.backend.dto.TravelGroupDTO;
import com.se_07.backend.entity.*;
import com.se_07.backend.repository.*;
import com.se_07.backend.service.impl.TravelGroupServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TravelGroupServiceTest {

    @Mock
    private TravelGroupRepository travelGroupRepository;

    @Mock
    private TravelGroupMemberRepository memberRepository;

    @Mock
    private TravelGroupApplicationRepository applicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DestinationRepository destinationRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private GroupChatManageService groupChatManageService;

    @InjectMocks
    private TravelGroupServiceImpl travelGroupService;

    private User testUser;
    private Destination testDestination;
    private TravelGroup testGroup;
    private CreateTravelGroupRequest createRequest;
    private TravelGroupMember testMember;
    private TravelGroupApplication testApplication;

    @BeforeEach
    void setUp() {
        // 设置测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setAvatarUrl("avatar.jpg");
        testUser.setSignature("test signature");

        // 设置测试目的地
        testDestination = new Destination();
        testDestination.setId(1L);
        testDestination.setName("上海");
        testDestination.setDescription("魔都");
        testDestination.setImageUrl("shanghai.jpg");

        // 设置测试组团
        testGroup = new TravelGroup();
        testGroup.setId(1L);
        testGroup.setTitle("测试组团");
        testGroup.setDescription("测试描述");
        testGroup.setStartDate(LocalDate.now().plusDays(7));
        testGroup.setEndDate(LocalDate.now().plusDays(14));
        testGroup.setMaxMembers(6);
        testGroup.setCurrentMembers(1);
        testGroup.setEstimatedBudget(new BigDecimal("5000"));
        testGroup.setGroupType(TravelGroup.GroupType.自由行);
        testGroup.setStatus(TravelGroup.GroupStatus.招募中);
        testGroup.setIsPublic(true);
        testGroup.setCreator(testUser);
        testGroup.setDestination(testDestination);
        testGroup.setCreatedAt(LocalDateTime.now());
        testGroup.setUpdatedAt(LocalDateTime.now());
        testGroup.setGroupChatId(1L);
        testGroup.setTravelTags(new ArrayList<>()); // 初始化空的标签列表

        // 设置测试成员
        testMember = new TravelGroupMember();
        testMember.setId(1L);
        testMember.setGroup(testGroup);
        testMember.setUser(testUser);
        testMember.setRole(TravelGroupMember.MemberRole.创建者);
        testMember.setJoinStatus(TravelGroupMember.JoinStatus.已加入);
        testMember.setJoinDate(LocalDateTime.now());

        // 设置测试申请
        testApplication = new TravelGroupApplication();
        testApplication.setId(1L);
        testApplication.setGroup(testGroup);
        testApplication.setApplicant(testUser);
        testApplication.setMessage("我想加入");
        testApplication.setStatus(TravelGroupApplication.ApplicationStatus.待审核);
        testApplication.setApplyDate(LocalDateTime.now());

        // 设置创建请求
        createRequest = new CreateTravelGroupRequest();
        createRequest.setTitle("测试组团");
        createRequest.setDescription("测试描述");
        createRequest.setMaxMembers(6);
        createRequest.setStartDate(LocalDate.now().plusDays(7));
        createRequest.setEndDate(LocalDate.now().plusDays(14));
        createRequest.setEstimatedBudget(new BigDecimal("5000"));
        createRequest.setGroupType("自由行");
        createRequest.setIsPublic(true);
        createRequest.setDestinationId(1L);
        createRequest.setTravelTags(Arrays.asList("美食", "文化"));
    }

    @Test
    void testCreateTravelGroup_Success() {
        // 准备
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(testDestination));
        when(travelGroupRepository.save(any(TravelGroup.class))).thenReturn(testGroup);
        when(memberRepository.save(any(TravelGroupMember.class))).thenReturn(testMember);
        when(groupChatManageService.createGroup(anyString(), anyLong())).thenReturn(1L);
        when(tagRepository.findByTagIn(anyList())).thenReturn(Arrays.asList(new Tag(), new Tag()));
        when(memberRepository.findByGroupId(1L)).thenReturn(Arrays.asList(testMember));

        // 执行
        TravelGroupDTO result = travelGroupService.createTravelGroup(createRequest, 1L);

        // 验证
        assertNotNull(result);
        assertEquals("测试组团", result.getTitle());
        verify(travelGroupRepository, times(3)).save(any(TravelGroup.class));
        verify(memberRepository).save(any(TravelGroupMember.class));
        verify(groupChatManageService).createGroup(anyString(), anyLong());
    }

    @Test
    void testCreateTravelGroup_EmptyTitle() {
        // 准备
        createRequest.setTitle("");

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.createTravelGroup(createRequest, 1L));
        assertTrue(exception.getMessage().contains("标题不能为空"));
    }

    @Test
    void testCreateTravelGroup_NullTitle() {
        // 准备
        createRequest.setTitle(null);

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.createTravelGroup(createRequest, 1L));
        assertTrue(exception.getMessage().contains("标题不能为空"));
    }

    @Test
    void testCreateTravelGroup_InvalidMaxMembers() {
        // 准备
        createRequest.setMaxMembers(1);

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.createTravelGroup(createRequest, 1L));
        assertTrue(exception.getMessage().contains("成员数量必须大于等于2"));
    }

    @Test
    void testCreateTravelGroup_NullDates() {
        // 准备
        createRequest.setStartDate(null);
        createRequest.setEndDate(null);

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.createTravelGroup(createRequest, 1L));
        assertTrue(exception.getMessage().contains("开始日期和结束日期不能为空"));
    }

    @Test
    void testCreateTravelGroup_InvalidDateRange() {
        // 准备
        createRequest.setStartDate(LocalDate.now().plusDays(14));
        createRequest.setEndDate(LocalDate.now().plusDays(7));

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.createTravelGroup(createRequest, 1L));
        assertTrue(exception.getMessage().contains("开始日期不能晚于结束日期"));
    }

    @Test
    void testCreateTravelGroup_NullDestination() {
        // 准备
        createRequest.setDestinationId(null);

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.createTravelGroup(createRequest, 1L));
        assertTrue(exception.getMessage().contains("目的地不能为空"));
    }

    @Test
    void testCreateTravelGroup_UserNotFound() {
        // 准备
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.createTravelGroup(createRequest, 1L));
        assertTrue(exception.getMessage().contains("用户不存在"));
    }

    @Test
    void testCreateTravelGroup_DestinationNotFound() {
        // 准备
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(destinationRepository.findById(1L)).thenReturn(Optional.empty());

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.createTravelGroup(createRequest, 1L));
        assertTrue(exception.getMessage().contains("目的地不存在"));
    }

    @Test
    void testGetPublicRecruitingGroups() {
        // 准备
        List<TravelGroup> groups = Arrays.asList(testGroup);
        when(travelGroupRepository.findAll()).thenReturn(groups);
        when(memberRepository.countByGroupAndJoinStatus(any(TravelGroup.class), any(TravelGroupMember.JoinStatus.class)))
                .thenReturn(1L);
        when(memberRepository.findByGroupId(1L)).thenReturn(Arrays.asList(testMember));

        // 执行
        List<TravelGroupDTO> result = travelGroupService.getPublicRecruitingGroups(1L);

        // 验证
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("测试组团", result.get(0).getTitle());
    }

    @Test
    void testGetPublicRecruitingGroups_NotPublic() {
        // 准备
        testGroup.setIsPublic(false);
        List<TravelGroup> groups = Arrays.asList(testGroup);
        when(travelGroupRepository.findAll()).thenReturn(groups);

        // 执行
        List<TravelGroupDTO> result = travelGroupService.getPublicRecruitingGroups(1L);

        // 验证
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testGetPublicRecruitingGroups_NotRecruiting() {
        // 准备
        testGroup.setStatus(TravelGroup.GroupStatus.已满员);
        List<TravelGroup> groups = Arrays.asList(testGroup);
        when(travelGroupRepository.findAll()).thenReturn(groups);

        // 执行
        List<TravelGroupDTO> result = travelGroupService.getPublicRecruitingGroups(1L);

        // 验证
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testGetPublicRecruitingGroups_Full() {
        // 准备
        List<TravelGroup> groups = Arrays.asList(testGroup);
        when(travelGroupRepository.findAll()).thenReturn(groups);
        when(memberRepository.countByGroupAndJoinStatus(any(TravelGroup.class), any(TravelGroupMember.JoinStatus.class)))
                .thenReturn(6L); // 已满员

        // 执行
        List<TravelGroupDTO> result = travelGroupService.getPublicRecruitingGroups(1L);

        // 验证
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testGetPublicRecruitingGroupsWithSearch() {
        // 准备
        List<TravelGroup> groups = Arrays.asList(testGroup);
        when(travelGroupRepository.findAll()).thenReturn(groups);
        when(memberRepository.countByGroupAndJoinStatus(any(TravelGroup.class), any(TravelGroupMember.JoinStatus.class)))
                .thenReturn(1L);
        when(memberRepository.findByGroupId(1L)).thenReturn(Arrays.asList(testMember));

        // 执行
        List<TravelGroupDTO> result = travelGroupService.getPublicRecruitingGroupsWithSearch(1L, "测试", "groupName", null, null);

        // 验证
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetPublicRecruitingGroupsWithSearch_ByCreator() {
        // 准备
        List<TravelGroup> groups = Arrays.asList(testGroup);
        when(travelGroupRepository.findAll()).thenReturn(groups);
        when(memberRepository.countByGroupAndJoinStatus(any(TravelGroup.class), any(TravelGroupMember.JoinStatus.class)))
                .thenReturn(1L);
        when(memberRepository.findByGroupId(1L)).thenReturn(Arrays.asList(testMember));

        // 执行
        List<TravelGroupDTO> result = travelGroupService.getPublicRecruitingGroupsWithSearch(1L, "testuser", "creator", null, null);

        // 验证
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetPublicRecruitingGroupsWithSearch_ByDestination() {
        // 准备
        List<TravelGroup> groups = Arrays.asList(testGroup);
        when(travelGroupRepository.findAll()).thenReturn(groups);
        when(memberRepository.countByGroupAndJoinStatus(any(TravelGroup.class), any(TravelGroupMember.JoinStatus.class)))
                .thenReturn(1L);
        when(memberRepository.findByGroupId(1L)).thenReturn(Arrays.asList(testMember));

        // 执行
        List<TravelGroupDTO> result = travelGroupService.getPublicRecruitingGroupsWithSearch(1L, "上海", "destination", null, null);

        // 验证
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetPublicRecruitingGroupsWithSearch_DateFilter() {
        // 准备
        List<TravelGroup> groups = Arrays.asList(testGroup);
        when(travelGroupRepository.findAll()).thenReturn(groups);
        when(memberRepository.countByGroupAndJoinStatus(any(TravelGroup.class), any(TravelGroupMember.JoinStatus.class)))
                .thenReturn(1L);
        when(memberRepository.findByGroupId(1L)).thenReturn(Arrays.asList(testMember));

        // 执行
        List<TravelGroupDTO> result = travelGroupService.getPublicRecruitingGroupsWithSearch(1L, null, null, 
                LocalDate.now().plusDays(5).toString(), LocalDate.now().plusDays(15).toString());

        // 验证
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetGroupsByDestination() {
        // 准备
        List<TravelGroup> groups = Arrays.asList(testGroup);
        when(travelGroupRepository.findByStatusOrderByCreatedAtDesc(any(TravelGroup.GroupStatus.class)))
                .thenReturn(groups);
        when(memberRepository.countByGroupAndJoinStatus(any(TravelGroup.class), any(TravelGroupMember.JoinStatus.class)))
                .thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // 执行
        List<TravelGroupDTO> result = travelGroupService.getGroupsByDestination(1L, 1L);

        // 验证
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetUserCreatedGroups() {
        // 准备
        List<TravelGroup> groups = Arrays.asList(testGroup);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(travelGroupRepository.findByCreatorId(1L)).thenReturn(groups);
        when(memberRepository.findByGroupId(1L)).thenReturn(Arrays.asList(testMember));

        // 执行
        List<TravelGroupDTO> result = travelGroupService.getUserCreatedGroups(1L);

        // 验证
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetUserCreatedGroups_UserNotFound() {
        // 准备
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.getUserCreatedGroups(1L));
        assertTrue(exception.getMessage().contains("用户不存在"));
    }

    @Test
    void testGetUserJoinedGroups() {
        // 准备
        List<TravelGroupMember> memberships = Arrays.asList(testMember);
        when(memberRepository.findByUserId(1L)).thenReturn(memberships);
        when(memberRepository.findByGroupId(1L)).thenReturn(Arrays.asList(testMember));

        // 执行
        List<TravelGroupDTO> result = travelGroupService.getUserJoinedGroups(1L);

        // 验证
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetGroupDetail() {
        // 准备
        when(travelGroupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(memberRepository.findByGroupId(1L)).thenReturn(Arrays.asList(testMember));

        // 执行
        TravelGroupDTO result = travelGroupService.getGroupDetail(1L, 1L);

        // 验证
        assertNotNull(result);
        assertEquals("测试组团", result.getTitle());
    }

    @Test
    void testGetGroupDetail_GroupNotFound() {
        // 准备
        when(travelGroupRepository.findById(1L)).thenReturn(Optional.empty());

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.getGroupDetail(1L, 1L));
        assertTrue(exception.getMessage().contains("组团不存在"));
    }

    @Test
    void testProcessApplication_Success() {
        // 准备
        when(travelGroupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(memberRepository.save(any(TravelGroupMember.class))).thenReturn(testMember);

        // 执行
        travelGroupService.processApplication(1L, 1L, 1L, true);

        // 验证
        verify(memberRepository).save(any(TravelGroupMember.class));
        verify(travelGroupRepository).save(any(TravelGroup.class));
        verify(applicationRepository).save(any(TravelGroupApplication.class));
    }

    @Test
    void testProcessApplication_NotCreator() {
        // 准备
        when(travelGroupRepository.findById(1L)).thenReturn(Optional.of(testGroup));

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.processApplication(1L, 1L, 2L, true));
        assertTrue(exception.getMessage().contains("只有组团创建者可以处理申请"));
    }

    @Test
    void testProcessApplication_ApplicationNotFound() {
        // 准备
        when(travelGroupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(applicationRepository.findById(1L)).thenReturn(Optional.empty());

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.processApplication(1L, 1L, 1L, true));
        assertTrue(exception.getMessage().contains("申请不存在"));
    }

    @Test
    void testProcessApplication_WrongGroup() {
        // 准备
        TravelGroup otherGroup = new TravelGroup();
        otherGroup.setId(2L);
        testApplication.setGroup(otherGroup);
        
        when(travelGroupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.processApplication(1L, 1L, 1L, true));
        assertTrue(exception.getMessage().contains("申请不属于该组团"));
    }

    @Test
    void testProcessApplication_AlreadyProcessed() {
        // 准备
        testApplication.setStatus(TravelGroupApplication.ApplicationStatus.已同意);
        when(travelGroupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.processApplication(1L, 1L, 1L, true));
        assertTrue(exception.getMessage().contains("该申请已被处理"));
    }

    @Test
    void testProcessApplication_GroupFull() {
        // 准备
        testGroup.setCurrentMembers(6);
        testGroup.setMaxMembers(6);
        when(travelGroupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.processApplication(1L, 1L, 1L, true));
        assertTrue(exception.getMessage().contains("组团已满员"));
    }

    @Test
    void testWithdrawApplication_Success() {
        // 准备
        when(applicationRepository.findByGroupIdAndApplicantIdAndStatus(1L, 1L, TravelGroupApplication.ApplicationStatus.待审核))
                .thenReturn(Optional.of(testApplication));

        // 执行
        travelGroupService.withdrawApplication(1L, 1L);

        // 验证
        verify(applicationRepository).delete(testApplication);
    }

    @Test
    void testWithdrawApplication_NotFound() {
        // 准备
        when(applicationRepository.findByGroupIdAndApplicantIdAndStatus(1L, 1L, TravelGroupApplication.ApplicationStatus.待审核))
                .thenReturn(Optional.empty());

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.withdrawApplication(1L, 1L));
        assertTrue(exception.getMessage().contains("申请不存在"));
    }

    @Test
    void testWithdrawApplication_AlreadyProcessed() {
        // 准备
        testApplication.setStatus(TravelGroupApplication.ApplicationStatus.已同意);
        when(applicationRepository.findByGroupIdAndApplicantIdAndStatus(1L, 1L, TravelGroupApplication.ApplicationStatus.待审核))
                .thenReturn(Optional.of(testApplication));

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.withdrawApplication(1L, 1L));
        assertTrue(exception.getMessage().contains("该申请已被处理,无法撤回"));
    }

    @Test
    void testLeaveGroup_Success() {
        // 准备
        testMember.setRole(TravelGroupMember.MemberRole.成员);
        when(travelGroupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(memberRepository.findByGroupIdAndUserId(1L, 1L)).thenReturn(Optional.of(testMember));

        // 执行
        travelGroupService.leaveGroup(1L, 1L);

        // 验证
        verify(memberRepository).delete(testMember);
        verify(travelGroupRepository).save(any(TravelGroup.class));
    }

    @Test
    void testLeaveGroup_GroupNotFound() {
        // 准备
        when(travelGroupRepository.findById(1L)).thenReturn(Optional.empty());

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.leaveGroup(1L, 1L));
        assertTrue(exception.getMessage().contains("组团不存在"));
    }

    @Test
    void testLeaveGroup_NotMember() {
        // 准备
        when(travelGroupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(memberRepository.findByGroupIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.leaveGroup(1L, 1L));
        assertTrue(exception.getMessage().contains("您不是该组团的成员"));
    }

    @Test
    void testLeaveGroup_CreatorCannotLeave() {
        // 准备
        when(travelGroupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(memberRepository.findByGroupIdAndUserId(1L, 1L)).thenReturn(Optional.of(testMember));

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.leaveGroup(1L, 1L));
        assertTrue(exception.getMessage().contains("创建者不能退出组团"));
    }

    @Test
    void testCancelGroup_Success() {
        // 准备
        when(travelGroupRepository.findById(1L)).thenReturn(Optional.of(testGroup));

        // 执行
        travelGroupService.cancelGroup(1L, 1L);

        // 验证
        verify(travelGroupRepository).save(any(TravelGroup.class));
    }

    @Test
    void testCancelGroup_NotCreator() {
        // 准备
        when(travelGroupRepository.findById(1L)).thenReturn(Optional.of(testGroup));

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.cancelGroup(1L, 2L));
        assertTrue(exception.getMessage().contains("只有创建者可以取消组团"));
    }

    @Test
    void testCancelGroup_NotRecruiting() {
        // 准备
        testGroup.setStatus(TravelGroup.GroupStatus.已满员);
        when(travelGroupRepository.findById(1L)).thenReturn(Optional.of(testGroup));

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.cancelGroup(1L, 1L));
        assertTrue(exception.getMessage().contains("只能取消招募中的组团"));
    }

    @Test
    void testUpdateGroupStatus_Success() {
        // 准备
        testGroup.setStatus(TravelGroup.GroupStatus.已满员); // 设置为已满员状态，才能转换为已结束
        testGroup.setTravelTags(new ArrayList<>()); // 保证不为null
        testGroup.setCreator(testUser); // 保证不为null
        testGroup.setDestination(testDestination); // 保证不为null
        when(travelGroupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(travelGroupRepository.save(any(TravelGroup.class))).thenReturn(testGroup); // 确保save返回testGroup
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // 执行
        TravelGroupDTO result = travelGroupService.updateGroupStatus(1L, "已结束", 1L);

        // 验证
        assertNotNull(result);
        verify(travelGroupRepository).save(any(TravelGroup.class));
    }

    @Test
    void testUpdateGroupStatus_NotCreator() {
        // 准备
        when(travelGroupRepository.findById(1L)).thenReturn(Optional.of(testGroup));

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.updateGroupStatus(1L, "已结束", 2L));
        assertTrue(exception.getMessage().contains("只有创建者可以更新组团状态"));
    }

    @Test
    void testUpdateGroupStatus_InvalidTransition() {
        // 准备
        when(travelGroupRepository.findById(1L)).thenReturn(Optional.of(testGroup));

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.updateGroupStatus(1L, "招募中", 1L));
        assertTrue(exception.getMessage().contains("不允许的状态转换"));
    }

    @Test
    void testApplyToJoinGroup_Success() {
        // 准备
        when(travelGroupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(memberRepository.findByGroupIdAndUserId(1L, 1L)).thenReturn(Optional.empty());
        when(applicationRepository.findByGroupIdAndApplicantIdAndStatus(eq(1L), eq(1L), any(TravelGroupApplication.ApplicationStatus.class)))
                .thenReturn(Optional.empty());
        when(applicationRepository.save(any(TravelGroupApplication.class))).thenReturn(testApplication);

        // 执行
        travelGroupService.applyToJoinGroup(1L, 1L, "我想加入");

        // 验证
        verify(applicationRepository).save(any(TravelGroupApplication.class));
    }

    @Test
    void testApplyToJoinGroup_AlreadyMember() {
        // 准备
        when(travelGroupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(memberRepository.findByGroupIdAndUserId(1L, 1L)).thenReturn(Optional.of(testMember));

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.applyToJoinGroup(1L, 1L, "我想加入"));
        assertTrue(exception.getMessage().contains("您已经是该组团成员"));
    }

    @Test
    void testApplyToJoinGroup_PendingApplication() {
        // 准备
        when(travelGroupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(memberRepository.findByGroupIdAndUserId(1L, 1L)).thenReturn(Optional.empty());
        when(applicationRepository.findByGroupIdAndApplicantIdAndStatus(eq(1L), eq(1L), eq(TravelGroupApplication.ApplicationStatus.待审核)))
                .thenReturn(Optional.of(testApplication));

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.applyToJoinGroup(1L, 1L, "我想加入"));
        assertTrue(exception.getMessage().contains("您已经申请过该组团，请等待审核结果"));
    }

    @Test
    void testApplyToJoinGroup_ApprovedApplication() {
        // 准备
        when(travelGroupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(memberRepository.findByGroupIdAndUserId(1L, 1L)).thenReturn(Optional.empty());
        when(applicationRepository.findByGroupIdAndApplicantIdAndStatus(eq(1L), eq(1L), eq(TravelGroupApplication.ApplicationStatus.待审核)))
                .thenReturn(Optional.empty());
        when(applicationRepository.findByGroupIdAndApplicantIdAndStatus(eq(1L), eq(1L), eq(TravelGroupApplication.ApplicationStatus.已同意)))
                .thenReturn(Optional.of(testApplication));

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.applyToJoinGroup(1L, 1L, "我想加入"));
        assertTrue(exception.getMessage().contains("您的申请已被同意，请检查是否已经是成员"));
    }

    @Test
    void testHandleApplication_Success() {
        // 准备
        when(travelGroupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(memberRepository.save(any(TravelGroupMember.class))).thenReturn(testMember);

        // 执行
        travelGroupService.handleApplication(1L, 1L, 1L, true);

        // 验证
        verify(memberRepository).save(any(TravelGroupMember.class));
        verify(travelGroupRepository).save(any(TravelGroup.class));
        verify(applicationRepository).save(any(TravelGroupApplication.class));
    }

    @Test
    void testHandleApplication_Reject() {
        // 准备
        when(travelGroupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

        // 执行
        travelGroupService.handleApplication(1L, 1L, 1L, false);

        // 验证
        verify(applicationRepository).save(any(TravelGroupApplication.class));
    }

    @Test
    void testGetGroupApplications_Success() {
        // 准备
        when(memberRepository.findByGroupIdAndUserId(1L, 1L)).thenReturn(Optional.of(testMember));
        when(applicationRepository.findByGroupId(1L)).thenReturn(Arrays.asList(testApplication));

        // 执行
        List<Map<String, Object>> result = travelGroupService.getGroupApplications(1L, 1L);

        // 验证
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).get("id"));
    }

    @Test
    void testGetGroupApplications_NotAuthorized() {
        // 准备
        when(memberRepository.findByGroupIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.getGroupApplications(1L, 1L));
        assertTrue(exception.getMessage().contains("无权查看"));
    }

    @Test
    void testGetGroupApplications_NotCreatorOrAdmin() {
        // 准备
        testMember.setRole(TravelGroupMember.MemberRole.成员);
        when(memberRepository.findByGroupIdAndUserId(1L, 1L)).thenReturn(Optional.of(testMember));

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.getGroupApplications(1L, 1L));
        assertTrue(exception.getMessage().contains("无权查看"));
    }

    @Test
    void testGetRecommendedGroups() {
        // 准备
        List<TravelGroup> groups = Arrays.asList(testGroup);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(travelGroupRepository.findAll()).thenReturn(groups);
        when(memberRepository.findByGroupId(1L)).thenReturn(Arrays.asList(testMember));

        // 执行
        List<TravelGroupDTO> result = travelGroupService.getRecommendedGroups(1L);

        // 验证
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetRecommendedGroups_UserNotFound() {
        // 准备
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.getRecommendedGroups(1L));
        assertTrue(exception.getMessage().contains("用户不存在"));
    }

    @Test
    void testGetRecommendationsByPreferences() {
        // 准备
        List<TravelGroup> groups = Arrays.asList(testGroup);
        when(travelGroupRepository.findAll()).thenReturn(groups);
        when(memberRepository.findByGroupId(1L)).thenReturn(Arrays.asList(testMember));

        // 执行
        List<TravelGroupDTO> result = travelGroupService.getRecommendationsByPreferences(1L, Arrays.asList("美食", "文化"));

        // 验证
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetUserStatusInGroup() {
        // 准备
        when(travelGroupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(memberRepository.findByGroupIdAndUserId(1L, 1L)).thenReturn(Optional.of(testMember));
        when(applicationRepository.findByGroupIdAndApplicantIdAndStatus(eq(1L), eq(1L), any(TravelGroupApplication.ApplicationStatus.class)))
                .thenReturn(Optional.empty());

        // 执行
        Map<String, Object> result = travelGroupService.getUserStatusInGroup(1L, 1L);

        // 验证
        assertNotNull(result);
        assertTrue((Boolean) result.get("isCreator"));
        assertTrue((Boolean) result.get("isMember"));
        assertFalse((Boolean) result.get("hasPendingApplication"));
    }

    @Test
    void testGetUserStatusInGroup_GroupNotFound() {
        // 准备
        when(travelGroupRepository.findById(1L)).thenReturn(Optional.empty());

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.getUserStatusInGroup(1L, 1L));
        assertTrue(exception.getMessage().contains("组团不存在"));
    }

    @Test
    void testRemoveUserFromGroup() {
        // 准备
        when(travelGroupRepository.findById(1L)).thenReturn(Optional.of(testGroup));

        // 执行
        travelGroupService.removeUserFromGroup(1L, 1L);

        // 验证
        verify(groupChatManageService).removeUserFromGroup(1L, 1L);
    }

    @Test
    void testRemoveUserFromGroup_GroupNotFound() {
        // 准备
        when(travelGroupRepository.findById(1L)).thenReturn(Optional.empty());

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> travelGroupService.removeUserFromGroup(1L, 1L));
        assertTrue(exception.getMessage().contains("组团不存在"));
    }

    @Test
    void testRemoveUserFromGroup_NoGroupChat() {
        // 准备
        testGroup.setGroupChatId(null);
        when(travelGroupRepository.findById(1L)).thenReturn(Optional.of(testGroup));

        // 执行
        travelGroupService.removeUserFromGroup(1L, 1L);

        // 验证
        verify(groupChatManageService, never()).removeUserFromGroup(anyLong(), anyLong());
    }
} 