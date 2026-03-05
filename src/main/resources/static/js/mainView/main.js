// 안녕하세요
/**
 * KUMO Map Application
 * 기능: 구글 맵 연동, 클러스터링, GPS 기반 주변 공고 검색, UI 인터랙션
 */

// ============================================================
// [1] 전역 상태 관리 (State Management)
// ============================================================
const AppState = {
    map: null,                // 구글 맵 객체
    markerCluster: null,      // 마커 클러스터 객체
    jobMarkers: [],           // 개별 마커 배열
    debounceTimer: null,      // 디바운스 타이머
    currentXhr: null,          // 현재 진행 중인 AJAX 요청 (취소용)
    lastBounds: null,
    maskPolygon: null,          // 지도 경계선
    ignoreIdle: false, // 지도가 강제 이동 중일 때 자동 갱신을 막는 스위치
    isFilterMode: false, // 저장/최근 탭이 켜져 있을 때 갱신을 '영구적'으로 막는 스위치
    userLocation: null,    // 내 GPS 위치 저장용
    isLocationMode: false,  // 내 주변 보기 모드 켜짐 여부
    scrapedJobIds: new Set() // 내가 찜한 공고 ID들을 기억할 수첩!
};

// ============================================================
// [2] 초기화 및 이벤트 바인딩 (Init & Events)
// ============================================================
$(document).ready(function() {
    // 바텀 시트 핸들 클릭 이벤트
    $('.sheet-handle').on('click', function() {
        const $sheet = $('#bottomSheet');
        const $sheetTitle = $('#sheetTitle');

        $sheetTitle.text(MapMessages.titleExplore);

        $sheet.toggleClass('active');

        if ($sheet.hasClass('active')) {
            UIManager.closeJobCard();
        }
    });

    // 지도 초기화 (Google Maps API 콜백으로 실행됨)
    window.initMap = MapManager.init;

    $(".btn-close-card").on('click', function () {
        UIManager.closeJobCard();
    });

    $(".nav-item").on('click', function () {
        const $this = $(this);
        const tabName = $this.data('tab');

        // 채팅 탭은 화면 이동이므로 바로 실행
        if (tabName === 'chat') {
            UIManager.switchTab('chat');
            return;
        }

        // 이미 켜진 탭을 '한 번 더' 눌렀을 때 -> 선택 해제 및 '자유 탐색' 모드로 복귀!
        if ($this.hasClass('active')) {
            $this.removeClass('active');
            UIManager.switchTab('explore');
            return;
        }

        // 토글 로직을 제거하고 직관적으로 탭을 이동하게 만듭니다.
        $('.nav-item').removeClass('active');
        $this.addClass('active');
        UIManager.switchTab(tabName);
    });

    // 🌟 [삭제 완료] 옛날 구형 채팅 위젯 드래그 이벤트 제거됨!

    // 로그인한 상태라면, 페이지가 켜지자마자 찜 목록을 수첩에 적어둡니다.
    if (typeof isUserLoggedIn !== 'undefined' && isUserLoggedIn) {
        JobService.initSavedJobs();
    }
});

