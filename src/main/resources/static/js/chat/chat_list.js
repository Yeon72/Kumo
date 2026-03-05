/* --- src/main/resources/static/js/chat_list.js --- */

// 방 입장 함수 (수정됨)
function enterRoom(roomId) {
    // 🌟 URL 대신, HTML에서 넘겨준 전역 변수에서 내 아이디를 꺼냅니다!
    const userId = window.MY_USER_ID;

    if (userId) {
        location.href = '/chat/room/' + roomId + '?userId=' + userId;
    } else {
        alert("로그인 정보(userId)를 불러오지 못했습니다. 새로고침 후 다시 시도해주세요.");
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
                    // 이름에 검색어가 포함되어 있으면 보여주고, 없으면 숨김
                    if (name.startsWith(keyword)) {
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
        pinText.innerText = '고정';

        const myTime = chatItem.querySelector('.chat-time').innerText;
        const otherRooms = Array.from(chatList.querySelectorAll('.chat-item:not(.is-pinned)'));
        let targetRoom = otherRooms.find(room => room.querySelector('.chat-time').innerText < myTime);

        if (targetRoom) chatList.insertBefore(chatItem, targetRoom);
        else chatList.appendChild(chatItem);
    } else {
        chatItem.classList.add('is-pinned');
        pinText.innerText = '해제';
        chatList.prepend(chatItem);
    }

    document.querySelectorAll('.options-dropdown.show').forEach(m => m.classList.remove('show'));
}

// 삭제 버튼 클릭
function deleteRoom(event, element) {
    event.stopPropagation();
    if (confirm("정말 이 채팅방을 나가시겠습니까?")) {
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
    const myUserId = urlParams.get('userId');

    if (!myUserId) return;

    var socket = new SockJS('/ws-stomp');
    stompListClient = Stomp.over(socket);

    stompListClient.debug = null;

    stompListClient.connect({}, function (frame) {
        console.log('✅ [LIVE] 로비(목록) 웹소켓 연결 완료!');

        // ★ 핵심: 백엔드가 '나(myUserId)'에게 보내는 전용 알림 파이프를 구독합니다.
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
// ★ [LIVE] 완벽 조준 완료된 실시간 목록 갱신 (중복 함수 제거) ★
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
            if (newMsg.messageType === 'IMAGE') recentMsgSpan.innerText = '(사진)';
            else if (newMsg.messageType === 'FILE') recentMsgSpan.innerText = '(파일)';
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
            targetRoom.style.backgroundColor = 'rgba(255, 184, 0, 0.15)';
            setTimeout(() => targetRoom.style.backgroundColor = '', 1000);

            if (firstUnpinned) chatListContainer.insertBefore(targetRoom, firstUnpinned);
            else chatListContainer.appendChild(targetRoom);
        }

        // 4. 안읽음 상태 평가 및 뱃지 업데이트
        const urlParams = new URLSearchParams(window.location.search);
        const myUserId = urlParams.get('userId');

        // 만약 내가 보낸 게 아니면 무조건 "안 읽음(true)"
        const isUnread = (String(newMsg.senderId) !== String(myUserId));
        targetRoom.setAttribute('data-unread', String(isUnread));

        // 🌟 실시간 뱃지 그리기
        toggleUnreadBadge(targetRoom, isUnread);

        // 5. 현재 탭(필터)에 맞춰 방 숨김/표시 처리
        const activeTab = document.querySelector('.tab-btn.active');
        if (activeTab) {
            const filterText = activeTab.innerText.trim();
            if (filterText === '읽지않음' && !isUnread) targetRoom.style.display = 'none';
            else if (filterText === '읽음' && isUnread) targetRoom.style.display = 'none';
            else targetRoom.style.display = '';
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