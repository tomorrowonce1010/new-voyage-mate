import React, { useState, useEffect, useRef } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { FriendList, GroupList } from '../components/ChatList';
import ChatMain from '../components/ChatMain';
import FriendChatMain from '../components/FriendChatMain';
import GroupChatMain from '../components/GroupChatMain';
import './Chat.css';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';


function UserDropdown({ results, loading, onView, onAddFriend, addingId, visible, onClose, friendIds, myId }) {
  const ref = useRef();
  useEffect(() => {
    if (!visible) return;
    const handler = (e) => {
      if (ref.current && !ref.current.contains(e.target)) onClose();
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [visible, onClose]);
  if (!visible) return null;
  return (
    <div className="user-dropdown" ref={ref}>
      {loading ? <div className="user-dropdown-item">Searching...</div> :
        results.length === 0 ? <div className="user-dropdown-item">No user found.</div> :
        results.map(user => {
          const isFriend = friendIds && friendIds.includes(user.id);
          const isSelf = myId && user.id === myId;
          return (
            <div className="user-dropdown-item" key={user.id}>
              <span className="avatar" style={{
                width: '32px',
                height: '32px',
                borderRadius: '50%',
                background: '#e3f0ff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '1.1rem',
                color: '#1976d2',
                overflow: 'hidden'
              }}>
                {user.avatar ? (
                  <img className="avatar-image"
                    src={user.avatar}
                    alt={user.username}
                    style={{
                      width: '100%',
                      height: '100%',
                      objectFit: 'cover'
                    }}
                    onError={(e) => {
                      e.target.style.display = 'none';
                      e.target.parentElement.textContent = user.username?.charAt(0) || 'U';
                    }}
                  />
                ) : (
                  user.username?.charAt(0) || 'U'
                )}
              </span>
              <span className="username">{user.username}</span>
              <button onClick={() => onView(user.id)}>View</button>
              <button
                onClick={() => onAddFriend(user.id)}
                disabled={addingId === user.id || isFriend || isSelf}
              >
                {isSelf ? 'Self' : isFriend ? 'Friend' : (addingId === user.id ? 'Adding...' : 'Add')}
              </button>
            </div>
          );
        })}
    </div>
  );
}

export default function Chat() {
  const location = useLocation();
  const navigate = useNavigate();
  const [tab, setTab] = useState('friend');
  const [current, setCurrent] = useState(null);
  const [input, setInput] = useState('');
  const [msgs, setMsgs] = useState([]);
  const [searchInput, setSearchInput] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [addingId, setAddingId] = useState(null);
  const [dropdownVisible, setDropdownVisible] = useState(false);
  const [friendList, setFriendList] = useState([]);
  const [friendLoading, setFriendLoading] = useState(false);
  const [myId, setMyId] = useState(null);
  const [showCreateGroup, setShowCreateGroup] = useState(false);
  const [newGroupName, setNewGroupName] = useState('');
  const [showAddUser, setShowAddUser] = useState(false);
  const [addUserId, setAddUserId] = useState('');
  const [groupMemberSearch, setGroupMemberSearch] = useState('');
  const [selectedGroupMembers, setSelectedGroupMembers] = useState([]);
  const searchTimeout = useRef();
  const wsUrl = 'http://localhost:8080/api/ws'; // WebSocket端点
  const stompClientRef = useRef(null);

  // 将 groupList 和 groupLoading useState 移到这里
  const [groupList, setGroupList] = useState([]);
  const [groupLoading, setGroupLoading] = useState(false);
  // 新增：所有群聊历史消息
  const [groupHistories, setGroupHistories] = useState({});

  // 1. 在Chat组件state中增加选中好友数组
  const [selectedFriends, setSelectedFriends] = useState([]);
  const [showRenameModal, setShowRenameModal] = useState(false);
  const [renameValue, setRenameValue] = useState('');

  // 2. 选择框切换
  const handleSelectFriend = (friend) => {
    console.log('[操作] 选中好友:', friend);
    setTab('friend');
    setCurrent(friend);
  };

  // 3. 删除好友
  const handleDeleteFriend = async (friendId) => {
    console.log('[操作] 删除好友:', friendId);
    if (!window.confirm('确定要删除该好友吗？')) return;
    if (!myId) return;
    const form = new URLSearchParams();
    form.append('userId', myId);
    form.append('friendId', friendId);
    // 路径改为 /api/friends/delete，Content-Type 明确指定
    const res = await fetch('http://localhost:8080/api/friends/delete', {
      method: 'POST',
      body: form,
      credentials: 'include',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      }
    });
    refreshFriends();
    // 删除后如果当前聊天对象就是被删好友，自动关闭聊天
    if (current && current.id === friendId) {
      setCurrent(null);
      setInput('');
      alert('你已删除该好友，聊天已关闭');
      console.log('[操作] 删除后自动关闭当前聊天');
    }
    // 本地模拟收到推送，立即刷新体验
    const fakePush = { type: 'friend_removed', userId: myId, friendId };
    console.log('[本地模拟推送] 收到好友变动消息:', fakePush);
  };

  useEffect(() => {
    if (location.pathname === '/chat/group') setTab('group');
    else setTab('friend');
  }, [location.pathname]);

  // 获取当前用户ID
  useEffect(() => {
    fetch('/api/auth/status', { credentials: 'include' })
      .then(res => res.ok ? res.json() : null)
      .then(data => {
        if (data && data.userId) setMyId(Number(data.userId)); // 强制转换为数字
      });
  }, []);

  // 抽出刷新好友列表的函数
  const refreshFriends = React.useCallback(() => {
    console.log('[操作] 刷新好友列表, tab:', tab, 'myId:', myId);
    if (tab !== 'friend' || !myId) return;
    setFriendLoading(true);
    const form = new URLSearchParams();
    form.append('userId', myId);
    fetch('http://localhost:8080/api/auth/friends/list', {
      method: 'POST',
      body: form,
      credentials: 'include', // 携带cookie
    })
      .then(res => res.ok ? res.json() : Promise.reject(res))
      .then(async (data) => {
        const ids = data && data.success && Array.isArray(data.friends) ? data.friends : [];
        if (ids.length === 0) {
          setFriendList([]);
          setFriendLoading(false);
          return;
        }
        // 批量获取用户信息
        const users = await Promise.all(ids.map(id =>
          fetch(`http://localhost:8080/api/users/homepage/${id}`, {
            credentials: 'include'
          })
            .then(r => r.ok ? r.json() : null)
            .catch(() => null)
        ));
        // 获取每个用户的profile信息
        const userProfiles = await Promise.all(users.filter(Boolean).map(u =>
          fetch(`/api/users/homepage/${u.id}`, { credentials: 'include' })
            .then(r => r.ok ? r.json() : null)
            .catch(() => null)
        ));

        setFriendList(users.filter(Boolean).map((u, index) => {
          const profile = userProfiles[index];
          return {
            id: u.id,
            name: u.username,
            avatar: (profile?.avatarUrl || profile?.avatar) ? `/api${profile?.avatarUrl || profile?.avatar}` : u.username?.charAt(0) || 'U',
            lastMsg: u.lastMsg || '',
            unread: u.unread || 0
          };
        }));
        setFriendLoading(false);
        console.log('[结果] 好友列表:', users.filter(Boolean));
      })
      .catch(() => {
        setFriendList([]);
        setFriendLoading(false);
        console.error('[错误] 获取好友列表失败');
      });
  }, [tab, myId]);

  // useEffect 里用 refreshFriends
  useEffect(() => {
    refreshFriends();
  }, [tab, myId, refreshFriends]);

  // 添加好友
  const handleAddFriend = async (userId) => {
    console.log('[操作] 添加好友:', userId);
    setAddingId(userId);
    try {
      if (!myId) return;
      const form = new URLSearchParams();
      form.append('userId', myId);
      form.append('friendId', userId);
      // 路径改为 /friends/add，Content-Type 明确指定
      const res = await fetch('http://localhost:8080/api/friends/add', {
        method: 'POST',
        body: form,
        credentials: 'include',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        }
      });
      if (res.ok) {
        const data = await res.json();
        if (data && data.success) {
          setSearchResults(results => results.map(u => u.id === userId ? { ...u, added: true } : u));
          refreshFriends(); // 添加好友成功后刷新好友列表
          console.log('[结果] 添加好友成功:', userId);
          // 本地模拟收到推送，立即刷新体验
          const fakePush = { type: 'friend_added', userId: myId, friendId: userId };
          console.log('[本地模拟推送] 收到好友变动消息:', fakePush);
          // 可选：如需立即高亮或切换聊天对象，可在此处处理
        }
      }
    } finally {
      setAddingId(null);
    }
  };

  // 修正查看好友主页功能，跳转到 /user-homepage/{userId}
  const handleViewUser = (user) => {
    const userId = typeof user === 'object' ? user.id : user;
    navigate(`/user-homepage/${userId}?from=chat`);
  };

  // 创建群聊
  async function createGroup(groupName, creatorUserId) {
    console.log('[操作] 创建群聊:', groupName, creatorUserId);
    const form = new URLSearchParams();
    form.append('groupName', groupName);
    form.append('creatorUserId', creatorUserId);
    const res = await fetch('http://localhost:8080/api/group/create', {
      method: 'POST',
      body: form,
      credentials: 'include'
    });
    const data = await res.json();
    if (data.success) {
      // 可选：刷新群聊列表
      console.log('[结果] 创建群聊成功:', data.groupId);
      return data.groupId;
    } else {
      console.error('[错误] 创建群聊失败:', data);
      throw new Error('创建群聊失败');
    }
  }

  // 拉人入群
  async function addUserToGroup(groupId, userId) {
    console.log('[操作] 拉人入群:', groupId, userId);
    const form = new URLSearchParams();
    form.append('groupId', groupId);
    form.append('userId', userId);
    const res = await fetch('http://localhost:8080/api/group/addUser', {
      method: 'POST',
      body: form,
      credentials: 'include'
    });
    const data = await res.json();
    if (!data.success) {
      console.error('[错误] 拉人入群失败:', data);
      throw new Error('拉人入群失败');
    }
    console.log('[结果] 拉人入群成功:', userId);
  }

  // 获取群聊列表
  const refreshGroups = React.useCallback(() => {
    console.log('[操作] 刷新群聊列表, tab:', tab, 'myId:', myId);
    if (tab !== 'group' || !myId) return;
    setGroupLoading(true);
    const form = new URLSearchParams();
    form.append('userId', myId);
    fetch('http://localhost:8080/api/group/listByUser', {
      method: 'POST',
      body: form,
      credentials: 'include',
    })
      .then(res => res.ok ? res.json() : Promise.reject(res))
      .then(data => {
        console.log('后端原始返回数据:', data);
        let groups = [];
        // 自动适配一层或两层数组
        if (Array.isArray(data)) {
          if (Array.isArray(data[0])) {
            groups = data[0].map(g => ({
              id: g.groupId,
              name: g.groupName,
              avatar: g.avatar || g.groupName?.charAt(0) || 'G',
              lastMsg: '',
              unread: 0
            }));
          } else if (data.length && data[0]?.groupId) {
            groups = data.map(g => ({
              id: g.groupId,
              name: g.groupName,
              avatar: g.avatar || g.groupName?.charAt(0) || 'G',
              lastMsg: '',
              unread: 0
            }));
          }
        }
        console.log(`[群聊] 用户ID:`, myId, '获取到群聊列表:', groups);
        setGroupList(groups);
        setGroupLoading(false);
      })
      .catch(() => {
        setGroupList([]);
        setGroupLoading(false);
        console.error('[错误] 获取群聊列表失败');
      });
  }, [tab, myId]);

  // useEffect 里用 refreshGroups
  useEffect(() => {
    refreshGroups();
  }, [tab, myId, refreshGroups]);

  // 监听tab和groupList变化，切换到group时自动选中第一个群聊
  useEffect(() => {
    if (tab === 'group' && groupList.length > 0) {
      setCurrent(groupList[0]);
    }
  }, [tab, groupList]);

  // 每次进入tab==='friend'时，强制current设为null，要求用户手动选择好友
  useEffect(() => {
    if (tab === 'friend') {
      setCurrent(null);
    }
  }, [tab]);

  const list = tab === 'friend' ? friendList : groupList;
  // 发送消息（WebSocket）
  const handleSend = () => {
    if (!input.trim() || !current) return;
    if (tab === 'group') {
      // 群聊消息
      const msg = {
        groupId: current.id,
        userId: myId,
        content: input,
        // messageTime由后端生成
      };
      if (stompClientRef.current && stompClientRef.current.connected) {
        console.log('[操作] 发送群聊消息:', msg);
        stompClientRef.current.publish({
          destination: '/app/groupchat',
          body: JSON.stringify(msg),
        });
        setInput('');
      }
    } else {
      // 私聊消息
      const msg = {
        fromId: myId,
        toId: current.id,
        content: input,
        // messageTime由后端生成
      };
      if (stompClientRef.current && stompClientRef.current.connected) {
        console.log('[操作] 发送私聊消息:', msg);
        stompClientRef.current.publish({
          destination: '/app/chat',
          body: JSON.stringify(msg),
        });
        setInput('');
      }
    }
  };

  // 用户名搜索（仅friend页）
  const handleUserSearch = (value) => {
    setSearchInput(value);
    if (searchTimeout.current) clearTimeout(searchTimeout.current);
    if (!value.trim()) {
      setSearchResults([]);
      setDropdownVisible(false);
      return;
    }
    setSearchLoading(true);
    setDropdownVisible(true);
    searchTimeout.current = setTimeout(async () => {
      try {
        const res = await fetch(`/api/users/search?username=${encodeURIComponent(value)}`, { credentials: 'include' });
        if (res.ok) {
          const data = await res.json();
          // 获取每个用户的profile信息
          const usersWithProfiles = await Promise.all(data.map(async user => {
            try {
              const profileRes = await fetch(`/api/users/homepage/${user.id}`, { credentials: 'include' });
              const profile = await profileRes.json();
              console.log('User profile:', profile); // 添加日志
              return {
                ...user,
                avatar: profile?.avatarUrl ? `/api${profile.avatarUrl}` : null
              };
            } catch (err) {
              return user;
            }
          }));
          setSearchResults(usersWithProfiles);
        } else {
          setSearchResults([]);
        }
      } catch {
        setSearchResults([]);
      } finally {
        setSearchLoading(false);
      }
    }, 350);
  };

  // 获取两名用户的历史聊天记录（POST + form参数）
  const fetchChatHistory = async (userId1, userId2) => {
    console.log(`[Chat] 请求聊天记录: userId1=${userId1}, userId2=${userId2}`);
    const form = new URLSearchParams();
    form.append('userId1', userId1);
    form.append('userId2', userId2);
    const res = await fetch('http://localhost:8080/api/chat/history', {
      method: 'POST',
      body: form,
      credentials: 'include'
    });
    if (!res.ok) {
      console.error(`[Chat] 获取聊天记录失败: HTTP ${res.status}`);
      throw new Error('获取聊天记录失败');
    }
    const data = await res.json();
    console.log(`[Chat] 聊天记录响应:`, data);
    return data;
  };

  // 拉取所有群聊历史消息
  const fetchAllGroupHistories = React.useCallback((uid) => {
    if (!uid) return;
    console.log('[操作] 拉取所有群聊历史消息, userId:', uid);
    fetch(`http://localhost:8080/api/group/historyByUser?userId=${uid}`, {
      method: 'GET',
      credentials: 'include',
    })
      .then(res => res.ok ? res.json() : Promise.reject(res))
      .then(data => {
        setGroupHistories(data || {});
        console.log('[结果] 群聊历史消息:', data);
      })
      .catch(() => {
        setGroupHistories({});
        console.error('[错误] 获取群聊历史消息失败');
      });
  }, []);

  // 进入群聊tab或用户变化时拉取所有群聊历史
  useEffect(() => {
    if (tab === 'group' && myId) {
      fetchAllGroupHistories(myId);
    }
  }, [tab, myId, fetchAllGroupHistories]);

  // 切换群聊时切换msgs
  useEffect(() => {
    if (tab === 'group' && current?.id) {
      setMsgs(groupHistories[current.id] || []);
    }
  }, [tab, current, groupHistories]);

  // 私聊历史消息逻辑保持不变
  useEffect(() => {
    if (!myId || !current?.id) return;
    if (tab === 'friend') {
      fetchChatHistory(myId, current.id)
        .then(msgs => {
          setMsgs(msgs);
        })
        .catch(() => setMsgs([]));
    }
  }, [myId, current, tab]);

  // 记录是否已建立过订阅，避免重复连接
  const wsActivatedRef = useRef(false);

  useEffect(() => {
    // 只要myId有值且未激活过订阅，就建立WebSocket订阅
    if (!myId || wsActivatedRef.current) return;
    wsActivatedRef.current = true;
    console.log('[WebSocket订阅启动] myId:', myId, '类型:', typeof myId);
    const topicFriend = `/topic/friend.${myId}`;
    const topicGroupMember = `/topic/group.member.${myId}`;
    const topicChat = `/topic/chat.${myId}`;
    console.log('[WebSocket订阅topic]', topicFriend, topicGroupMember, topicChat);
    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      reconnectDelay: 5000,
      onConnect: () => {
        console.log('[WebSocket已连接] myId:', myId);
        // 群聊成员变动
        client.subscribe(topicGroupMember, (msg) => {
          // 始终用msg.body解析
          let body = {};
          try {
            body = JSON.parse(msg.body);
          } catch (e) {
            console.error('[推送] 群聊成员消息解析失败', msg.body, e);
            return;
          }
          console.log('[收到群聊成员推送]', body);
          if (body.type === 'group_member_removed') {
            if (current && current.id === body.groupId) {
              setCurrent(null);
              setTab('friend');
              alert('你已被移出该群聊');
            }
            refreshGroups();
          } else if (body.type === 'group_member_added') {
            // 新增成员推送，自动刷新群聊列表
            console.log('[推送] 有成员被拉入群聊，自动刷新群聊列表:', body);
            refreshGroups();
          }
        });
        // 好友变动推送
        client.subscribe(topicFriend, (msg) => {
          console.log('[收到好友推送]', msg);
            const body = JSON.parse(msg.body);
          console.log('[推送] 收到好友变动消息:', body);
          if (body.type === 'friend_added') {
            console.log('[推送] 新增好友，自动刷新好友列表:', body);
            refreshFriends();
          } else if (body.type === 'friend_removed') {
            console.log('[推送] 有好友被删除，自动刷新好友列表:', body);
            refreshFriends();
            if (current && (current.id === body.friendId || current.id === body.userId)) {
              setCurrent(null);
              alert('当前好友已被删除，聊天已关闭');
            }
          }
        });
        // 个人消息订阅
        client.subscribe(topicChat, (msg) => {
          console.log('[收到个人消息推送]', msg);
          const body = JSON.parse(msg.body);
            if (
              tab === 'friend' && current &&
              ((body.fromId === current.id && body.toId === myId) || (body.fromId === myId && body.toId === current.id))
            ) {
              setMsgs(prev => [...prev, body]);
            }
          });
      },
    });
    client.activate();
    stompClientRef.current = client;
    return () => {
      client.deactivate();
      wsActivatedRef.current = false;
    };
  }, [myId, wsUrl]);

  // 动态订阅当前群聊的消息推送，保证聊天信息实时显示
  useEffect(() => {
    if (!myId || !current || tab !== 'group' || !current.id) return;
    const topicGroup = `/topic/group.${current.id}`;
    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(topicGroup, (msg) => {
          let body = {};
          try {
            body = JSON.parse(msg.body);
          } catch (e) {
            console.error('[群聊消息推送解析失败]', msg.body, e);
            return;
          }
          // 只追加当前群聊的消息
          if (current && tab === 'group' && current.id === body.groupId) {
            setMsgs(prev => [...prev, body]);
          }
        });
      },
    });
    client.activate();
    return () => client.deactivate();
  }, [myId, wsUrl, current, tab]);

  // 动态订阅私聊消息推送，保证好友聊天信息实时显示
  useEffect(() => {
    if (!myId) return;
    const topicChat = `/topic/chat.${myId}`;
    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(topicChat, (msg) => {
          let body = {};
          try {
            body = JSON.parse(msg.body);
          } catch (e) {
            console.error('[私聊消息推送解析失败]', msg.body, e);
            return;
          }
          // 只追加当前好友的消息
          if (
            tab === 'friend' && current &&
            ((body.fromId === current.id && body.toId === myId) || (body.fromId === myId && body.toId === current.id))
          ) {
            setMsgs(prev => [...prev, body]);
          }
        });
      },
    });
    client.activate();
    return () => client.deactivate();
  }, [myId, wsUrl, current, tab]);

  // 切换聊天对象时自动清空输入框
  useEffect(() => {
    setInput('');
  }, [current]);

  // 1. 新增好友项设置菜单状态
  const [friendMenuOpenId, setFriendMenuOpenId] = useState(null);

  // 2. 打开/关闭菜单
  const openFriendMenu = (friendId) => setFriendMenuOpenId(friendId);
  const closeFriendMenu = () => setFriendMenuOpenId(null);

  // 3. 修改好友列表渲染，分开好友和群聊
  const renderFriendList = () => (
    <div>
      <h3>好友列表</h3>
      <ul style={{padding:0}}>
        {friendList.map(friend => (
          <li key={friend.id} style={{ display: 'flex', alignItems: 'center', gap: 8, listStyle:'none', marginBottom:4, position:'relative' }}>
            
            <span style={{flex:1, cursor:'pointer'}} onClick={() => setCurrent(friend)}>{friend.name}</span>
            <button onClick={e => { e.stopPropagation(); openFriendMenu(friend.id); }} style={{ marginLeft: 8 }}>⚙️</button>
            {friendMenuOpenId === friend.id && (
              <div style={{ position:'absolute', right:0, top:'100%', background:'#fff', border:'1px solid #ddd', borderRadius:4, zIndex:10, minWidth:100 }} onMouseLeave={closeFriendMenu}>
                <div style={{ padding:'8px 12px', cursor:'pointer' }} onClick={() => { handleDeleteFriend(friend.id); closeFriendMenu(); }}>删除好友</div>
                <div style={{ padding:'8px 12px', cursor:'pointer' }} onClick={() => { handleViewUser(friend.id); closeFriendMenu(); }}>查看主页</div>
              </div>
            )}
          </li>
        ))}
      </ul>
    </div>
  );

  const renderGroupList = () => (
    <div>
      <h3>群聊列表</h3>
      <ul style={{padding:0}}>
        {groupList.map(group => (
          <li key={group.id} style={{ display: 'flex', alignItems: 'center', gap: 8, listStyle:'none', marginBottom:4 }}>
            <span style={{flex:1, cursor:'pointer'}} onClick={() => setCurrent(group)}>{group.name}</span>
          </li>
        ))}
      </ul>
    </div>
  );

  // 退出群聊功能
  const handleExitGroup = async (group) => {
    if (!window.confirm(`确定要退出群聊“${group.name}”吗？`)) return;
    if (!myId) return;
    const form = new URLSearchParams();
    form.append('groupId', group.id);
    form.append('userId', myId);
    await fetch('http://localhost:8080/api/group/leave', {
      method: 'POST',
      body: form,
      credentials: 'include',
    });
    // 退出后刷新群聊列表
    refreshGroups();
    // 如果当前聊天就是该群，切换到第一个群聊或清空
    setTimeout(() => {
      setGroupList(prev => {
        const newList = prev.filter(g => g.id !== group.id);
        setCurrent(newList[0] || null);
        return newList;
      });
    }, 200);
  };

  // 群聊改名方法
  const handleRenameGroup = async (group) => {
    if (!renameValue.trim()) return;
    console.log('[操作] 群聊改名, groupId:', group.id, '新群名:', renameValue.trim());
    const form = new URLSearchParams();
    form.append('groupId', group.id);
    form.append('newName', renameValue.trim());
    const res = await fetch('/api/group/updateName', {
      method: 'POST',
      body: form,
      credentials: 'include',
    });
    if (res.ok) {
      const data = await res.json();
      if (data.success) {
        refreshGroups();
        setCurrent(prev => prev && prev.id === group.id ? { ...prev, name: renameValue.trim() } : prev);
        setShowRenameModal(false);
        setRenameValue('');
        console.log('[结果] 群聊改名成功:', data);
      }
    }
  };

  // 群成员查看弹窗相关state
  const [showGroupMembers, setShowGroupMembers] = useState(false);
  const [groupMembers, setGroupMembers] = useState([]);
  const [memberSearch, setMemberSearch] = useState('');

  // 查看群成员方法
  const handleViewGroupMembers = async (group) => {
    console.log('[操作] 查看群成员, group:', group);
    const res = await fetch(`/api/group/members?groupId=${group.id}`, { credentials: 'include' });
    const data = await res.json();
    setGroupMembers(data);
    setShowGroupMembers(true);
    setMemberSearch('');
    console.log('[结果] 群成员:', data);
  };

  // 踢出成员方法
  const handleRemoveMember = async (member) => {
    if (!current) return;
    if (!window.confirm(`确定要将 ${member.username} 踢出群聊吗？`)) return;
    console.log('[操作] 踢出群成员:', member);
    const form = new URLSearchParams();
    form.append('groupId', current.id);
    form.append('userId', member.id);
    await fetch('/api/group/removeUser', {
      method: 'POST',
      body: form,
      credentials: 'include'
    });
    // 踢出后刷新成员列表
    handleViewGroupMembers(current);
  };

  // 邀请好友进群相关state
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [inviteSearch, setInviteSearch] = useState('');

  // 邀请好友方法（传userId）
  const handleInviteFriend = async (userId) => {
    if (!userId || !current) return;
    console.log('[操作] 邀请好友进群:', userId, '群聊:', current.id);
    const form = new URLSearchParams();
    form.append('groupId', current.id);
    form.append('userId', userId);
    await fetch('/api/group/addUser', {
      method: 'POST',
      body: form,
      credentials: 'include'
    });
    alert('已添加进群！');
    // 邀请后刷新群成员列表
    handleViewGroupMembers(current);
  };

  // 监听被踢出群聊的WebSocket推送
  useEffect(() => {
    if (!myId) return;
    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      reconnectDelay: 5000,
      onConnect: () => {
        // 群聊成员变动
        client.subscribe(`/topic/group.member.${myId}`, (msg) => {
          const body = JSON.parse(msg.body);
          if (body.type === 'group_member_removed') {
            if (current && current.id === body.groupId) {
              setCurrent(null);
              setTab('friend');
              alert('你已被移出该群聊');
            }
            refreshGroups();
          }
        });
        // 好友变动推送
        client.subscribe(`/topic/friend.${myId}`, (msg) => {
          const body = JSON.parse(msg.body);
          console.log('[推送] 收到好友变动消息:', body);
          if (body.type === 'friend_added') {
            console.log('[推送] 新增好友，自动刷新好友列表:', body);
            refreshFriends();
            // 可选：如果当前聊天对象正好是新加的好友，可以高亮或自动切换
            // if (current && current.id === body.friendId) {
            //   setTab('friend');
            //   setCurrent({ id: body.friendId });
            //   setInput('');
            //   console.log('[推送] 新增好友后自动切换到该好友聊天');
            // }
          } else if (body.type === 'friend_removed') {
            console.log('[推送] 有好友被删除，自动刷新好友列表:', body);
            refreshFriends();
            // 如果当前聊天对象就是被删除的好友，自动切换
            if (current && (current.id === body.friendId || current.id === body.userId)) {
              setCurrent(null);
              alert('当前好友已被删除，聊天已关闭');
            }
          }
        });
      },
    });
    client.activate();
    return () => client.deactivate();
  }, [myId, wsUrl]);

  // 过滤已在群内的好友
  const invitedIds = groupMembers.map(m => m.id);
  const availableFriends = friendList || [];

  // 多人建群方法
  const handleCreateGroup = async () => {
    if (!newGroupName.trim() || !myId) return;
    console.log('[操作] 多人建群, 群名:', newGroupName, '创建人:', myId, '成员:', selectedGroupMembers);
    const body = {
      groupName: newGroupName.trim(),
      creatorUserId: myId,
      memberIds: selectedGroupMembers
    };
    const res = await fetch('/api/group/createWithMembers', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
      credentials: 'include'
    });
    const data = await res.json();
    if (data.success) {
      setShowCreateGroup(false);
      setNewGroupName('');
      setSelectedGroupMembers([]);
      setGroupMemberSearch('');
      refreshGroups();
      alert('群聊创建成功！');
      console.log('[结果] 多人建群成功:', data);
    } else {
      console.error('[错误] 多人建群失败:', data);
    }
  };

  // 拉取单个群聊历史消息
  const fetchGroupHistory = async (groupId, userId) => {
    // 兼容后端接口，拉全量再筛选
    const res = await fetch(`http://localhost:8080/api/group/historyByUser?userId=${userId}`, {
      method: 'GET',
      credentials: 'include'
    });
    if (!res.ok) throw new Error('获取群聊历史消息失败');
    const data = await res.json();
    // data 是 { [groupId]: [msg, ...], ... }
    return data && data[groupId] ? data[groupId] : [];
  };

  useEffect(() => {
    if (!current) {
      setMsgs([]);
      console.log('[切换会话] 当前会话为空，消息区已清空');
      return;
    }
    if (tab === 'group') {
      if (myId && current.id) {
        fetchGroupHistory(current.id, myId)
          .then(msgsArr => {
            setMsgs(msgsArr || []);
            console.log(`[切换群聊] 当前群聊:`, current, `历史消息条数:`, msgsArr.length);
          })
          .catch(() => {
            setMsgs([]);
            console.log(`[切换群聊] 当前群聊:`, current, '获取历史消息失败，消息区已清空');
          });
      }
    } else if (tab === 'friend') {
      if (myId && current.id) {
        fetchChatHistory(myId, current.id)
          .then(msgs => {
            setMsgs(msgs);
            console.log(`[切换好友] 当前好友:`, current, `历史消息条数:`, msgs.length);
          })
          .catch(() => {
            setMsgs([]);
            console.log(`[切换好友] 当前好友:`, current, '获取历史消息失败，消息区已清空');
          });
      }
    }
  }, [current, tab, myId]);

  return (
    <div className="chat-page">
      {tab === 'friend' ? (
        <div className="chat-list-panel">
          <div className="chat-friend-search-bar">
            <span className="search-icon">🔍</span>
            <input
              type="text"
              className="chat-friend-search-input"
              placeholder="搜索用户名..."
              value={searchInput}
              onChange={e => handleUserSearch(e.target.value)}
              onFocus={() => searchInput && setDropdownVisible(true)}
              autoComplete="off"
            />
            <UserDropdown
              results={searchResults}
              loading={searchLoading}
              onView={handleViewUser}
              onAddFriend={handleAddFriend}
              addingId={addingId}
              visible={dropdownVisible && !!searchInput}
              onClose={() => setDropdownVisible(false)}
              friendIds={friendList.map(f=>f.id)}
              myId={myId}
            />
          </div>
          {tab === 'friend' && (
            <FriendList
              list={friendList}
              current={current}
              setCurrent={handleSelectFriend}
              onDelete={handleDeleteFriend}
              onView={handleViewUser}
            />
          )}
          <div className="chat-list-wrapper">
            {friendLoading ? <div style={{padding: '24px', color: '#888'}}>Loading friends...</div> :
              <FriendList list={list} current={current} setCurrent={setCurrent} />}
          </div>
        </div>
      ) : (
        <div className="chat-list-panel">
          {tab === 'group' && (
            <div style={{padding: '12px 16px', borderBottom: '1px solid #eee', background: '#fafbfc'}}>
              <button
                className="btn-primary"
                style={{fontSize:'1rem',padding:'6px 18px',borderRadius:6}}
                onClick={() => setShowCreateGroup(true)}
              >
                创建群聊
              </button>
            </div>
          )}
          <div className="chat-list-wrapper">
            {tab === 'group' && (
              <GroupList
                list={groupList}
                current={current}
                setCurrent={setCurrent}
                onExit={handleExitGroup}
                onRename={() => setShowRenameModal(true)}
                onViewMembers={handleViewGroupMembers}
              />
            )}
          </div>
        </div>
      )}
      <div className="chat-main-wrapper">
        {current ? (
          tab === 'friend' ? (
            <FriendChatMain
              current={current}
              msgs={msgs}
              input={input}
              setInput={setInput}
              onSend={handleSend}
              myId={myId}
            />
          ) : (
            <GroupChatMain
              current={current}
              msgs={msgs}
              input={input}
              setInput={setInput}
              onSend={handleSend}
              myId={myId}
            />
          )
        ) : (
          <div style={{height:'100%',display:'flex',alignItems:'center',justifyContent:'center',color:'#888',fontSize:'1.2em'}}>请选择好友或群聊开始聊天</div>
        )}
      </div>
      {/* 创建群聊弹窗 */}
      {showCreateGroup && (
        <div className="modal-overlay show" onClick={() => setShowCreateGroup(false)}>
          <div className="modal-content modal-w400" onClick={e => e.stopPropagation()}>
            <h3 className="modal-title">创建群聊</h3>
            <input
              type="text"
              placeholder="请输入群聊名称"
              value={newGroupName}
              onChange={e => setNewGroupName(e.target.value)}
              style={{width:'100%',padding:'8px',margin:'12px 0'}}
            />
            <input
              type="text"
              placeholder="搜索好友"
              value={groupMemberSearch}
              onChange={e => setGroupMemberSearch(e.target.value)}
              style={{width:'100%',padding:'8px',margin:'12px 0'}}
            />
            <div style={{maxHeight:240,overflowY:'auto',marginBottom:8,background:'#fafbfc',borderRadius:6,padding:'4px 0'}}>
              {friendList && friendList.length > 0 ? (
                (() => {
                  const filtered = friendList.filter(f => (f.name || f.username || '').includes(groupMemberSearch));
                  if (filtered.length === 0) {
                    return <div style={{color:'#888',padding:'12px'}}>无匹配好友</div>;
                  }
                  return filtered.map(f => (
                    <label key={f.id} style={{
                      display:'flex',alignItems:'center',marginBottom:4,padding:'6px 12px',
                      background: selectedGroupMembers.includes(f.id) ? '#eaf1ff' : 'transparent',
                      borderRadius:4,cursor:'pointer'
                    }}>
                      <input
                        type="checkbox"
                        checked={selectedGroupMembers.includes(f.id)}
                        onChange={e => {
                          if (e.target.checked) {
                            setSelectedGroupMembers(prev => [...prev, f.id]);
                          } else {
                            setSelectedGroupMembers(prev => prev.filter(id => id !== f.id));
                          }
                        }}
                        style={{marginRight:8}}
                      />
                      <span>{f.name || f.username || f.nickname || f.id}</span>
                    </label>
                  ));
                })()
              ) : (
                <div style={{color:'#888',padding:'12px'}}>暂无好友</div>
              )}
            </div>
            <div className="modal-actions">
              <button className="modal-btn secondary" onClick={() => setShowCreateGroup(false)}>取消</button>
              <button className="modal-btn primary" onClick={handleCreateGroup} disabled={!newGroupName.trim()}>创建</button>
            </div>
          </div>
        </div>
      )}
      {/* 拉人入群弹窗 */}
      {showAddUser && current && (
        <div className="modal-overlay show" onClick={() => setShowAddUser(false)}>
          <div className="modal-content modal-w400" onClick={e => e.stopPropagation()}>
            <h3 className="modal-title">邀请用户入群</h3>
            <input
              type="text"
              placeholder="请输入用户ID"
              value={addUserId}
              onChange={e => setAddUserId(e.target.value)}
              style={{width:'100%',padding:'8px',margin:'18px 0',fontSize:'1.1rem'}}
            />
            <div className="modal-actions">
              <button className="modal-btn secondary" onClick={() => setShowAddUser(false)}>取消</button>
              <button className="modal-btn primary" onClick={async () => {
                try {
                  await addUserToGroup(current.id, addUserId);
                  setShowAddUser(false);
                  setAddUserId('');
                  // 可选：刷新群成员列表
                  refreshGroups();
                } catch (e) {
                  alert(e.message);
                }
              }}>邀请</button>
            </div>
          </div>
        </div>
      )}
      {/* 群成员弹窗渲染 */}
      {showGroupMembers && (
        <div className="modal-overlay show" onClick={() => setShowGroupMembers(false)}>
          <div className="modal-content modal-w400" onClick={e => e.stopPropagation()}>
            <h3 className="modal-title">群成员列表</h3>
            <button style={{marginBottom:12,padding:'6px 16px',borderRadius:6,border:'1px solid #e0e0e0',background:'#eaf1ff',color:'#333',cursor:'pointer'}} onClick={() => setShowInviteModal(true)}>邀请好友进群</button>
            <input
              type="text"
              placeholder="搜索成员昵称"
              value={memberSearch}
              onChange={e => setMemberSearch(e.target.value)}
              style={{width:'100%',padding:'8px',margin:'12px 0'}}
            />
            <div style={{maxHeight:320,overflowY:'auto'}}>
              {groupMembers
                .filter(m => (m.username || '').includes(memberSearch))
                .map(m => (
                  <div key={m.id} style={{padding:'6px 0',borderBottom:'1px solid #eee',display:'flex',alignItems:'center',justifyContent:'space-between'}}>
                    <div style={{display:'flex',alignItems:'center'}}>
                      <span className="avatar" style={{marginRight:8,display:'flex',alignItems:'center',justifyContent:'center',width:28,height:28}}>
                        {(m.avatarUrl && m.avatarUrl.trim()) ? (
                          <img
                            src={m.avatarUrl}
                            alt={m.username}
                            style={{width:28,height:28,borderRadius:'50%',objectFit:'cover',display:'block'}}
                            onError={e => { e.target.style.display = 'none'; }}
                          />
                        ) : (
                          <span style={{width:28,height:28,display:'flex',alignItems:'center',justifyContent:'center',borderRadius:'50%',background:'#eaf1ff',color:'#555',fontWeight:600,fontSize:16}}>
                            {m.username?.charAt(0) || 'U'}
                          </span>
                        )}
                      </span>
                      <span>{m.username}</span>
                    </div>
                    <div>
                      <button style={{marginLeft:12,padding:'4px 10px',borderRadius:5,border:'1px solid #e0e0e0',background:'#fff0f0',color:'#e74c3c',cursor:'pointer'}} onClick={() => navigate(`/user-homepage/${m.id}?from=chat`)}>查看主页</button>
                      {m.id !== myId && (
                        <button style={{marginLeft:8,padding:'4px 10px',borderRadius:5,border:'1px solid #e0e0e0',background:'#fff0f0',color:'#e74c3c',cursor:'pointer'}} onClick={() => handleRemoveMember(m)}>踢出</button>
                      )}
                    </div>
                  </div>
                ))}
              {groupMembers.length === 0 && <div style={{color:'#888',padding:'12px'}}>暂无成员</div>}
            </div>
            <div className="modal-actions">
              <button className="modal-btn primary" onClick={() => setShowGroupMembers(false)}>关闭</button>
            </div>
          </div>
        </div>
      )}
      {/* 邀请好友弹窗 */}
      {showInviteModal && (
        <div className="modal-overlay show" onClick={() => setShowInviteModal(false)}>
          <div className="modal-content modal-w400" onClick={e => e.stopPropagation()}>
            <h3 className="modal-title">邀请好友进群</h3>
            <input
              type="text"
              placeholder="搜索好友昵称"
              value={inviteSearch}
              onChange={e => setInviteSearch(e.target.value)}
              style={{width:'100%',padding:'8px',margin:'12px 0'}}
            />
            <div style={{maxHeight:320,overflowY:'auto'}}>
              {/* 始终显示所有好友 */}
              {friendList && friendList.length > 0 ? (
                friendList
                  .filter(f => ((f.name || f.username || '').includes(inviteSearch)))
                  .map(f => {
                    const isInGroup = groupMembers.some(m => m.id === f.id);
                    return (
                      <div key={f.id} style={{padding:'6px 0',borderBottom:'1px solid #eee'}}>
                        <div style={{display:'flex',alignItems:'center',justifyContent:'space-between'}}>
                          <span>{f.name || f.username || f.nickname || f.id}</span>
                          {isInGroup && (
                            <span style={{marginLeft:12,padding:'4px 10px',borderRadius:5,background:'#f5f5f5',color:'#aaa',fontSize:13,fontWeight:600}}>已在群里</span>
                          )}
                        </div>
                        {!isInGroup && (
                          <div style={{marginTop:8,display:'flex',justifyContent:'flex-end'}}>
                            <button
                              style={{
                                padding:'4px 14px',
                                borderRadius:6,
                                border:'none',
                                background:'#1976d2',
                                color:'#fff',
                                fontWeight:600,
                                fontSize:14,
                                cursor:'pointer',
                                boxShadow:'0 1px 4px rgba(25,118,210,0.08)',
                                transition:'background 0.18s'
                              }}
                              onClick={() => handleInviteFriend(f.id)}
                            >邀请入群</button>
                          </div>
                        )}
                      </div>
                    );
                  })
              ) : (
                <div style={{color:'#888',padding:'12px'}}>暂无可邀请好友</div>
              )}
            </div>
            <div className="modal-actions">
              <button className="modal-btn primary" onClick={() => setShowInviteModal(false)}>关闭</button>
            </div>
          </div>
        </div>
      )}
      {/* 群聊改名弹窗 */}
      {showRenameModal && current && (
        <div className="modal-overlay show" onClick={() => setShowRenameModal(false)}>
          <div className="modal-content modal-w400" onClick={e => e.stopPropagation()}>
            <h3 className="modal-title">修改群聊名称</h3>
            <input
              type="text"
              value={renameValue}
              onChange={e => setRenameValue(e.target.value)}
              placeholder="请输入新群名"
              style={{width:'100%',padding:'8px',margin:'18px 0'}}
            />
            <div className="modal-actions">
              <button className="modal-btn secondary" onClick={() => setShowRenameModal(false)}>取消</button>
              <button className="modal-btn primary" onClick={() => handleRenameGroup(current)} disabled={!renameValue.trim()}>确定</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
} 