// ============================================================
// [3] 지도 관리자 (Map Manager)
// ============================================================
const MapManager = {
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

    bindMapEvents: function() {
        const map = AppState.map;

        map.addListener("idle", () => {
            if(AppState.ignoreIdle || AppState.isFilterMode){
                return;
            }

            clearTimeout(AppState.debounceTimer);

            AppState.debounceTimer = setTimeout(() => {
                const bounds = map.getBounds();

                if (AppState.lastBounds && bounds.equals(AppState.lastBounds)) {
                    return;
                }

                AppState.lastBounds = bounds;
                JobService.loadJobs(bounds);

            }, 500);
        });

        map.addListener("click", () => {
            UIManager.closeJobCard();
        });
    },

    moveToCurrentLocation: function() {
        if (!navigator.geolocation) {
            alert("브라우저가 위치 정보를 지원하지 않습니다.");
            return;
        }

        navigator.geolocation.getCurrentPosition(
            (position) => {
                const pos = {
                    lat: position.coords.latitude,
                    lng: position.coords.longitude,
                };

                AppState.userLocation = pos;
                AppState.map.setCenter(pos);
                AppState.map.setZoom(15);

                new google.maps.Marker({
                    position: pos,
                    map: AppState.map,
                    title: "내 위치",
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
                    clearTimeout(AppState.debounceTimer);
                    const bounds = AppState.map.getBounds();
                    AppState.lastBounds = bounds;
                    JobService.loadJobs(bounds);
                });
            },
            () => { alert("위치 정보를 가져올 수 없습니다."); }
        );
    },

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
        const borderStyle = MapManager.getBoundaryStyle(isDark);

        AppState.maskPolygon = new google.maps.Polygon({
            paths: [worldCoords, ...tokyoPaths, ...osakaCityPaths, ...kansaiPaths],
            strokeColor: borderStyle.strokeColor,
            strokeOpacity: borderStyle.strokeOpacity,
            strokeWeight: borderStyle.strokeWeight,
            fillColor: "#000000",
            fillOpacity: 0.6,
            map: AppState.map,
            clickable: false
        });
    },

    observeThemeChange: function() {
        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                if (mutation.attributeName === 'class') {
                    const isDarkMode = document.body.classList.contains('dark-mode');
                    MapManager.setMapStyle(isDarkMode);
                }
            });
        });

        observer.observe(document.body, { attributes: true });
    },

    setMapStyle: function(isDark) {
        if (!AppState.map) return;
        const newStyle = isDark ? MapStyles.dark : MapStyles.light;
        AppState.map.setOptions({ styles: newStyle });

        if (AppState.maskPolygon) {
            AppState.maskPolygon.setMap(null);
        }

        MapManager.drawMasking();
    },

    getBoundaryStyle: function (isDark) {
        const boundaryColor = isDark ? '#FF6B6B' : '#fB0000';
        return {
            strokeColor : boundaryColor,
            strokeOpacity: 1.0,
            strokeWeight: 2
        }
    },

    changeRegion: function(regionCode) {
        if (!AppState.map) return;

        AppState.ignoreIdle = true;
        let targetPos;
        let targetZoom = 10;

        if (regionCode === 'tokyo') {
            targetPos = { lat: 35.6895, lng: 139.6921 };
            targetZoom = 18;
        } else if (regionCode === 'osaka') {
            targetPos = { lat: 34.6938, lng: 135.5019 };
            targetZoom = 18;
        }

        AppState.map.panTo(targetPos);
        AppState.map.setZoom(targetZoom);

        setTimeout(() => {
            AppState.ignoreIdle = false;
            const bounds = AppState.map.getBounds();
            AppState.lastBounds = bounds;
            JobService.loadJobs(bounds);
        }, 800);
    },

    moveToJobLocation: function(lat, lng) {
        if (!AppState.map || !lat || !lng) return;

        AppState.ignoreIdle = true;
        const targetPos = { lat: parseFloat(lat), lng: parseFloat(lng) };
        AppState.map.panTo(targetPos);
        AppState.map.setZoom(18);

        google.maps.event.addListenerOnce(AppState.map, "idle", function() {
            setTimeout(() => {
                AppState.lastBounds = AppState.map.getBounds();
                AppState.ignoreIdle = false;

                const targetMarker = AppState.jobMarkers.find(
                    m => m.getPosition().lat().toFixed(4) === targetPos.lat.toFixed(4) &&
                        m.getPosition().lng().toFixed(4) === targetPos.lng.toFixed(4)
                );

                if (targetMarker) {
                    targetMarker.setAnimation(google.maps.Animation.BOUNCE);
                    setTimeout(() => targetMarker.setAnimation(null), 2500);
                }
            }, 100);
        });
    },
};

