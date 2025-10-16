import React, { useEffect, useRef, useCallback, useImperativeHandle, forwardRef } from 'react';
import './Map.css';
import amapConfig from '../config/amapConfig';

window._AMapSecurityConfig = {
    securityJsCode: amapConfig.securityJsCode,
};

const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));
    
const RouteMap = forwardRef(({ centre, markers = [], isOverview = false }, ref) => {
    const mapRef = useRef(null);
    const markersRef = useRef([]);
    const polylineRef = useRef([]);
    const routingServicesRef = useRef([]); // 存储路由服务实例
    const mountedRef = useRef(true); // 跟踪组件是否已卸载
    const mapInitializedRef = useRef(false); // 跟踪地图是否已初始化
    const AMapRef = useRef(null); // 存储AMap实例

    // 暴露重新初始化方法给父组件
    useImperativeHandle(ref, () => ({
        reinitializeMap: async () => {
            console.log('强制重新初始化地图');
            cleanupMap();
            if (mountedRef.current) {
                await sleep(4000);
            initMap();
            }
        }
    }));

    // 清理函数
    const cleanupMap = useCallback(() => {
        console.log('[地图] 开始清空地图内容');
        // 清理路由服务
        if (routingServicesRef.current.length > 0) {
            routingServicesRef.current.forEach(service => {
                if (service && typeof service.destroy === 'function') {
                    try {
                        service.destroy();
                    } catch (error) {
                        console.warn('清理路由服务时出错:', error);
                    }
                }
            });
            routingServicesRef.current = [];
        }

        // 清理标记
        if (markersRef.current.length > 0) {
            markersRef.current.forEach(marker => {
                if (marker && mapRef.current && typeof mapRef.current.remove === 'function') {
                    try {
                        mapRef.current.remove(marker);
                    } catch (error) {
                        console.warn('清理标记时出错:', error);
                    }
                }
            });
            markersRef.current = [];
        }

        // 清理路线
        if (polylineRef.current.length > 0) {
            polylineRef.current.forEach(line => {
                if (Array.isArray(line)) {
                    line.forEach(l => {
                        if (l && mapRef.current && typeof mapRef.current.remove === 'function') {
                            try {
                                mapRef.current.remove(l);
                            } catch (error) {
                                console.warn('清理路线时出错:', error);
                            }
                        }
                    });
                } else if (line && mapRef.current && typeof mapRef.current.remove === 'function') {
                    try {
                        mapRef.current.remove(line);
                    } catch (error) {
                        console.warn('清理路线时出错:', error);
                    }
                }
                });
            polylineRef.current = [];
        }

        // 销毁地图实例
        if (mapRef.current && typeof mapRef.current.destroy === 'function') {
            try {
                mapRef.current.destroy();
            } catch (error) {
                console.warn('销毁地图实例时出错:', error);
            }
            mapRef.current = null;
            mapInitializedRef.current = false;
        }
        console.log('[地图] 地图内容清空完成');
    }, []);

    // 检查地图实例是否有效
    const isMapValid = useCallback(() => {
        return mountedRef.current && 
               mapRef.current && 
               mapInitializedRef.current && 
               typeof mapRef.current.add === 'function';
    }, []);

    // 安全的地图操作包装器
    const safeMapOperation = useCallback((operation, fallback = null) => {
        try {
            if (isMapValid()) {
                return operation();
            }
        } catch (error) {
            console.warn('地图操作失败，尝试重新初始化:', error);
            // 如果地图操作失败，尝试重新初始化
            setTimeout(() => {
                if (mountedRef.current) {
                    cleanupMap();
                    initMap();
                }
            }, 100);
        }
        return fallback;
    }, [isMapValid, cleanupMap]);

    // 创建自定义标记HTML内容
    const createMarkerContent = useCallback((markerData, index) => {
        const markerDiv = document.createElement('div');
        markerDiv.className = 'custom-marker';
        markerDiv.style.cssText = `
            position: relative;
            width: 40px;
            height: 40px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            border-radius: 50%;
            border: 3px solid white;
            box-shadow: 0 4px 12px rgba(0,0,0,0.3);
            display: flex;
            align-items: center;
            justify-content: center;
            color: white;
            font-weight: bold;
            font-size: 14px;
            cursor: pointer;
            transition: all 0.3s ease;
        `;
        
        markerDiv.innerHTML = `<span>${index + 1}</span>`;
        
        markerDiv.addEventListener('mouseenter', () => {
            markerDiv.style.transform = 'scale(1.2)';
            markerDiv.style.boxShadow = '0 6px 20px rgba(0,0,0,0.4)';
        });
        
        markerDiv.addEventListener('mouseleave', () => {
            markerDiv.style.transform = 'scale(1)';
            markerDiv.style.boxShadow = '0 4px 12px rgba(0,0,0,0.3)';
        });
        
        return markerDiv;
    }, []);

    // 计算两点之间的路线
    const calculateRoute = useCallback(async(AMap, map, from, to, transportMode) => {
        return new Promise(async(resolve, reject) => {
            await sleep(500);
            // 检查组件是否已卸载
            if (!mountedRef.current) {
                reject(new Error('Component unmounted'));
                return;
            }


            // 检查此地图实例是否有效
            if (!isMapValid()) {
                reject(new Error('Invalid map instance'));
                return;
            }

            let routingService;
            const fromPoint = new AMap.LngLat(from.longitude, from.latitude);
            const toPoint = new AMap.LngLat(to.longitude, to.latitude);

            try {
                // 根据交通方式选择不同的路径规划服务
                switch (transportMode) {
                    case '步行':
                        routingService = new AMap.Walking({
                            map: map,
                            hideMarkers: true,
                        });
                        break;
                    case '骑行':
                        routingService = new AMap.Riding({
                            map: map,
                            hideMarkers: true,
                        });
                        break;
                    case '公共交通':
                        routingService = new AMap.Transfer({
                            map: map,
                            hideMarkers: true,
                            policy: AMap.TransferPolicy.LEAST_TIME
                        });
                        break;
                    case '驾车':
                    default:
                        routingService = new AMap.Driving({
                            map: map,
                            hideMarkers: true,
                        });
                }

                // 保存路由服务实例以便清理
                routingServicesRef.current.push(routingService);

                routingService.search(fromPoint, toPoint, (status, result) => {
                    // 再次检查组件状态和地图实例
                    if (!isMapValid()) {
                        reject(new Error('Component unmounted or map destroyed'));
                        return;
                    }

                    if (status === 'complete' && result.routes && result.routes.length > 0) {
                        resolve(result);
                    } else {
                        // 如果路径规划失败，使用直线连接
                        const polyline = new AMap.Polyline({
                            path: [fromPoint, toPoint],
                            isOutline: false,
                            borderWeight: 2,
                            strokeColor: getTransportColor(transportMode),
                            strokeOpacity: 0.9,
                            strokeWeight: 3,
                            strokeStyle: 'dashed',
                            lineJoin: 'round',
                            lineCap: 'round',
                            zIndex: 10
                        });
                        resolve({ polyline });
                    }
                });
            } catch (error) {
                reject(error);
            }
        });
    }, [isMapValid]);

    // 获取交通方式对应的颜色
    const getTransportColor = useCallback((mode) => {
        switch (mode) {
            case '步行':
                return '#4caf50';
            case '骑行':
                return '#2196f3';
            case '驾车':
                return '#ff0000';
            case '公共交通':
                return '#ff9800';
            default:
                return '#bdbdbd';
        }
    }, []);

    // 更新标记和路线的函数
    const updateMarkers = useCallback(async (AMap, map, newMarkers) => {
        console.log(`[地图] 更新标记和路径，markers数量: ${newMarkers.length}`);

        // 检查组件是否已卸载
        if (!mountedRef.current) return;

        // 检查地图实例是否有效
        if (!isMapValid()) {
            console.warn('地图实例无效，跳过更新');
            return;
        }

        // 临时禁用错误弹窗
        const originalErrorHandler = window.onerror;
        const originalUnhandledRejectionHandler = window.onunhandledrejection;
        
        window.onerror = () => true; // 阻止错误弹窗
        window.onunhandledrejection = (event) => {
            event.preventDefault();
            return true;
        };

        try {
            // 添加新标记
            if (newMarkers && newMarkers.length > 0) {
                console.log(`[地图] 本次渲染 ${newMarkers.length} 个标记`, newMarkers);
                // 添加标记
                newMarkers.forEach((markerData, index) => {
                    if (!isMapValid()) return;
                    if (markerData.longitude && markerData.latitude) {
                        const markerContent = createMarkerContent(markerData, index);
                        const position = new AMap.LngLat(markerData.longitude, markerData.latitude);
                        const marker = new AMap.Marker({
                            position: position,
                            content: markerContent,
                            offset: new AMap.Pixel(-20, -20),
                            anchor: 'center'
                        });

                        const infoWindow = new AMap.InfoWindow({
                            content: `
                                <div style="padding: 15px; max-width: 280px; font-family: Arial, sans-serif;">
                                    <div style="display: flex; align-items: center; margin-bottom: 10px;">
                                        <div style="width: 8px; height: 8px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 50%; margin-right: 8px;"></div>
                                        <h4 style="margin: 0; color: #333; font-size: 16px; font-weight: 600;">${markerData.activityName || markerData.name || '未知地点'}</h4>
                                    </div>
                                    ${markerData.dayInfo ? `<p style="margin: 6px 0; color: #4dabf7; font-size: 12px; font-weight: 500;">📅 ${markerData.dayInfo}</p>` : ''}
                                    ${markerData.description ? `<p style="margin: 8px 0; color: #666; font-size: 13px; line-height: 1.4;">${markerData.description}</p>` : ''}
                                    ${markerData.address ? `<p style="margin: 8px 0; color: #999; font-size: 12px; line-height: 1.3;">📍 ${markerData.address}</p>` : ''}
                                    <div style="margin-top: 10px; padding-top: 8px; border-top: 1px solid #eee; font-size: 11px; color: #888;">
                                        标记 #${index + 1}
                                    </div>
                                </div>
                            `,
                            offset: [0, -30],
                            closeWhenClickMap: true
                        });

                        marker.on('click', () => {
                            if (isMapValid()) {
                            infoWindow.open(map, marker.getPosition());
                            }
                        });

                        // 使用安全的地图操作
                        safeMapOperation(() => {
                            mapRef.current.add(marker);
                        markersRef.current.push(marker);
                        });
                    }
                });

                // 只在非总览模式下绘制路线
                if (!isOverview) {
                    console.log('[地图] 开始渲染路径');
                    // 绘制路线
                    for (let i = 0; i < newMarkers.length - 1; i++) {
                        if (!isMapValid()) break;

                        const from = newMarkers[i];
                        const to = newMarkers[i + 1];
                        
                        // 判断是否为跨天连接
                        const isCrossDay = from.dayInfo && to.dayInfo && from.dayInfo !== to.dayInfo;
                        if (isCrossDay) continue; // 跨天直接跳过，不画线
                        
                        const mode = to.transport || '';
                        let transportMode = '驾车'; // 默认驾车
                        if (mode.includes('步行')) transportMode = '步行';
                        else if (mode.includes('骑行')) transportMode = '骑行';
                        else if (mode.includes('公共交通')) transportMode = '公共交通';
                        
                        try {
                            const routeResult = await calculateRoute(AMap, map, from, to, transportMode);
                            if (!isMapValid()) return;
                            
                            if (routeResult.polyline) {
                                polylineRef.current.push(routeResult.polyline);
                                safeMapOperation(() => {
                                    mapRef.current.add(routeResult.polyline);
                                });
                            } else if (routeResult.routes && routeResult.routes.length > 0) {
                                const route = routeResult.routes[0];
                                if (route.steps) {
                                    route.steps.forEach(step => {
                                        if (step.polyline && isMapValid()) {
                                            polylineRef.current.push(step.polyline);
                                            safeMapOperation(() => {
                                                mapRef.current.add(step.polyline);
                                            });
                                        }
                                    });
                                }
                            }
                        } catch (error) {
                            console.error('路径规划失败:', error);
                        }
                    }
                }
            }
                                    } catch (error) {
            console.error('更新标记和路线时出错:', error);
        } finally {
            // 恢复原始错误处理器
            window.onerror = originalErrorHandler;
            window.onunhandledrejection = originalUnhandledRejectionHandler;
        }
        console.log('[地图] 地图渲染完成');
    }, [calculateRoute, createMarkerContent, isOverview, isMapValid, safeMapOperation]);

    // 将initMap提取到组件作用域
    const initMap = useCallback(() => {
        console.log('[地图] 开始初始化地图');
        if (!mountedRef.current) return;
        
        // 临时禁用错误弹窗
        const originalErrorHandler = window.onerror;
        const originalUnhandledRejectionHandler = window.onunhandledrejection;
        
        window.onerror = () => true; // 阻止错误弹窗
        window.onunhandledrejection = (event) => {
            event.preventDefault();
            return true;
        };
        
        try {
            window.AMapLoader.load({
                key: amapConfig.key,
                version: amapConfig.version,
                plugins: ['AMap.Driving', 'AMap.Walking', 'AMap.Transfer', 'AMap.Riding'],
            }).then((AMap) => {
                console.log('[地图] 高德API加载完成，准备创建地图实例');
                if (!mountedRef.current) return;
                
                AMapRef.current = AMap;
                
                let mapCenter = [104.114129, 37.550339];
                let mapZoom = 4;
                if (Array.isArray(centre) && centre.length === 2 && typeof centre[0] === 'number' && typeof centre[1] === 'number') {
                    mapCenter = centre;
                    mapZoom = 11;
                }
                
                // 检查容器是否存在
                const container = document.getElementById('container');
                if (!container) return;
                
                const map = new AMap.Map('container', {
                    viewMode: '2D',
                    zoom: mapZoom,
                    center: mapCenter,
                    dragEnable: true,
                    scrollWheel: true,
                    doubleClickZoom: true,
                    keyboardEnable: true,
                    jogEnable: true,
                    animateEnable: true,
                    resizeEnable: true
                });
                
                mapRef.current = map;
                mapInitializedRef.current = true;
                
                // 初始化标记和路线
                if (mountedRef.current) {
                    console.log(`[地图] 初始化时渲染 ${markers.length} 个标记`);
                    updateMarkers(AMap, map, markers);
                }
            }).catch(error => {
                console.error('地图加载失败:', error);
            });
        } finally {
            // 恢复原始错误处理器
            window.onerror = originalErrorHandler;
            window.onunhandledrejection = originalUnhandledRejectionHandler;
        }
    }, [centre, markers, updateMarkers]);

    // 只初始化一次地图
    useEffect(() => {
        mountedRef.current = true;
        mapInitializedRef.current = false;
        
        if (!window.AMapLoader) {
            const loaderScript = document.createElement('script');
            loaderScript.src = 'https://webapi.amap.com/loader.js';
            loaderScript.async = true;
            loaderScript.onload = initMap;
            document.body.appendChild(loaderScript);
                                    } else {
            initMap();
        }
        
        return () => {
            mountedRef.current = false;
            mapInitializedRef.current = false;
            cleanupMap();
        };
    }, [cleanupMap, initMap]);

    // centre变化时，仅setCenter
    useEffect(() => {
        if (isMapValid() && Array.isArray(centre) && centre.length === 2 && typeof centre[0] === 'number' && typeof centre[1] === 'number') {
            safeMapOperation(() => {
                mapRef.current.setCenter(centre);
                mapRef.current.setZoom(11);
            });
        }
    }, [centre, isMapValid, safeMapOperation]);

    // markers变化时，更新标记和路线
    useEffect(() => {
        if (isMapValid() && AMapRef.current) {
            try {
                cleanupMap();
                if (!isMapValid()) {
                    initMap();
                } else {
                    updateMarkers(AMapRef.current, mapRef.current, markers);
                }
            } catch (error) {
                console.warn('更新地图时出错:', error);
                // 如果更新失败，重新初始化地图
                setTimeout(() => {
                    if (mountedRef.current) {
                        cleanupMap();
                        initMap();
                    }
                }, 100);
            }
        }
    }, [markers, cleanupMap, initMap, updateMarkers, isMapValid]);

    return (
        <div style={{
            border: '2px solid #1a2a6c',
            borderRadius: '16px',
            width: '100%',
            maxWidth: '900px',
            height: '100%',
            margin: '0 auto',
            background: '#fff',
            boxShadow: '0 8px 24px rgba(0,0,0,0.12)',
            overflow: 'hidden',
            display: 'flex',
            alignItems: 'stretch',
            justifyContent: 'center',
            position: 'relative',
        }}>
            <div id="container" style={{width: '100%', height: '100%'}}></div>
        </div>
    );
});

RouteMap.displayName = 'RouteMap';

export default RouteMap;
