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

    // ✅ 로그인 페이지에서 넘어온 경우 감지
    const isFromLogin = document.referrer.includes('/login') ||
                        document.referrer.includes('/Login');

    // ✅ 로그인에서 왔으면 기존 세션 기록 초기화 → 애니메이션 강제 실행
    if (isFromLogin) {
        sessionStorage.removeItem('hasVisitedHome');
    }

    if (isReload || !hasVisited || isFromLogin) {
        // [A] 새로고침 / 첫 방문 / 로그인 후 진입: 애니메이션 실행
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

    // 일별 지원자 수 차트 초기화
    initApplicantChart();
});

function initApplicantChart() {
    const ctx = document.getElementById('applicantChart');
    if (!ctx) return;

    // Thymeleaf에서 전달받은 전역 변수 (chartLabels, chartData) 사용
    const labels = typeof chartLabels !== 'undefined' ? chartLabels : [];
    const data = typeof chartData !== 'undefined' ? chartData : [];

    new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: '지원자 수',
                data: data,
                borderColor: '#7db4e6',
                backgroundColor: 'rgba(125, 180, 230, 0.1)',
                borderWidth: 2,
                fill: true,
                tension: 0.4,
                pointBackgroundColor: '#7db4e6',
                pointRadius: 4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: false
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        stepSize: 1,
                        precision: 0
                    },
                    grid: {
                        display: true,
                        color: 'rgba(0,0,0,0.05)'
                    }
                },
                x: {
                    grid: {
                        display: false
                    }
                }
            }
        }
    });
}