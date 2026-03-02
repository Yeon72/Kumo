function sendScoutOffer(userId) {
    if (confirm("해당 인재에게 스카우트 제의를 보내시겠습니까?")) {
        // TODO: 실제 제의하기 API 호출 로직 (예: fetch /Recruiter/api/scout)
        alert("제의를 보냈습니다. (userId: " + userId + ")");
    }
}

document.addEventListener('DOMContentLoaded', function() {
  const navEntries = performance.getEntriesByType("navigation")[0];
  const isReload = navEntries && navEntries.type === "reload";
  const hasVisited = sessionStorage.getItem("hasVisitedHome");

  if (isReload || !hasVisited) {
    // [A] 새로고침 또는 첫 방문: 클래스 추가해서 애니메이션 실행
    document.body.classList.add("start-animation");
    sessionStorage.setItem("hasVisitedHome", "true");
  } else {
    // [B] 메뉴 이동 시: 클래스를 제거하고 모든 효과를 'none'으로 강제 고정
    document.body.classList.remove("start-animation");

    const targets = [
      ".welcome-section",
      ".stat-row",
      ".graph-container",
      ".dashboard-right-sidebar",
    ];
    targets.forEach((selector) => {
      const el = document.querySelector(selector);
      if (el) {
        el.style.animation = "none";
        el.style.opacity = "1";
        el.style.transform = "none";
      }
    });
  }
});

window.addEventListener("load", function () {
  const container = document.querySelector(".main-container");
  if (container) {
    container.classList.add("fade-in");
  }
});
