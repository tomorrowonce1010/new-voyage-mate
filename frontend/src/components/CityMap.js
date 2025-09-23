import React, { useEffect, useState } from 'react';
import AMapLoader from '@amap/amap-jsapi-loader';
import './CityMap.css';

// 设置高德地图安全密钥
window._AMapSecurityConfig = {
    securityJsCode: '48a09726706b392b4c3c8907ac8cff2b',
};

const CityMap = ({ center = [] }) => {
    const [loading, setLoading] = useState(true);
    const [fullscreen, setFullscreen] = useState(false);

    useEffect(() => {
        let timer;
        function tryInitMap(containerId = 'container') {
            const container = document.getElementById(containerId);
            if (!container) {
                timer = setTimeout(() => tryInitMap(containerId), 50);
                return;
            }
            setLoading(true);
            AMapLoader.load({
                key: '0a98c59a1aac46b8430bed72a75cff36',
                version: '2.0',
                plugins: ['AMap.ToolBar', 'AMap.Scale'],
            })
                .then((AMap) => {
                    const mapInstance = new AMap.Map(containerId, {
                        viewMode: '2D',
                        zoom: 8,
                        center,
                        mapStyle: 'amap://styles/normal',
                    });
                    mapInstance.addControl(new AMap.Zoom());
                    mapInstance.addControl(new AMap.Scale());
                    mapInstance.addControl(new AMap.ToolBar({ position: 'RB' }));
                    setLoading(false);
                })
                .catch(() => setLoading(false));
        }
        tryInitMap();
        return () => {
            if (timer) clearTimeout(timer);
        };
    }, [center]);

    // 全屏地图初始化
    useEffect(() => {
        if (fullscreen) {
            let timer;
            function tryInitMapFull() {
                const container = document.getElementById('container-fullscreen');
                if (!container) {
                    timer = setTimeout(tryInitMapFull, 50);
                    return;
                }
                setLoading(true);
                AMapLoader.load({
                    key: '0a98c59a1aac46b8430bed72a75cff36',
                    version: '2.0',
                    plugins: ['AMap.ToolBar', 'AMap.Scale'],
                })
                    .then((AMap) => {
                        const mapInstance = new AMap.Map('container-fullscreen', {
                            viewMode: '2D',
                            zoom: 10,
                            center,
                            mapStyle: 'amap://styles/normal',
                        });
                        var s = new AMap.Scale();
                        var t = new AMap.ToolBar({ position: 'RB' });
                        mapInstance.addControl(s);
                        mapInstance.addControl(t);
                        setLoading(false);
                    })
                    .catch(() => setLoading(false));
            }
            tryInitMapFull();
            return () => {
                if (timer) clearTimeout(timer);
            };
        }
    }, [fullscreen, center]);

    return (
        <>
        <div style={{
            border: '2px solid #1a2a6c',
            borderRadius: '16px',
            width: '100%',
            maxWidth: '900px',
            height: '250px',
            margin: '40px auto',
            background: '#fff',
            boxShadow: '0 8px 24px rgba(0,0,0,0.12)',
            overflow: 'hidden',
            display: 'flex',
            alignItems: 'stretch',
            justifyContent: 'center',
            position: 'relative',
            cursor: 'zoom-in',
        }} onClick={() => setFullscreen(true)}>
            <div id="container" style={{ width: '100%', height: '100%' }}></div>
            {loading && (
                <div style={{
                    position: 'absolute',
                    width: '100%',
                    height: '100%',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    background: 'rgba(255,255,255,0.6)',
                    zIndex: 2
                }}>
                    <div className="loading-spinner"></div>
                </div>
            )}
            <div style={{position:'absolute',right:10,top:10,background:'#fff',borderRadius:'8px',padding:'2px 8px',fontSize:'12px',opacity:0.7}}>点击放大</div>
        </div>
        {fullscreen && (
            <div style={{
                position: 'fixed',
                left: 0,
                top: 0,
                width: '100vw',
                height: '100vh',
                background: 'rgba(0,0,0,0.7)',
                zIndex: 9999,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
            }} onClick={() => setFullscreen(false)}>
                <div style={{position:'relative',width:'90vw',height:'80vh',background:'#fff',borderRadius:'16px',overflow:'hidden',boxShadow:'0 8px 32px rgba(0,0,0,0.25)'}} onClick={e => e.stopPropagation()}>
                    <div id="container-fullscreen" style={{width:'100%',height:'100%'}}></div>
                    <button style={{position:'absolute',top:10,right:10,zIndex:2,background:'#fff',border:'none',borderRadius:'50%',width:36,height:36,fontSize:20,cursor:'pointer',boxShadow:'0 2px 8px rgba(0,0,0,0.15)'}} onClick={()=>setFullscreen(false)} title="关闭">×</button>
                </div>
            </div>
        )}
        </>
    );
};

export default CityMap;