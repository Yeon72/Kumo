// ==========================================================
// 🌟 [추가] 부모 창(메인 사이트) 다크모드 실시간 동기화
// ==========================================================
function syncDarkMode() {
    try {
        // 부모 창의 body에 'dark-mode' 클래스가 있는지 확인
        if (window.parent.document.body.classList.contains('dark-mode')) {
            document.body.classList.add('dark-mode');
        } else {
            document.body.classList.remove('dark-mode');
        }
    } catch (e) {
        console.log("iframe 다크모드 동기화 대기 중...");
    }
}

// 1. 채팅창이 처음 켜질 때 즉시 다크모드 검사
syncDarkMode();

// 2. 사용자가 사이트에서 다크모드 토글을 누를 때 '실시간'으로 감지해서 적용
try {
    const observer = new MutationObserver(syncDarkMode);
    // 부모 창의 body 클래스가 변하는 것을 감시합니다.
    observer.observe(window.parent.document.body, { attributes: true, attributeFilter: ['class'] });
} catch (e) {
    console.log("MutationObserver 연결 실패 (단독 실행 모드)");
}
// ==========================================================

// 방 입장 함수 (수정됨)
function enterRoom(roomId) {
    const userId = window.MY_USER_ID;

    // 🌟 1. 현재 주소창에서 lang 파라미터를 꺼내옵니다 (없으면 기본값 kr)
    const currentLang = window.getKumoLang();

    if (userId) {
        // 🌟 2. 주소 맨 끝에 &lang=${currentLang} 를 붙여서 이동시킵니다!
        location.href = `/chat/room/${roomId}?userId=${userId}&lang=${currentLang}`;
    } else {
        alert(window.CHAT_LANG.noLogin);
    }
}

// DOMContentLoaded 이벤트
document.addEventListener("DOMContentLoaded", function () {
    const searchIcon = document.getElementById('searchIcon');
    const searchBar = document.getElementById('searchBar');
    const searchInput = document.getElementById('searchInput');

    // 검색창 토글
    if (searchIcon) {
        searchIcon.onclick = function () {
            searchBar.classList.toggle('active');
            if (searchBar.classList.contains('active')) {
                setTimeout(() => searchInput.focus(), 200);
            } else {
                // 검색창 닫을 때 검색어 지우고 '전체' 탭으로 초기화
                searchInput.value = '';
                const allTabBtn = document.querySelector('.tab-btn');
                if (allTabBtn) filterRooms('all', allTabBtn);
            }
        };
    }

    // 이름 검색 로직
    if (searchInput) {
        searchInput.addEventListener('input', function () {
            const keyword = this.value.trim().toLowerCase();
            const rooms = document.querySelectorAll('.chat-item');

            // 검색할 때는 '전체' 탭 기준으로 모든 방을 보여준 상태에서 필터링
            document.querySelectorAll('.tab-btn').forEach(tab => tab.classList.remove('active'));
            const allTabBtn = document.querySelector('.tab-btn');
            if (allTabBtn) allTabBtn.classList.add('active');

            rooms.forEach(room => {
                const nameElement = room.querySelector('.chat-name');
                if (nameElement) {
                    const name = nameElement.innerText.toLowerCase();
                    // 🌟 [수정 1] startsWith -> includes 로 변경 (중간 글자 검색 허용!)
                    if (name.includes(keyword)) {
                        room.style.display = '';
                    } else {
                        room.style.display = 'none';
                    }
                }
            });
        });
    }

    // 탭 토글
    const tabBtns = document.querySelectorAll('.tab-btn');
    tabBtns.forEach(btn => {
        btn.addEventListener('click', function () {
            if (searchInput) searchInput.value = '';
            tabBtns.forEach(b => b.classList.remove('active'));
            this.classList.add('active');

            // 탭을 클릭할 때 필터링 로직이 즉시 작동하도록 연동
            const filterType = this.getAttribute('data-filter');
            filterRooms(filterType, this);
        });
    });

    // 🌟 페이지 최초 로드 시 '안 읽음 뱃지' 초기 렌더링
    initializeUnreadBadges();

    // 로비 웹소켓 연결
    connectChatList();
});

// 🌟 페이지 접속 시 data-unread 상태에 따라 뱃지 그리기
function initializeUnreadBadges() {
    const rooms = document.querySelectorAll('.chat-item');
    rooms.forEach(room => {
        const isUnread = room.getAttribute('data-unread') === 'true';
        toggleUnreadBadge(room, isUnread);
    });
}

