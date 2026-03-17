/**
 * search_job_list.js
 * 구인 공고 검색 리스트 화면에서 사용되는 프론트엔드 비즈니스 로직을 담당합니다.
 * 필터 조작, URL 파라미터 파싱, 비동기 검색 데이터 요청 및 테이블 렌더링 기능을 수행합니다.
 */

/**
 * 애플리케이션의 전역 상태를 관리하는 객체입니다.
 * @type {Object}
 * @property {Set} scrapedJobIds 사용자가 스크랩(찜)한 공고의 식별자(ID_Source) 목록
 */
const AppState = {
    scrapedJobIds: new Set()
}

/**
 * DOMContentLoaded 이벤트 리스너
 * URL 파라미터를 파싱하여 필터 UI를 초기화하고, 로그인 상태에 따라
 * 사용자의 스크랩 데이터를 미리 로드한 후 최초 검색을 실행합니다.
 */
$(document).ready(function() {
    const urlParams = new URLSearchParams(window.location.search);
    const keyword = urlParams.get('keyword') || '';
    const mainRegion = urlParams.get('mainRegion') || 'tokyo';
    const subRegion = urlParams.get('subRegion') || '';

    $('#keywordInput').val(keyword);
    $('#mainRegion').val(mainRegion);

    updateSubRegions();
    if (subRegion) {
        $('#subRegion').val(subRegion);
    }

    if (typeof isUserLoggedIn !== 'undefined' && isUserLoggedIn) {
        SearchService.initSavedJobs(() => SearchService.fetchList());
    } else {
        SearchService.fetchList();
    }

    $('#mainRegion').on('change', function() {
        updateSubRegions();
        SearchService.fetchList();
    });

    $('#subRegion').on('change', SearchService.fetchList);

    $('#btnSearch').on('click', SearchService.fetchList);
    $('#keywordInput').on('keyup', function(e) {
        if (e.key === 'Enter') SearchService.fetchList();
    });
});

/**
 * 선택된 메인 지역(도쿄/오사카)에 따라 서브 지역(구/시) 셀렉트 박스의 옵션 목록을 동적으로 갱신합니다.
 */
function updateSubRegions() {
    const mainRegion = $('#mainRegion').val();
    const $subSelect = $('#subRegion');

    $subSelect.empty();
    $subSelect.append(`<option value="">${LIST_MESSAGES.allRegion}</option>`);

    if (RegionData[mainRegion]) {
        RegionData[mainRegion].forEach(sub => {
            $subSelect.append(`<option value="${sub}">${sub}</option>`);
        });
    }
}

/**
 * 비동기 검색 요청, 데이터 렌더링 및 스크랩 상태 동기화를 담당하는 서비스 객체입니다.
 */
