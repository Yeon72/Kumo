/**
 * 전역 다국어 설정 변수 (HTML 템플릿에서 주입, 기본값: 'ko')
 * @type {string}
 */
const currentLang = window.CURRENT_LANG || 'ko';

/**
 * DOMContentLoaded 이벤트 리스너
 * 백엔드에서 전달된 탭 상태를 초기화하고 사용자 통계 데이터를 호출하며,
 * 전체 선택 체크박스 이벤트를 바인딩합니다.
 */
document.addEventListener('DOMContentLoaded', () => {
    const activeTab = window.ACTIVE_TAB || 'all';
    switchTab(activeTab);

    fetchUserStats();

    const checkAll = document.getElementById('checkAll');
    if(checkAll){
        checkAll.addEventListener('change', function() {
            document.querySelectorAll('input[name="userIds"]').forEach(cb => cb.checked = this.checked);
        });
    }
});

/**
 * 활성화된 탭 버튼 및 탭 콘텐츠 화면을 전환합니다.
 *
 * @param {string} tabName 전환할 대상 탭의 식별자
 */
function switchTab(tabName) {
    document.querySelectorAll('.tab-item').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));

    const tabBtn = document.getElementById('tab-btn-' + tabName);
    const tabContent = document.getElementById('tab-content-' + tabName);

    if (tabBtn && tabContent) {
        tabBtn.classList.add('active');
        tabContent.classList.add('active');
    }
}

/**
 * 다국어(언어) 설정을 변경하고 페이지를 새로고침하며, 현재 열려있는 탭 상태를 URL 파라미터로 유지합니다.
 *
 * @param {string} lang 변경할 언어 코드
 */
function changeLanguage(lang) {
    const url = new URL(window.location.href);
    url.searchParams.set('lang', lang);

    const activeTabEl = document.querySelector('.tab-content.active');
    if(activeTabEl) {
        url.searchParams.set('tab', activeTabEl.id.replace('tab-content-', ''));
    }

    window.location.href = url.toString();
}

/**
 * 특정 구인자의 가입을 승인(상태를 ACTIVE로 변경)하는 API 요청을 전송합니다.
 *
 * @param {string|number} userId 승인할 구인자의 고유 식별자
 */
function approveRecruiter(userId) {
    const msg = currentLang === 'ja' ? "この求人者を承認しますか？" : "이 구인자의 가입을 승인하시겠습니까?";
    if(confirm(msg)) {
        const payload = {
            userId: userId,
            role: "RECRUITER",
            status: "ACTIVE"
        };

        fetch('/admin/user/edit', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        }).then(response => {
            if(response.ok) {
                alert(currentLang === 'ja' ? "承認されました。" : "승인 완료되었습니다.");
                const url = new URL(window.location.href);
                url.searchParams.set('tab', 'approval');
                window.location.href = url.toString();
            } else {
                alert("Error");
            }
        });
    }
}

/**
 * 서버로부터 사용자 통계 데이터를 비동기 요청하여 가져온 후 UI를 갱신합니다.
 */
async function fetchUserStats() {
    try {
        const response = await fetch('/admin/user/stats');
        if (!response.ok) throw new Error('Network error');
        const data = await response.json();

        document.getElementById('totalUsers').innerText = data.totalUsers.toLocaleString();
        document.getElementById('newUsers').innerText = data.newUsers.toLocaleString();
        document.getElementById('activeUsers').innerText = data.activeUsers.toLocaleString();
        document.getElementById('inactiveUsers').innerText = data.inactiveUsers.toLocaleString();
    } catch (error) {
        console.error("통계 로딩 실패:", error);
    }
}

/**
 * 사용자 정보 수정을 위한 모달 창을 열고, 기존 데이터를 폼에 세팅합니다.
 *
 * @param {string|number} id 사용자 고유 식별자
 * @param {string} currentRole 현재 부여된 권한 (예: SEEKER, RECRUITER)
 * @param {string} currentStatus 현재 계정 상태 (예: ACTIVE, INACTIVE)
 */
function openEditModal(id, currentRole, currentStatus) {
    document.getElementById('editUserId').value = id;
    document.getElementById('editRole').value = currentRole;
    document.getElementById('editStatus').value = currentStatus;
    document.getElementById('editModal').style.display = 'flex';
}

/**
 * 사용자 정보 수정 모달 창을 닫습니다.
 */
function closeEditModal() {
    document.getElementById('editModal').style.display = 'none';
}

/**
 * 폼에 입력된 데이터를 바탕으로 사용자 권한 및 상태 수정 요청(API)을 전송합니다.
 */
function submitEdit() {
    const userId = document.getElementById('editUserId').value;
    const newRole = document.getElementById('editRole').value;
    const newStatus = document.getElementById('editStatus').value;

    const msg = currentLang === 'ja' ? "修正しますか？" : "수정하시겠습니까?";
    if(confirm(msg)) {
        const payload = {
            userId: userId,
            role: newRole,
            status: newStatus
        };

        fetch('/admin/user/edit', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        }).then(response => {
            if(response.ok) {
                alert(currentLang === 'ja' ? "修正が完了しました。" : "수정이 완료되었습니다.");
                closeEditModal();
                location.reload();
            } else {
                alert(currentLang === 'ja' ? "修正に失敗しました。" : "수정에 실패했습니다.");
            }
        }).catch(error => {
            console.error('Error:', error);
            alert("Network Error");
        });
    }
}

