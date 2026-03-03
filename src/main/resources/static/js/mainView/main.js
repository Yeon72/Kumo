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
    ignoreIdle: false, // 🌟 [NEW] 지도가 강제 이동 중일 때 자동 갱신을 막는 스위치
    isFilterMode: false, // 🌟 [NEW] 저장/최근 탭이 켜져 있을 때 갱신을 '영구적'으로 막는 스위치
    userLocation: null,    // 🌟 [NEW] 내 GPS 위치 저장용
    isLocationMode: false  // 🌟 [NEW] 내 주변 보기 모드 켜짐 여부
};

// ============================================================
// [2] 초기화 및 이벤트 바인딩 (Init & Events)
// ============================================================
$(document).ready(function() {
    // 바텀 시트 핸들 클릭 이벤트
    $('.sheet-handle').on('click', function() {
        const $sheet = $('#bottomSheet');
        $sheet.toggleClass('active');

        if ($sheet.hasClass('active')) {
            UIManager.closeJobCard();
        }
    });

    // 지도 초기화 (Google Maps API 콜백으로 실행됨)
    window.initMap = MapManager.init;

    $(".btn-close-card").on('click', function () {
        UIManager.closeJobCard();
    })

    $(".nav-item").on('click', function () {
        const $this = $(this);
        const tabName = $this.data('tab');

        // 채팅 탭은 화면 이동이므로 바로 실행
        if (tabName === 'chat') {
            UIManager.switchTab('chat');
            return;
        }

        // 🌟 [핵심 UX] 이미 켜진 탭을 '한 번 더' 눌렀을 때 -> 선택 해제 및 '자유 탐색' 모드로 복귀!
        if ($this.hasClass('active')) {
            $this.removeClass('active');
            UIManager.switchTab('explore'); // 🌟 새로운 '자유 탐색' 모드 실행
            return;
        }

        // 🌟 [핵심 변경] 토글 로직을 제거하고 직관적으로 탭을 이동하게 만듭니다.
        $('.nav-item').removeClass('active');
        $this.addClass('active');
        UIManager.switchTab(tabName);
    })

    if ($('#chatWidget')){
        ChatWidgetManager.initDraggable();
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

        // 1. 현재 브라우저가 다크모드 인지 확인 하기
        const isDark = document.body.classList.contains('dark-mode') || localStorage.getItem('theme') === 'dark';

        // 2. 초기 스타일 결정
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

        // 4. 다크모드 변경 감지기 실행
        MapManager.observeThemeChange();
    },

    bindMapEvents: function() {
        const map = AppState.map;

        // 🌟 [복구] 이 부분(이벤트 리스너)이 빠져 있었습니다!
        // 지도가 멈출 때(idle)마다 실행한다는 명령이 없어서 동작을 안 했던 겁니다.
        map.addListener("idle", () => {
// 🌟 [핵심 변경] 강제 이동 중(ignoreIdle)이거나 필터 모드(isFilterMode)일 때는 갱신 정지!
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

        // 3. 지도 배경 클릭 시 카드 닫기
        map.addListener("click", () => {
            UIManager.closeJobCard();
        });
    },

    // 내 위치로 이동 (GPS)
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

                // 내 위치 파란 점 표시
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

                // 🌟 [핵심 수정] 이동이 끝난 직후(idle) 즉시 데이터 로딩
                // 일반적인 idle 리스너는 0.5초 딜레이가 있지만, 여기서는 즉시 실행합니다.
                google.maps.event.addListenerOnce(AppState.map, 'idle', function() {

                    // 전역 idle 리스너에 의해 중복 실행되는 것을 방지하기 위해 타이머 취소
                    clearTimeout(AppState.debounceTimer);

                    // 즉시 로딩 실행
                    const bounds = AppState.map.getBounds();

                    // 🌟 [추가] 강제 로딩 시에도 현재 범위를 '마지막 범위'로 등록해둬야
                    // 이후에 자동 idle 이벤트가 중복 실행되는 것을 막을 수 있습니다.
                    AppState.lastBounds = bounds;

                    JobService.loadJobs(bounds);
                });
            },
            () => { alert("위치 정보를 가져올 수 없습니다."); }
        );
    },

    // 마스킹(배경 어둡게) 그리기
    drawMasking: function() {
        const worldCoords = [
            { lat: 85, lng: -180 }, { lat: 85, lng: 0 }, { lat: 85, lng: 180 },
            { lat: -85, lng: 180 }, { lat: -85, lng: 0 }, { lat: -85, lng: -180 },
            { lat: 85, lng: -180 }
        ];

        // GeoJSON 유틸함수 사용 (하단 정의)
        const tokyoPaths = typeof tokyoGeoJson !== 'undefined' ? Utils.getPathsFromGeoJson(tokyoGeoJson) : [];
        const osakaCityPaths = typeof osakaCityGeoJson !== 'undefined' ? Utils.getPathsFromGeoJson(osakaCityGeoJson) : [];
        const kansaiPaths = typeof osakaGeoJson !== 'undefined' ? Utils.getPathsFromGeoJson(osakaGeoJson, 1) : [];

        // 다크모드 감지 함수
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


    // 🌟 [NEW] 테마 변경 실시간 감지 함수
    observeThemeChange: function() {
        // MutationObserver: HTML 요소의 변화를 감시하는 기능
        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                // body 태그의 class 속성이 변했을 때만 실행
                if (mutation.attributeName === 'class') {
                    const isDarkMode = document.body.classList.contains('dark-mode');
                    MapManager.setMapStyle(isDarkMode);
                }
            });
        });

        // body 태그 감시 시작 (속성 변화 감지)
        observer.observe(document.body, { attributes: true });
    },

    // 🌟 [NEW] 지도 스타일 갈아끼우기 함수
    setMapStyle: function(isDark) {
        if (!AppState.map) return;

        const newStyle = isDark ? MapStyles.dark : MapStyles.light;

        // setOptions를 통해 실행 중에 스타일만 쏙 바꿉니다.
        AppState.map.setOptions({ styles: newStyle });

        console.log(`🎨 지도 테마 변경: ${isDark ? 'Dark' : 'Light'}`);

        if (AppState.maskPolygon) {
            AppState.maskPolygon.setMap(null);
        }

        MapManager.drawMasking();
    },

    // 🎨 [NEW] 모드에 따른 경계선 반환 함수
    getBoundaryStyle: function (isDark) {
        const boundaryColor = isDark ? '#FF6B6B' : '#fB0000';

        return {
            strokeColor : boundaryColor,
            strokeOpacity: 1.0,
            strokeWeight: 2
        }
    },

    // 🌟 [NEW] 지역 변경 함수
    changeRegion: function(regionCode) {
        if (!AppState.map) return;

        // 1. 지도가 휙 이동하는 동안 쓸데없는 API 요청이 가지 않도록 스위치 ON
        AppState.ignoreIdle = true;

        // 2. 지역별 좌표 설정
        let targetPos;
        let targetZoom = 10; // 기본 줌 레벨

        if (regionCode === 'tokyo') {
            targetPos = { lat: 35.6895, lng: 139.6921 };
            targetZoom = 18;
        } else if (regionCode === 'osaka') {
            targetPos = { lat: 34.6938, lng: 135.5019 };
            targetZoom = 18; // 오사카는 11 정도가 보기 좋을 수 있습니다.
        }

        // 3. 지도 카메라 부드럽게 이동 (panTo)
        AppState.map.panTo(targetPos);
        AppState.map.setZoom(targetZoom);

        // 4. 이동이 완료된 후 새로운 지역의 데이터를 불러오도록 타이머 세팅
        setTimeout(() => {
            AppState.ignoreIdle = false; // 스위치 OFF (이제 다시 자동 갱신됨)

            // 현재 화면 범위 저장 및 데이터 요청
            const bounds = AppState.map.getBounds();
            AppState.lastBounds = bounds;
            JobService.loadJobs(bounds);

        }, 800); // 0.8초 후 (지도가 부드럽게 날아가는 시간 대기)
    },

    // 🌟 [NEW] 시트에서 리스트 클릭 시 해당 위치로 지도 슉~ 이동하기
    moveToJobLocation: function(lat, lng) {
        if (!AppState.map || !lat || !lng) return;

        // 1. 지도가 휙 이동하는 동안 새 데이터 불러오기(idle) 방지 스위치 ON!
        AppState.ignoreIdle = true;

        // 2. 해당 위치로 부드럽게 카메라 이동 및 줌인
        const targetPos = { lat: parseFloat(lat), lng: parseFloat(lng) };
        AppState.map.panTo(targetPos);
        AppState.map.setZoom(18); // 상세히 볼 수 있게 줌 레벨 조정

        // 3. 이동이 완전히 끝났을 때의 처리
        google.maps.event.addListenerOnce(AppState.map, "idle", function() {
            setTimeout(() => {
                // 현재 이동한 위치를 '마지막 위치'로 강제 저장해둬서
                // 스위치를 끈 직후에 데이터 갱신이 또 일어나는 것을 완벽 차단!
                AppState.lastBounds = AppState.map.getBounds();
                AppState.ignoreIdle = false; // 스위치 OFF (이제 다시 손으로 움직이면 갱신됨)

                // (보너스) 이동한 곳의 마커를 찾아서 통통 튀게(Bounce) 만들어주기!
                const targetMarker = AppState.jobMarkers.find(
                    m => m.getPosition().lat().toFixed(4) === targetPos.lat.toFixed(4) &&
                        m.getPosition().lng().toFixed(4) === targetPos.lng.toFixed(4)
                );

                if (targetMarker) {
                    targetMarker.setAnimation(google.maps.Animation.BOUNCE);
                    setTimeout(() => targetMarker.setAnimation(null), 2500); // 2.5초 뒤 멈춤
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

        // 🌟 삼항 연산자 대신 MapMessages 사용
        $('#listBody').html(`<tr><td colspan="7" class="msg-box">${MapMessages.loading}</td></tr>`);

        // 파라미터 준비
        const params = JobService.prepareParams(bounds);

        // 이전 요청 취소 (AbortController 대신 jQuery xhr.abort 사용)
        if (AppState.currentXhr && AppState.currentXhr.readyState !== 4) {
            AppState.currentXhr.abort();
        }

        // jQuery AJAX 요청
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

        // 언어 설정
        const currentLang = new URLSearchParams(window.location.search).get('lang') === 'ja' ? 'ja' : 'kr';
        UIManager.updateTableHeader(currentLang);
        params.lang = currentLang;

        return params;
    },

    processData: function(data) {
        let filteredData = data; // 기본적으로는 서버에서 온 데이터를 그대로 씀

        // 🌟 [핵심 로직] 내 주변 모드이고, 내 GPS 위치를 아는 상태라면?
        if (AppState.isLocationMode && AppState.userLocation) {
            const RADIUS_KM = 3.0; // 🎯 원하는 반경을 설정하세요! (예: 3km = 3.0)

            filteredData = data.filter(job => {
                if (!job.lat || !job.lng) return false; // 좌표 없는 공고는 제외

                // Utils의 함수로 내 위치와 공고 위치 사이의 거리를 계산 (km 단위)
                const dist = Utils.getDistanceFromLatLonInKm(
                    AppState.userLocation.lat,
                    AppState.userLocation.lng,
                    job.lat,
                    job.lng
                );

                return dist <= RADIUS_KM; // 계산된 거리가 반경 이내인 것만 통과!
            });

            console.log(`📍 내 반경 ${RADIUS_KM}km 이내 필터링: 전체 ${data.length}개 -> ${filteredData.length}개 남음`);
        }

        // UI 업데이트 (원본 data 대신 걸러진 filteredData를 넣어줍니다!)
        MarkerManager.clearMarkers();
        UIManager.renderList(filteredData);
        MarkerManager.renderMarkers(filteredData);
    },

    // 🌟 [저장된 공고] DB에서 스크랩 내역 가져오기
    loadSavedJobs: function() {
        const currentLang = new URLSearchParams(window.location.search).get('lang') === 'ja' ? 'ja' : 'kr'; // 언어 확인

        $.ajax({
            url: `/api/scraps?lang=${currentLang}`, // 🌟 URL에 언어 추가
            method: 'GET',
            dataType: 'json',
            success: function(data) {
                UIManager.renderList(data, true);
                MarkerManager.renderMarkers(data);

                // 🌟 [추가] 찜한 공고 리스트를 가져오면 바텀 시트를 위로 열어주기!
                $('#bottomSheet').addClass('active');
                UIManager.closeJobCard(); // 혹시 열려있는 카드가 있으면 닫기

                // (선택) 찜한 마커들이 한눈에 보이게 카메라 조절 원하시면 주석 해제
                // MapManager.fitBoundsToData(data);
            },
            error: function(err) {
                console.error("찜한 목록 불러오기 실패:", err);
                $('#listBody').html(`<tr><td colspan="7" class="msg-box">${MapMessages.savedFail}</td></tr>`);
            }
        });
    },

    // 🌟 [최근 본 공고] 브라우저 로컬 스토리지에서 가져오기
    loadRecentJobs: function() {
        const recentJobsJson = localStorage.getItem('kumo_recent_jobs');
        let recentJobs = [];

        if (recentJobsJson) {
            recentJobs = JSON.parse(recentJobsJson);
        }

        // ========================================================
        // 🌟 [수정 완료] 1개만 제한하던 코드를 지우고, 배열 전체(최대 20개)를 넘겨줍니다!
        // ========================================================
        UIManager.renderList(recentJobs);
        MarkerManager.renderMarkers(recentJobs);

        // 👉 바텀 시트를 위로 스르륵 올립니다.
        $('#bottomSheet').addClass('active');

        // 👉 바텀 시트가 올라올 때, 기존에 떠있던 카드가 있다면 가려지지 않게 닫아줍니다.
        UIManager.closeJobCard();

        // 👉 지도 카메라를 최근 본 공고들이 모두 화면에 들어오도록 조절합니다.
        // (두 번째 인자로 false를 넘겨서 카드가 자동으로 열리지 않도록 막습니다)
        // MapManager.fitBoundsToData(recentJobs, false);
    },

    addRecentJob: function(jobData) {
        if (!jobData || !jobData.id) return;

        // 1. 기존 데이터 꺼내오기 (없으면 빈 배열)
        const recentStr = localStorage.getItem('kumo_recent_jobs');
        let recentJobs = recentStr ? JSON.parse(recentStr) : [];

        // 2. 중복 제거 (이미 본 공고를 또 눌렀다면, 예전 기록을 지우고 최신으로 올리기 위해)
        recentJobs = recentJobs.filter(job => job.id !== jobData.id);

        // 3. 배열의 맨 앞(최신)에 추가
        recentJobs.unshift(jobData);

        // 4. 최대 20개까지만 유지 (용량 낭비 방지)
        if (recentJobs.length > 20) {
            recentJobs = recentJobs.slice(0, 20); // 20개까지만 자르기
        }

        // 5. 다시 문자열로 바꿔서 로컬스토리지에 저장
        localStorage.setItem('kumo_recent_jobs', JSON.stringify(recentJobs));

        console.log(`💾 최근 본 공고 저장됨 (총 ${recentJobs.length}개)`);
    },

    // 🌟 [추가] 검색바에서 검색 실행 시 새 창으로 이동하는 로직
    searchJobs: function() {
        const keyword = $('#keywordInput').val().trim();
        const currentLang = new URLSearchParams(window.location.search).get('lang') || 'kr';

        // 지역은 빼고 키워드만 달고 넘어갑니다.
        let url = `/map/search_list?lang=${currentLang}`;
        if (keyword) {
            url += `&keyword=${encodeURIComponent(keyword)}`;
        }

        window.location.href = url;
    }
};

// ============================================================
// [5] 마커 관리자 (Marker Manager - Clustering)
// ============================================================
const MarkerManager = {
    renderMarkers: function(jobs) {
        if (!jobs || jobs.length === 0) return;

        const map = AppState.map;
        AppState.jobMarkers = []; // 초기화

        // 마커 생성
        const markers = jobs
            .filter(job => job.lat && job.lng)
            .map(job => {
                // 🌟 [변경] 기본 마커 대신 커스텀 SVG 마커 적용
                // 여기서 파란색(#4285F4)이나 빨간색(#EA4335) 등 원하는 색상을 지정할 수 있습니다.
                // 사진처럼 숫자를 넣고 싶다면 label 속성을 사용합니다.
                const marker = new google.maps.Marker({
                    position: { lat: job.lat, lng: job.lng },
                    icon: MarkerManager.createCustomMarkerIcon('#EA4335'), // 빨간색 마커 예시 (파란색은 #4285F4)
                });

                marker.addListener("click", () => {
                    UIManager.openJobCard(job);
                });

                return marker;
            });

        AppState.jobMarkers = markers;

        // 클러스터러 업데이트
        if (AppState.markerCluster) {
            AppState.markerCluster.clearMarkers();
            AppState.markerCluster.addMarkers(markers);
        } else {
            AppState.markerCluster = new markerClusterer.MarkerClusterer({
                map,
                markers,
                renderer: MarkerManager.getClusterRenderer(), // 커스텀 스타일
                algorithm: new markerClusterer.GridAlgorithm({
                    gridSize: 80, // 구 단위 느낌
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



    // 클러스터 스타일 정의 (🌟 첨부해주신 이미지 스타일의 플랫 구름!)
    getClusterRenderer: function() {
        return {
            render: ({ count, position }) => {

                // 1. 밑바닥이 평평하고(Z), 4개의 둥근 봉우리가 있는 구름 Path
                const cloudPath = "M 10 22 C 2 22, 2 12, 9 13 C 9 3, 23 3, 23 11 C 25 5, 34 7, 31 14 C 38 14, 38 22, 30 22 Z";

                // 2. 구름 색상 (올려주신 이미지의 예쁜 파스텔 블루톤 적용)
                let cloudColor = "#4285F4"; // 기본: 파스텔 블루
                if (count >= 10) cloudColor = "#4285F4"; // 10개 이상: 조금 더 진한 블루
                if (count >= 50) cloudColor = "#4285F4"; // 50개 이상: 구글 맵 기본 블루

                return new google.maps.Marker({
                    // 텍스트(숫자) 설정
                    label: {
                        text: String(count),
                        color: "white", // 파스텔톤 위에서는 흰색 글씨가 예쁩니다
                        fontSize: "14px",
                        fontWeight: "bold"
                    },
                    position,
                    // 아이콘 디자인
                    icon: {
                        path: cloudPath,
                        scale: 1.8,                 // 🌟 구름 크기 조절 (필요시 1.5 ~ 2.0 사이로 조절해보세요)
                        fillColor: cloudColor,
                        fillOpacity: 0.95,

                        // 🌟 올려주신 이미지처럼 깔끔하게 보이도록 테두리 두께를 줄였습니다
                        strokeWeight: 1.5,
                        strokeColor: "#ffffff",

                        // 밑바닥이 평평한 구름의 시각적 정중앙 좌표(19, 14)로 텍스트 위치 완벽 정렬
                        anchor: new google.maps.Point(19, 14),
                        labelOrigin: new google.maps.Point(19, 14)
                    },
                    zIndex: Number(google.maps.Marker.MAX_ZINDEX) + count,
                });
            }
        };
    },

    // 🌟 [추가] 마커용 SVG 아이콘을 생성하는 헬퍼 함수
    // color: 마커 배경색 (예: #4285F4)
    createCustomMarkerIcon: function(color) {
        // 사진과 비슷한 둥근 물방울(핀) 모양의 SVG 패스입니다.
        const svgPath = 'M 12,0 C 5.373,0 0,5.373 0,12 c 0,7.194 10.74,22.25 11.31,23.03 l 0.69,0.97 l 0.69,-0.97 C 13.26,34.25 24,19.194 24,12 C 24,5.373 18.627,0 12,0 Z';

        return {
            path: svgPath,
            fillColor: color, // 마커 배경색
            fillOpacity: 1, // 불투명도 80%
            strokeWeight: 1, // 테두리 두께
            strokeColor: '#ffffff', // 테두리는 흰색
            anchor: new google.maps.Point(12, 34), // 뾰족한 끝이 정확한 좌표를 가리키도록 앵커 포인트 설정
            labelOrigin: new google.maps.Point(12, 12), // 텍스트(라벨)가 들어갈 중앙 위치
            scale: 1 // 마커 크기 조정
        };
    },
};

// ============================================================
// [6] UI 관리자 (UI Manager - jQuery)
const UIManager = {
    // 🔄 [NEW] 하단 탭 전환 함수
    // 🔄 [Refactored] 탭 기능 분기 처리
    // 🔄 [Refactored] 탭 기능 분기 처리
    switchTab: function(tabName) {
        console.log(`탭 전환 기능 실행: ${tabName}`);

        const $sheetTitle = $('#sheetTitle');
        const currentLang = new URLSearchParams(window.location.search).get('lang') === 'ja' ? 'ja' : 'kr';

        if (tabName === 'nearby') {
            $sheetTitle.text(currentLang === 'ja' ? '周辺の求人情報 📍' : '내 주변 구직 정보 📍');

            // 🌟 1. '내 주변'도 필터이므로 지도 이동 갱신을 멈춥니다(Lock)!
            AppState.isFilterMode = true;
            AppState.isLocationMode = true; // 반경 3km 필터 켜기

            // GPS 위치로 날아간 뒤, 그 위치에서 딱 1번만 데이터를 불러옵니다.
            MapManager.moveToCurrentLocation();
        }
        else if (tabName === 'saved') {
            $sheetTitle.text(currentLang === 'ja' ? 'お気に入りの求人 ⭐' : '즐겨찾기한 구직 정보 ⭐');
            AppState.isFilterMode = true;
            AppState.isLocationMode = false;
            JobService.loadSavedJobs();
        }
        else if (tabName === 'recent') {
            $sheetTitle.text(currentLang === 'ja' ? '最近見た求人 🕒' : '최근 본 구직 정보 🕒');
            AppState.isFilterMode = true;
            AppState.isLocationMode = false;
            JobService.loadRecentJobs();
        }
        else if (tabName === 'explore') {
            // 🌟 4. [NEW] 토글이 풀린 자유 탐색 모드!
            $sheetTitle.text(currentLang === 'ja' ? '求人情報リスト 📋' : '구직 정보 리스트 📋');

            AppState.isFilterMode = false; // 지도 이동 갱신(idle) 다시 켜기!
            AppState.isLocationMode = false; // 반경 3km 필터 끄기!

            // 현재 화면에 보이는 범위 기준으로 모든 공고를 즉시 다시 불러옵니다.
            if (AppState.map) {
                const bounds = AppState.map.getBounds();
                if (bounds) {
                    AppState.lastBounds = bounds;
                    JobService.loadJobs(bounds);
                }
            }
        }
        else if (tabName === 'chat') {
            ChatWidgetManager.open();
        }
    },

    // 🌟 [핵심 수정] jobs 옆에 isSavedMode = false 를 꼭 넣어주셔야 합니다!
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
            const thumb = job.thumbnailUrl || 'https://placehold.co/40';
            const dateStr = job.writeTime || MapMessages.fbTime;
            const contact = job.contactPhone || '-';
            const detailUrl = `/map/jobs/detail?id=${job.id}&source=${job.source}&lang=${currentLang}`;

            const clickAttr = (job.lat && job.lng)
                ? `onclick="MapManager.moveToJobLocation(${job.lat}, ${job.lng})"`
                : `onclick="alert('지도 좌표 정보가 없습니다.')"`;

            let btnClass = 'btn';
            let btnText = MapMessages.btnSave;
            let unsaveText = currentLang === 'ja' ? '保存解除' : '찜해제';

            // 🌟 1. 모드에 따라 버튼 디자인과 텍스트를 먼저 바꿉니다.
            if (isSavedMode) {
                btnClass = "btn btn-saved"; // 노란색 클래스 적용
                btnText = unsaveText;
            }

            // 🌟 2. 로그인 여부에 따라 찜 버튼 HTML을 다르게 생성합니다.
            const saveBtnHtml = isUserLoggedIn
                ? `<button class="${btnClass}" data-id="${job.id}" data-source="${job.source}" onclick="UIManager.toggleListScrap(this, ${isSavedMode})">${btnText}</button>`
                : '';

            // 🌟 3. 최종 HTML 조립
            html += `
            <tr>
                <td>
                    <span class="title-text" style="cursor: pointer; text-decoration: underline; color: var(--text-main);" ${clickAttr}>
                        ${title}
                    </span>
                    <span class="badge bg-blue">${MapMessages.badgeRecruit}</span>
                    <span class="badge bg-yellow">${MapMessages.badgeUrgent}</span>
                </td>
                <td><a href="#" class="company-text">${company}</a></td>
                <td><span class="addr-text">${address}</span></td>
                <td><span class="wage-text">${wage}</span></td>
                <td><span class="contact-text">${contact}</span></td>
                <td>
                    <div class="profile-wrap">
                        <img src="${thumb}" class="profile-img" onerror="this.src='https://placehold.co/40?text=No+Img'">
                        <div class="profile-info">
                            <div>Admin</div>
                            <div>${dateStr}</div>
                        </div>
                    </div>
                </td>
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

        // 🌟 삼항 연산자 싹 지우고 MapMessages 적용!
        $('#card-company').text(job.companyName || MapMessages.fbCompany);
        $('#card-manager').text(job.manager || MapMessages.fbManager);
        $('#card-title').text(job.title);

        $('.job-address').html(`${MapMessages.labelAddress} <span id="card-address">${job.address || '-'}</span>`);
        $('#card-phone').text(job.contactPhone || '-');

        $('#jobDetailCard .btn-outline').text(MapMessages.btnSaveCard);
        $('#btn-detail').text(MapMessages.btnDetailCard);

        const $img = $('#card-img');
        $img.attr('src', job.thumbnailUrl || 'https://placehold.co/300');
        $img.off('error').on('error', function() { $(this).attr('src', 'https://placehold.co/300?text=No+Image'); });

        $('#btn-detail').off('click').on('click', function() {
            window.location.href = detailUrl;
        });

        $card.show();
        $('#bottomSheet').removeClass('active');

        JobService.addRecentJob(job);
    },

    closeJobCard: function() {
        $('#jobDetailCard').hide();
    },

    // 🌟 테이블 헤더 언어 변경 함수도 엄청나게 짧아집니다!
    updateTableHeader: function() {
        const headers = $('#tableHeader th');
        // HTML에서 선언한 MapMessages.table 배열을 그대로 입혀줍니다.
        headers.each(function(index) {
            if(MapMessages.table[index]) $(this).text(MapMessages.table[index]);
        });
    },

    // 🌟 [NEW] 리스트 테이블 안에서 직접 찜하기/해제를 누를 때 작동하는 함수
    toggleListScrap: function(btnElement, isSavedMode) {
        const $btn = $(btnElement);
        const jobId = $btn.data('id');
        const source = $btn.data('source');
        const currentLang = new URLSearchParams(window.location.search).get('lang') === 'ja' ? 'ja' : 'kr';

        $.ajax({
            url: '/api/scraps',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ targetPostId: jobId, targetSource: source }),
            success: function(response) {
                if (response.isScraped) {
                    // 찜 등록 시 노란색으로 변경
                    $btn.addClass('btn-saved').text(currentLang === 'ja' ? '保存解除' : '찜해제');
                } else {
                    // 🌟 찜 해제 시!
                    if (isSavedMode) {
                        // 저장된 공고 탭에서 해제했다면, 리스트에서 스르륵 사라지게 만듭니다 (고급 UX)
                        $btn.closest('tr').fadeOut(300, function() {
                            $(this).remove();
                            // 다 지워서 남은 게 없으면 '공고 없음' 메시지 띄우기
                            if ($('#listBody tr').length === 0) {
                                $('#listBody').html(`<tr><td colspan="7" class="msg-box">${MapMessages.emptyJob}</td></tr>`);
                            }
                        });
                    } else {
                        // 일반 주변 일자리 탭이라면 원래 회색 버튼으로 복구
                        $btn.removeClass('btn-saved').text(MapMessages.btnSave);
                    }
                }
            },
            error: function() {
                alert("처리 중 오류가 발생했습니다.");
            }
        });
    }
};

// ============================================================
// [7] 유틸리티 (Utils)
// ============================================================
const Utils = {
    // GeoJSON -> Google Maps Paths
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

    // 거리 계산 (km)
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

// ============================================================
// [🌟 채팅 팝업 관리자]
// ============================================================
const ChatWidgetManager = {

    // 채팅창 열기 (초간단!)
    open: function() {
        const widget = document.getElementById('chatWidget');

        console.log("✅ 채팅 팝업 열기 (이미 iframe은 로드되어 있음!)");

        // 그냥 숨김 클래스만 제거하면 됩니다.
        widget.classList.remove('hidden');
        widget.classList.remove('minimized');
    },

    // 채팅창 닫기
    close: function() {
        document.getElementById('chatWidget').classList.add('hidden');

        // 네비게이션 탭 상태 원상복구
        if (typeof $ !== 'undefined') {
            $('.nav-item').removeClass('active');
            $('.nav-item[data-tab="nearby"]').addClass('active');
        }
    },

    // 최소화 토글
    toggleMinimize: function() {
        document.getElementById('chatWidget').classList.toggle('minimized');
    },

    // 🌟 드래그 앤 드롭 마법!
    initDraggable: function() {
        const widget = document.getElementById('chatWidget');
        const header = document.querySelector('.chat-widget-header');
        const iframe = document.getElementById('chatIframe');

        let isDragging = false;
        let offsetX, offsetY;

        header.addEventListener('mousedown', (e) => {
            isDragging = true;
            const rect = widget.getBoundingClientRect();
            offsetX = e.clientX - rect.left;
            offsetY = e.clientY - rect.top;

            // 드래그 중 iframe 안으로 마우스가 들어가면 끊기는 현상 방지
            iframe.style.pointerEvents = 'none';

            document.addEventListener('mousemove', onMouseMove);
            document.addEventListener('mouseup', onMouseUp);
        });

        function onMouseMove(e) {
            if (!isDragging) return;
            // 팝업을 마우스 따라다니게 좌표 이동
            widget.style.left = `${e.clientX - offsetX}px`;
            widget.style.top = `${e.clientY - offsetY}px`;
            widget.style.bottom = 'auto'; // bottom 고정 해제
            widget.style.right = 'auto';  // right 고정 해제
        }

        function onMouseUp() {
            isDragging = false;
            iframe.style.pointerEvents = 'auto'; // 드래그 끝나면 iframe 클릭 원상복구
            document.removeEventListener('mousemove', onMouseMove);
            document.removeEventListener('mouseup', onMouseUp);
        }
    }
};