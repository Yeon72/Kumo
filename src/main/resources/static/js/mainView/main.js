/**
 * KUMO Map Application
 * 기능: 구글 맵 연동, 클러스터링, GPS 기반 주변 공고 검색, UI 인터랙션
 */

/**
 * 애플리케이션의 전역 상태를 관리하는 객체입니다.
 * 지도 객체, 마커 배열, 필터 상태 및 사용자 위치 정보를 보관합니다.
 */
const AppState = {
    map: null,               // 구글 맵 객체
    markerCluster: null,     // 마커 클러스터 객체
    jobMarkers: [],          // 개별 마커 배열
    debounceTimer: null,     // 디바운스 타이머
    currentXhr: null,        // 현재 진행 중인 AJAX 요청 (취소용)
    lastBounds: null,        // 직전 지도 영역 정보
    maskPolygon: null,       // 지도 경계선 폴리곤
    ignoreIdle: false,       // 지도가 강제 이동 중일 때 자동 갱신 방지 스위치
    isFilterMode: false,     // 저장/최근 탭 활성화 시 자동 갱신 차단 스위치
    userLocation: null,      // 사용자의 GPS 위치 정보
    isLocationMode: false,   // 내 주변 보기 모드 활성 여부
    scrapedJobIds: new Set() // 사용자가 찜한 공고 ID 세트
};

/**
 * DOM 로드 완료 후 초기 이벤트 바인딩 및 설정을 수행합니다.
 */
$(document).ready(function() {
    /**
     * 바텀 시트 핸들 클릭 이벤트: 지도의 탐색 리스트를 토글합니다.
     */
    $('.sheet-handle').on('click', function() {
        const $sheet = $('#bottomSheet');
        const $sheetTitle = $('#sheetTitle');

        $sheetTitle.text(MapMessages.titleExplore);
        $sheet.toggleClass('active');

        if ($sheet.hasClass('active')) {
            UIManager.closeJobCard();
        }
    });

    /**
     * 구글 지도 초기화 콜백 등록
     */
    window.initMap = MapManager.init;

    /**
     * 공고 상세 카드 닫기 버튼 이벤트
     */
    $(".btn-close-card").on('click', function () {
        UIManager.closeJobCard();
    });

    /**
     * 네비게이션 탭 클릭 이벤트: 각 기능별 필터링 모드를 전환합니다.
     */
    $(".nav-item").on('click', function () {
        const $this = $(this);
        const tabName = $this.data('tab');

        if (tabName === 'chat') {
            UIManager.switchTab('chat');
            return;
        }

        if ($this.hasClass('active')) {
            $this.removeClass('active');
            UIManager.switchTab('explore');
            return;
        }

        $('.nav-item').removeClass('active');
        $this.addClass('active');
        UIManager.switchTab(tabName);
    });

    /**
     * 로그인 상태일 경우 초기 찜 목록(스크랩) 동기화
     */
    if (typeof isUserLoggedIn !== 'undefined' && isUserLoggedIn) {
        JobService.initSavedJobs();
    }
});

/**
 * 구글 지도 생성, 이벤트 리스너 바인딩, 테마 동기화 등 지도 핵심 기능을 담당합니다.
 */