// 필터링 핵심 함수
function filterRooms(filterType, clickedTab) {
    if (clickedTab) {
        document.querySelectorAll('.tab-btn').forEach(tab => tab.classList.remove('active'));
        clickedTab.classList.add('active');
    }

    const rooms = document.querySelectorAll('.chat-item');

    rooms.forEach(room => {
        const isUnread = room.getAttribute('data-unread') === 'true';

        if (filterType === 'all' || !filterType) {
            room.style.display = '';
        } else if (filterType === 'unread') {
            room.style.display = isUnread ? '' : 'none';
        } else if (filterType === 'read') {
            room.style.display = !isUnread ? '' : 'none';
        }
    });
}

// 점 세 개 클릭 시 메뉴 열기/닫기
function toggleOptionsMenu(event, iconElement) {
    event.stopPropagation();
    document.querySelectorAll('.options-dropdown.show').forEach(menu => {
        if (menu !== iconElement.nextElementSibling) {
            menu.classList.remove('show');
        }
    });
    iconElement.nextElementSibling.classList.toggle('show');
}

// 화면 밖 클릭 시 드롭다운 닫기
document.addEventListener('click', function (event) {
    if (!event.target.matches('.options-icon')) {
        document.querySelectorAll('.options-dropdown.show').forEach(m => m.classList.remove('show'));
    }
});

// 고정/고정해제 버튼
function togglePinRoom(event, element) {
    event.stopPropagation();

    const chatItem = element.closest('.chat-item');
    const chatList = document.querySelector('.chat-list');
    const pinText = element.querySelector('.pin-text');

    if (!chatItem) return;

    const isPinned = chatItem.classList.contains('is-pinned');

    chatItem.style.transition = 'opacity 0.3s ease';
    chatItem.style.opacity = '0.5';
    setTimeout(() => chatItem.style.opacity = '1', 300);

    if (isPinned) {
        chatItem.classList.remove('is-pinned');
        pinText.innerText = window.CHAT_LANG.unpin; // 고정 시

        const myTime = chatItem.querySelector('.chat-time').innerText;
        const otherRooms = Array.from(chatList.querySelectorAll('.chat-item:not(.is-pinned)'));
        let targetRoom = otherRooms.find(room => room.querySelector('.chat-time').innerText < myTime);

        if (targetRoom) chatList.insertBefore(chatItem, targetRoom);
        else chatList.appendChild(chatItem);
    } else {
        chatItem.classList.add('is-pinned');
        pinText.innerText = window.CHAT_LANG.pin; // 고정 해제 시
        chatList.prepend(chatItem);
    }

    document.querySelectorAll('.options-dropdown.show').forEach(m => m.classList.remove('show'));
}

// 삭제 버튼 클릭
function deleteRoom(event, element) {
    event.stopPropagation();
    if (confirm(window.CHAT_LANG.deleteConfirm)) {
        const chatItem = element.closest('.chat-item');
        if (chatItem) {
            chatItem.style.transition = 'all 0.3s ease';
            chatItem.style.opacity = '0';
            chatItem.style.transform = 'translateX(20px)';
            setTimeout(() => chatItem.remove(), 300);
        }
    } else {
        document.querySelectorAll('.options-dropdown.show').forEach(m => m.classList.remove('show'));
    }
}

// ====================================================================
// ★★★ [LIVE] 채팅 리스트 실시간 동기화 (웹소켓 엔진) ★★★
// ====================================================================

var stompListClient = null;

function connectChatList() {
    const urlParams = new URLSearchParams(window.location.search);
    let myUserId = urlParams.get('userId');

    // 🌟 URL에 userId 파라미터가 없으면 전역 변수에서 가져오도록 방어 로직 추가!
    if (!myUserId && window.MY_USER_ID) {
        myUserId = window.MY_USER_ID;
    }

    if (!myUserId) return;

    var socket = new SockJS('/ws-stomp');
    stompListClient = Stomp.over(socket);

    stompListClient.debug = null;

    stompListClient.connect({}, function (frame) {
        console.log('✅ [LIVE] 로비(목록) 웹소켓 연결 완료!');

        // 백엔드가 '나'에게 보내는 전용 알림 파이프 구독
        const myLobbyTopic = '/sub/chat/user/' + myUserId;

        stompListClient.subscribe(myLobbyTopic, function (messageOutput) {
            var newMsg = JSON.parse(messageOutput.body);
            updateChatListUI(newMsg);
        });

    }, function (error) {
        console.error('❌ [LIVE] 로비 연결 끊김! 3초 후 재연결...', error);
        setTimeout(connectChatList, 3000);
    });
}

