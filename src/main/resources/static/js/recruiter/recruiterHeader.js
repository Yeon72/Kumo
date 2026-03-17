document.addEventListener("DOMContentLoaded", function () {
    /**
     * 로컬 스토리지에 저장된 테마(다크모드/라이트모드)를 확인하여 초기 UI를 구성하고,
     * FOUC 방지를 위해 적용된 애니메이션 차단 및 강제 숨김 처리 요소들을 초기화합니다.
     */
    const body = document.body;
    const html = document.documentElement;
    const theme = localStorage.getItem("theme");
    const toggleBtn = document.getElementById("darkModeBtn");
    const icon = document.getElementById("darkModeIcon");

    if (theme === "dark") {
        body.classList.add("dark-mode");
        if (icon) {
            icon.classList.replace("fa-sun", "fa-moon");
            icon.classList.replace("fa-regular", "fa-solid");
        }
    }

    requestAnimationFrame(() => {
        window.scrollTo(0, 0);
        body.style.opacity = "1";
        body.style.visibility = "visible";

        const header = document.querySelector(".authenticated-header, .custom-header");
        if (header) {
            header.style.opacity = "1";
            header.style.visibility = "visible";
        }

        setTimeout(() => {
            const styleTag = document.getElementById("page-transition-style");
            if (styleTag) styleTag.remove();
        }, 50);

        setTimeout(() => {
            const preventTransition = document.getElementById("prevent-load-transition");
            if (preventTransition) preventTransition.remove();
        }, 400);
    });

    if (toggleBtn) {
        toggleBtn.addEventListener("click", () => {
            const isDark = body.classList.toggle("dark-mode");
            html.classList.toggle("dark-mode");
            localStorage.setItem("theme", isDark ? "dark" : "light");

            if (icon) {
                if (isDark) {
                    icon.classList.replace("fa-sun", "fa-moon");
                    icon.classList.replace("fa-regular", "fa-solid");
                } else {
                    icon.classList.replace("fa-moon", "fa-sun");
                    icon.classList.replace("fa-solid", "fa-regular");
                }
            }

            if (!isDark) html.style.cssText = "";
        });
    }

    /**
     * 헤더 영역의 드롭다운 메뉴(언어 변경, 프로필, 알림) 동작을 제어합니다.
     * 클릭 시 활성화 상태를 토글하며, 알림 메뉴 오픈 시 데이터를 비동기로 로드합니다.
     */
    const dropdownConfigs = [
        { btnId: "langBtn", menuId: "langMenu" },
        { btnId: "profileBtn", menuId: "profileMenu" },
        { btnId: "notifyBtn", menuId: "notifyMenu" },
    ];

    dropdownConfigs.forEach((config) => {
        const btn = document.getElementById(config.btnId);
        const menu = document.getElementById(config.menuId);

        if (btn && menu) {
            btn.addEventListener("click", (e) => {
                e.stopPropagation();
                const isAlreadyOpen = menu.classList.contains("show");

                document
                    .querySelectorAll(
                        ".notify-dropdown, .lang-dropdown, .profile-dropdown",
                    )
                    .forEach((m) => m.classList.remove("show"));

                if (!isAlreadyOpen) {
                    menu.classList.add("show");
                    if (config.btnId === "notifyBtn") {
                        const nList = document.getElementById("notifyList");
                        const exBtn = document.getElementById("expandBtn");
                        if (nList) nList.classList.remove("expanded");

                        const span = exBtn ? exBtn.querySelector("span") : null;
                        const icon = exBtn ? exBtn.querySelector("i") : null;
                        if (span && exBtn) span.innerText = exBtn.getAttribute("data-more") || "더 보기";
                        if (icon) icon.className = "fa-solid fa-chevron-down";

                        loadNotifications();
                    }
                }
            });
        }
    });

    document.addEventListener("click", () => {
        document
            .querySelectorAll(".notify-dropdown, .lang-dropdown, .profile-dropdown")
            .forEach((m) => m.classList.remove("show"));
    });

    document
        .querySelectorAll(".notify-dropdown, .lang-dropdown, .profile-dropdown")
        .forEach((menu) =>
            menu.addEventListener("click", (e) => e.stopPropagation()),
        );

    /**
     * 알림 시스템의 DOM 엘리먼트 및 초기 상태를 설정합니다.
     */
    const notifyList = document.getElementById("notifyList");
    const notifyBadge = document.querySelector(".notify-badge");
    const expandBtn = document.getElementById("expandBtn");
    const markAllReadBtn = document.getElementById("markAllReadBtn");
    const deleteAllBtn = document.getElementById("deleteAllBtn");

    let emptyTemplate = null;
    const originalEmpty = document.getElementById("notifyEmpty");
    if (originalEmpty) emptyTemplate = originalEmpty.cloneNode(true);

    updateBadgeCount();

    if (expandBtn) {
        expandBtn.addEventListener("click", function (e) {
            e.stopPropagation();
            if (!notifyList) return;

            const isExpanded = notifyList.classList.toggle("expanded");
            const span = this.querySelector("span");
            const icon = this.querySelector("i");
            const moreTxt = this.getAttribute("data-more") || "더 보기";
            const foldTxt = this.getAttribute("data-fold") || "접기";

            if (isExpanded) {
                if (span) span.innerText = foldTxt;
                if (icon) icon.className = "fa-solid fa-chevron-up";
            } else {
                if (span) span.innerText = moreTxt;
                if (icon) icon.className = "fa-solid fa-chevron-down";
            }
        });
    }

    if (markAllReadBtn) {
        markAllReadBtn.addEventListener("click", () => {
            fetch("/api/notifications/read-all", { method: "PATCH" }).then((res) => {
                if (res.ok) {
                    document
                        .querySelectorAll(".notify-item.unread")
                        .forEach((item) => item.classList.remove("unread"));
                    updateBadgeCount();
                }
            });
        });
    }


    if (deleteAllBtn) {
        deleteAllBtn.addEventListener("click", () => {
            const confirmMsg = deleteAllBtn.getAttribute("data-confirm") || "모든 알림을 삭제하시겠습니까?";
            if (!confirm(confirmMsg)) return;
            fetch("/api/notifications", { method: "DELETE" }).then((res) => {
                if (res.ok) {
                    renderEmptyState();
                    updateBadgeCount();
                }
            });
        });
    }

    /**
     * 서버로부터 수신된 알림 목록을 비동기 요청하여 렌더링합니다.
     */
    function loadNotifications() {
        fetch("/api/notifications")
            .then((res) => res.json())
            .then((data) => {
                notifyList.innerHTML = "";
                if (!data || data.length === 0) {
                    renderEmptyState();
                } else {
                    data.forEach((n) =>
                        notifyList.insertAdjacentHTML(
                            "beforeend",
                            createNotificationHTML(n),
                        ),
                    );
                }
                updateBadgeCount();
            })
            .catch((err) => console.error("알림 로드 실패:", err));
    }

    /**
     * 알림 내역이 없을 경우 출력할 빈 상태(Empty State) UI를 렌더링합니다.
     */
    function renderEmptyState() {
        notifyList.innerHTML = "";
        if (emptyTemplate) {
            const emptyClone = emptyTemplate.cloneNode(true);
            emptyClone.style.display = "flex";
            notifyList.appendChild(emptyClone);
        }
    }

    /**
     * 알림 데이터 객체를 기반으로 HTML 마크업 문자열을 생성합니다.
     *
     * @param {Object} notif 렌더링할 알림 데이터 객체
     * @returns {string} 생성된 알림 아이템 HTML 문자열
     */
    function createNotificationHTML(notif) {
        const isRead = notif.read || notif.isRead;
        const readClass = isRead ? "" : "unread";
        const timeStr = timeAgo(notif.createdAt);
        return `
            <div class="notify-item ${readClass}" onclick="readAndGo('${notif.targetUrl}', ${notif.notificationId}, this)">
                <div class="notify-icon"><i class="fa-solid fa-bell"></i></div>
                <div class="notify-content">
                    <p class="notify-text"><strong>${notif.title}</strong><br>${notif.content}</p>
                    <span class="notify-time">${timeStr}</span>
                </div>
                <i class="fa-solid fa-xmark delete-btn" onclick="deleteNotification(event, ${notif.notificationId}, this)"></i>
            </div>`;
    }

    /**
     * 미열람 알림 개수를 서버로부터 조회하여 알림 뱃지 UI를 갱신합니다.
     */
    function updateBadgeCount() {
        fetch("/api/notifications/unread-count")
            .then((res) => res.json())
            .then((count) => {
                if (count > 0) {
                    notifyBadge.style.display = "flex";
                    notifyBadge.innerText = count > 99 ? "99+" : count;
                } else {
                    notifyBadge.style.display = "none";
                }
            })
            .catch(() => {
                notifyBadge.style.display = "none";
            });
    }

    window.refreshBadge = updateBadgeCount;
});

