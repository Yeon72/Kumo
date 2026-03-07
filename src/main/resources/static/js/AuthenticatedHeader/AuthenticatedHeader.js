document.addEventListener("DOMContentLoaded", function() {

    /* ==============================
       1. 다크모드 및 기본 설정
       ============================== */
    const body = document.body;
    const html = document.documentElement;
    const theme = localStorage.getItem('theme');
    const toggleBtn = document.getElementById('darkModeBtn');
    const icon = document.getElementById('darkModeIcon');

    // 로컬 스토리지 테마 적용
    if (theme === 'dark') {
        body.classList.add('dark-mode');
        html.classList.add('dark-mode');
        if(icon) {
            icon.classList.replace('fa-regular', 'fa-solid');
            icon.classList.replace('fa-sun', 'fa-moon');
        }
    }

    if (toggleBtn) {
        toggleBtn.addEventListener('click', () => {
            const isDark = body.classList.toggle('dark-mode');
            html.classList.toggle('dark-mode');
            localStorage.setItem('theme', isDark ? 'dark' : 'light');

            if(icon) {
                if (isDark) {
                    icon.classList.replace('fa-sun', 'fa-moon');
                    icon.classList.replace('fa-regular', 'fa-solid');
                } else {
                    icon.classList.replace('fa-moon', 'fa-sun');
                    icon.classList.replace('fa-solid', 'fa-regular');
                }
            }
        });
    }

    /* ==============================
       2. 드롭다운 통합 관리
       ============================== */
    const dropdownConfigs = [
        { btnId: 'langBtn', menuId: 'langMenu' },
        { btnId: 'profileBtn', menuId: 'profileMenu' },
        { btnId: 'notifyBtn', menuId: 'notifyMenu' }
    ];

    dropdownConfigs.forEach(config => {
        const btn = document.getElementById(config.btnId);
        const menu = document.getElementById(config.menuId);

        if (btn && menu) {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const isAlreadyOpen = menu.classList.contains('show');

                // 모든 드롭다운 닫기
                document.querySelectorAll('.notify-dropdown, .lang-dropdown, .profile-dropdown')
                        .forEach(m => m.classList.remove('show'));

                // 방금 클릭한 게 닫혀있었다면 열기
                if (!isAlreadyOpen) {
                    menu.classList.add('show');
                    if (config.btnId === 'notifyBtn') {
                        const nList = document.getElementById('notifyList');
                        const exBtn = document.getElementById('expandBtn');
                        if(nList) nList.classList.remove('expanded'); 
                        
                        const span = exBtn ? exBtn.querySelector('span') : null;
                        const icon = exBtn ? exBtn.querySelector('i') : null;
                        if(span && exBtn) span.innerText = exBtn.getAttribute('data-more') || "더 보기";
                        if(icon) icon.className = 'fa-solid fa-chevron-down';
                        
                        loadNotifications(); // 알림창 열 때 데이터 로드
                    }
                }
            });
        }
    });

    // 화면 클릭 시 닫기
    document.addEventListener('click', () => {
        document.querySelectorAll('.notify-dropdown, .lang-dropdown, .profile-dropdown')
                .forEach(m => m.classList.remove('show'));
    });

    // 메뉴 내부 클릭 시 닫힘 방지
    document.querySelectorAll('.notify-dropdown, .lang-dropdown, .profile-dropdown')
            .forEach(menu => {
                menu.addEventListener('click', (e) => e.stopPropagation());
            });


    /* ==============================
       3. 알림 시스템 로직
       ============================== */
    const notifyList = document.getElementById('notifyList');
    const notifyBadge = document.querySelector('.notify-badge');
    const expandBtn = document.getElementById('expandBtn');
    const markAllReadBtn = document.getElementById('markAllReadBtn');
    const deleteAllBtn = document.getElementById('deleteAllBtn');

    // "알림 없음" 요소를 미리 복제해둠
    let emptyTemplate = null;
    const originalEmpty = document.getElementById('notifyEmpty');
    if (originalEmpty) {
        emptyTemplate = originalEmpty.cloneNode(true);
    }

    // 초기 뱃지 로드
    updateBadgeCount();

    // [3-1] 더 보기 / 접기 버튼
    if (expandBtn) {
        expandBtn.addEventListener('click', function(e) {
            e.stopPropagation();
            if (!notifyList) return;
            
            const isExpanded = notifyList.classList.toggle('expanded');
            const span = this.querySelector('span');
            const icon = this.querySelector('i');
            const moreTxt = this.getAttribute('data-more') || "더 보기";
            const foldTxt = this.getAttribute('data-fold') || "접기";

            if (isExpanded) {
                if(span) span.innerText = foldTxt;
                if(icon) icon.className = 'fa-solid fa-chevron-up';
            } else {
                if(span) span.innerText = moreTxt;
                if(icon) icon.className = 'fa-solid fa-chevron-down';
            }
        });
    }

    // [3-2] 모두 읽음
    if (markAllReadBtn) {
        markAllReadBtn.addEventListener('click', () => {
            fetch('/api/notifications/read-all', { method: 'PATCH' })
                .then(res => {
                    if (res.ok) {
                        document.querySelectorAll('.notify-item.unread').forEach(item => item.classList.remove('unread'));
                        updateBadgeCount();
                    }
                });
        });
    }

    // [3-3] 전체 삭제
    if (deleteAllBtn) {
        deleteAllBtn.addEventListener('click', () => {
            const confirmMsg = deleteAllBtn.getAttribute('data-confirm') || "모든 알림을 삭제하시겠습니까?";
            if(!confirm(confirmMsg)) return;
            fetch('/api/notifications', { method: 'DELETE' })
                .then(res => {
                    if (res.ok) {
                        renderEmptyState();
                        updateBadgeCount();
                    }
                });
        });
    }

    // [3-4] 알림 로드 및 렌더링
    function loadNotifications() {
        console.log("알림 로드 시도...");
        fetch('/api/notifications')
            .then(res => res.json())
            .then(data => {
                console.log("받은 알림 데이터:", data);
                if (!notifyList) return;
                
                notifyList.innerHTML = ''; 

                if (!data || !Array.isArray(data) || data.length === 0) {
                    renderEmptyState();
                    if(expandBtn) expandBtn.style.display = 'none'; 
                } else {
                    data.forEach(n => {
                        notifyList.insertAdjacentHTML('beforeend', createNotificationHTML(n));
                    });
                    if(expandBtn) {
                        expandBtn.style.display = data.length > 4 ? 'block' : 'none';
                    }
                }
                updateBadgeCount();
            })
            .catch(err => console.error("알림 로드 실패:", err));
    }

    function renderEmptyState() {
        notifyList.innerHTML = '';
        if (emptyTemplate) {
            const emptyClone = emptyTemplate.cloneNode(true);
            emptyClone.style.display = 'flex'; 
            notifyList.appendChild(emptyClone);
        }
    }

    function createNotificationHTML(notif) {
        const isRead = notif.read || notif.isRead;
        const readClass = isRead ? '' : 'unread';
        const timeStr = window.timeAgo ? window.timeAgo(notif.createdAt) : notif.createdAt;

        return `
            <div class="notify-item ${readClass}" onclick="window.readAndGo('${notif.targetUrl}', ${notif.notificationId}, this)">
                <div class="notify-icon"><i class="fa-solid fa-bell"></i></div>
                <div class="notify-content">
                    <p class="notify-text"><strong>${notif.title}</strong><br>${notif.content}</p>
                    <span class="notify-time">${timeStr}</span>
                </div>
                <i class="fa-solid fa-xmark delete-btn" onclick="window.deleteNotification(event, ${notif.notificationId}, this)"></i>
            </div>
        `;
    }

    function updateBadgeCount() {
        fetch('/api/notifications/unread-count')
            .then(res => res.json())
            .then(count => {
                if (count > 0) {
                    notifyBadge.style.display = 'flex';
                    notifyBadge.innerText = count > 99 ? '99+' : count;
                } else {
                    notifyBadge.style.display = 'none';
                }
            })
            .catch(() => { notifyBadge.style.display = 'none'; });
    }

    window.refreshBadge = updateBadgeCount;
});

