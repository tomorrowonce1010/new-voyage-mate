import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import amapManager from '../utils/amapManager';
import './DestinationDetail.css';
import CityMap from "../components/CityMap";

const DestinationDetail = () => {
  const navigate = useNavigate();
  const { id } = useParams();
  const [destinationData, setDestinationData] = useState(null);
  const [attractions, setAttractions] = useState([]);
  const [allAttractions, setAllAttractions] = useState([]); // 存储所有景点用于搜索
  const [topTags, setTopTags] = useState([]);
  const [allTags, setAllTags] = useState([]); // 所有可用标签
  const [attractionTags, setAttractionTags] = useState({}); // 存储每个景点的标签
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [liveWeather, setLiveWeather] = useState(null);
  const [forecast, setForecast] = useState([]);
  const [weatherError, setWeatherError] = useState("");
  const [amapLoaded, setAmapLoaded] = useState(false);
  const [showForecast, setShowForecast] = useState(false);
  
  // 搜索相关状态
  const [searchText, setSearchText] = useState('');
  const [debouncedSearchText, setDebouncedSearchText] = useState('');
  const [selectedTags, setSelectedTags] = useState([]);
  const [isSearchActive, setIsSearchActive] = useState(false);
  const [tagsLoading, setTagsLoading] = useState(false);
  const [tagsProgress, setTagsProgress] = useState({ current: 0, total: 0 });

  useEffect(() => {
    fetchDestinationData();
    fetchAttractions();
    fetchTopTags();
    fetchAllTags();
  }, [id]);

  useEffect(() => {
    fetchAttractions();
  }, [currentPage, selectedTags, debouncedSearchText]);

  // 防抖处理搜索文本
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearchText(searchText);
    }, 300);

    return () => clearTimeout(timer);
  }, [searchText]);

  // 动态加载高德JS API
  useEffect(() => {
    amapManager.load(['AMap.Weather']).then(() => {
      setAmapLoaded(true);
    }).catch(() => {
      setWeatherError("高德JS API脚本加载失败，请检查网络或Key");
    });
  }, []);

  // 查询天气（自动查当前城市）
  useEffect(() => {
    if (!destinationData || !amapLoaded) return;
    setWeatherError("");
    setLiveWeather(null);
    setForecast([]);
    
    amapManager.getWeather(destinationData.name).then((weatherData) => {
      if (weatherData.live) {
        setLiveWeather(weatherData.live);
      } else {
        setWeatherError("未查询到实时天气，请检查城市名称");
      }
      if (weatherData.forecast) {
        setForecast(weatherData.forecast);
      }
    }).catch(() => {
      setWeatherError("高德JS API未加载，请稍后重试");
    });
  }, [destinationData, amapLoaded]);

  const fetchDestinationData = async () => {
    try {
      const response = await fetch(`http://localhost:8080/api/destinations/${id}`);
      if (response.ok) {
        const data = await response.json();
        setDestinationData(data);
      } else {
        // 如果找不到数据，跳转回explore页面
        navigate('/explore');
      }
    } catch (error) {
      console.error('获取目的地数据失败:', error);
      navigate('/explore');
    }
  };

  const fetchAttractions = async () => {
    setLoading(true);
    try {
      let url = '';
      let isPaginatedResult = false;
      
      // 判断搜索模式
      const hasSearchText = debouncedSearchText.trim() !== '';
      const hasTags = selectedTags.length > 0;
      
      if (hasSearchText && hasTags) {
        // 语义搜索 + 标签组合
        console.log("景点搜索: semantic + tags combined");
        const tagParams = selectedTags.map(tag => `tags=${encodeURIComponent(tag)}`).join('&');
        url = `http://localhost:8080/api/attractions/semantic-search-by-tags/${id}?${tagParams}&keyword=${encodeURIComponent(debouncedSearchText.trim())}&page=${currentPage}&size=8`;
        isPaginatedResult = true; // 语义搜索现在支持分页
      } else if (hasSearchText) {
        // 纯语义搜索
        console.log("景点搜索: semantic search");
        url = `http://localhost:8080/api/attractions/semantic-search/${id}?keyword=${encodeURIComponent(debouncedSearchText.trim())}&page=${currentPage}&size=8`;
        isPaginatedResult = true; // 语义搜索现在支持分页
      } else if (hasTags) {
        // 纯标签过滤（支持分页）
        console.log("景点搜索: tags only");
        const params = new URLSearchParams();
        params.append('page', currentPage);
        params.append('size', 8);
        params.append('tag', selectedTags.join(','));
        url = `http://localhost:8080/api/attractions/destination/${id}?${params.toString()}`;
        isPaginatedResult = true;
      } else {
        // 默认热门景点（支持分页）
        console.log("景点搜索: hot attractions");
        const params = new URLSearchParams();
        params.append('page', currentPage);
        params.append('size', 8);
        url = `http://localhost:8080/api/attractions/destination/${id}?${params.toString()}`;
        isPaginatedResult = true;
      }

      const response = await fetch(url);
      
      if (response.ok) {
        let data = await response.json();
        
        if (isPaginatedResult && data.content) {
          // 分页数据
          setAttractions(data.content);
          setTotalPages(data.totalPages);
          setTotalElements(data.totalElements || 0);
        } else if (!isPaginatedResult && Array.isArray(data)) {
          // 语义搜索返回的数组数据
          setAttractions(data);
          setTotalPages(1); // 语义搜索暂不支持分页，设为1页
          setTotalElements(data.length);
        } else {
          // 兜底处理
          const results = data.content || data;
          setAttractions(results);
          setTotalPages(1);
          setTotalElements(results.length);
        }
        
        // 获取景点标签
        const attractionsToProcess = isPaginatedResult && data.content ? data.content : (Array.isArray(data) ? data : []);
        fetchAttractionTags(attractionsToProcess);
      }
    } catch (error) {
      console.error('获取景点数据失败:', error);
      setAttractions([]);
      setTotalPages(0);
      setTotalElements(0);
    } finally {
      setLoading(false);
    }
  };

  // 获取景点标签
  const fetchAttractionTags = async (attractionList) => {
    const newAttractionTags = {};
    
    // 为每个景点获取标签
    await Promise.all(
      attractionList.map(async (attraction) => {
        try {
          const response = await fetch(`http://localhost:8080/api/attractions/${attraction.id}/top-tags?count=3`);
          if (response.ok) {
            const tags = await response.json();
            newAttractionTags[attraction.id] = tags;
          } else {
            newAttractionTags[attraction.id] = [];
          }
        } catch (error) {
          console.error(`获取景点 ${attraction.id} 标签失败:`, error);
          newAttractionTags[attraction.id] = [];
        }
      })
    );
    
    setAttractionTags(prev => ({ ...prev, ...newAttractionTags }));
  };

  const fetchTopTags = async () => {
    try {
      const response = await fetch(`http://localhost:8080/api/destinations/${id}/top-tags?count=6`);
      if (response.ok) {
        const tags = await response.json();
        setTopTags(tags);
      }
    } catch (error) {
      console.error('获取标签数据失败:', error);
      // 如果获取失败，设置默认标签
      setTopTags(['热门目的地']);
    }
  };

  // 获取所有目的地标签
  const fetchAllTags = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/destinations/tags');
      if (response.ok) {
        const tags = await response.json();
        setAllTags(tags);
      }
    } catch (error) {
      console.error('获取所有标签失败:', error);
      setAllTags([]);
    }
  };

  const handlePageChange = (newPage) => {
    if (newPage >= 0 && newPage < totalPages) {
      setCurrentPage(newPage);
      // 清空当前页景点的标签缓存，在fetchAttractions中会重新获取
    }
  };

  // 标签点击处理
  const handleTagClick = (tag) => {
    setSelectedTags(prev => {
      let next;
      if (prev.includes(tag)) {
        next = prev.filter(t => t !== tag);
      } else {
        next = [...prev, tag];
      }
      // 重置到第一页
      setCurrentPage(0);
      setIsSearchActive(next.length > 0);
      return next;
    });
  };

  // 搜索文本处理
  const handleSearchChange = (text) => {
    setSearchText(text);
    setIsSearchActive(text.trim() !== '' || selectedTags.length > 0);
    setCurrentPage(0);
  };

  // 生成景点图片URL（使用占位符图片）
  const getAttractionImageUrl = (attraction) => {
    console.log('处理景点图片URL:', attraction.name, attraction.imageUrl);
    
    if (attraction.imageUrl) {
      // 如果图片URL为空，使用占位符
      if (attraction.imageUrl === '') {
        console.log('图片URL为空，使用占位符:', attraction.name);
        return getPlaceholderImageUrl(attraction.name);
      }

      // 如果是完整的HTTP/HTTPS URL，直接使用
      if (attraction.imageUrl.startsWith('http://') || attraction.imageUrl.startsWith('https://')) {
        console.log('使用网络图片URL:', attraction.imageUrl);
        return attraction.imageUrl;
      }
      
      // 如果是外部图片URL（包含content_media_external_images），直接使用
      if (attraction.imageUrl.includes('content_media_external_images')) {
        console.log('使用外部图片URL:', attraction.imageUrl);
        return attraction.imageUrl;
      }
      
      // 如果是本地图片URL（以/开头），添加/api前缀
      if (attraction.imageUrl.startsWith('/')) {
        const fullUrl = `/api${attraction.imageUrl}`;
        console.log('生成的完整本地URL:', fullUrl);
        return fullUrl;
      }
      
      // 如果是其他格式的本地图片URL，添加/api/images/前缀
      const fullUrl = `/api/images/${attraction.imageUrl}`;
      console.log('生成的完整URL:', fullUrl);
      return fullUrl;
    }
    // 如果没有图片，使用占位符
    console.log('使用占位符图片');
    return getPlaceholderImageUrl(attraction.name);
  };

  // 生成占位符图片URL
  const getPlaceholderImageUrl = (name) => {
    const color = Math.floor(Math.random()*16777215).toString(16);
    return `data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 200 150'><rect width='200' height='150' fill='%23${color}'/><text x='100' y='75' font-family='Arial' font-size='16' fill='white' text-anchor='middle' dy='0.3em'>${name}</text></svg>`;
  };

  // 添加图片加载错误处理
  const handleImageError = (event, attraction) => {
    // 检查是否已经处理过错误，避免重复处理
    if (event.target.getAttribute('data-error-handled') === 'true') {
      return;
    }
    
    console.error('图片加载失败:', {
      attraction: attraction.name,
      originalUrl: attraction.imageUrl,
      attemptedUrl: event.target.src,
      error: event
    });
    
    // 如果加载失败，使用占位符图片
    event.target.style.backgroundImage = `url("${getPlaceholderImageUrl(attraction.name)}")`;
    
    // 添加一个标记，避免重复处理
    event.target.setAttribute('data-error-handled', 'true');
  };

  // 添加图片加载成功处理
  const handleImageLoad = (event, attraction) => {
    console.log('图片加载成功:', {
      attraction: attraction.name,
      url: event.target.src
    });
  };

  // 处理景点拖拽开始
  const handleAttractionDragStart = (e, attraction) => {
    // 准备拖拽数据
    const dragData = {
      id: attraction.id,
      name: attraction.name,
      description: attraction.description || '',
      category: attraction.category || '',
      imageUrl: attraction.imageUrl || '',
      joinCount: attraction.joinCount || 0,
      longitude: attraction.longitude,
      latitude: attraction.latitude,
      destinationId: id, // 当前目的地ID
      destinationName: destinationData?.name || ''
    };
    
    e.dataTransfer.setData('application/json', JSON.stringify(dragData));
    e.dataTransfer.effectAllowed = 'copy';
    
    // 添加拖拽样式
    e.target.style.opacity = '0.5';
  };

  // 处理景点拖拽结束
  const handleAttractionDragEnd = (e) => {
    e.target.style.opacity = '1';
  };

  // 格式化营业时间
  const formatOpeningHours = (openingHours) => {
    if (!openingHours) return '';
    
    // 如果是字符串，尝试解析为对象
    const hoursObj = typeof openingHours === 'string' ? JSON.parse(openingHours) : openingHours;
    
    // 中文星期映射
    const dayMapping = {
      monday: '周一',
      tuesday: '周二',
      wednesday: '周三',
      thursday: '周四',
      friday: '周五',
      saturday: '周六',
      sunday: '周日'
    };

    // 检查是否所有时间都相同
    const firstTime = hoursObj[Object.keys(hoursObj)[0]];
    const allSameTime = Object.values(hoursObj).every(time => time === firstTime);

    if (allSameTime) {
      return `每天 ${firstTime}`;
    }

    // 否则按天显示
    return Object.entries(hoursObj)
      .map(([day, time]) => `${dayMapping[day.toLowerCase()]}: ${time}`)
      .join('；');
  };

  if (!destinationData) {
    return (
      <div className="destination-detail-page">
        <div style={{ textAlign: 'center', padding: '50px' }}>
          <div>加载中...</div>
        </div>
      </div>
    );
  }

  return (
    <div className="destination-detail-page">
      <a 
        href="#" 
        className="back-link"
        onClick={(e) => { e.preventDefault(); navigate('/explore'); }}
      >
        <span style={{ fontSize: '1.2rem' }}>←</span> 返回探索页面
      </a>

      <div className="detail-layout">
        {/* Left Column */}
        <div className="left-column">
          <CityMap center={[destinationData.longitude,destinationData.latitude]}></CityMap>
          <h1 className="destination-detail-page-title" style={{ fontSize: '1.8rem', marginBottom: '15px' }}>
            {destinationData.name}
          </h1>
          <div className="destination-tags">
            {topTags.map((tag, index) => (
              <span key={index} className="destination-tag">{tag}</span>
            ))}
            <span className="destination-tag">热度: {destinationData.joinCount || 0}</span>
          </div>
          <p className="destination-description">{destinationData.description}</p>
          {/* 天气信息区域 */}
          <div style={{marginTop:'18px',background:'#f6f6f6',borderRadius:'8px',padding:'14px 18px',boxShadow:'0 2px 8px rgba(0,0,0,0.04)'}}>
            <div style={{display:'flex',alignItems:'center',gap:'12px'}}>
              <span style={{fontSize:'22px'}}>🌤️</span>
              <span style={{fontWeight:600,fontSize:'1.1rem'}}>天气</span>
              <button onClick={()=>setShowForecast(true)} style={{marginLeft:'auto',background:'#e3f2fd',color:'#1976d2',border:'none',borderRadius:'6px',padding:'4px 12px',cursor:'pointer'}}>查看天气预报</button>
            </div>
            {weatherError && <div style={{color:'red',marginTop:'8px'}}>{weatherError}</div>}
            {liveWeather ? (
              <div style={{marginTop:'8px',fontSize:'15px',lineHeight:'1.8'}}>
                <div>城市/区：{liveWeather.city}</div>
                <div>天气：{liveWeather.weather}</div>
                <div>温度：{liveWeather.temperature}℃</div>
                <div>风向：{liveWeather.windDirection}</div>
                <div>风力：{liveWeather.windPower}级</div>
                <div>湿度：{liveWeather.humidity}</div>
                <div>更新时间：{liveWeather.reportTime}</div>
              </div>
            ) : (
              <div style={{marginTop:'8px',color:'#888'}}>天气信息加载中...</div>
            )}
          </div>
          {/* 天气预报弹窗 */}
          {showForecast && (
            <div style={{position:'fixed',left:0,top:0,width:'100vw',height:'100vh',background:'rgba(0,0,0,0.25)',zIndex:9999,display:'flex',alignItems:'center',justifyContent:'center'}} onClick={()=>setShowForecast(false)}>
              <div style={{background:'#fff',borderRadius:'12px',padding:'28px 32px',minWidth:'340px',maxWidth:'90vw',boxShadow:'0 8px 32px rgba(0,0,0,0.18)',position:'relative'}} onClick={e=>e.stopPropagation()}>
                <button style={{position:'absolute',top:10,right:10,background:'#e3f2fd',color:'#1976d2',border:'none',borderRadius:'50%',width:32,height:32,fontSize:18,cursor:'pointer'}} onClick={()=>setShowForecast(false)}>×</button>
                <h3 style={{marginBottom:'18px',fontWeight:600,fontSize:'1.2rem'}}>未来4天天气预报</h3>
                {forecast.length > 0 ? forecast.map((item, idx) => (
                  <div key={idx} style={{borderBottom:'1px solid #eee',marginBottom:8,paddingBottom:8}}>
                    <div>日期：{item.date}</div>
                    <div>白天天气：{item.dayWeather}，夜间天气：{item.nightWeather}</div>
                    <div>白天温度：{item.dayTemp}℃，夜间温度：{item.nightTemp}℃</div>
                    <div>白天风向：{item.dayWindDir}，夜间风向：{item.nightWindDir}</div>
                    <div>白天风力：{item.dayWindPower}，夜间风力：{item.nightWindPower}</div>
                  </div>
                )) : <div style={{color:'#888'}}>暂无天气预报数据</div>}
              </div>
            </div>
          )}
        </div>

        <div className="divider"></div>

        {/* Right Column */}
        <div className="right-column">
          {/* 搜索区域 */}
          <div className="search-section">
            <div className="search-header">
              <h2 className="section-header">热门景点推荐</h2>
            </div>
            
            {/* 搜索框 */}
            <div className="search-input-wrapper">
              <span className="search-icon">🔍</span>
              <input
                type="text"
                className="search-input"
                placeholder="搜索景点名称或描述..."
                value={searchText}
                onChange={(e) => handleSearchChange(e.target.value)}
              />
            </div>

            {/* 标签选择 */}
            <div className="tags-section">
              <label className="tags-label">筛选标签</label>
              <div className="tags-container">
                {allTags.slice(0, 30).map(tag => (
                  <span
                    key={tag}
                    className={`tag-item ${selectedTags.includes(tag) ? 'active' : ''}`}
                    onClick={() => handleTagClick(tag)}
                  >
                    {tag}
                  </span>
                ))}
              </div>
            </div>

            {/* 搜索结果提示 */}
            {isSearchActive && (
              <div className="search-results-info">
                <span>
                  {debouncedSearchText && (
                    <span style={{ color: '#1976d2', fontWeight: '600' }}>
                      {debouncedSearchText && selectedTags.length > 0 ? '语义搜索' : '语义搜索'}: "{debouncedSearchText}"
                    </span>
                  )}
                  {debouncedSearchText && selectedTags.length > 0 && ', '}
                  {selectedTags.length > 0 && `已选标签: ${selectedTags.join(', ')}`}
                </span>
                <span className="results-count">
                  共 {totalElements} 个景点
                  {debouncedSearchText && (
                    <span style={{ color: '#666', fontSize: '0.9em', marginLeft: '8px' }}>
                      (基于AI语义理解)
                    </span>
                  )}
                </span>
              </div>
            )}
          </div>
          {loading ? (
            <div style={{ textAlign: 'center', padding: '50px' }}>
              <div>
                {debouncedSearchText ? '正在使用AI语义搜索景点...' : '加载中...'}
              </div>
            </div>
          ) : (
            <>
              <div className="attractions-grid">
                {attractions.map((attraction) => (
                  <div 
                    key={attraction.id} 
                    className="attraction-card"
                    draggable
                    onDragStart={(e) => handleAttractionDragStart(e, attraction)}
                    onDragEnd={handleAttractionDragEnd}
                  >
                    <div 
                      className="card-image" 
                      style={{ 
                        backgroundImage: `url(${getAttractionImageUrl(attraction)})`,
                        backgroundSize: 'cover',
                        backgroundPosition: 'center',
                        display: 'flex', 
                        alignItems: 'center', 
                        justifyContent: 'center', 
                        color: '#fff', 
                        fontSize: '1.1rem', 
                        fontWeight: '600' 
                      }}
                      onError={(e) => handleImageError(e, attraction)}
                      onLoad={(e) => handleImageLoad(e, attraction)}
                    >
                    </div>
                    <div className="card-info">
                      <div className="card-header" style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                        <h3 className="card-title" style={{ marginBottom: '0' }}>{attraction.name}</h3>
                        <div className="card-tags" style={{ display: 'flex', gap: '6px' }}>
                          <span className="card-tag" > {attraction.category}</span>
                          <span 
                            className="card-tag" 
                            style={{ 
                              background: '#e3f2fd'
                            }}
                          >
                            热度: {attraction.joinCount || 0}
                          </span>
                        </div>
                      </div>
                      
                      {/* 营业时间显示 */}
                      {attraction.openingHours && (
                        <div className="opening-hours">
                          <span style={{ marginRight: '8px' }}>⏰</span>
                          <span>营业时间: {formatOpeningHours(attraction.openingHours)}</span>
                        </div>
                      )}
                      
                      {/* 景点标签显示 */}
                      {tagsLoading && !attractionTags[attraction.id] && (
                        <div className="attraction-top-tags" style={{ margin: '8px 0', display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                          <span className="top-tag" style={{ color: '#999', fontStyle: 'italic' }}>
                            标签加载中... ({tagsProgress.current}/{tagsProgress.total})
                          </span>
                        </div>
                      )}
                      {attractionTags[attraction.id] && attractionTags[attraction.id].length > 0 && (
                        <div className="attraction-top-tags" style={{ margin: '8px 0', display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                          {attractionTags[attraction.id].map((tag, tagIndex) => (
                            <span 
                              key={tagIndex} 
                              className={`top-tag ${selectedTags.includes(tag)?'active':''}`} 
                              onClick={() => handleTagClick(tag)}
                              style={{cursor:'pointer'}}
                            >{tag}</span>
                          ))}
                        </div>
                      )}
                      
                      <p className="card-description">{attraction.description || '暂无描述'}</p>
                    </div>
                  </div>
                ))}
              </div>

              {/* 分页控件 */}
              {totalPages > 1 && (
                <div className="pagination" style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', margin: '20px 0', gap: '10px' }}>
                  <button 
                    onClick={() => handlePageChange(currentPage - 1)} 
                    disabled={currentPage === 0}
                    style={{ padding: '8px 12px', border: '1px solid #ddd', borderRadius: '4px', background: currentPage === 0 ? '#f5f5f5' : '#fff' }}
                  >
                    上一页
                  </button>
                  <span style={{ margin: '0 15px' }}>
                    第 {currentPage + 1} 页 / 共 {totalPages} 页
                  </span>
                  <button 
                    onClick={() => handlePageChange(currentPage + 1)} 
                    disabled={currentPage >= totalPages - 1}
                    style={{ padding: '8px 12px', border: '1px solid #ddd', borderRadius: '4px', background: currentPage >= totalPages - 1 ? '#f5f5f5' : '#fff' }}
                  >
                    下一页
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
};

export default DestinationDetail; 