const MapManager = {
    /**
     * 지도 객체를 생성하고 초기 중심 좌표 및 스타일을 설정합니다.
     */
    init: function() {
        const mapElement = document.getElementById('map');
        if (!mapElement) return;

        const tokyo = { lat: 35.6804, lng: 139.7690 };
        const isDark = document.body.classList.contains('dark-mode') || localStorage.getItem('theme') === 'dark';
        const initialStyle = isDark ? MapStyles.dark : MapStyles.light;

        AppState.map = new google.maps.Map(mapElement, {
            center: tokyo,
            zoom: 10,
            disableDefaultUI: true,
            styles: initialStyle,
            gestureHandling: 'greedy',
            maxZoom: 18
        });

        MapManager.drawMasking();
        MapManager.bindMapEvents();
        MapManager.observeThemeChange();
    },

    /**
     * 지도의 idle(정지), 클릭 등 주요 이벤트를 바인딩합니다.
     */
    bindMapEvents: function() {
        const map = AppState.map;

        const triggerFetch = (delay) => {
            if(AppState.ignoreIdle || AppState.isFilterMode) return;

            clearTimeout(AppState.debounceTimer);
            AppState.debounceTimer = setTimeout(() => {
                const bounds = map.getBounds();
                if (!bounds || (AppState.lastBounds && bounds.equals(AppState.lastBounds))) return;

                AppState.lastBounds = bounds;
                JobService.loadJobs(bounds);
            }, delay);
        };

        map.addListener("idle", () => triggerFetch(100));
        map.addListener("click", () => UIManager.closeJobCard());
    },

    /**
     * 사용자의 현재 GPS 위치를 추적하여 지도의 중심으로 이동시킵니다.
     */
    moveToCurrentLocation: function() {
        if (!navigator.geolocation) {
            alert("브라우저가 위치 정보를 지원하지 않습니다.");
            return;
        }

        navigator.geolocation.getCurrentPosition(
            (position) => {
                const pos = { lat: position.coords.latitude, lng: position.coords.longitude };
                AppState.userLocation = pos;
                AppState.map.setCenter(pos);
                AppState.map.setZoom(15);

                new google.maps.Marker({
                    position: pos,
                    map: AppState.map,
                    icon: {
                        path: google.maps.SymbolPath.CIRCLE,
                        scale: 10,
                        fillColor: "#4285F4",
                        fillOpacity: 1,
                        strokeWeight: 2,
                        strokeColor: "white",
                    },
                });

                google.maps.event.addListenerOnce(AppState.map, 'idle', function() {
                    const bounds = AppState.map.getBounds();
                    AppState.lastBounds = bounds;
                    JobService.loadJobs(bounds);
                });
            },
            () => { alert("위치 정보를 가져올 수 없습니다."); }
        );
    },

    /**
     * 행정구역 경계 데이터를 기반으로 지도 외곽에 어두운 마스킹을 적용합니다.
     */
    drawMasking: function() {
        const worldCoords = [
            { lat: 85, lng: -180 }, { lat: 85, lng: 0 }, { lat: 85, lng: 180 },
            { lat: -85, lng: 180 }, { lat: -85, lng: 0 }, { lat: -85, lng: -180 },
            { lat: 85, lng: -180 }
        ];

        const tokyoPaths = typeof tokyoGeoJson !== 'undefined' ? Utils.getPathsFromGeoJson(tokyoGeoJson) : [];
        const osakaCityPaths = typeof osakaCityGeoJson !== 'undefined' ? Utils.getPathsFromGeoJson(osakaCityGeoJson) : [];
        const kansaiPaths = typeof osakaGeoJson !== 'undefined' ? Utils.getPathsFromGeoJson(osakaGeoJson, 1) : [];

        const isDark = document.body.classList.contains('dark-mode');
        const style = MapManager.getBoundaryStyle(isDark);

        AppState.maskPolygon = new google.maps.Polygon({
            paths: [worldCoords, ...tokyoPaths, ...osakaCityPaths, ...kansaiPaths],
            strokeColor: style.strokeColor,
            strokeOpacity: 1.0,
            strokeWeight: 2,
            fillColor: "#000000",
            fillOpacity: 0.6,
            map: AppState.map,
            clickable: false
        });
    },

    /**
     * 브라우저 테마 변경을 감지하여 실시간으로 지도 스타일을 갱신합니다.
     */
    observeThemeChange: function() {
        const observer = new MutationObserver(() => {
            const isDark = document.body.classList.contains('dark-mode');
            MapManager.setMapStyle(isDark);
        });
        observer.observe(document.body, { attributes: true });
    },

    /**
     * 테마에 따른 지도 옵션 및 마스킹 폴리곤을 재설정합니다.
     */
    setMapStyle: function(isDark) {
        if (!AppState.map) return;
        AppState.map.setOptions({ styles: isDark ? MapStyles.dark : MapStyles.light });
        if (AppState.maskPolygon) AppState.maskPolygon.setMap(null);
        MapManager.drawMasking();
    },

    /**
     * 테마별 경계선 색상을 반환합니다.
     */
    getBoundaryStyle: function(isDark) {
        return { strokeColor: isDark ? '#FF6B6B' : '#fB0000' };
    },

    /**
     * 선택된 지역(도쿄/오사카)의 주요 좌표로 화면을 전환합니다.
     */
    changeRegion: function(regionCode) {
        if (!AppState.map) return;
        AppState.isFilterMode = false;
        AppState.isLocationMode = false;
        AppState.ignoreIdle = true;

        let targetPos = (regionCode === 'tokyo') ? { lat: 35.6895, lng: 139.6921 } : { lat: 34.6938, lng: 135.5019 };
        AppState.map.panTo(targetPos);
        AppState.map.setZoom(18);

        google.maps.event.addListenerOnce(AppState.map, "idle", () => {
            AppState.ignoreIdle = false;
            JobService.loadJobs(AppState.map.getBounds());
        });
    }
};

/**
 * 서버 API와 통신하여 구인 공고 데이터를 가져오고 메모리에 적재하는 서비스 객체입니다.
 */