// ============================================================
// [4] 데이터 서비스 (Job Service - AJAX)
// ============================================================
const JobService = {
    loadJobs: function(bounds) {
        if (!AppState.map) return;

        $('#listBody').html(`<tr><td colspan="7" class="msg-box">${MapMessages.loading}</td></tr>`);

        const params = JobService.prepareParams(bounds);

        if (AppState.currentXhr && AppState.currentXhr.readyState !== 4) {
            AppState.currentXhr.abort();
        }

        AppState.currentXhr = $.ajax({
            url: '/map/api/jobs',
            method: 'GET',
            data: params,
            dataType: 'json',
            success: function(data) {
                JobService.processData(data);
            },
            error: function(xhr, status, error) {
                if (status !== 'abort') {
                    console.error("AJAX Error:", error);
                    $('#listBody').html(`<tr><td colspan="7" class="msg-box">${MapMessages.loadFail}</td></tr>`);
                }
            }
        });
    },

    prepareParams: function(bounds) {
        const params = {};
        if (bounds) {
            const ne = bounds.getNorthEast();
            const sw = bounds.getSouthWest();
            params.minLat = sw.lat();
            params.maxLat = ne.lat();
            params.minLng = sw.lng();
            params.maxLng = ne.lng();
        } else {
            const urlParams = new URLSearchParams(window.location.search);
            params.minLat = urlParams.get('minLat') || 0;
        }

        const currentLang = new URLSearchParams(window.location.search).get('lang') === 'ja' ? 'ja' : 'kr';
        UIManager.updateTableHeader(currentLang);
        params.lang = currentLang;

        return params;
    },

    processData: function(data) {
        let filteredData = data;

        if (AppState.isLocationMode && AppState.userLocation) {
            const RADIUS_KM = 3.0;

            filteredData = data.filter(job => {
                if (!job.lat || !job.lng) return false;
                const dist = Utils.getDistanceFromLatLonInKm(
                    AppState.userLocation.lat,
                    AppState.userLocation.lng,
                    job.lat,
                    job.lng
                );
                return dist <= RADIUS_KM;
            });
        }

        MarkerManager.clearMarkers();
        UIManager.renderList(filteredData);
        MarkerManager.renderMarkers(filteredData);
    },

    loadSavedJobs: function() {
        const currentLang = new URLSearchParams(window.location.search).get('lang') === 'ja' ? 'ja' : 'kr';

        $.ajax({
            url: `/api/scraps?lang=${currentLang}`,
            method: 'GET',
            dataType: 'json',
            success: function(data) {
                AppState.scrapedJobIds.clear();
                if(data && data.length > 0) {
                    data.forEach(job => AppState.scrapedJobIds.add(job.id + '_' + job.source));
                }

                UIManager.renderList(data, true);
                MarkerManager.renderMarkers(data);

                $('#bottomSheet').addClass('active');
                UIManager.closeJobCard();
            },
            error: function(err) {
                console.error("찜한 목록 불러오기 실패:", err);
                $('#listBody').html(`<tr><td colspan="7" class="msg-box">${MapMessages.savedFail}</td></tr>`);
            }
        });
    },

    loadRecentJobs: function() {
        const recentJobsJson = sessionStorage.getItem('kumo_recent_jobs');
        let recentJobs = [];

        if (recentJobsJson) {
            recentJobs = JSON.parse(recentJobsJson);
        }

        UIManager.renderList(recentJobs);
        MarkerManager.renderMarkers(recentJobs);

        $('#bottomSheet').addClass('active');
        UIManager.closeJobCard();
    },

    addRecentJob: function(jobData) {
        if (!jobData || !jobData.id) return;

        const recentStr = sessionStorage.getItem('kumo_recent_jobs');
        let recentJobs = recentStr ? JSON.parse(recentStr) : [];

        recentJobs = recentJobs.filter(job => job.id !== jobData.id);
        recentJobs.unshift(jobData);

        if (recentJobs.length > 20) {
            recentJobs = recentJobs.slice(0, 20);
        }

        sessionStorage.setItem('kumo_recent_jobs', JSON.stringify(recentJobs));
    },

    searchJobs: function() {
        const keyword = $('#keywordInput').val().trim();
        const currentLang = new URLSearchParams(window.location.search).get('lang') || 'kr';

        let url = `/map/search_list?lang=${currentLang}`;
        if (keyword) {
            url += `&keyword=${encodeURIComponent(keyword)}`;
        }

        window.location.href = url;
    },

    initSavedJobs: function() {
        const currentLang = new URLSearchParams(window.location.search).get('lang') === 'ja' ? 'ja' : 'kr';
        $.ajax({
            url: `/api/scraps?lang=${currentLang}`,
            method: 'GET',
            dataType: 'json',
            success: function(data) {
                AppState.scrapedJobIds.clear();
                if(data && data.length > 0) {
                    data.forEach(job => AppState.scrapedJobIds.add(job.id + '_' + job.source));
                }
            }
        });
    },
};

