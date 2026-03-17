/**
 * 부모 창(메인 프레임)의 다크모드 클래스 변경 상태를 감지하여
 * 현재 iframe 화면의 다크모드를 실시간으로 동기화합니다.
 */
function syncDarkMode() {
    try {
        if (window.parent.document.body.classList.contains('dark-mode')) {
            document.body.classList.add('dark-mode');
        } else {
            document.body.classList.remove('dark-mode');
        }
    } catch (e) {
        console.log("iframe 다크모드 동기화 대기 중...");
    }
}

syncDarkMode();

try {
    const observer = new MutationObserver(syncDarkMode);
    observer.observe(window.parent.document.body, { attributes: true, attributeFilter: ['class'] });
} catch (e) {
    console.log("MutationObserver 연결 실패");
}

/**
 * 선택한 특정 채팅방으로 입장(이동)합니다.
 * 로그인 정보가 없을 경우 다국어 메시지를 포함한 경고창을 출력합니다.
 *
 * @param {string|number} roomId 입장할 채팅방의 고유 식별자
 */
function enterRoom(roomId) {
    const userId = window.MY_USER_ID;
    const currentLang = window.CURRENT_LANG || 'kr';

    if (userId) {
        location.href = `/chat/room/${roomId}?userId=${userId}&lang=${currentLang}`;
    } else {
        const msg = (window.CHAT_LANG && window.CHAT_LANG.noLogin) ? window.CHAT_LANG.noLogin : '로그인이 필요합니다.';
        alert(msg);
    }
}

/**
 * DOMContentLoaded 이벤트 리스너
 * 채팅 목록 화면의 검색 바 제어, 필터링 탭 이벤트 바인딩 및
 * 실시간 웹소켓 연결 초기화 작업을 수행합니다.
 */
document.addEventListener("DOMContentLoaded", function () {
    const searchIcon = document.getElementById('searchIcon');
    const searchBar = document.getElementById('searchBar');
    const searchInput = document.getElementById('searchInput');

    if (searchIcon) {
        searchIcon.onclick = function () {
            searchBar.classList.toggle('active');
            if (searchBar.classList.contains('active')) {
                setTimeout(() => searchInput.focus(), 200);
            } else {
                searchInput.value = '';
                filterRooms('all');
            }
        };
    }

    if (searchInput) {
        searchInput.addEventListener('input', function () {
            const keyword = this.value.trim().toLowerCase();
            const rooms = document.querySelectorAll('.chat-item');
            rooms.forEach(room => {
                const name = room.querySelector('.chat-name').innerText.toLowerCase();
                room.style.display = name.includes(keyword) ? '' : 'none';
            });
        });
    }

    const tabBtns = document.querySelectorAll('.tab-btn');
    tabBtns.forEach(btn => {
        btn.addEventListener('click', function () {
            tabBtns.forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            filterRooms(this.getAttribute('data-filter'));
        });
    });

    initializeUnreadBadges();
    connectChatList();
});

/**
 * 렌더링된 각 채팅방 아이템들의 데이터 속성을 확인하여
 * 미확인(읽지 않음) 뱃지 UI를 초기화합니다.
 */
function initializeUnreadBadges() {
    document.querySelectorAll('.chat-item').forEach(room => {
        toggleUnreadBadge(room, room.getAttribute('data-unread') === 'true');
    });
}

/**
 * 탭 조건(전체, 읽지 않음, 읽음)에 따라 채팅방 목록을 필터링하여 화면에 노출합니다.
 *
 * @param {string} filterType 필터링할 탭의 유형 ('all', 'unread', 'read')
 */
function filterRooms(filterType) {
    const rooms = document.querySelectorAll('.chat-item');
    rooms.forEach(room => {
        const isUnread = room.getAttribute('data-unread') === 'true';
        if (filterType === 'all') room.style.display = '';
        else if (filterType === 'unread') room.style.display = isUnread ? '' : 'none';
        else if (filterType === 'read') room.style.display = !isUnread ? '' : 'none';
    });
}

/**
 * 각 채팅방 아이템 우측의 옵션 드롭다운 메뉴를 토글(표시/숨김)합니다.
 *
 * @param {Event} event 클릭 이벤트 객체
 * @param {HTMLElement} iconElement 클릭된 아이콘 DOM 요소
 */
function toggleOptionsMenu(event, iconElement) {
    event.stopPropagation();
    const dropdown = iconElement.nextElementSibling;
    document.querySelectorAll('.options-dropdown.show').forEach(menu => {
        if (menu !== dropdown) menu.classList.remove('show');
    });
    dropdown.classList.toggle('show');
}

/**
 * 옵션 메뉴 외부 영역 클릭 시 열려있는 모든 드롭다운 메뉴를 숨깁니다.
 */
document.addEventListener('click', () => {
    document.querySelectorAll('.options-dropdown.show').forEach(m => m.classList.remove('show'));
});

/**
 * 특정 채팅방을 목록 최상단에 고정(Pin)하거나 해제합니다.
 *
 * @param {Event} event 클릭 이벤트 객체
 * @param {HTMLElement} element 클릭된 메뉴 항목 DOM 요소
 */
