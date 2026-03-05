/** ?????????????????????????
 * job_detail.js
 * - HTML에서 선언된 'isUserLoggedIn'과 'MESSAGES' 객체를 바로 사용합니다!
 * - 삼항 연산자와 URL 파라미터 파싱 로직은 완전히 삭제되었습니다.
 */

// =========================================
// [기능 1] 신고 모달 관련 로직
// =========================================
function openReportModal() {
    if (!isUserLoggedIn) {
        if (confirm(MESSAGES.loginRequired)) location.href = '/login';
        return;
    }
    document.getElementById('reportModal').style.display = 'flex';
    document.body.style.overflow = 'hidden';
}

function closeReportModal() {
    document.getElementById('reportModal').style.display = 'none';
    document.body.style.overflow = 'auto';
    document.getElementById('reportType').value = "";
    document.getElementById('reportDetail').value = "";
}

function submitReport() {
    const type = document.getElementById('reportType').value;
    const detail = document.getElementById('reportDetail').value;

    // 리소스에서 가져온 메시지 사용
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
                    if(confirm(MESSAGES.loginRequired)) location.href = '/login';
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

// =========================================
// 🌟 [기능 2] 구인 신청 관련 로직 (수정됨)
// =========================================
function applyForJob(btnElement) {
    // 1. 로그인 여부 체크
    if (!isUserLoggedIn) {
        if (confirm(MESSAGES.loginRequired)) location.href = '/login';
        return;
    }

    // 2. 버튼에 심어둔 공고 정보 가져오기
    const postId = btnElement.getAttribute('data-id');
    const source = btnElement.getAttribute('data-source');

    if (!postId || !source) {
        alert("Error: ID or Source Not Found");
        return;
    }

    // 3. 지원 확인 메시지 (다국어 처리를 위해 lang 변수 확인, 기본값 kr)
    // HTML에 선언된 언어 변수가 없다면 기본 한국어로 동작합니다.
    const lang = typeof currentLang !== 'undefined' ? currentLang : 'kr';
    const confirmMsg = lang === 'ja' ? "この求人に応募しますか？" : "이 공고에 지원하시겠습니까?";

    if (!confirm(confirmMsg)) {
        return; // 취소 누르면 함수 종료
    }

    // 4. 백엔드로 보낼 JSON 데이터 포장 (ApplicationDTO.ApplyRequest 규격에 맞춤)
    const payload = {
        targetPostId: parseInt(postId),
        targetSource: source
    };

    // 5. 서버로 POST 요청 쏘기
    fetch('/map/api/apply', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
    })
        .then(async response => {
            // 서버에서 보낸 메시지 추출
            const message = await response.text();

            if (response.ok) {
                // 성공 (200 OK)
                alert(message); // 서버가 보낸 "구인 신청이 완료되었습니다."

                // UX 향상: 버튼 비활성화 및 스타일 변경
                btnElement.disabled = true;
                btnElement.innerText = lang === 'ja' ? '応募完了' : '지원 완료';
                btnElement.style.backgroundColor = '#6c757d';
                btnElement.style.borderColor = '#6c757d';
                btnElement.style.cursor = 'not-allowed';

            } else if (response.status === 401) {
                // 세션 만료 등으로 인한 비로그인 상태
                if (confirm(MESSAGES.loginRequired)) location.href = '/login';
            } else {
                // 실패 (400 중복 지원, 403 권한 없음 등 백엔드에서 던진 에러 메시지)
                alert(message);
            }
        })
        .catch(error => {
            console.error("지원 처리 에러:", error);
            alert(MESSAGES.processError || "처리 중 오류가 발생했습니다. 다시 시도해 주세요.");
        });
}

// ==========================================
// [기능 3] 즐겨찾기 (스크랩) 기능 구현
// ==========================================
function toggleScrap(btnElement) {
    if (!isUserLoggedIn) {
        if (confirm(MESSAGES.loginRequired)) location.href = '/login';
        return;
    }

    const $btn = $(btnElement);
    const jobId = $btn.data('id');
    const $svg = $btn.find('svg');
    // 🌟 [핵심 추가] 버튼의 data-source 속성에서 TOKYO, OSAKA 등의 값을 꺼내옵니다!
    const source = $btn.data('source');

    // 만약 값이 제대로 안 읽힌다면 에러 방지
    if (!jobId || !source) {
        console.error("찜하기 실패: ID 또는 Source 값을 찾을 수 없습니다.", {jobId, source});
        return;
    }

    $.ajax({
        url: '/api/scraps',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({ targetPostId: jobId , targetSource: source}),
        success: function(response) {

            // 🌟 [핵심 변경] 스프링이 is를 빼고 보냈을 경우(scraped)까지 완벽하게 체크합니다!
            const isScrapedResult = response.isScraped !== undefined ? response.isScraped : response.scraped;

            if (isScrapedResult) {
                $svg.attr('fill', '#4285F4').attr('stroke', '#4285F4');
            } else {
                $svg.attr('fill', 'none').attr('stroke', '#999');
            }
        },
        error: function(xhr) {
            if (xhr.status === 401) {
                if (confirm(MESSAGES.loginRequired)) location.href = '/login';
            } else {
                alert(MESSAGES.processError);
            }
        }
    });
}

// ==========================================
// [기능 4] 공고 수정 및 삭제 (작성자 전용)
// ==========================================

// 1. 공고 수정 페이지로 이동
function editJob(btnElement) {
    const postId = btnElement.getAttribute('data-id');
    const source = btnElement.getAttribute('data-source');

    if (!postId || !source) {
        alert("Error: ID or Source Not Found");
        return;
    }

    // TODO: 나중에 실제 공고 수정 폼 페이지 URL로 변경하세요.
    // 기존 공고의 id와 source를 달아서 보내면, 수정 페이지 컨트롤러에서 이 값을 받아 기존 데이터를 폼에 채워줄 수 있습니다.
    const editUrl = `/map/jobs/edit?id=${postId}&source=${source}`;
    window.location.href = editUrl;
}

// 2. 공고 완전 삭제
function deleteJob(btnElement) {
    const postId = btnElement.getAttribute('data-id');
    const source = btnElement.getAttribute('data-source');

    if (!postId || !source) {
        alert("Error: ID or Source Not Found");
        return;
    }

    const lang = typeof currentLang !== 'undefined' ? currentLang : 'kr';
    const confirmMsg = lang === 'ja' ? "本当にこの求人を削除しますか？\n(削除すると元に戻せません)" : "정말로 이 공고를 삭제하시겠습니까?\n(삭제 후 복구할 수 없습니다.)";

    // 사용자 확인
    if (!confirm(confirmMsg)) {
        return;
    }

    // 서버로 DELETE 요청 전송
    fetch(`/map/api/jobs?id=${postId}&source=${source}`, {
        method: 'DELETE'
    })
        .then(async response => {
            const message = await response.text();

            if (response.ok) {
                alert(lang === 'ja' ? "削除が完了しました。" : "삭제가 완료되었습니다.");
                // 삭제 성공 시, 공고 목록(메인) 페이지로 튕겨내기
                window.location.href = '/map/main';
            } else {
                // 권한 없음 등의 에러
                alert(message);
            }
        })
        .catch(error => {
            console.error("삭제 에러:", error);
            alert(lang === 'ja' ? "処理中にエラーが発生しました。" : "처리 중 서버 오류가 발생했습니다.");
        });
}