/**
 * 다국어 설정을 변경하고 페이지를 새로고침합니다.
 *
 * @param {string} lang 변경할 언어 코드 ('ko', 'ja')
 */
window.changeLang = function (lang) {
    const url = new URL(window.location.href);
    url.searchParams.set("lang", lang);
    window.location.href = url.toString();
};

/**
 * 단일 알림 항목을 삭제하는 비동기 요청을 수행합니다.
 *
 * @param {Event} event 클릭 이벤트 객체
 * @param {string|number} id 삭제할 알림의 고유 식별자
 * @param {HTMLElement} btn 클릭된 삭제 버튼 요소
 */
window.deleteNotification = function (event, id, btn) {
    event.stopPropagation();
    fetch(`/api/notifications/${id}`, { method: "DELETE" }).then((res) => {
        if (res.ok) {
            const item = btn.closest(".notify-item");
            item.remove();
            const list = document.getElementById("notifyList");
            if (list.querySelectorAll(".notify-item").length === 0) {
                if (window.refreshBadge) window.refreshBadge();
            }
            window.refreshBadge();
        }
    });
};

/**
 * 알림 클릭 시 읽음 처리를 수행한 후 해당 알림의 대상 URL로 이동합니다.
 *
 * @param {string} url 이동할 대상 경로
 * @param {string|number} id 읽음 처리할 알림 식별자
 * @param {HTMLElement} el 클릭된 알림 요소
 */