const SearchService = {
    /**
     * 로그인된 사용자가 이전에 스크랩(찜)했던 공고 식별자 목록을 서버로부터 조회하여
     * AppState 객체에 초기화(캐싱)합니다.
     *
     * @param {Function} callback 초기화 완료 후 실행할 콜백 함수
     */
    initSavedJobs: function(callback) {
        const currentLang = new URLSearchParams(window.location.search).get('lang') || 'kr';
        $.ajax({
            url: `/api/scraps?lang=${currentLang}`,
            method: 'GET',
            dataType: 'json',
            success: function(data) {
                AppState.scrapedJobIds.clear();
                if(data && data.length > 0) {
                    data.forEach(job => AppState.scrapedJobIds.add(job.id + '_' + job.source));
                }
                if (callback) callback();
            }
        });
    },

    /**
     * 사용자가 입력한 필터 조건을 바탕으로 서버에 비동기 검색을 요청합니다.
     * 검색이 실행될 때 브라우저의 URL 주소도 새로고침 없이 동기화하여 필터 상태를 보존합니다.
     */
    fetchList: function() {
        const keyword = $('#keywordInput').val().trim();
        const mainRegion = $('#mainRegion').val();
        const subRegion = $('#subRegion').val();
        const currentLang = new URLSearchParams(window.location.search).get('lang') || 'kr';

        const newUrl = `/map/search_list?lang=${currentLang}&mainRegion=${mainRegion}&subRegion=${encodeURIComponent(subRegion)}&keyword=${encodeURIComponent(keyword)}`;
        window.history.pushState(null, '', newUrl);

        $('#searchListBody').html(`<tr><td colspan="7" style="text-align:center; padding: 40px;">${LIST_MESSAGES.loading}</td></tr>`);

        $.ajax({
            url: '/map/api/jobs/search',
            method: 'GET',
            data: {
                keyword: keyword,
                mainRegion: mainRegion,
                subRegion: subRegion,
                lang: currentLang
            },
            dataType: 'json',
            success: function(response) {
                SearchService.renderTable(response);
            },
            error: function(xhr, status, error) {
                console.error("검색 실패:", error);
                $('#searchListBody').html(`<tr><td colspan="7" style="text-align:center; padding: 40px; color: red;">${LIST_MESSAGES.error}</td></tr>`);
            }
        });
    },

    /**
     * 서버로부터 응답받은 공고 데이터 목록을 기반으로 검색 결과 테이블의 HTML DOM을 동적으로 생성하여 렌더링합니다.
     * 스크랩 캐시(AppState)를 확인하여 찜 버튼의 UI 상태를 결정합니다.
     *
     * @param {Array} jobs 렌더링할 구인 공고 데이터 배열
     */
    renderTable: function(jobs) {
        const $tbody = $('#searchListBody');

        const currentLang = new URLSearchParams(window.location.search).get('lang') || 'kr';

        if (!jobs || jobs.length === 0) {
            $tbody.html(`<tr><td colspan="7" style="text-align:center; padding: 40px; color: #888;">${LIST_MESSAGES.empty}</td></tr>`);
            return;
        }

        let html = '';
        jobs.forEach(job => {
            const detailUrl = `/map/jobs/detail?id=${job.id}&source=${job.source}&lang=${currentLang}`;

            const jobSignature = job.id + '_' + job.source;
            const isSaved = AppState.scrapedJobIds.has(jobSignature);

            let btnClass = 'btn-outline';
            let btnText = LIST_MESSAGES.saveBtn || '찜하기';

            if (isSaved) {
                btnClass = 'btn-saved';
                btnText = LIST_MESSAGES.unsaveBtn || (currentLang === 'ja' ? '保存解除' : '찜해제');
            }

            const saveBtnHtml = isUserLoggedIn
                ? `<button class="${btnClass}" onclick="SearchService.toggleSearchScrap(this, ${job.id}, '${job.source}')">${btnText}</button>`
                : '';


            let managerName = job.managerName;
            if (job.userId === 9999 || !managerName) {
                managerName = "Admin";
            }


            html += `
            <tr>
                <td>
                    <div class="job-title-cell">
                        <span class="job-title-text">${job.title || '제목 없음'}</span>
                    </div>
                </td>
                <td class="text-blue font-weight-bold">${job.companyName || '-'}</td>
                <td>${job.address || '-'}</td>
                <td>
                    <div class="wage-box">
                        <span class="wage-type">${LIST_MESSAGES.wageType}</span>
                        <span class="wage-amount">${job.wage || '-'}</span>
                    </div>
                </td>
                <td class="text-muted">${job.contactPhone || '-'}</td>
                <td>
                    <div class="author-box">
                        <img src="${job.profileImageUrl || '/images/common/default_profile.png'}" class="author-img">
                        <div class="author-info">
                            <span class="author-name">${managerName}</span>
                        </div>
                    </div>
                </td>
                <td>
                    <div class="action-buttons">
                        ${saveBtnHtml}
                        <button class="btn-filled" onclick="location.href='${detailUrl}'">${LIST_MESSAGES.detailBtn}</button>
                    </div>
                </td>
            </tr>`;
        });

        $tbody.html(html);
    },

    /**
     * 검색 리스트 내 특정 공고의 스크랩(찜) 상태를 토글하는 비동기 통신을 수행합니다.
     * 서버 응답 성공 시 버튼의 디자인(클래스 및 텍스트)과 내부 상태 캐시를 갱신합니다.
     *
     * @param {HTMLElement} btnElement 클릭된 스크랩 버튼 DOM 요소
     * @param {number|string} jobId 대상 공고 식별자
     * @param {string} source 대상 공고 데이터 출처
     */
    toggleSearchScrap: function(btnElement, jobId, source) {
        const $btn = $(btnElement);
        const currentLang = new URLSearchParams(window.location.search).get('lang') || 'kr';
        const jobSignature = jobId + '_' + source;

        $.ajax({
            url: '/api/scraps',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ targetPostId: jobId, targetSource: source }),
            success: function(response) {
                let isSaved = false;
                if (typeof response === 'boolean') isSaved = response;
                else if (response && response.isScraped !== undefined) isSaved = response.isScraped;
                else if (response && response.scraped !== undefined) isSaved = response.scraped;
                else if (response && response.result !== undefined) isSaved = response.result;

                if (isSaved) {
                    $btn.removeClass('btn-outline').addClass('btn-saved').text(LIST_MESSAGES.unsaveBtn || (currentLang === 'ja' ? '保存解除' : '찜해제'));
                    AppState.scrapedJobIds.add(jobSignature);
                } else {
                    $btn.removeClass('btn-saved').addClass('btn-outline').text(LIST_MESSAGES.saveBtn || '찜하기');
                    AppState.scrapedJobIds.delete(jobSignature);
                }
            },
            error: function() {
                alert("처리 중 오류가 발생했습니다.");
            }
        });
    }
};