const JobService = {
    /**
     * 현재 지도 영역 내의 공고 데이터를 비동기로 호출합니다.
     */
    loadJobs: function(bounds) {
        if (!AppState.map) return;
        $('#listBody').html(`<tr><td colspan="7" class="msg-box">${MapMessages.loading}</td></tr>`);
        const params = JobService.prepareParams(bounds);

        if (AppState.currentXhr) AppState.currentXhr.abort();

        AppState.currentXhr = $.ajax({
            url: '/map/api/jobs',
            method: 'GET',
            data: params,
            dataType: 'json',
            success: (data) => JobService.processData(data),
            error: (xhr, status) => {
                if (status !== 'abort') $('#listBody').html(`<tr><td colspan="7" class="msg-box">${MapMessages.loadFail}</td></tr>`);
            }
        });
    },

    /**
     * 요청 파라미터를 구성합니다. (위도/경도 범위 및 언어 설정)
     */
    prepareParams: function(bounds) {
        const ne = bounds.getNorthEast();
        const sw = bounds.getSouthWest();
        const lang = new URLSearchParams(window.location.search).get('lang') === 'ja' ? 'ja' : 'kr';
        UIManager.updateTableHeader(lang);
        return { minLat: sw.lat(), maxLat: ne.lat(), minLng: sw.lng(), maxLng: ne.lng(), lang: lang };
    },

    /**
     * 수신된 데이터를 필터링하고 UI 렌더링을 호출합니다.
     */
    processData: function(data) {
        let filtered = data;
        if (AppState.isLocationMode && AppState.userLocation) {
            filtered = data.filter(j => Utils.getDistanceFromLatLonInKm(AppState.userLocation.lat, AppState.userLocation.lng, j.lat, j.lng) <= 3.0);
        }
        MarkerManager.clearMarkers();
        UIManager.renderList(filtered);
        MarkerManager.renderMarkers(filtered);
    },

    /**
     * 사용자가 찜한 공고 목록을 서버에서 조회합니다.
     */
    loadSavedJobs: function() {
        const lang = new URLSearchParams(window.location.search).get('lang') === 'ja' ? 'ja' : 'kr';
        $.ajax({
            url: `/api/scraps?lang=${lang}`,
            method: 'GET',
            success: (data) => {
                AppState.scrapedJobIds.clear();
                if(data) data.forEach(j => AppState.scrapedJobIds.add(j.id + '_' + j.source));
                UIManager.renderList(data, true);
                MarkerManager.renderMarkers(data);
                $('#bottomSheet').addClass('active');
                UIManager.closeJobCard();
            }
        });
    },

    /**
     * 사용자가 입력한 키워드로 공고를 검색하고 결과 페이지로 이동합니다.
     */
    searchJobs: function() {
        const keyword = $('#keywordInput').val().trim();
        const lang = new URLSearchParams(window.location.search).get('lang') || 'kr';
        const region = $('#regionSelect').val() || 'tokyo';
        let url = `/map/search_list?lang=${lang}&mainRegion=${region}`;
        if (keyword) url += `&keyword=${encodeURIComponent(keyword)}`;
        window.location.href = url;
    },

    /**
     * 초기 로드 시 찜 상태 데이터를 세팅합니다.
     */
    initSavedJobs: function() {
        const lang = new URLSearchParams(window.location.search).get('lang') === 'ja' ? 'ja' : 'kr';
        $.ajax({
            url: `/api/scraps?lang=${lang}`,
            method: 'GET',
            success: (data) => {
                if(data) data.forEach(j => AppState.scrapedJobIds.add(j.id + '_' + j.source));
            }
        });
    }
};

/**
 * 지도 위의 마커 및 클러스터링 UI 요소를 관리합니다.
 */