/* ==============================
   4. 전역 유틸리티 함수
   ============================== */
window.changeLang = function(lang) {
    const url = new URL(window.location.href);
    url.searchParams.set('lang', lang);
    window.location.href = url.toString();
};

window.deleteNotification = function(event, id, btn) {
    event.stopPropagation();
    fetch(`/api/notifications/${id}`, { method: 'DELETE' })
        .then(res => {
            if (res.ok) {
                const item = btn.closest('.notify-item');
                item.remove();
                if (document.getElementById('notifyList').querySelectorAll('.notify-item').length === 0) {
                     window.refreshBadge();
                }
                window.refreshBadge();
            }
        });
};

window.readAndGo = function(url, id, el) {
    if (!el.classList.contains('unread')) {
        location.href = url;
        return;
    }
    fetch(`/api/notifications/${id}/read`, { method: 'PATCH' })
        .then(() => location.href = url)
        .catch(() => location.href = url);
};

window.timeAgo = function(dateString) {
    if (!dateString) return "";
    const diff = Math.floor((new Date() - new Date(dateString)) / 1000);

    const headerTitle = document.querySelector('.notify-header span');
    const isTextJA = headerTitle && headerTitle.innerText.trim() === '通知';

    const isJA = document.documentElement.lang === 'ja' ||
                 location.href.includes('lang=ja') ||
                 isTextJA;

    const i18n = isJA
        ? { now: "今", min: "分前", hr: "時間前", day: "日前" }
        : { now: "방금 전", min: "분 전", hr: "시간 전", day: "일 전" };

    if (diff < 60) return i18n.now;
    if (diff < 3600) return Math.floor(diff / 60) + i18n.min;
    if (diff < 86400) return Math.floor(diff / 3600) + i18n.hr;
    return Math.floor(diff / 86400) + i18n.day;
};