window.readAndGo = function (url, id, el) {
    if (!el.classList.contains("unread")) {
        location.href = url;
        return;
    }
    fetch(`/api/notifications/${id}/read`, { method: "PATCH" })
        .then(() => (location.href = url))
        .catch(() => (location.href = url));
};

/**
 * 지정된 날짜 문자열을 현재 시간과 비교하여 경과 시간(예: '방금 전', '시간 전')으로 포맷팅합니다.
 *
 * @param {string} dateString 포맷팅할 기준 날짜 문자열
 * @returns {string} 다국어가 적용된 경과 시간 텍스트
 */
window.timeAgo = function (dateString) {
    if (!dateString) return "";
    const diff = Math.floor((new Date() - new Date(dateString)) / 1000);

    const headerTitle = document.querySelector(".notify-header span");
    const isTextJA = headerTitle && headerTitle.innerText.trim() === "通知";
    const isJA =
        document.documentElement.lang === "ja" ||
        location.href.includes("lang=ja") ||
        isTextJA;

    const i18n = isJA
        ? { now: "今", min: "分前", hr: "時間前", day: "日前" }
        : { now: "방금 전", min: "분 전", hr: "시간 전", day: "일 전" };

    if (diff < 60) return i18n.now;
    if (diff < 3600) return Math.floor(diff / 60) + i18n.min;
    if (diff < 86400) return Math.floor(diff / 3600) + i18n.hr;
    return Math.floor(diff / 86400) + i18n.day;
};

/**
 * 브라우저 리소스 로드 완료(load) 시 실행되는 이벤트 리스너
 * FOUC 방지를 위해 적용된 애니메이션 차단 요소를 제거하여 정상 렌더링을 허용합니다.
 */
window.addEventListener('load', () => {
    const preventTransition = document.getElementById('prevent-load-transition');
    if (preventTransition) preventTransition.remove();
});