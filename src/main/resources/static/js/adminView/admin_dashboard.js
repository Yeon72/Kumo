/** * 전역 다국어 설정 변수 (HTML 템플릿에서 주입, 기본값: 'ko')
 * @type {string}
 */
const currentLang = window.CURRENT_LANG || 'ko';

/** * 차트 및 UI 렌더링에 사용되는 다국어 텍스트 객체
 * @type {Object}
 */
const TEXT = {
    barLabel: currentLang === 'ja' ? '登録数' : '등록 수',
    lineLabel: currentLang === 'ja' ? '会員数' : '회원 수',
    noData: currentLang === 'ja' ? 'データなし' : '데이터 없음'
};

let barChart, doughnutChart, lineChart;
let dashboardData = {};

/**
 * 다국어(언어) 설정을 변경하고 페이지를 새로고침합니다.
 *
 * @param {string} lang 변경할 언어 코드 ('ko', 'ja')
 */
function changeLanguage(lang) {
    const url = new URL(window.location.href);
    url.searchParams.set('lang', lang);
    window.location.href = url.toString();
}

/**
 * 대시보드 내의 막대(Bar), 도넛(Doughnut), 꺾은선(Line) 차트 인스턴스를 초기화합니다.
 * Chart.js 라이브러리를 사용합니다.
 */