/**
 * 특정 사용자 계정에 대한 삭제 요청(API)을 전송합니다.
 *
 * @param {string|number} id 삭제할 사용자의 고유 식별자
 */
function deleteUser(id) {
    if(confirm(currentLang === 'ja' ? "本当に削除しますか？" : "정말 삭제하시겠습니까?")) {
        fetch('/admin/user/delete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId: id })
        }).then(response => {
            if(response.ok) {
                alert(currentLang === 'ja' ? "削除されました。" : "삭제되었습니다.");
                location.reload();
            } else {
                alert(currentLang === 'ja' ? "削除に失敗しました。" : "삭제에 실패했습니다.");
            }
        }).catch(error => {
            console.error('Error:', error);
            alert("Network Error");
        });
    }
}

/**
 * 구인자 심사(가입 승인) 처리를 위한 모달 창을 열고 데이터를 세팅합니다.
 *
 * @param {HTMLElement} btn 클릭된 버튼 DOM 요소 (데이터 속성 포함)
 */
function openApprovalModal(btn) {
    const id = btn.getAttribute('data-id');
    const name = btn.getAttribute('data-name');
    const email = btn.getAttribute('data-email');
    const date = btn.getAttribute('data-date');
    const evidencesStr = btn.getAttribute('data-evidences');

    document.getElementById('approveUserId').value = id;

    const labelName = currentLang === 'ja' ? '名前' : '이름';
    const labelEmail = currentLang === 'ja' ? 'メール' : '이메일';
    const labelDate = currentLang === 'ja' ? '加入日' : '가입일';

    document.getElementById('approveUserInfo').innerHTML = `
        <b>${labelName}:</b> ${name} <br>
        <b>${labelEmail}:</b> ${email} <br>
        <b>${labelDate}:</b> ${date}
    `;

    const evidenceBox = document.getElementById('approveEvidenceList');
    evidenceBox.innerHTML = '';

    if (evidencesStr && evidencesStr.trim() !== '') {
        const urls = evidencesStr.split(',');
        urls.forEach((url, idx) => {
            const btnText = currentLang === 'ja' ? `書類 ${idx + 1} 確認` : `증빙서류 ${idx + 1} 확인하기`;
            evidenceBox.innerHTML += `
                <a href="${url}" target="_blank" class="btn-outline" style="text-align:center; text-decoration:none; display:block; padding: 10px;">
                    <i class="fa-solid fa-file-invoice"></i> ${btnText}
                </a>
            `;
        });
    } else {
        const noDataMsg = currentLang === 'ja' ? '添付された書類がありません。' : '첨부된 증빙서류가 없습니다.';
        evidenceBox.innerHTML = `<div style="padding:15px; background:#f8f9fa; border-radius:8px; text-align:center; color:#999; font-size:13px; border: 1px dashed #ccc;">${noDataMsg}</div>`;
    }

    document.getElementById('approvalModal').style.display = 'flex';
}

/**
 * 구인자 심사 모달 창을 닫습니다.
 */
function closeApprovalModal() {
    document.getElementById('approvalModal').style.display = 'none';
}

/**
 * 심사 모달에서 승인 버튼 클릭 시, 해당 구인자의 상태를 활성화(ACTIVE)하는 요청을 전송합니다.
 */
function approveFromModal() {
    const userId = document.getElementById('approveUserId').value;
    const msg = currentLang === 'ja' ? "この求人者を承認しますか？" : "이 구인자의 가입을 승인하시겠습니까?";

    if(confirm(msg)) {
        const payload = {
            userId: userId,
            role: "RECRUITER",
            status: "ACTIVE"
        };

        fetch('/admin/user/edit', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        }).then(response => {
            if(response.ok) {
                alert(currentLang === 'ja' ? "承認されました。" : "승인 완료되었습니다.");
                const url = new URL(window.location.href);
                url.searchParams.set('tab', 'approval');
                window.location.href = url.toString();
            } else {
                alert("Error");
            }
        });
    }
}

/**
 * 심사 모달에서 거절 버튼 클릭 시, 해당 구인자의 가입을 거절하고 계정을 완전 삭제하는 요청을 전송합니다.
 */
function rejectFromModal() {
    const userId = document.getElementById('approveUserId').value;
    const msg = currentLang === 'ja' ? "本当に拒否(削除)しますか？" : "가입을 거절하고 계정을 삭제하시겠습니까?";

    if(confirm(msg)) {
        fetch('/admin/user/delete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId: userId })
        }).then(response => {
            if(response.ok) {
                alert(currentLang === 'ja' ? "拒否(削除)されました。" : "가입 거절 및 삭제 처리되었습니다.");
                const url = new URL(window.location.href);
                url.searchParams.set('tab', 'approval');
                window.location.href = url.toString();
            } else {
                alert("Error");
            }
        });
    }
}