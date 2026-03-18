/**
 * 지원자 관리 모달 및 플로팅 채팅에서 참조할 전역 상태 변수입니다.
 */
let currentAppId = null;
let currentSeekerId = null;
let currentJobId = null;
let currentJobSource = null;

/**
 * 지원자 목록에서 선택한 인재의 기본 정보를 모달에 세팅하고,
 * 서버로부터 상세 이력서 데이터를 비동기 조회하여 렌더링을 준비합니다.
 *
 * @param {HTMLElement} btn 클릭된 이력서 열람 버튼 DOM 요소
 */
async function loadResumeData(btn) {
    currentAppId = btn.getAttribute('data-app-id');
    currentSeekerId = btn.getAttribute('data-seeker-id');
    currentJobId = btn.getAttribute('data-job-id');
    currentJobSource = btn.getAttribute('data-source');

    document.getElementById('modalName').innerText = btn.getAttribute('data-name');
    document.getElementById('modalContact').innerText = btn.getAttribute('data-contact') || '-';
    document.getElementById('modalEmail').innerText = btn.getAttribute('data-email') || '-';
    document.getElementById('modalJob').innerText = btn.getAttribute('data-job');

    const modalBody = document.getElementById('dynamicResumeBody');
    modalBody.innerHTML = `<div class="text-center py-5"><div class="spinner-border text-primary" role="status"></div><p class="mt-2 text-muted">${appMsg.loading}</p></div>`;

    try {
        const response = await fetch(`/Recruiter/api/resume/${currentSeekerId}`);
        if (!response.ok) throw new Error('데이터 로드 실패');
        const data = await response.json();
        setTimeout(() => { renderResume(data, modalBody); }, 300);
    } catch (error) {
        modalBody.innerHTML = `<div class="text-center text-danger py-5"><i class="bi bi-exclamation-triangle fs-1"></i><p class="mt-2">${appMsg.loadFail}</p></div>`;
    }
}

/**
 * 전달받은 이력서 데이터 객체를 HTML 마크업으로 변환하여 지정된 컨테이너에 삽입합니다.
 *
 * @param {Object} data 서버로부터 응답받은 이력서 상세 데이터
 * @param {HTMLElement} container 렌더링된 HTML이 삽입될 DOM 요소
 */
function renderResume(data, container) {
    let html = '';

    if (data.profile) {
        html += `<div class="resume-section"><h5><i class="bi bi-person-lines-fill me-2"></i>${appMsg.labelIntro} (<span class="text-primary">${data.profile.careerType || appMsg.notEntered}</span>)</h5><p class="text-secondary mb-0" style="line-height: 1.6; white-space: pre-wrap;">${data.profile.selfPr || appMsg.notEntered}</p></div>`;
    }

    if (data.condition) {
        html += `<div class="resume-section"><h5><i class="bi bi-check2-square me-2"></i>${appMsg.labelCondition}</h5><div class="spec-item"><span class="label">${appMsg.labelJob}</span> ${data.condition.desiredJob || appMsg.notEntered}</div><div class="spec-item"><span class="label">${appMsg.labelSalary}</span> ${data.condition.salaryType || ''} ${data.condition.desiredSalary || appMsg.notEntered}</div></div>`;
    }

    html += `<div class="resume-section"><h5><i class="bi bi-briefcase-fill me-2"></i>${appMsg.labelCareer}</h5>`;
    if (data.careers && data.careers.length > 0) {
        data.careers.forEach(c => {
            html += `<div class="border-bottom pb-2 mb-2 last-no-border"><div class="fw-bold fs-6">${c.companyName} <span class="text-muted small fw-normal ms-2">(${c.department || '-'})</span></div><div class="text-muted small">${c.startDate || ''} ~ ${c.endDate || appMsg.employed}</div><div class="mt-1 text-secondary" style="font-size: 0.9rem; white-space: pre-wrap;">${c.description || ''}</div></div>`;
        });
    } else { html += `<p class="text-muted small">${appMsg.noCareer}</p>`; }
    html += `</div><div class="row">`;

    html += `<div class="col-md-6"><div class="resume-section h-100"><h5><i class="bi bi-mortarboard-fill me-2"></i>${appMsg.labelEducation}</h5>`;
    if (data.education) {
        let e = data.education;
        let statusText = e.status === 'GRADUATED' ? appMsg.graduated : (e.status === 'ATTENDING' ? appMsg.attending : (e.status === 'EXPECTED' ? appMsg.expected : appMsg.dropout));
        html += `<div class="spec-item mb-2"><span class="fw-bold d-block">${e.schoolName}</span> <span class="text-muted small">${e.major || ''} (${statusText})</span></div>`;
    } else { html += `<p class="text-muted small">${appMsg.noEducation}</p>`; }
    html += `</div></div>`;

    html += `<div class="col-md-6"><div class="resume-section h-100"><h5><i class="bi bi-award-fill me-2"></i>${appMsg.labelCert}</h5>`;
    let certsExist = false;
    if (data.certificates && data.certificates.length > 0) {
        data.certificates.forEach(c => { html += `<div class="spec-item mb-2"><span class="fw-bold d-block">${c.certName}</span> <span class="text-muted small">${c.issuer || '-'} (${c.acquisitionYear || '-'})</span></div>`; });
        certsExist = true;
    }
    if (data.languages && data.languages.length > 0) {
        data.languages.forEach(l => { html += `<div class="spec-item mb-2"><span class="fw-bold d-block">${l.language}</span> <span class="text-muted small">Level: ${l.level || '-'}</span></div>`; });
        certsExist = true;
    }
    if (!certsExist) { html += `<p class="text-muted small">${appMsg.noCert}</p>`; }
    html += `</div></div></div>`;

    if (data.documents && data.documents.length > 0) {
        html += `<div class="resume-section mt-3"><h5><i class="bi bi-file-earmark-pdf-fill me-2"></i>${appMsg.labelFile}</h5>`;
        data.documents.forEach(d => { html += `<a href="${d.fileUrl}" class="btn btn-sm btn-light border text-primary me-2 mb-2" target="_blank"><i class="bi bi-download me-1"></i> ${d.fileName}</a>`; });
        html += `</div>`;
    }
    container.innerHTML = html;
}

