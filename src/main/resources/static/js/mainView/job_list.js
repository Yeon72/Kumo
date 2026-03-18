/**
 * job_list.js
 * 구인 목록 페이지의 데이터를 동적으로 렌더링하고 관리하는 프론트엔드 비즈니스 로직을 담당합니다.
 */

/**
 * DOMContentLoaded 이벤트 리스너
 * URL 파라미터를 파싱하여 언어 설정 및 지도 좌표(Bounds)를 추출한 뒤,
 * 해당 조건에 맞는 구인 공고 데이터를 서버 API로부터 비동기(Fetch) 조회합니다.
 */
document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);

    const currentLang = urlParams.get('lang') === 'ja' ? 'ja' : 'ko';

    const apiParams = new URLSearchParams();
    apiParams.append('minLat', urlParams.get('minLat'));
    apiParams.append('maxLat', urlParams.get('maxLat'));
    apiParams.append('minLng', urlParams.get('minLng'));
    apiParams.append('maxLng', urlParams.get('maxLng'));

    apiParams.append('lang', currentLang);

    if (currentLang === 'ja') {
        const headers = document.querySelectorAll('#tableHeader th');
        const jpHeaders = ['タイトル', '会社名', '勤務地', '給与', '連絡先', '担当者', '管理'];
        headers.forEach((th, idx) => th.innerText = jpHeaders[idx]);
    }

    fetch(`/map/api/jobs?${apiParams.toString()}`)
        .then(res => res.json())
        .then(data => renderList(data, currentLang))
        .catch(err => {
            console.error(err);
            document.getElementById('listBody').innerHTML =
                `<tr><td colspan="7" class="msg-box">
                    ${currentLang === 'ja' ? 'データを読み込めませんでした。' : '데이터를 불러오지 못했습니다.'}
                </td></tr>`;
        });
});

/**
 * API로부터 전달받은 구인 공고 배열 데이터를 테이블(DOM)에 동적으로 렌더링합니다.
 * 다국어 설정(lang)에 따라 출력되는 텍스트 및 배지(Badge) 언어가 자동 변환됩니다.
 *
 * @param {Array} jobs 렌더링할 구인 공고 데이터 배열
 * @param {string} lang 현재 설정된 다국어 언어 코드 ('ko' 또는 'ja')
 */
function renderList(jobs, lang) {
    const tbody = document.getElementById('listBody');

    if (!jobs || jobs.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="msg-box">
            ${lang === 'ja' ? '現在、この地域には求人がありません。' : '현재 이 지역에 공고가 없습니다.'}
        </td></tr>`;
        return;
    }

    let html = '';
    jobs.forEach(job => {

        const title = job.title;
        const company = job.companyName;
        const wage = job.wage;

        const address = job.address || '-';

        const thumb = job.thumbnailUrl || 'https://via.placeholder.com/40';
        const dateStr = job.writeTime || (lang === 'ja' ? 'ついさっき' : '방금 전');
        const contact = job.contactPhone || '-';

        html += `
        <tr>
            <td>
                <span class="title-text">${title}</span>
                <span class="badge bg-blue">${lang === 'ja' ? '募集中' : '구인중'}</span>
                <span class="badge bg-yellow">${lang === 'ja' ? '急募' : '급구'}</span>
            </td>
            <td><a href="#" class="company-text">${company}</a></td>
            <td><span class="addr-text">${address.split(' ')[0]}</span></td>
            <td><span class="wage-text">${wage}</span></td>
            <td style="color:#666; font-size:12px;">${contact}</td>
            <td>
                <div class="profile-wrap">
                    <img src="${thumb}" class="profile-img">
                    <div class="profile-info">
                        <div>Admin</div>
                        <div>${dateStr}</div>
                    </div>
                </div>
            </td>
            <td>
                <div class="btn-wrap">
                    <button class="btn">${lang === 'ja' ? '保存' : '찜'}</button>
                    <button class="btn btn-view" onclick="window.open('/jobs/${job.id}')">
                        ${lang === 'ja' ? '詳細' : '상세'}
                    </button>
                </div>
            </td>
        </tr>
        `;
    });

    tbody.innerHTML = html;
}