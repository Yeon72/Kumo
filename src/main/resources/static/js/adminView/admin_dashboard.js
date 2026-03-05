// 🌟 HTML에서 넘겨준 전역 변수를 받아서 사용합니다.
const currentLang = window.CURRENT_LANG || 'ko';

const TEXT = {
    barLabel: currentLang === 'ja' ? '登録数' : '등록 수',
    lineLabel: currentLang === 'ja' ? '会員数' : '회원 수',
    noData: currentLang === 'ja' ? 'データなし' : '데이터 없음'
};

let barChart, doughnutChart, lineChart;
let dashboardData = {};

function changeLanguage(lang) {
    const url = new URL(window.location.href);
    url.searchParams.set('lang', lang);
    window.location.href = url.toString();
}

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

// 🌟 HTML에서 넘겨준 btnElement 파라미터를 사용하도록 수정
function toggleRegion(region, btnElement) {
    document.querySelectorAll('.toggle-btn').forEach(btn => btn.classList.remove('active'));
    if (btnElement) {
        btnElement.classList.add('active');
    }
    updateRegionChart(region);
}

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