// ============================================================
// [5] 마커 관리자 (Marker Manager - Clustering)
// ============================================================
const MarkerManager = {
    renderMarkers: function(jobs) {
        if (!jobs || jobs.length === 0) return;

        const map = AppState.map;
        AppState.jobMarkers = [];

        const markers = jobs
            .filter(job => job.lat && job.lng)
            .map(job => {
                const marker = new google.maps.Marker({
                    position: { lat: job.lat, lng: job.lng },
                    icon: MarkerManager.createCustomMarkerIcon('#EA4335'),
                });

                marker.addListener("click", () => {
                    UIManager.openJobCard(job);
                });

                return marker;
            });

        AppState.jobMarkers = markers;

        if (AppState.markerCluster) {
            AppState.markerCluster.clearMarkers();
            AppState.markerCluster.addMarkers(markers);
        } else {
            AppState.markerCluster = new markerClusterer.MarkerClusterer({
                map,
                markers,
                renderer: MarkerManager.getClusterRenderer(),
                algorithm: new markerClusterer.GridAlgorithm({
                    gridSize: 80,
                    maxZoom: 15
                })
            });
        }
    },

    clearMarkers: function() {
        if (AppState.markerCluster) {
            AppState.markerCluster.clearMarkers();
        }
        AppState.jobMarkers = [];
    },

    getClusterRenderer: function() {
        return {
            render: ({ count, position }) => {
                const cloudPath = "M 10 22 C 2 22, 2 12, 9 13 C 9 3, 23 3, 23 11 C 25 5, 34 7, 31 14 C 38 14, 38 22, 30 22 Z";
                let cloudColor = "#4285F4";

                return new google.maps.Marker({
                    label: {
                        text: String(count),
                        color: "white",
                        fontSize: "14px",
                        fontWeight: "bold"
                    },
                    position,
                    icon: {
                        path: cloudPath,
                        scale: 1.8,
                        fillColor: cloudColor,
                        fillOpacity: 0.95,
                        strokeWeight: 1.5,
                        strokeColor: "#ffffff",
                        anchor: new google.maps.Point(19, 14),
                        labelOrigin: new google.maps.Point(19, 14)
                    },
                    zIndex: Number(google.maps.Marker.MAX_ZINDEX) + count,
                });
            }
        };
    },

    createCustomMarkerIcon: function(color) {
        const svgPath = 'M 12,0 C 5.373,0 0,5.373 0,12 c 0,7.194 10.74,22.25 11.31,23.03 l 0.69,0.97 l 0.69,-0.97 C 13.26,34.25 24,19.194 24,12 C 24,5.373 18.627,0 12,0 Z';

        return {
            path: svgPath,
            fillColor: color,
            fillOpacity: 1,
            strokeWeight: 1,
            strokeColor: '#ffffff',
            anchor: new google.maps.Point(12, 34),
            labelOrigin: new google.maps.Point(12, 12),
            scale: 1
        };
    },
};

