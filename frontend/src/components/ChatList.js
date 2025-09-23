import React from 'react';
import './ChatList.css';

export function FriendList({ list, current, setCurrent, onDelete, onView }) {
  const [menuOpenId, setMenuOpenId] = React.useState(null);
  const menuRef = React.useRef();

  // 点击空白关闭菜单
  React.useEffect(() => {
    function handleClick(e) {
      if (menuRef.current && !menuRef.current.contains(e.target)) {
        setMenuOpenId(null);
      }
    }
    if (menuOpenId !== null) {
      document.addEventListener('mousedown', handleClick);
    } else {
      document.removeEventListener('mousedown', handleClick);
    }
    return () => document.removeEventListener('mousedown', handleClick);
  }, [menuOpenId]);

  return (
    <div className="chat-list" style={{maxHeight: '650px', overflowY: 'auto'}}>
      {list.map(item => (
        <div
          key={item.id}
          className={`chat-list-item${current?.id === item.id ? ' selected' : ''}`}
          onClick={() => setCurrent(item)}
          style={{
            borderRadius: 10,
            marginBottom: 4,
            padding: '0 20px',
            minHeight: 48,
            display: 'flex',
            alignItems: 'center',
            background: current?.id === item.id ? '#eaf1ff' : '#fff',
            fontWeight: 400,
            cursor: 'pointer',
            boxShadow: current?.id === item.id ? '0 2px 8px rgba(80,120,255,0.08)' : 'none',
            transition: 'background 0.2s, box-shadow 0.2s'
          }}
          onMouseEnter={e => e.currentTarget.style.background = '#f5f7fa'}
          onMouseLeave={e => e.currentTarget.style.background = current?.id === item.id ? '#eaf1ff' : '#fff'}
        >
          <span className="avatar" style={{
            width: '40px',
            height: '40px',
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            background: '#e3f0ff',
            color: '#1976d2',
            fontSize: '1.2rem',
            fontWeight: '500',
            overflow: 'hidden'
          }}>
            {typeof item.avatar === 'string' && item.avatar.startsWith('/api') ? (
              <img
                src={item.avatar}
                alt={item.name}
                style={{
                  width: '100%',
                  height: '100%',
                  objectFit: 'cover'
                }}
                onError={(e) => {
                  e.target.style.display = 'none';
                  e.target.parentElement.textContent = item.name.charAt(0);
                }}
              />
            ) : (
              item.name.charAt(0)
            )}
          </span>
          <div className="info" style={{flex:1}}>
            <div className="name" style={{fontWeight:600, fontSize:16, color:'#222'}}>{item.name}</div>
            <div className="last-msg" style={{color:'#888', fontSize:13, marginTop:2}}>{item.lastMsg}</div>
          </div>
          <button
            className="friend-settings-btn"
            onClick={e => { e.stopPropagation(); setMenuOpenId(item.id); }}
            style={{ marginLeft: 8 }}
          >⚙️</button>
          {menuOpenId === item.id && (
            <div
              ref={menuRef}
              className="friend-menu-dropdown"
              style={{
                position: 'absolute',
                right: 0,
                top: '100%',
                zIndex: 10,
                background: '#fff',
                border: '1px solid #eee',
                borderRadius: 6,
                boxShadow: '0 2px 8px rgba(0,0,0,0.12)',
                minWidth: 120,
                marginTop: 4
              }}
              onMouseLeave={() => setMenuOpenId(null)}
            >
              <div
                style={{ padding: '8px 16px', cursor: 'pointer' }}
                onClick={e => { e.stopPropagation(); setMenuOpenId(null); onView(item); }}
              >查看主页</div>
              <div
                style={{ padding: '8px 16px', cursor: 'pointer', color: '#e74c3c' }}
                onClick={e => { e.stopPropagation(); setMenuOpenId(null); onDelete(item.id); }}
              >删除好友</div>
            </div>
          )}
        </div>
      ))}
    </div>
  );
}

export function GroupList({ list, current, setCurrent, onExit, onRename, onViewMembers }) {
  const [menuOpenId, setMenuOpenId] = React.useState(null);
  const menuRef = React.useRef();

  // 点击空白关闭菜单
  React.useEffect(() => {
    function handleClick(e) {
      if (menuRef.current && !menuRef.current.contains(e.target)) {
        setMenuOpenId(null);
      }
    }
    if (menuOpenId !== null) {
      document.addEventListener('mousedown', handleClick);
    } else {
      document.removeEventListener('mousedown', handleClick);
    }
    return () => document.removeEventListener('mousedown', handleClick);
  }, [menuOpenId]);

  return (
    <div className="chat-list">
      {list.map(item => (
        <div
          key={item.id}
          className={`chat-list-item${current?.id === item.id ? ' selected' : ''}`}
          onClick={() => setCurrent(item)}
          style={{
            borderRadius: 10,
            marginBottom: 4,
            padding: '0 20px',
            minHeight: 48,
            display: 'flex',
            alignItems: 'center',
            background: current?.id === item.id ? '#eaf1ff' : '#fff',
            fontWeight: 400,
            cursor: 'pointer',
            boxShadow: current?.id === item.id ? '0 2px 8px rgba(80,120,255,0.08)' : 'none',
            transition: 'background 0.2s, box-shadow 0.2s',
            position: 'relative'
          }}
          onMouseEnter={e => e.currentTarget.style.background = '#f5f7fa'}
          onMouseLeave={e => e.currentTarget.style.background = current?.id === item.id ? '#eaf1ff' : '#fff'}
        >
          <div className="info" style={{flex:1}}>
            <div className="name" style={{fontWeight:600, fontSize:16, color:'#222'}}>{item.name}</div>
            <div className="last-msg" style={{color:'#888', fontSize:13, marginTop:2}}>{item.lastMsg}</div>
          </div>
          <button
            className="group-settings-btn"
            onClick={e => { e.stopPropagation(); setMenuOpenId(item.id); }}
            style={{ marginLeft: 8 }}
          >⚙️</button>
          {menuOpenId === item.id && (
            <div
              ref={menuRef}
              className="group-menu-dropdown"
              style={{
                position: 'absolute',
                right: 0,
                top: '100%',
                zIndex: 10,
                background: '#fff',
                border: '1px solid #eee',
                borderRadius: 6,
                boxShadow: '0 2px 8px rgba(0,0,0,0.12)',
                minWidth: 140,
                marginTop: 4
              }}
              onMouseLeave={() => setMenuOpenId(null)}
            >
              <div
                style={{ padding: '8px 16px', cursor: 'pointer' }}
                onClick={e => { e.stopPropagation(); setMenuOpenId(null); onViewMembers && onViewMembers(item); }}
              >查看群成员</div>
              <div
                style={{ padding: '8px 16px', cursor: 'pointer' }}
                onClick={e => { e.stopPropagation(); setMenuOpenId(null); onRename && onRename(item); }}
              >修改群名</div>
              <div
                style={{ padding: '8px 16px', cursor: 'pointer', color: '#e74c3c' }}
                onClick={e => { e.stopPropagation(); setMenuOpenId(null); onExit && onExit(item); }}
              >退出群聊</div>
            </div>
          )}
        </div>
      ))}
    </div>
  );
} 