/**
 * 공고 상세 페이지에서 사용되는 프론트엔드 비즈니스 로직을 담당합니다.
 * HTML에서 선언된 'isUserLoggedIn'과 'MESSAGES' 객체를 전역으로 활용하며,
 * 다국어(언어) 감지 시 외부 함수의 로드 실패를 대비한 안전장치(Fallback)가 적용되어 있습니다.
 */

/**
 * 신고 모달창을 엽니다.
 * 비로그인 상태일 경우 로그인 페이지로 이동할지 묻습니다.
 */
function openReportModal() {
    if (!isUserLoggedIn) {
        if (confirm(MESSAGES.loginRequired)) location.href = '/login';
        return;
    }
    document.getElementById('reportModal').style.display = 'flex';
    document.body.style.overflow = 'hidden';
}

/**
 * 신고 모달창을 닫고 입력 폼을 초기화합니다.
 */
function closeReportModal() {
    document.getElementById('reportModal').style.display = 'none';
    document.body.style.overflow = 'auto';
    document.getElementById('reportType').value = "";
    document.getElementById('reportDetail').value = "";
}

/**
 * 서버로 신고 데이터를 전송합니다.
 * 유효성 검사 후 POST 요청을 수행하며, 처리 결과에 따라 알림을 표시합니다.
 */
function submitReport() {
    const type = document.getElementById('reportType').value;
    const detail = document.getElementById('reportDetail').value;

    if (!type) {
        alert(MESSAGES.selectReportType);
        return;
    }

    const applyBtn = document.querySelector('.btn-apply');
    const targetId = applyBtn ? applyBtn.getAttribute('data-id') : null;
    const targetSource = applyBtn ? applyBtn.getAttribute('data-source') : null;

    if (!targetId) {
        alert("Error: ID Not Found");
        return;
    }

    if (confirm(MESSAGES.confirmReport)) {
        const reportData = {
            targetPostId: targetId,
            targetSource: targetSource,
            reasonCategory: type,
            description: detail
        };

        fetch('/map/api/reports', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(reportData)
        })
            .then(response => {
                if (response.ok) {
                    alert(MESSAGES.reportSuccess);
                    closeReportModal();
                } else if (response.status === 401) {
                    if (confirm(MESSAGES.loginRequired)) location.href = '/login';
                } else {
                    return response.text().then(text => { throw new Error(text) });
                }
            })
            .catch(error => {
                console.error('Error:', error);
                alert(MESSAGES.reportError);
            });
    }
}

/**
 * 구직자가 공고에 구인 신청(지원)을 수행합니다.
 * 언어 설정에 따른 다국어 알림을 지원하며, 중복 지원 및 서버 에러를 처리합니다.
 * * @param {HTMLElement} btnElement 클릭된 지원하기 버튼 요소
 */
function applyForJob(btnElement) {
    if (!isUserLoggedIn) {
        if (confirm(MESSAGES.loginRequired)) location.href = '/login';
        return;
    }

    const postId = btnElement.getAttribute('data-id');
    const source = btnElement.getAttribute('data-source');

    if (!postId || !source) {
        alert("Error: ID or Source Not Found");
        return;
    }

    const lang = (typeof window.getKumoLang === 'function') ? window.getKumoLang() : 'ko';
    const confirmMsg = lang === 'ja' ? "この求人に応募しますか？" : "이 공고에 지원하시겠습니까?";

    if (!confirm(confirmMsg)) {
        return;
    }

    const payload = {
        targetPostId: parseInt(postId),
        targetSource: source
    };

    fetch('/map/api/apply', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
    })
        .then(async response => {
            await response.text();

            if (response.ok) {
                alert(lang === 'ja' ? '求人の応募が完了しました。' : '구인 신청이 완료되었습니다.');

                btnElement.disabled = true;
                btnElement.innerText = lang === 'ja' ? '応募完了' : '지원 완료';
                btnElement.style.backgroundColor = '#6c757d';
                btnElement.style.borderColor = '#6c757d';
                btnElement.style.cursor = 'not-allowed';

            } else if (response.status === 400) {
                alert(lang === 'ja' ? 'すでに応募した求人です。' : '이미 지원하신 공고입니다.');
            } else if (response.status === 401) {
                if (confirm(MESSAGES.loginRequired)) location.href = '/login';
            } else {
                alert(lang === 'ja' ? '処理中にエラーが発生しました。' : '처리 중 서버 오류가 발생했습니다.');
            }
        })
        .catch(error => {
            console.error("지원 처리 에러:", error);
            alert(lang === 'ja' ? '処理中にエラーが発生しました。' : '처리 중 오류가 발생했습니다. 다시 시도해 주세요.');
        });
}