// ============================================================
// [6] UI 관리자 (UI Manager - jQuery)
// ============================================================
const UIManager = {
    switchTab: function(tabName) {
        console.log(`탭 전환 기능 실행: ${tabName}`);

        const $sheetTitle = $('#sheetTitle');

        if (tabName === 'nearby') {
            $sheetTitle.text(MapMessages.titleNearby);
            AppState.isFilterMode = true;
            AppState.isLocationMode = true;
            MapManager.moveToCurrentLocation();
        }
        else if (tabName === 'saved') {
            $sheetTitle.text(MapMessages.titleSaved);
            AppState.isFilterMode = true;
            AppState.isLocationMode = false;
            JobService.loadSavedJobs();
        }
        else if (tabName === 'recent') {
            $sheetTitle.text(MapMessages.titleRecent);
            AppState.isFilterMode = true;
            AppState.isLocationMode = false;
            JobService.loadRecentJobs();
        }
        else if (tabName === 'explore') {
            $sheetTitle.text(MapMessages.titleExplore);
            AppState.isFilterMode = false;
            AppState.isLocationMode = false;

            if (AppState.map) {
                const bounds = AppState.map.getBounds();
                if (bounds) {
                    AppState.lastBounds = bounds;
                    JobService.loadJobs(bounds);
                }
            }
        }
        else if (tabName === 'chat') {
            // 🌟 [핵심 변경] 새 프래그먼트 호출!
            openGlobalChatList();
        }
    },

    renderList: function(jobs, isSavedMode = false) {
        const $tbody = $('#listBody');
        const currentLang = new URLSearchParams(window.location.search).get('lang') === 'ja' ? 'ja' : 'kr';

        if (!jobs || jobs.length === 0) {
            $tbody.html(`<tr><td colspan="7" class="msg-box">${MapMessages.emptyJob}</td></tr>`);
            return;
        }

        let html = '';
        jobs.forEach(job => {
            const title = job.title || MapMessages.fbTitle;
            const company = job.companyName || MapMessages.fbCompany;
            const wage = job.wage || MapMessages.fbWage;
            const address = job.address || '-';
            const contact = job.contactPhone || '-';
            let manager = job.manager;
            if (job.userId === 9999 || !manager) {
                manager = "Admin";
            }
            const detailUrl = `/map/jobs/detail?id=${job.id}&source=${job.source}&lang=${currentLang}`;

            const clickAttr = (job.lat && job.lng)
                ? `onclick="MapManager.moveToJobLocation(${job.lat}, ${job.lng})"`
                : `onclick="alert('지도 좌표 정보가 없습니다.')"`;

            const jobSignature = job.id + '_' + job.source;
            const isSaved = AppState.scrapedJobIds.has(jobSignature);

            let btnClass = 'btn';
            let btnText = MapMessages.btnSave;
            let unsaveText = currentLang === 'ja' ? '保存解除' : '찜해제';

            if (isSavedMode || isSaved) {
                btnClass = "btn btn-saved";
                btnText = unsaveText;
            }

            const saveBtnHtml = isUserLoggedIn
                ? `<button class="${btnClass}" data-id="${job.id}" data-source="${job.source}" onclick="UIManager.toggleListScrap(this, ${isSavedMode})">${btnText}</button>`
                : '';

            html += `
            <tr>
                <td>
                    <span class="title-text" style="cursor: pointer; text-decoration: underline; color: var(--text-main);" ${clickAttr}>
                        ${title}
                    </span>
                    <span class="badge bg-blue">${MapMessages.badgeRecruit}</span>
                </td>
                <td><a href="#" class="company-text">${company}</a></td>
                <td><span class="addr-text">${address}</span></td>
                <td><span class="wage-text">${wage}</span></td>
                <td><span class="contact-text">${contact}</span></td>
                <td><span class="contact-text">${manager}</span></td>
                <td>
                     <div class="btn-wrap">
                        ${saveBtnHtml}
                        <button class="btn btn-view" onclick="location.href='${detailUrl}'">
                            ${MapMessages.btnDetail}
                        </button>
                     </div>
                </td>
            </tr>`;
        });

        $tbody.html(html);
        UIManager.updateTableHeader();
    },

    openJobCard: function(job) {
        const currentLang = new URLSearchParams(window.location.search).get('lang') === 'ja' ? 'ja' : 'kr';
        const detailUrl = `/map/jobs/detail?id=${job.id}&source=${job.source}&lang=${currentLang}`;
        const $card = $('#jobDetailCard');

        $('#card-company').text(job.companyName || MapMessages.fbCompany);
        $('#card-manager').text(job.manager || MapMessages.fbManager);
        $('#card-title').text(job.title);

        $('.job-address').html(`${MapMessages.labelAddress} <span id="card-address">${job.address || '-'}</span>`);
        $('#card-phone').text(job.contactPhone || '-');

        $('#jobDetailCard .btn-scrap').text(MapMessages.btnSaveCard);
        $('#btn-detail').text(MapMessages.btnDetailCard);

        const $img = $('#card-img');
        $img.attr('src', job.thumbnailUrl || 'https://placehold.co/300');
        $img.off('error').on('error', function() { $(this).attr('src', 'https://placehold.co/300?text=No+Image'); });

        $('#btn-detail').off('click').on('click', function() {
            window.location.href = detailUrl;
        });

        const $scrapBtn = $('#jobDetailCard .btn-scrap');

        if (isUserLoggedIn) {
            $scrapBtn.show();

            const jobSignature = job.id + '_' + job.source;
            const isSaved = AppState.scrapedJobIds.has(jobSignature);

            if (isSaved) {
                $scrapBtn.addClass('favorite').text(currentLang === 'ja' ? '保存解除' : '찜해제');
            } else {
                $scrapBtn.removeClass('favorite').text(MapMessages.btnSaveCard);
            }

            $scrapBtn.off('click').on('click', function() {
                UIManager.toggleCardScrap(job.id, job.source);
            });
        } else {
            $scrapBtn.hide();
        }

        $card.show();
        $('#bottomSheet').removeClass('active');

        JobService.addRecentJob(job);
    },

    closeJobCard: function() {
        $('#jobDetailCard').hide();
    },

    updateTableHeader: function() {
        const headers = $('#tableHeader th');
        headers.each(function(index) {
            if(MapMessages.table[index]) $(this).text(MapMessages.table[index]);
        });
    },

    toggleListScrap: function(btnElement, isSavedMode) {
        const $btn = $(btnElement);
        const jobId = $btn.data('id');
        const source = $btn.data('source');
        const currentLang = new URLSearchParams(window.location.search).get('lang') === 'ja' ? 'ja' : 'kr';
        const jobSignature = jobId + '_' + source;

        $.ajax({
            url: '/api/scraps',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ targetPostId: jobId, targetSource: source }),
            success: function(response) {
                let isSaved = false;
                if (typeof response === 'boolean') isSaved = response;
                else if (response && response.isScraped !== undefined) isSaved = response.isScraped;
                else if (response && response.scraped !== undefined) isSaved = response.scraped;
                else if (response && response.result !== undefined) isSaved = response.result;

                const $cardBtn = $('#jobDetailCard .btn-scrap');

                if (isSaved) {
                    $btn.addClass('btn-saved').text(currentLang === 'ja' ? '保存解除' : '찜해제');
                    AppState.scrapedJobIds.add(jobSignature);
                    $cardBtn.addClass('favorite').text(currentLang === 'ja' ? '保存解除' : '찜해제');
                } else {
                    if (isSavedMode) {
                        $btn.closest('tr').fadeOut(300, function() {
                            $(this).remove();
                            if ($('#listBody tr').length === 0) {
                                $('#listBody').html(`<tr><td colspan="7" class="msg-box">${MapMessages.emptyJob}</td></tr>`);
                            }
                        });
                    } else {
                        $btn.removeClass('btn-saved').text(MapMessages.btnSave);
                    }
                    AppState.scrapedJobIds.delete(jobSignature);
                    $cardBtn.removeClass('favorite').text(MapMessages.btnSaveCard);
                }
            },
            error: function() {
                alert("처리 중 오류가 발생했습니다.");
            }
        });
    },

    toggleCardScrap: function(jobId, source) {
        const currentLang = new URLSearchParams(window.location.search).get('lang') === 'ja' ? 'ja' : 'kr';
        const jobSignature = jobId + '_' + source;

        $.ajax({
            url: '/api/scraps',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ targetPostId: jobId, targetSource: source }),
            success: function(response) {
                let isSaved = false;
                if (typeof response === 'boolean') isSaved = response;
                else if (response && response.isScraped !== undefined) isSaved = response.isScraped;
                else if (response && response.scraped !== undefined) isSaved = response.scraped;
                else if (response && response.result !== undefined) isSaved = response.result;

                const $cardBtn = $('#jobDetailCard .btn-scrap');
                const $listBtn = $(`#listBody .btn[data-id='${jobId}'][data-source='${source}']`);

                if (isSaved) {
                    $cardBtn.addClass('favorite').text(currentLang === 'ja' ? '保存解除' : '찜해제');
                    $listBtn.addClass('btn-saved').text(currentLang === 'ja' ? '保存解除' : '찜해제');
                    AppState.scrapedJobIds.add(jobSignature);

                    const activeTab = $('.nav-item.active').data('tab');

                    if (activeTab === 'saved') {
                        JobService.loadSavedJobs();
                    }
                    else if (activeTab === 'recent') {
                        JobService.loadRecentJobs();
                    }
                    else if (activeTab === 'nearby' || activeTab === 'explore' || !activeTab) {
                        if (AppState.lastBounds) {
                            JobService.loadJobs(AppState.lastBounds);
                        }
                    }

                } else {
                    $cardBtn.removeClass('favorite').text(MapMessages.btnSaveCard);
                    AppState.scrapedJobIds.delete(jobSignature);

                    if (AppState.isFilterMode && !AppState.isLocationMode) {
                        $listBtn.closest('tr').fadeOut(300, function() {
                            $(this).remove();
                            if ($('#listBody tr').length === 0) {
                                $('#listBody').html(`<tr><td colspan="7" class="msg-box">${MapMessages.emptyJob}</td></tr>`);
                            }
                        });
                    } else {
                        $listBtn.removeClass('btn-saved').text(MapMessages.btnSave);
                    }
                }
            },
            error: function() {
                alert("처리 중 오류가 발생했습니다.");
            }
        });
    },
};

