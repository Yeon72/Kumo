document.addEventListener("DOMContentLoaded", function () {
  /* ==============================
       1. 다크모드 초기화 및 애니메이션 복구
       ============================== */
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

  // 🌟 사장님의 원본 복구 로직 (이상한 transition 꼼수 다 뺐습니다!)
  requestAnimationFrame(() => {
    window.scrollTo(0, 0);
    body.style.opacity = "1";
    body.style.visibility = "visible";

    // position: fixed인 헤더는 body와 별도로 명시적 복구
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

  // 다크모드 토글 이벤트
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

  /* ==============================
     이 아래부터는 사장님의 원래 코드 (드롭다운, 알림 기능 등) 그대로 유지!
     ============================== */

  /* ==============================
       2. 드롭다운 통합 관리
       ============================== */
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

  /* ==============================
       3. 알림 시스템 로직
       ============================== */
  const notifyList = document.getElementById("notifyList");
  const notifyBadge = document.querySelector(".notify-badge");
  const expandBtn = document.getElementById("expandBtn");
  const markAllReadBtn = document.getElementById("markAllReadBtn");
  const deleteAllBtn = document.getElementById("deleteAllBtn");

  // "알림 없음" 요소 백업
  let emptyTemplate = null;
  const originalEmpty = document.getElementById("notifyEmpty");
  if (originalEmpty) emptyTemplate = originalEmpty.cloneNode(true);

  updateBadgeCount();

  // [3-1] 더 보기 / 접기 버튼
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

  // [3-2] 모두 읽음
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
  

  // [3-4] 알림 로드 및 렌더링
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

  function renderEmptyState() {
    notifyList.innerHTML = "";
    if (emptyTemplate) {
      const emptyClone = emptyTemplate.cloneNode(true);
      emptyClone.style.display = "flex";
      notifyList.appendChild(emptyClone);
    }
  }

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

  // [3-5] 뱃지 카운트
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

/* ==============================
   4. 전역 유틸리티 함수
   ============================== */
window.changeLang = function (lang) {
  const url = new URL(window.location.href);
  url.searchParams.set("lang", lang);
  window.location.href = url.toString();
};

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

window.readAndGo = function (url, id, el) {
  if (!el.classList.contains("unread")) {
    location.href = url;
    return;
  }
  fetch(`/api/notifications/${id}/read`, { method: "PATCH" })
    .then(() => (location.href = url))
    .catch(() => (location.href = url));
};

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

// 페이지 로딩이 다 끝나면, 애니메이션 차단벽을 치워줍니다!
window.addEventListener('load', () => {
    const preventTransition = document.getElementById('prevent-load-transition');
    if (preventTransition) preventTransition.remove();
});