const MarkerManager = {
    /**
     * 공고 배열을 마커 객체로 변환하여 지도에 표시하고 클러스터링을 적용합니다.
     */
    renderMarkers: function(jobs) {
        if (!jobs || jobs.length === 0) return;
        const markers = jobs.filter(j => j.lat && j.lng).map(j => {
            const m = new google.maps.Marker({
                position: { lat: j.lat, lng: j.lng },
                icon: MarkerManager.createCustomMarkerIcon('#EA4335'),
            });
            m.addListener("click", () => UIManager.openJobCard(j));
            return m;
        });
        AppState.jobMarkers = markers;
        if (AppState.markerCluster) {
            AppState.markerCluster.clearMarkers();
            AppState.markerCluster.addMarkers(markers);
        } else {
            AppState.markerCluster = new markerClusterer.MarkerClusterer({
                map: AppState.map,
                markers,
                renderer: MarkerManager.getClusterRenderer(),
                algorithm: new markerClusterer.GridAlgorithm({ gridSize: 80, maxZoom: 15 })
            });
        }
    },

    /**
     * 모든 마커를 제거합니다.
     */
    clearMarkers: function() {
        if (AppState.markerCluster) AppState.markerCluster.clearMarkers();
        AppState.jobMarkers = [];
    },

    /**
     * 클러스터 마커의 구름 모양 커스텀 디자인을 정의합니다.
     */
    getClusterRenderer: function() {
        return {
            render: ({ count, position }) => {
                const path = "M 10 22 C 2 22, 2 12, 9 13 C 9 3, 23 3, 23 11 C 25 5, 34 7, 31 14 C 38 14, 38 22, 30 22 Z";
                return new google.maps.Marker({
                    label: { text: String(count), color: "#4285F4", fontSize: "14px", fontWeight: "bold" },
                    position,
                    icon: {
                        path: path, scale: 2.5, fillColor: "#ffffff", fillOpacity: 0.95,
                        strokeWeight: 2.0, strokeColor: "#4285F4", anchor: new google.maps.Point(19, 14),
                        labelOrigin: new google.maps.Point(19, 14)
                    },
                    zIndex: Number(google.maps.Marker.MAX_ZINDEX) + count
                });
            }
        };
    },

    /**
     * 개별 마커의 SVG 아이콘 경로를 반환합니다.
     */
    createCustomMarkerIcon: function(color) {
        return {
            path: 'M 12,0 C 5.373,0 0,5.373 0,12 c 0,7.194 10.74,22.25 11.31,23.03 l 0.69,0.97 l 0.69,-0.97 C 13.26,34.25 24,19.194 24,12 C 24,5.373 18.627,0 12,0 Z',
            fillColor: color, fillOpacity: 1, strokeWeight: 1, strokeColor: '#ffffff',
            anchor: new google.maps.Point(12, 34), scale: 1
        };
    }
};

/**
 * 테이블 리스트 렌더링, 상세 카드 제어 등 화면상의 UI 변화를 전담합니다.
 */