// ============================================================
// [7] 유틸리티 (Utils)
// ============================================================
const Utils = {
    getPathsFromGeoJson: function(json, specificIndex = -1) {
        const paths = [];
        if (!json) return paths;
        const features = (json.type === "FeatureCollection") ? json.features : [json];

        features.forEach(f => {
            if (!f.geometry) return;
            if (f.geometry.type === "MultiPolygon") {
                f.geometry.coordinates.forEach((polygon, index) => {
                    if (specificIndex >= 0 && index !== specificIndex) return;
                    paths.push(polygon[0].map(c => ({ lat: c[1], lng: c[0] })));
                });
            } else if (f.geometry.type === "Polygon") {
                paths.push(f.geometry.coordinates[0].map(c => ({ lat: c[1], lng: c[0] })));
            }
        });
        return paths;
    },

    getDistanceFromLatLonInKm: function(lat1, lon1, lat2, lon2) {
        const R = 6371;
        const dLat = Utils.deg2rad(lat2 - lat1);
        const dLon = Utils.deg2rad(lon2 - lon1);
        const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Utils.deg2rad(lat1)) * Math.cos(Utils.deg2rad(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2);
        const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    },

    deg2rad: function(deg) {
        return deg * (Math.PI / 180);
    }
};

// 🌟 [삭제 완료] ChatWidgetManager 완전 삭제! 이제 floatingChat.js가 전부 알아서 합니다!