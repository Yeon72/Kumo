/* --- src/main/resources/static/js/chat_list.js --- */

// 방 입장 함수
function enterRoom(roomId) {
    const urlParams = new URLSearchParams(window.location.search);
    const userId = urlParams.get('userId');

    if (userId) {
        location.href = '/chat/room/' + roomId + '?userId=' + userId;
    } else {
        alert("로그인 정보(userId)가 없습니다! URL을 확인해주세요.");
    }
}

// DOMContentLoaded 이벤트 (검색바 + 탭 메뉴)
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
                // (참고: 검색창 닫을 때 탭 버튼 오류 방지를 위해 임시로 빈 값 처리)
                if (document.querySelector('.tab-btn.active')) {
                    filterRooms('all', document.querySelector('.tab-btn.active'));
                }
            }
        };
    }

    // 탭 메뉴 토글 로직
    const tabBtns = document.querySelectorAll('.tab-btn');
    tabBtns.forEach(btn => {
        btn.addEventListener('click', function () {
            tabBtns.forEach(b => b.classList.remove('active'));
            this.classList.add('active');
        });
    });
});

// 필터링 핵심 함수
function filterRooms(filterType, clickedTab) {
    // 탭 버튼 색상 변경 (clickedTab이 존재할 때만 실행)
    if (clickedTab) {
        document.querySelectorAll('.tab-btn').forEach(tab => tab.classList.remove('active'));
        clickedTab.classList.add('active');
    }

    const rooms = document.querySelectorAll('.chat-item');

    rooms.forEach(room => {
        const unreadData = room.getAttribute('data-unread') || 'false';
        const isUnread = (unreadData.trim() === 'true');

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
    const dropdown = iconElement.nextElementSibling;
    dropdown.classList.toggle('show');
}

// 화면 밖 클릭 시 드롭다운 닫기
document.addEventListener('click', function (event) {
    if (!event.target.matches('.options-icon')) {
        document.querySelectorAll('.options-dropdown.show').forEach(menu => {
            menu.classList.remove('show');
        });
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
        let targetRoom = otherRooms.find(room => {
            const roomTime = room.querySelector('.chat-time').innerText;
            return roomTime < myTime;
        });

        if (targetRoom) {
            chatList.insertBefore(chatItem, targetRoom);
        } else {
            chatList.appendChild(chatItem);
        }

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
            setTimeout(() => {
                chatItem.remove();
            }, 300);
        }
    } else {
        document.querySelectorAll('.options-dropdown.show').forEach(m => m.classList.remove('show'));
    }
}