// ==========================================================
// ★ [LIVE] 완벽 조준 완료된 실시간 목록 갱신
// ==========================================================
function updateChatListUI(newMsg) {
    const chatItems = document.querySelectorAll('.chat-item');
    let targetRoom = null;

    chatItems.forEach(item => {
        const onclickAttr = item.getAttribute('onclick');
        if (onclickAttr && onclickAttr.includes(`enterRoom(${newMsg.roomId})`)) {
            targetRoom = item;
        }
    });

    if (targetRoom) {
        const recentMsgSpan = targetRoom.querySelector('.chat-preview');
        const timeSpan = targetRoom.querySelector('.chat-time');

        // 1. 메시지 내용 교체
        if (recentMsgSpan) {
            if (newMsg.messageType === 'IMAGE') recentMsgSpan.innerText = window.CHAT_LANG.image;
            else if (newMsg.messageType === 'FILE') recentMsgSpan.innerText = window.CHAT_LANG.file;
            else recentMsgSpan.innerText = newMsg.content;
        }

        // 2. 시간 교체
        if (timeSpan && newMsg.createdAt) {
            let timeStr = newMsg.createdAt;
            if (timeStr.includes(' ')) timeStr = timeStr.split(' ')[1].substring(0, 5);
            else if (timeStr.includes('T')) timeStr = timeStr.split('T')[1].substring(0, 5);
            timeSpan.innerText = timeStr;
        }

        // 3. 방을 맨 위로 끌어올리기
        if (!targetRoom.classList.contains('is-pinned')) {
            const chatListContainer = document.querySelector('.chat-list');
            const firstUnpinned = chatListContainer.querySelector('.chat-item:not(.is-pinned)');

            targetRoom.style.transition = 'background-color 0.4s ease';
            targetRoom.style.backgroundColor = 'rgba(125, 180, 230, 0.15)'; // 하늘색 테마에 맞게 톤 조절
            setTimeout(() => targetRoom.style.backgroundColor = '', 1000);

            if (firstUnpinned) chatListContainer.insertBefore(targetRoom, firstUnpinned);
            else chatListContainer.appendChild(targetRoom);
        }

        // 4. 안읽음 상태 평가 및 뱃지 업데이트
        const urlParams = new URLSearchParams(window.location.search);
        let myUserId = urlParams.get('userId');
        if (!myUserId && window.MY_USER_ID) {
            myUserId = window.MY_USER_ID;
        }

        // 만약 내가 보낸 게 아니면 무조건 "안 읽음(true)"
        const isUnread = (String(newMsg.senderId) !== String(myUserId));
        targetRoom.setAttribute('data-unread', String(isUnread));

        // 🌟 실시간 뱃지 그리기
        toggleUnreadBadge(targetRoom, isUnread);

        // 🌟 [수정 2] 5. 현재 탭(필터) 및 검색어에 맞춰 방 숨김/표시 완벽 방어 처리!
        const activeTab = document.querySelector('.tab-btn.active');
        const searchInput = document.getElementById('searchInput');
        const keyword = searchInput ? searchInput.value.trim().toLowerCase() : '';
        const roomName = targetRoom.querySelector('.chat-name') ? targetRoom.querySelector('.chat-name').innerText.toLowerCase() : '';

        if (activeTab) {
            const activeFilter = activeTab.getAttribute('data-filter');
            let shouldShow = true; // 일단 보여준다고 가정

            // 1) 탭 조건 검사
            if (activeFilter === 'unread' && !isUnread) shouldShow = false;
            if (activeFilter === 'read' && isUnread) shouldShow = false;

            // 2) 검색어 조건 검사 (검색어가 있는데 이름에 없으면 숨김!)
            if (keyword && !roomName.includes(keyword)) {
                shouldShow = false;
            }

            // 최종 결정
            targetRoom.style.display = shouldShow ? '' : 'none';
        }
    } else {
        // 백엔드 연결 후 방 생성 로직이 필요한 부분
        console.log("목록에 없는 새로운 방입니다. (화면 새로고침 권장)");
    }
}

// 🌟 채팅방 요소에 빨간 동그라미(뱃지)를 붙이거나 떼는 함수
function toggleUnreadBadge(roomElement, isUnread) {
    const infoDiv = roomElement.querySelector('.chat-info');
    if (!infoDiv) return;

    let badge = infoDiv.querySelector('.unread-dot');

    if (isUnread) {
        if (!badge) {
            // 뱃지가 없으면 생성 (빨간 점)
            badge = document.createElement('div');
            badge.className = 'unread-dot';
            badge.style.width = '10px';
            badge.style.height = '10px';
            badge.style.backgroundColor = '#fa5252'; // 빨간색
            badge.style.borderRadius = '50%';
            badge.style.position = 'absolute';
            badge.style.right = '20px';
            badge.style.top = '30px';
            infoDiv.appendChild(badge);
        }
    } else {
        if (badge) {
            badge.remove(); // 읽었으면 삭제
        }
    }
}