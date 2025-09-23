import React, { useRef, useEffect } from 'react';
import './ChatMain.css';

export default function GroupChatMain({ current, msgs, input, setInput, onSend, myId, friendList }) {
  const bodyRef = useRef(null);
  useEffect(() => {
    if (bodyRef.current) {
      bodyRef.current.scrollTop = bodyRef.current.scrollHeight;
    }
  }, [msgs]);

  return (
    <div className="chat-main">
      <div className="chat-header">
        {current ? (
          <>
            <span className="name">{current.name}</span>
          </>
        ) : (
          <span className="name" style={{color:'#888'}}>Please select a friend or group to start chatting</span>
        )}
      </div>
      <div className="chat-body" ref={bodyRef}>
        {current && msgs.map((msg, i) => {
          const isMe = String(msg.userId) === String(myId);
          const senderName = isMe ? '我' : (msg.userName || '未知用户');
          let timeStr = '';
          if (msg.messageTime) {
            timeStr = msg.messageTime.includes('T')
              ? msg.messageTime.replace('T', ' ').slice(0, 19)
              : msg.messageTime.slice(0, 19);
          }
          return (
            <div key={msg.messageId || i} style={{display:'flex', flexDirection:'column', alignItems: isMe ? 'flex-end' : 'flex-start', marginBottom: '16px'}}>
              <div style={{display:'flex', flexDirection: isMe ? 'row-reverse' : 'row', alignItems:'flex-end', width:'100%'}}>
                <div style={{maxWidth:'70%', display:'flex', flexDirection:'column', alignItems: isMe ? 'flex-end' : 'flex-start'}}>
                  <div className="msg-username" style={{color:'#888', fontSize:'0.92em', marginBottom:'2px', textAlign: isMe ? 'right' : 'left'}}>{senderName}</div>
                  <div className="msg-bubble" style={{background: isMe ? '#9edad1' : '#fceef5', color:'#564d4d', borderRadius: '12px', padding:'8px 14px', fontSize:'1.05em', boxShadow:'0 1px 4px #eee', textAlign:'left', wordBreak:'break-all', minWidth: '36px'}}>
                    {msg.content}
                  </div>
                  <div className="msg-time" style={{marginTop:'2px', color:'#bbb', fontSize:'0.88em', textAlign: isMe ? 'right' : 'left'}}>{timeStr}</div>
                </div>
              </div>
            </div>
          );
        })}
      </div>
      <div className="chat-input-bar">
        <input
          type="text"
          value={input}
          onChange={e => setInput(e.target.value)}
          placeholder="Type a message..."
          onKeyDown={e => e.key === 'Enter' && onSend()}
          disabled={!current}
        />
        <button onClick={onSend} disabled={!current || !input.trim()}>Send</button>
      </div>
    </div>
  );
} 