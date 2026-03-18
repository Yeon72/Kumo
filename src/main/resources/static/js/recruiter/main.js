/**
 * 선택한 인재에게 스카우트 제의를 보내는 동작을 처리합니다.
 *
 * @param {string|number} userId 스카우트 제의를 받을 대상 사용자의 식별자
 */
function sendScoutOffer(userId) {
    if (confirm("해당 인재에게 스카우트 제의를 보내시겠습니까?")) {
        alert("제의를 보냈습니다. (userId: " + userId + ")");
    }
}

/**
 * DOMContentLoaded 이벤트 리스너
 * 페이지 진입 경로(최초 방문, 새로고침, 로그인 직후 진입 등)를 분석하여
 * 메인 대시보드의 초기 환영 애니메이션 실행 여부를 동적으로 제어합니다.
 */
document.addEventListener('DOMContentLoaded', function() {
    const navEntries = performance.getEntriesByType("navigation")[0];
    const isReload = navEntries && navEntries.type === "reload";
    const hasVisited = sessionStorage.getItem("hasVisitedHome");

    const isFromLogin = document.referrer.includes('/login') ||
        document.referrer.includes('/Login');

    if (isFromLogin) {
        sessionStorage.removeItem('hasVisitedHome');
    }

    if (isReload || !hasVisited || isFromLogin) {
        document.body.classList.add("start-animation");
        sessionStorage.setItem("hasVisitedHome", "true");
    } else {
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

/**
 * 브라우저 리소스 로드 완료(load) 이벤트 리스너
 * 메인 컨테이너에 페이드 인(Fade-in) 효과를 적용하고 지원자 수 통계 차트를 초기화합니다.
 */
window.addEventListener("load", function () {
    const container = document.querySelector(".main-container");
    if (container) {
        container.classList.add("fade-in");
    }

    initApplicantChart();
});

/**
 * 일별 지원자 수 변동 추이를 보여주는 라인 차트를 초기화하고 화면에 렌더링합니다.
 * 서버(Thymeleaf)로부터 주입받은 전역 변수 chartLabels와 chartData를 참조합니다.
 */
function initApplicantChart() {
    const ctx = document.getElementById('applicantChart');
    if (!ctx) return;

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