/**
 * 공고를 스크랩(즐겨찾기) 하거나 취소합니다.
 * * @param {HTMLElement} btnElement 클릭된 스크랩 버튼 요소
 */
function toggleScrap(btnElement) {
    if (!isUserLoggedIn) {
        if (confirm(MESSAGES.loginRequired)) location.href = '/login';
        return;
    }

    const $btn = $(btnElement);
    const jobId = $btn.data('id');
    const source = $btn.data('source');
    const $svg = $btn.find('svg');

    if (!jobId || !source) {
        console.error("찜하기 실패: ID 또는 Source 값을 찾을 수 없습니다.", { jobId, source });
        return;
    }

    $.ajax({
        url: '/api/scraps',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({ targetPostId: jobId, targetSource: source }),
        success: function (response) {
            const isScrapedResult = response.isScraped !== undefined ? response.isScraped : response.scraped;

            if (isScrapedResult) {
                $svg.attr('fill', '#4285F4').attr('stroke', '#4285F4');
            } else {
                $svg.attr('fill', 'none').attr('stroke', '#999');
            }
        },
        error: function (xhr) {
            if (xhr.status === 401) {
                if (confirm(MESSAGES.loginRequired)) location.href = '/login';
            } else {
                alert(MESSAGES.processError);
            }
        }
    });
}

/**
 * 구인자가 본인이 작성한 공고의 수정 페이지로 이동합니다.
 * * @param {HTMLElement} btnElement 클릭된 수정 버튼 요소
 */
function editJob(btnElement) {
    const postId = btnElement.getAttribute('data-id');
    const source = btnElement.getAttribute('data-source');

    if (!postId || !source) {
        alert("Error: ID or Source Not Found");
        return;
    }

    const editUrl = `/Recruiter/editJobPosting?id=${postId}&region=${source}`;
    window.location.href = editUrl;
}

/**
 * 구인자가 본인이 작성한 공고를 삭제합니다.
 * 다국어 지원 및 외부 JS 로드 실패에 대한 안전장치가 포함되어 있습니다.
 * * @param {HTMLElement} btnElement 클릭된 삭제 버튼 요소
 */
function deleteJob(btnElement) {
    const postId = btnElement.getAttribute('data-id');
    const source = btnElement.getAttribute('data-source');

    if (!postId || !source) {
        alert("Error: ID or Source Not Found");
        return;
    }

    const lang = (typeof window.getKumoLang === 'function') ? window.getKumoLang() : 'kr';
    const confirmMsg = lang === 'ja' ? "本当にこの求人を削除しますか？\n(削除すると元に戻せません)" : "정말로 이 공고를 삭제하시겠습니까?\n(삭제 후 복구할 수 없습니다.)";

    if (!confirm(confirmMsg)) {
        return;
    }

    fetch(`/map/api/jobs?id=${postId}&source=${source}`, {
        method: 'DELETE',
    })
        .then(async response => {
            const message = await response.text();

            if (response.ok) {
                alert(lang === 'ja' ? "削除が完了しました。" : "삭제가 완료되었습니다.");
                window.location.href = '/Recruiter/JobManage';
            } else {
                alert(message);
            }
        })
        .catch(error => {
            console.error("삭제 에러:", error);
            alert(lang === 'ja' ? "処理中にエラーが発生しました。" : "처리 중 서버 오류가 발생했습니다.");
        });
}