function initCharts() {
    const ctxBar = document.getElementById('barChart').getContext('2d');
    barChart = new Chart(ctxBar, {
        type: 'bar',
        data: {
            labels: [],
            datasets: [{
                label: TEXT.barLabel,
                data: [],
                backgroundColor: '#3b5bdb',
                borderRadius: 15,
                barThickness: 30
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: { beginAtZero: true, grid: { borderDash: [5, 5] } },
                x: { grid: { display: false } }
            }
        }
    });

    const ctxDoughnut = document.getElementById('doughnutChart').getContext('2d');
    doughnutChart = new Chart(ctxDoughnut, {
        type: 'doughnut',
        data: {
            labels: [],
            datasets: [{
                data: [],
                borderWidth: 2,
                borderColor: '#ffffff'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '65%',
            plugins: {
                legend: { display: false },
                tooltip: { enabled: true }
            }
        }
    });

    const ctxLine = document.getElementById('lineChart').getContext('2d');
    const gradient = ctxLine.createLinearGradient(0, 0, 0, 300);
    gradient.addColorStop(0, 'rgba(51, 154, 240, 0.4)');
    gradient.addColorStop(1, 'rgba(51, 154, 240, 0.0)');

    lineChart = new Chart(ctxLine, {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: TEXT.lineLabel,
                data: [],
                borderColor: '#339af0',
                backgroundColor: gradient,
                fill: true,
                tension: 0.4,
                pointRadius: 3
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
                y: { beginAtZero: true, grid: { borderDash: [5, 5] } },
                x: { grid: { display: false } }
            }
        }
    });
}

/**
 * 서버로부터 관리자 대시보드 통계 데이터를 비동기 요청하여 가져옵니다.
 * 데이터를 성공적으로 수신하면 UI 갱신 함수를 호출합니다.
 */
async function fetchDashboardData() {
    try {
        const response = await fetch('/admin/data');
        if (!response.ok) throw new Error('Network response was not ok');
        dashboardData = await response.json();
        updateUI(dashboardData);
    } catch (error) {
        console.error("데이터 로딩 중 오류 발생:", error);
        document.getElementById('totalPosts').innerText = "0";
        document.getElementById('newPosts').innerText = "0";
    }
}

/**
 * 응답받은 통계 데이터를 기반으로 화면 내 텍스트 수치 및 차트 데이터를 업데이트합니다.
 *
 * @param {Object} data 대시보드 통계 데이터 객체
 */
function updateUI(data) {
    document.getElementById('totalUsers').innerText = data.totalUsers.toLocaleString();
    document.getElementById('newUsers').innerText = data.newUsers.toLocaleString();
    document.getElementById('totalPosts').innerText = data.totalPosts.toLocaleString();
    document.getElementById('newPosts').innerText = data.newPosts.toLocaleString();

    document.querySelectorAll('.loading').forEach(el => el.classList.remove('loading'));

    const sortedWeekly = Object.entries(data.weeklyPostStats || {})
        .sort((a, b) => a[0].localeCompare(b[0]));
    barChart.data.labels = sortedWeekly.map(entry => entry[0].substring(5));
    barChart.data.datasets[0].data = sortedWeekly.map(entry => entry[1]);
    barChart.update();

    updateRegionChart('osaka');

    const sortedMonthly = Object.entries(data.monthlyUserStats || {});
    lineChart.data.labels = sortedMonthly.map(entry => entry[0]);
    lineChart.data.datasets[0].data = sortedMonthly.map(entry => entry[1]);
    lineChart.update();
}

/**
 * 도넛 차트용 커스텀 범례(Legend) UI를 동적으로 생성하여 렌더링합니다.
 *
 * @param {Object} chart 범례를 생성할 대상 Chart.js 인스턴스
 */
function generateCustomLegend(chart) {
    const legendContainer = document.getElementById('customLegend');
    legendContainer.innerHTML = '';

    const data = chart.data;
    if (!data.labels.length || !data.datasets.length) return;

    const listUl = document.createElement('ul');
    listUl.className = 'legend-list';

    data.labels.forEach((label, index) => {
        const color = data.datasets[0].backgroundColor[index];
        const value = data.datasets[0].data[index];

        const li = document.createElement('li');
        li.className = 'legend-item';

        const colorBox = document.createElement('span');
        colorBox.className = 'legend-color-box';
        colorBox.style.backgroundColor = color;

        const textSpan = document.createElement('span');
        textSpan.className = 'legend-text';
        textSpan.innerText = label;
        textSpan.title = label;

        const valueSpan = document.createElement('span');
        valueSpan.className = 'legend-value';
        valueSpan.innerText = value.toLocaleString();

        li.appendChild(colorBox);
        li.appendChild(textSpan);
        li.appendChild(valueSpan);
        listUl.appendChild(li);
    });

    legendContainer.appendChild(listUl);
}

/**
 * 지역 토글 버튼 클릭 시 활성화 상태를 변경하고 해당 지역의 차트 데이터를 갱신합니다.
 *
 * @param {string} region 선택된 지역 코드 ('osaka' 또는 'tokyo')
 * @param {HTMLElement} btnElement 클릭된 버튼 DOM 요소
 */
function toggleRegion(region, btnElement) {
    document.querySelectorAll('.toggle-btn').forEach(btn => btn.classList.remove('active'));
    if (btnElement) {
        btnElement.classList.add('active');
    }
    updateRegionChart(region);
}

/**
 * 선택된 지역에 따라 도넛 차트(구/시별 통계) 데이터를 갱신합니다.
 *
 * @param {string} region 지역 코드 ('osaka' 또는 'tokyo')
 */
function updateRegionChart(region) {
    let stats = {};
    if (region === 'osaka') {
        stats = dashboardData.osakaWardStats || {};
    } else {
        stats = dashboardData.tokyoWardStats || {};
    }

    if (Object.keys(stats).length === 0) {
        doughnutChart.data.labels = [TEXT.noData];
        doughnutChart.data.datasets[0].data = [1];
        doughnutChart.data.datasets[0].backgroundColor = ['#e9ecef'];
    } else {
        doughnutChart.data.labels = Object.keys(stats);
        doughnutChart.data.datasets[0].data = Object.values(stats);
        doughnutChart.data.datasets[0].backgroundColor = ['#364fc7', '#e599f7', '#339af0', '#ff922b', '#adb5bd', '#63e6be'];
    }
    doughnutChart.update();
    generateCustomLegend(doughnutChart);
}

document.addEventListener('DOMContentLoaded', () => {
    initCharts();
    fetchDashboardData();
});