function togglePinRoom(event, element) {
    event.stopPropagation();
    const chatItem = element.closest('.chat-item');
    const pinText = element.querySelector('.pin-text');
    if (!chatItem) return;

    const isPinned = chatItem.classList.contains('is-pinned');
    if (isPinned) {
        chatItem.classList.remove('is-pinned');
        pinText.innerText = window.CHAT_LANG.pin;
    } else {
        chatItem.classList.add('is-pinned');
        pinText.innerText = window.CHAT_LANG.unpin;
    }
    location.reload();
}

/**
 * 사용자가 참여 중인 특정 채팅방에서 퇴장(삭제)을 요청합니다.
 * 서버와 비동기 통신을 수행한 후, 성공 시 UI 상에서 해당 채팅방을 부드럽게 제거합니다.
 *
 * @param {Event} event 클릭 이벤트 객체
 * @param {HTMLElement} element 클릭된 삭제 메뉴 항목 DOM 요소
 */
function deleteRoom(event, element) {
    event.stopPropagation();
    const chatItem = element.closest('.chat-item');
    const roomId = chatItem.getAttribute('data-room-id');
    const userId = window.MY_USER_ID;

    const confirmMsg = (window.CHAT_LANG && window.CHAT_LANG.deleteConfirm)
        ? window.CHAT_LANG.deleteConfirm
        : '정말 이 채팅방을 나가시겠습니까?';

    if (confirm(confirmMsg)) {
        fetch(`/chat/room/exit/${roomId}?userId=${userId}`, {
            method: 'POST'
        }).then(res => {
            if (res.ok) {
                chatItem.style.transition = 'all 0.3s ease';
                chatItem.style.opacity = '0';
                chatItem.style.transform = 'translateX(20px)';
                setTimeout(() => chatItem.remove(), 300);
            } else {
                alert("삭제 실패");
            }
        }).catch(err => {
            console.error(err);
            alert("Network Error");
        });
    }
}

/**
 * 실시간 채팅방 목록 업데이트를 관리하는 STOMP 클라이언트 전역 변수
 * @type {Object|null}
 */
var stompListClient = null;

/**
 * 서버 웹소켓 엔드포인트에 접속하여 실시간 채팅 목록 업데이트 토픽을 구독합니다.
 */
function connectChatList() {
    const myUserId = window.MY_USER_ID;
    if (!myUserId) return;

    var socket = new SockJS('/ws-stomp');
    stompListClient = Stomp.over(socket);
    stompListClient.debug = null;

    stompListClient.connect({}, function () {
        stompListClient.subscribe('/sub/chat/user/' + myUserId, function (messageOutput) {
            updateChatListUI(JSON.parse(messageOutput.body));
        });
    }, function () {
        setTimeout(connectChatList, 3000);
    });
}

/**
 * 수신된 실시간 메시지 데이터를 바탕으로 채팅방 목록의 프리뷰(미리보기), 수신 시간,
 * 미확인 뱃지를 갱신하고 해당 채팅방 아이템을 목록 최상단으로 끌어올립니다.
 *
 * @param {Object} newMsg 웹소켓을 통해 수신된 신규 메시지 DTO 데이터
 */
function updateChatListUI(newMsg) {
    const targetRoom = document.querySelector(`.chat-item[data-room-id="${newMsg.roomId}"]`);
    if (targetRoom) {
        const preview = targetRoom.querySelector('.chat-preview');
        const timeSpan = targetRoom.querySelector('.chat-time');

        if (preview) {
            if (newMsg.messageType === 'IMAGE') preview.innerText = window.CHAT_LANG.image;
            else if (newMsg.messageType === 'FILE') preview.innerText = window.CHAT_LANG.file;
            else preview.innerText = newMsg.content;
        }

        if (timeSpan && newMsg.createdAt) {
            let timeStr = newMsg.createdAt.includes(' ') ? newMsg.createdAt.split(' ')[1].substring(0, 5) : newMsg.createdAt.substring(0, 5);
            timeSpan.innerText = timeStr;
        }

        const isUnread = (String(newMsg.senderId) !== String(window.MY_USER_ID));
        targetRoom.setAttribute('data-unread', String(isUnread));
        toggleUnreadBadge(targetRoom, isUnread);

        const container = document.querySelector('.chat-list');
        if (!targetRoom.classList.contains('is-pinned')) {
            container.prepend(targetRoom);
        }
    }
}

/**
 * 특정 채팅방 아이템에 미확인(읽지 않음) 상태를 나타내는 빨간색 점(뱃지) UI를 추가하거나 제거합니다.
 *
 * @param {HTMLElement} roomElement 대상 채팅방 아이템 DOM 요소
 * @param {boolean} isUnread 미확인 메시지 존재 여부
 */
function toggleUnreadBadge(roomElement, isUnread) {
    const infoDiv = roomElement.querySelector('.chat-info');
    if (!infoDiv) return;
    let badge = infoDiv.querySelector('.unread-dot');

    if (isUnread) {
        if (!badge) {
            badge = document.createElement('div');
            badge.className = 'unread-dot';
            badge.style = "width:10px; height:10px; background-color:#fa5252; border-radius:50%; position:absolute; right:20px; top:30px;";
            infoDiv.appendChild(badge);
        }
    } else if (badge) {
        badge.remove();
    }
}