const UIManager = {
    /**
     * 하단 바 선택 상태에 따라 콘텐츠를 전환합니다.
     */
    switchTab: function(tabName) {
        const $title = $('#sheetTitle');
        if (tabName === 'nearby') {
            $title.text(MapMessages.titleNearby);
            AppState.isFilterMode = true; AppState.isLocationMode = true;
            MapManager.moveToCurrentLocation();
        } else if (tabName === 'saved') {
            $title.text(MapMessages.titleSaved);
            AppState.isFilterMode = true; JobService.loadSavedJobs();
        } else if (tabName === 'explore') {
            $title.text(MapMessages.titleExplore);
            AppState.isFilterMode = false;
            if (AppState.map) JobService.loadJobs(AppState.map.getBounds());
        } else if (tabName === 'chat' && typeof openGlobalChatList === 'function') {
            openGlobalChatList();
        }
    },

    /**
     * 공고 리스트를 바텀 시트 테이블 내부에 동적 생성합니다.
     */
    renderList: function(jobs, isSavedMode = false) {
        const $tbody = $('#listBody');
        const lang = new URLSearchParams(window.location.search).get('lang') === 'ja' ? 'ja' : 'kr';
        if (!jobs || jobs.length === 0) {
            $tbody.html(`<tr><td colspan="7" class="msg-box">${MapMessages.emptyJob}</td></tr>`);
            return;
        }
        let html = '';
        jobs.forEach(j => {
            const jobSig = j.id + '_' + j.source;
            const isSaved = AppState.scrapedJobIds.has(jobSig);
            const btnTxt = isSaved ? (lang === 'ja' ? '保存解除' : '찜해제') : MapMessages.btnSave;
            const btnClass = isSaved ? 'btn btn-saved' : 'btn';

            html += `<tr>
                <td><span class="title-text" style="cursor:pointer; text-decoration:underline;" onclick="MapManager.moveToJobLocation(${j.lat}, ${j.lng})">${j.title || MapMessages.fbTitle}</span></td>
                <td>${j.companyName || MapMessages.fbCompany}</td>
                <td>${j.address || '-'}</td>
                <td>${j.wage || MapMessages.fbWage}</td>
                <td>${j.contactPhone || '-'}</td>
                <td>${j.managerName || 'Admin'}</td>
                <td>
                    <div class="btn-wrap">
                        ${isUserLoggedIn ? `<button class="${btnClass}" onclick="UIManager.toggleListScrap(this, ${isSavedMode})" data-id="${j.id}" data-source="${j.source}">${btnTxt}</button>` : ''}
                        <button class="btn btn-view" onclick="location.href='/map/jobs/detail?id=${j.id}&source=${j.source}&lang=${lang}'">${MapMessages.btnDetail}</button>
                    </div>
                </td>
            </tr>`;
        });
        $tbody.html(html);
        UIManager.updateTableHeader();
    },

    /**
     * 마커 클릭 시 우측 하단에 공고 요약 카드를 노출합니다.
     */
    openJobCard: function(job) {
        const lang = new URLSearchParams(window.location.search).get('lang') === 'ja' ? 'ja' : 'kr';
        $('#card-company').text(job.companyName || MapMessages.fbCompany);
        $('#card-title').text(job.title);
        $('.job-address').html(`${MapMessages.labelAddress} <span id="card-address">${job.address || '-'}</span>`);
        $('#card-phone').text(job.contactPhone || '-');
        $('#card-img').attr('src', job.thumbnailUrl || 'https://placehold.co/300');

        const $scrapBtn = $('#jobDetailCard .btn-scrap');
        if (isUserLoggedIn) {
            $scrapBtn.show();
            const isSaved = AppState.scrapedJobIds.has(job.id + '_' + job.source);
            $scrapBtn.toggleClass('favorite', isSaved).text(isSaved ? (lang === 'ja' ? '保存解除' : '찜해제') : MapMessages.btnSaveCard);
            $scrapBtn.off('click').on('click', () => UIManager.toggleCardScrap(job.id, job.source));
        } else {
            $scrapBtn.hide();
        }
        $('#jobDetailCard').show();
        $('#bottomSheet').removeClass('active');
    },

    /**
     * 공고 요약 카드를 숨깁니다.
     */
    closeJobCard: function() { $('#jobDetailCard').hide(); },

    /**
     * 다국어 설정에 따라 테이블 헤더 텍스트를 변경합니다.
     */
    updateTableHeader: function() {
        $('#tableHeader th').each(function(i) { if(MapMessages.table[i]) $(this).text(MapMessages.table[i]); });
    },

    /**
     * 리스트에서 찜하기를 처리하고 UI 상태를 변경합니다.
     */
    toggleListScrap: function(btn, isSavedMode) {
        const $btn = $(btn);
        const jobId = $btn.data('id');
        const source = $btn.data('source');
        const lang = new URLSearchParams(window.location.search).get('lang') === 'ja' ? 'ja' : 'kr';

        $.ajax({
            url: '/api/scraps',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ targetPostId: jobId, targetSource: source }),
            success: (res) => {
                const isSaved = res.isScraped || res.scraped || res === true;
                const sig = jobId + '_' + source;
                if (isSaved) {
                    $btn.addClass('btn-saved').text(lang === 'ja' ? '保存解除' : '찜해제');
                    AppState.scrapedJobIds.add(sig);
                } else {
                    if (isSavedMode) $btn.closest('tr').fadeOut(300);
                    else $btn.removeClass('btn-saved').text(MapMessages.btnSave);
                    AppState.scrapedJobIds.delete(sig);
                }
            }
        });
    }
};

/**
 * 좌표 계산 및 데이터 포맷 변환 등 범용 유틸리티 함수를 제공합니다.
 */
const Utils = {
    /**
     * GeoJson 데이터를 구글 맵 좌표 경로 배열로 변환합니다.
     */
    getPathsFromGeoJson: function(json, specificIndex = -1) {
        const paths = [];
        if (!json) return paths;
        const features = (json.type === "FeatureCollection") ? json.features : [json];
        features.forEach(f => {
            if (!f.geometry) return;
            if (f.geometry.type === "MultiPolygon") {
                f.geometry.coordinates.forEach((poly, idx) => {
                    if (specificIndex >= 0 && idx !== specificIndex) return;
                    paths.push(poly[0].map(c => ({ lat: c[1], lng: c[0] })));
                });
            } else if (f.geometry.type === "Polygon") {
                paths.push(f.geometry.coordinates[0].map(c => ({ lat: c[1], lng: c[0] })));
            }
        });
        return paths;
    },

    /**
     * 하버사인 공식을 사용하여 두 지점 간의 거리를 킬로미터 단위로 반환합니다.
     */
    getDistanceFromLatLonInKm: function(lat1, lon1, lat2, lon2) {
        const R = 6371;
        const dLat = (lat2 - lat1) * (Math.PI / 180);
        const dLon = (lon2 - lon1) * (Math.PI / 180);
        const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(lat1 * (Math.PI / 180)) * Math.cos(lat2 * (Math.PI / 180)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
};