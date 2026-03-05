// 🌟 HTML에서 선언한 타임리프 전역 변수 가져오기
const currentLang = window.CURRENT_LANG || 'ko';

function changeLanguage(lang) {
    const url = new URL(window.location.href);
    url.searchParams.set('lang', lang);
    window.location.href = url.toString();
}

function switchTab(tabName) {
    document.querySelectorAll('.tab-item').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
    document.getElementById('tab-btn-' + tabName).classList.add('active');
    document.getElementById('tab-content-' + tabName).classList.add('active');
}

function toggleAll(name, isChecked) {
    document.querySelectorAll(`input[name="${name}"]`).forEach(cb => cb.checked = isChecked);
}

// ----------------------------------------------------
// 공고 수정 로직
// ----------------------------------------------------
function openEditModal(source, id, status) {
    document.getElementById('editPostSource').value = source;
    document.getElementById('editPostId').value = id;
    document.getElementById('editPostStatus').value = status || 'RECRUITING';
    document.getElementById('editPostModal').style.display = 'flex';
}

function closeEditModal() {
    document.getElementById('editPostModal').style.display = 'none';
}

function submitEdit() {
    const source = document.getElementById('editPostSource').value;
    const id = document.getElementById('editPostId').value;
    const status = document.getElementById('editPostStatus').value;

    const msg = currentLang === 'ja' ? "修正しますか？" : "수정하시겠습니까?";
    if(confirm(msg)) {
        fetch('/admin/post/edit', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ source: source, id: id, status: status })
        }).then(res => {
            if (res.ok) {
                alert(currentLang === 'ja' ? "修正が完了しました。" : "수정이 완료되었습니다.");
                location.reload();
            } else {
                alert(currentLang === 'ja' ? "修正に失敗しました。" : "수정에 실패했습니다.");
            }
        }).catch(error => {
            console.error('Error:', error);
            alert("Network Error");
        });

        closeEditModal();
    }
}

// ----------------------------------------------------
// 신고 확인 및 상태 변경 로직
// ----------------------------------------------------
function openReportModal(id, email, category, desc, status) {
    document.getElementById('reportIdVal').value = id;
    document.getElementById('reportEmailVal').innerText = email;
    document.getElementById('reportCategoryVal').innerText = category;
    document.getElementById('reportDescVal').innerText = desc;
    document.getElementById('reportStatusSelect').value = status || 'PENDING';

    document.getElementById('reportModal').style.display = 'flex';
}

function closeReportModal() {
    document.getElementById('reportModal').style.display = 'none';
}

function submitReportStatus() {
    const reportId = document.getElementById('reportIdVal').value;
    const newStatus = document.getElementById('reportStatusSelect').value;

    const msg = currentLang === 'ja' ? "申告状態を変更しますか？" : "신고 상태를 변경하시겠습니까?";
    if(confirm(msg)) {
        fetch('/admin/report/edit', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ id: reportId, status: newStatus })
        }).then(res => {
            if(res.ok) {
                alert(currentLang === 'ja' ? "処理が完了しました。" : "처리가 완료되었습니다.");
                const url = new URL(window.location.href);
                url.searchParams.set('tab', 'report');
                window.location.href = url.toString();
            } else {
                alert(currentLang === 'ja' ? "処理に失敗しました。" : "처리에 실패했습니다.");
            }
        }).catch(error => {
            console.error('Error:', error);
            alert("Network Error");
        });

        closeReportModal();
    }
}

// ----------------------------------------------------
// 삭제 로직
// ----------------------------------------------------
function deleteOnePost(source, id) {
    const msg = currentLang === 'ja' ? "本当にこの求人を削除しますか？" : "정말 이 공고를 삭제하시겠습니까?";
    if(!confirm(msg)) return;

    fetch('/admin/post/delete', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ids: [source + "_" + id] })
    }).then(res => {
        if (res.ok) { location.reload(); }
        else { alert(currentLang === 'ja' ? "削除に失敗しました。" : "삭제 실패"); }
    });
}

function deleteSelectedItems() {
    const isReportTab = document.getElementById('tab-content-report').classList.contains('active');
    const targetName = isReportTab ? 'reportIds' : 'postIds';
    const checkedBoxes = document.querySelectorAll(`input[name="${targetName}"]:checked`);

    if (checkedBoxes.length === 0) {
        alert(currentLang === 'ja' ? "削除する項目を選択してください。" : "삭제할 항목을 선택해주세요.");
        return;
    }

    const ids = Array.from(checkedBoxes).map(cb => isReportTab ? parseInt(cb.value) : cb.value);
    if (!confirm(currentLang === 'ja' ? "選択した項目を削除しますか？" : "선택한 항목을 삭제하시겠습니까?")) return;

    fetch(isReportTab ? '/admin/report/delete' : '/admin/post/delete', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ids: ids })
    }).then(res => {
        if (res.ok) { location.reload(); }
        else { alert("Error"); }
    });
}