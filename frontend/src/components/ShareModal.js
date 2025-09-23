import React, { useState, useEffect, useRef } from 'react';
import './ShareModal.css';

const ShareModal = ({ isOpen, onClose, onShare, itineraryId, shareTip }) => {
    const [description, setDescription] = useState('');
    const [tags, setTags] = useState([]);
    const [selectedTags, setSelectedTags] = useState([]);
    const [shareCode, setShareCode] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');
    const modalRef = useRef(null);

    useEffect(() => {
        // 获取可用标签
        if (isOpen) {
            fetch('/api/itineraries/tags', {
                credentials: 'include', // 添加这行以包含 cookies
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                }
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`获取标签失败: ${response.status}`);
                    }
                    return response.json();
                })
                .then(data => {
                    console.log('Received tags:', data); // 添加调试日志
                    // 确保 data 是数组
                    const tagsArray = Array.isArray(data) ? data : [];
                    setTags(tagsArray);
                })
                .catch(err => {
                    console.error('Error fetching tags:', err); // 添加错误日志
                    setError('获取标签失败');
                    setTags([]); // 确保 tags 始终是数组
                });
        } else {
            // 当模态框关闭时重置状态
            setTags([]);
            setSelectedTags([]);
            setShareCode('');
            setError('');
        }
    }, [isOpen]);

    useEffect(() => {
        if (isOpen && modalRef.current) {
            // 获取当前行程卡片的位置
            const tripCard = document.querySelector(`[data-id="${itineraryId}"]`);
            if (tripCard) {
                const cardRect = tripCard.getBoundingClientRect();
                const modalHeight = modalRef.current.offsetHeight;
                const windowHeight = window.innerHeight;

                // 计算理想的modal位置：与行程卡片顶部对齐
                let idealTop = cardRect.top;

                // 确保modal不会超出屏幕底部或顶部
                if (idealTop + modalHeight > windowHeight - 40) { // 40px是底部边距
                  idealTop = Math.max(40, windowHeight - modalHeight - 40); // 40px是顶部边距
                }

                // 如果行程卡片在屏幕下方不可见，将modal放在屏幕中央
                if (cardRect.top > windowHeight) {
                  idealTop = (windowHeight - modalHeight) / 2;
                }

                // 应用计算出的位置
                modalRef.current.style.marginTop = `${idealTop}px`;
            }
        }
    }, [isOpen, itineraryId]);

    const handleTagClick = (tagId) => {
        setSelectedTags(prev => {
            if (prev.includes(tagId)) {
                return prev.filter(id => id !== tagId);
            } else {
                return [...prev, tagId];
            }
        });
    };

    const handleShare = async () => {
        if (!description.trim()) {
            setError('请输入行程描述');
            return;
        }

        setIsLoading(true);
        setError('');

        try {
            const response = await fetch(`/api/itineraries/${itineraryId}/share`, {
                method: 'POST',
                credentials: 'include',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    description: description.trim(),
                    tagIds: selectedTags
                })
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => null);
                throw new Error(errorData?.error || '分享失败');
            }

            const data = await response.json();
            setShareCode(data.shareCode);
            onShare && onShare(data.shareCode);
        } catch (err) {
            console.error('Share error:', err);
            setError(err.message || '分享失败');
        } finally {
            setIsLoading(false);
        }
    };

    const handleCopyShareCode = async (event) => {
        try {
            await navigator.clipboard.writeText(shareCode);
            // 显示复制成功提示
            setError(''); // 清除之前的错误信息

            // 使用事件目标而不是querySelector，避免null错误
            const button = event.target;
            if (button) {
                const originalText = button.textContent;
                button.textContent = '已复制!';
                setTimeout(() => {
                    button.textContent = originalText;
                }, 2000);
            }
        } catch (err) {
            console.error('复制失败:', err);
            setError('复制失败，请手动复制');
        }
    };

    const handleClose = () => {
        setDescription('');
        setSelectedTags([]);
        setShareCode('');
        setError('');
        onClose();
    };

    if (!isOpen) return null;

    return (
        <div className="share-modal-overlay" onClick={handleClose}>
            <div className="share-modal-content" onClick={e => e.stopPropagation()} ref={modalRef}>
                <div className="share-modal-header">
                    <h3>{shareCode ? '分享成功' : (shareTip === '所有人都可以在社区内查看您的行程' ? '公开行程' : '分享行程')}</h3>
                    <button className="share-modal-close" onClick={handleClose}>×</button>
                </div>

                <div className="share-modal-body">
                    {shareTip && !shareCode && (
                        <div className="share-tip">
                            <span className="tip-icon">ℹ️</span>
                            {shareTip}
                        </div>
                    )}

                    {!shareCode ? (
                        <>
                            <div className="form-group">
                                <label>行程描述</label>
                                <textarea
                                    value={description}
                                    onChange={e => setDescription(e.target.value)}
                                    placeholder="请输入行程描述..."
                                    rows={4}
                                    className={error && !description.trim() ? 'error' : ''}
                                />
                            </div>

                            <div className="form-group">
                                <label>选择标签（最多5个）</label>
                                <div className="tags-container">
                                    {Array.isArray(tags) && tags.length > 0 ? (
                                        tags.map(tag => (
                                            <button
                                                key={tag.id}
                                                onClick={() => handleTagClick(tag.id)}
                                                className={`tag-button ${selectedTags.includes(tag.id) ? 'selected' : ''}`}
                                                disabled={selectedTags.length >= 5 && !selectedTags.includes(tag.id)}
                                            >
                                                {tag.tag}
                                            </button>
                                        ))
                                    ) : (
                                        <div className="no-tags-message">暂无可用标签</div>
                                    )}
                                </div>
                            </div>

                            {error && <div className="error-message">{error}</div>}

                            <button
                                className="share-button"
                                onClick={handleShare}
                                disabled={isLoading || !description.trim()}
                            >
                                {isLoading ? '分享中...' : '分享'}
                            </button>
                        </>
                    ) : (
                        <div className="share-success-container">
                            <div className="success-icon">✅</div>
                            <h4 className="success-title">分享成功！</h4>
                            <p className="success-description">
                                您的行程已成功分享到社区，其他用户可以通过分享码查看您的行程。
                            </p>

                            <div className="share-code-section">
                                <label className="share-code-label">分享码</label>
                                <div className="share-code-display">
                                    <div className="share-code-text">{shareCode}</div>
                                    <button
                                        className="copy-button"
                                        onClick={handleCopyShareCode}
                                    >
                                        复制
                                    </button>
                                </div>
                                <p className="share-code-tip">
                                    复制分享码发送给朋友，他们就可以查看您的行程了
                                </p>
                            </div>

                            <div className="share-actions">
                                <button
                                    className="share-close-button"
                                    onClick={handleClose}
                                >
                                    完成
                                </button>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default ShareModal;