/**
 * 지원자의 합격 또는 불합격 상태를 서버에 전송하고, 성공 시 UI 테이블의 상태 뱃지를 즉시 갱신합니다.
 *
 * @param {string} status 적용할 지원 상태 코드 ('PASSED' 또는 'FAILED')
 */
async function updateAppStatus(status) {
    if (!currentAppId) return;

    const btnApprove = document.getElementById('btnApprove');
    const btnReject = document.getElementById('btnReject');
    btnApprove.disabled = true; btnReject.disabled = true;

    try {
        const res = await fetch(`/Recruiter/api/application/${currentAppId}/status?status=${status}`, {
            method: 'POST'
        });

        if (res.ok) {
            const modalEl = document.getElementById('resumeModal');
            const modal = bootstrap.Modal.getInstance(modalEl);
            modal.hide();

            const row = document.getElementById(`app-row-${currentAppId}`);
            const tbody = row.parentNode;

            const badgeContainer = row.querySelector('.status-cell');
            if (status === 'PASSED') {
                badgeContainer.innerHTML = `<span class="badge bg-success">${appMsg.badgePassed}</span>`;
            } else {
                badgeContainer.innerHTML = `<span class="badge bg-secondary">${appMsg.badgeFailed}</span>`;
            }
            row.classList.add('processed-row');
            tbody.appendChild(row);

        } else {
            alert(appMsg.updateFail);
        }
    } catch (e) {
        console.error(e);
        alert(appMsg.serverError);
    } finally {
        btnApprove.disabled = false; btnReject.disabled = false;
    }
}

/**
 * 선택한 지원자와 1:1 대화를 나눌 수 있는 플로팅 채팅방을 활성화합니다.
 * 현재 HTML 문서의 다국어 설정을 파악하여 채팅방 프레임 로드 시 URL 파라미터로 전달합니다.
 */
function actionChat() {
    if (!currentSeekerId || !currentJobId || !currentJobSource) {
        Swal.fire({ icon: 'error', title: appMsg.errorTitle, text: appMsg.chatError });
        return;
    }

    let currentLang = document.documentElement.lang || 'ko';

    if (currentLang === 'ko') {
        currentLang = 'kr';
    }

    const chatUrl = `/chat/create?seekerId=${currentSeekerId}&jobPostId=${currentJobId}&jobSource=${currentJobSource}&lang=${currentLang}`;

    const chatContainer = document.getElementById('floatingChatContainer');
    const chatFrame = document.getElementById('floatingChatFrame');

    chatFrame.src = chatUrl;
    chatContainer.style.display = 'flex';
    chatContainer.classList.remove('minimized');

    const resumeModalEl = document.getElementById('resumeModal');
    if (resumeModalEl) {
        const resumeModal = bootstrap.Modal.getInstance(resumeModalEl);
        if (resumeModal) resumeModal.hide();
    }
}

/**
 * 플로팅 채팅창의 최소화 상태를 토글합니다.
 */
function toggleMinimizeChat() {
    const container = document.getElementById('floatingChatContainer');
    container.classList.toggle('minimized');
}

/**
 * 플로팅 채팅창을 숨기고, 내부 iframe의 소스를 지워 웹소켓 연결 등을 정리합니다.
 */
function closeFloatingChat() {
    const container = document.getElementById('floatingChatContainer');
    const chatFrame = document.getElementById('floatingChatFrame');

    container.style.display = 'none';
    chatFrame.src = '';
}

/**
 * DOMContentLoaded 이벤트 리스너
 * 플로팅 채팅창 헤더에 마우스 다운/무브/업 이벤트를 바인딩하여
 * 화면 내에서 자유롭게 드래그 앤 드롭으로 이동할 수 있는 기능을 제공합니다.
 */
document.addEventListener('DOMContentLoaded', () => {
    const chatContainer = document.getElementById('floatingChatContainer');
    const chatHeader = document.getElementById('floatingChatHeader');

    if (!chatContainer || !chatHeader) return;

    let isDragging = false;
    let dragOffsetX, dragOffsetY;

    chatHeader.addEventListener('mousedown', (e) => {
        isDragging = true;
        const rect = chatContainer.getBoundingClientRect();
        dragOffsetX = e.clientX - rect.left;
        dragOffsetY = e.clientY - rect.top;

        chatContainer.style.transition = 'none';
    });

    document.addEventListener('mousemove', (e) => {
        if (!isDragging) return;

        let newX = e.clientX - dragOffsetX;
        let newY = e.clientY - dragOffsetY;

        chatContainer.style.bottom = 'auto';
        chatContainer.style.right = 'auto';
        chatContainer.style.left = newX + 'px';
        chatContainer.style.top = newY + 'px';
    });

    document.addEventListener('mouseup', () => {
        if (isDragging) {
            isDragging = false;
            chatContainer.style.transition = 'height 0.3s ease';
        }
    });
});