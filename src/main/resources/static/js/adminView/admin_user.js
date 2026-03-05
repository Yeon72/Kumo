// 🌟 HTML에서 선언한 타임리프 전역 변수 가져오기
const currentLang = window.CURRENT_LANG || 'ko';

document.addEventListener('DOMContentLoaded', () => {
    // 백엔드에서 내려준 탭 상태 (기본은 all)
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

// 언어 변경 시 탭 상태도 URL에 유지
function changeLanguage(lang) {
    const url = new URL(window.location.href);
    url.searchParams.set('lang', lang);

    // 현재 열려있는 탭 파악
    const activeTabEl = document.querySelector('.tab-content.active');
    if(activeTabEl) {
        url.searchParams.set('tab', activeTabEl.id.replace('tab-content-', ''));
    }

    window.location.href = url.toString();
}

// 구인자 승인 로직 (기존 유저 정보 수정 API를 재활용하여 상태만 ACTIVE로 변경)
function approveRecruiter(userId) {
    const msg = currentLang === 'ja' ? "この求人者を承認しますか？" : "이 구인자의 가입을 승인하시겠습니까?";
    if(confirm(msg)) {
        const payload = {
            userId: userId,
            role: "RECRUITER", // 권한 유지
            status: "ACTIVE"   // INACTIVE -> ACTIVE 로 변경
        };

        fetch('/admin/user/edit', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        }).then(response => {
            if(response.ok) {
                alert(currentLang === 'ja' ? "承認されました。" : "승인 완료되었습니다.");
                // 리로드 시 승인 탭 유지
                const url = new URL(window.location.href);
                url.searchParams.set('tab', 'approval');
                window.location.href = url.toString();
            } else {
                alert("Error");
            }
        });
    }
}

// 통계 호출 함수
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

function openEditModal(id, currentRole, currentStatus) {
    document.getElementById('editUserId').value = id;
    document.getElementById('editRole').value = currentRole;
    document.getElementById('editStatus').value = currentStatus;
    document.getElementById('editModal').style.display = 'flex';
}

function closeEditModal() {
    document.getElementById('editModal').style.display = 'none';
}

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

// ==========================================================
// 구인자 심사 모달 로직
// ==========================================================

// 1. 모달 열기 및 데이터 세팅
function openApprovalModal(btn) {
    const id = btn.getAttribute('data-id');
    const name = btn.getAttribute('data-name');
    const email = btn.getAttribute('data-email');
    const date = btn.getAttribute('data-date');
    const evidencesStr = btn.getAttribute('data-evidences'); // 쉼표로 연결된 URL 문자열

    // ID 저장
    document.getElementById('approveUserId').value = id;

    // 기본 정보 세팅
    const labelName = currentLang === 'ja' ? '名前' : '이름';
    const labelEmail = currentLang === 'ja' ? 'メール' : '이메일';
    const labelDate = currentLang === 'ja' ? '加入日' : '가입일';

    document.getElementById('approveUserInfo').innerHTML = `
        <b>${labelName}:</b> ${name} <br>
        <b>${labelEmail}:</b> ${email} <br>
        <b>${labelDate}:</b> ${date}
    `;

    // 증빙서류 리스트 세팅
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

    // 모달 띄우기
    document.getElementById('approvalModal').style.display = 'flex';
}

// 2. 모달 닫기
function closeApprovalModal() {
    document.getElementById('approvalModal').style.display = 'none';
}

// 3. 모달에서 [승인] 버튼 클릭 시
function approveFromModal() {
    const userId = document.getElementById('approveUserId').value;
    const msg = currentLang === 'ja' ? "この求人者を承認しますか？" : "이 구인자의 가입을 승인하시겠습니까?";

    if(confirm(msg)) {
        const payload = {
            userId: userId,
            role: "RECRUITER",
            status: "ACTIVE"   // 상태를 활성화로 변경
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

// 4. 모달에서 [거절] 버튼 클릭 시 (계정 삭제)
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