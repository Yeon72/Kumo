document.addEventListener('DOMContentLoaded', () => {

    // ==========================================
    // 1. 도쿄/오사카 지역 변경 로직
    // ==========================================
    const location1 = document.getElementById('location1');
    const location2 = document.getElementById('location2');

    if (location1 && location2) {
        const allOptions = Array.from(location2.querySelectorAll('option'));
        
        // 초기 로딩 시 선택되어 있는 값을 저장
        const initialValue2 = location2.value;

        location1.addEventListener('change', function() {
            const selectedPrefecture = this.value;
            const targetClass = 'ward-' + selectedPrefecture;
            
            location2.innerHTML = '';
            allOptions.forEach(option => {
                if (option.classList.contains(targetClass)) {
                    // 복제본을 추가하여 원본(allOptions)은 유지
                    const clone = option.cloneNode(true);
                    // 기존에 선택되었던 값과 일치하면 다시 선택 상태로 만듦
                    if (clone.value === initialValue2) {
                        clone.selected = true;
                    }
                    location2.appendChild(clone);
                }
            });
        });
        
        // 초기 실행 (기존 데이터가 있을 경우를 고려)
        location1.dispatchEvent(new Event('change'));
    }

    // 초기 상태 설정 (경력/신입 토글에 따른 보임/숨김)
    const initialCareerType = document.getElementById('careerType')?.value;
    if (initialCareerType === 'NEWCOMER') {
        const careerFields = document.getElementById('careerFields');
        const btnAddCareerWrapper = document.getElementById('btnAddCareer')?.parentElement;
        if (careerFields) {
            careerFields.style.display = 'none';
            careerFields.querySelectorAll('input, select, textarea').forEach(el => el.disabled = true);
        }
        if (btnAddCareerWrapper) btnAddCareerWrapper.style.display = 'none';
    }

    // ==========================================
// 2. 공통 폼 복사 함수 (Clone 마법)
// ==========================================
// 자격증/어학 템플릿 (아이템이 없을 때 사용)
const TEMPLATES = {
    certFields: `
        <div class="form-group row cert-item clonable-item">
            <label>자격증</label>
            <div class="input-group cert-group">
                <input type="text" name="certName" class="custom-input" style="flex:1">
                <input type="text" name="certPublisher" class="custom-input" style="flex:1">
                <select name="certYear" class="custom-select" style="flex:1">
                    <option value="" selected disabled>취득연도</option>
                    ${Array.from({length: 57}, (_, i) => 2026 - i).map(y => `<option value="${y}">${y}</option>`).join('')}
                </select>
                <button type="button" class="btn-delete-item"><i class="fa-solid fa-xmark"></i></button>
            </div>
        </div>`,
    langFields: `
        <div class="form-group row mt-5 lang-item clonable-item">
            <label>어학 능력</label>
            <div class="input-group cert-group">
                <input type="text" name="languageName" class="custom-input" style="flex:1">
                <div class="toggle-group multi-segment" style="flex:2">
                    <button type="button" class="toggle-btn active" data-value="ADVANCED">상급</button>
                    <button type="button" class="toggle-btn" data-value="INTERMEDIATE">중급</button>
                    <button type="button" class="toggle-btn" data-value="BEGINNER">초급</button>
                    <input type="hidden" name="languageLevel" value="ADVANCED">
                </div>
                <button type="button" class="btn-delete-item"><i class="fa-solid fa-xmark"></i></button>
            </div>
        </div>`
};

function cloneField(containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;

    const firstItem = container.querySelector('.clonable-item');

    // 🌟 아이템이 없으면 템플릿으로 생성, 있으면 기존처럼 복제
    if (!firstItem) {
        if (TEMPLATES[containerId]) {
            container.insertAdjacentHTML('beforeend', TEMPLATES[containerId]);
        }
        return;
    }

    const clone = firstItem.cloneNode(true);
    clone.querySelectorAll('input[type="text"], textarea').forEach(input => input.value = '');
    clone.querySelectorAll('select').forEach(select => select.selectedIndex = 0);
    clone.querySelectorAll('.toggle-group').forEach(group => {
        group.querySelectorAll('.toggle-btn').forEach((btn, index) => {
            if (index === 0) btn.classList.add('active');
            else btn.classList.remove('active');
        });
        const hiddenInput = group.querySelector('input[type="hidden"]');
        const firstBtn = group.querySelector('.toggle-btn');
        if (hiddenInput && firstBtn) hiddenInput.value = firstBtn.getAttribute('data-value');
    });
    container.appendChild(clone);
}

    // ==========================================
    // 3. 추가 버튼 클릭 이벤트
    // ==========================================
    document.getElementById('btnAddCareer')?.addEventListener('click', () => cloneField('careerFields'));
    document.getElementById('btnAddCert')?.addEventListener('click', () => cloneField('certFields'));
    document.getElementById('btnAddLang')?.addEventListener('click', () => cloneField('langFields'));

    // ==========================================
    // 4. 동적 요소 제어 (삭제 버튼 & 토글 버튼)
    // ==========================================
    document.addEventListener('click', function(e) {

// A. X 버튼 클릭 시 삭제
const deleteBtn = e.target.closest('.btn-delete-item');
if (deleteBtn) {
    const itemToRemove = deleteBtn.closest('.clonable-item');
    const container = itemToRemove.parentElement;
    // 🌟 경력사항만 1개 보호, 자격증/어학은 전부 삭제 가능
    if (container.id === 'careerFields' && container.querySelectorAll('.clonable-item').length <= 1) return;
    itemToRemove.remove();
    return;
}

        // B. 토글 버튼 클릭 시 동작 (경력/신입, 어학 상/중/초, 공개/비공개 등 모든 토글)
        if (e.target.classList.contains('toggle-btn')) {
            const btn = e.target;
            const group = btn.closest('.toggle-group');

            // 1) 기존 active 지우고 현재 버튼에 추가
            group.querySelectorAll('.toggle-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            // 2) 숨겨진 input 값 업데이트 (이 값이 서버로 전송됨)
            const hiddenInput = group.querySelector('input[type="hidden"]');
            if (hiddenInput) {
                hiddenInput.value = btn.getAttribute('data-value');

                // 3) [특수 로직] 경력/신입 토글인 경우 입력창 숨김/보임 처리
                if (hiddenInput.name === 'careerType') {
                    const careerFields = document.getElementById('careerFields');
                    const btnAddCareerWrapper = document.getElementById('btnAddCareer').parentElement;

                    if (hiddenInput.value === 'NEWCOMER') {
                        careerFields.style.display = 'none';
                        btnAddCareerWrapper.style.display = 'none';
                        careerFields.querySelectorAll('input, select, textarea').forEach(el => el.disabled = true);
                    } else {
                        careerFields.style.display = 'block';
                        btnAddCareerWrapper.style.display = 'block';
                        careerFields.querySelectorAll('input, select, textarea').forEach(el => el.disabled = false);
                    }
                }
            }
        }
    });

    // ==========================================
    // 4-1. 근무 기간 유효성 검사 (시작일 > 종료일 방지)
    // ==========================================
    const careerContainer = document.getElementById('careerFields');
    if (careerContainer) {
        careerContainer.addEventListener('change', function(e) {
            const target = e.target;
            // 연도 또는 월 선택 상자가 바뀐 경우
            if (target.tagName === 'SELECT' && (target.name.includes('Year') || target.name.includes('Month'))) {
                const careerItem = target.closest('.career-item');
                if (careerItem) {
                    validateCareerDates(careerItem);
                }
            }
        });
    }

    function validateCareerDates(item) {
        const sYear = item.querySelector('select[name="startYear"]').value;
        const sMonth = item.querySelector('select[name="startMonth"]').value;
        const eYear = item.querySelector('select[name="endYear"]').value;
        const eMonth = item.querySelector('select[name="endMonth"]').value;

        // 모든 값이 선택되었을 때만 비교
        if (sYear && sMonth && eYear && eMonth) {
            // 연도*100 + 월 방식으로 숫자를 만들어 크기 비교
            const startDateNum = (parseInt(sYear) * 100) + parseInt(sMonth);
            const endDateNum = (parseInt(eYear) * 100) + parseInt(eMonth);

            if (startDateNum > endDateNum) {
                alert("종료일이 시작일보다 빠를 수 없습니다.\n근무 기간을 확인해주세요.");
                
                // 종료일을 시작일과 동일하게 맞춤
                item.querySelector('select[name="endYear"]').value = sYear;
                item.querySelector('select[name="endMonth"]').value = sMonth;
            }
        }
    }

    // ==========================================
    // 5. 증빙서류 다중 업로드 & 이미지 미리보기 로직
    // ==========================================
    const evidenceFile = document.getElementById('evidenceFile');
    const btnUpload = document.getElementById('btnUpload');
    const fileNameDisplay = document.getElementById('fileNameDisplay');
    const previewContainer = document.getElementById('previewContainer');

    if (evidenceFile && btnUpload && fileNameDisplay && previewContainer) {
        const defaultPlaceholder = fileNameDisplay.getAttribute('placeholder') || '선택된 파일 없음';

        // 파일 첨부 버튼 클릭 시 진짜 input(hidden) 클릭 유도
        btnUpload.addEventListener('click', () => {
            evidenceFile.click();
        });

        // 파일이 선택되었을 때
        evidenceFile.addEventListener('change', function() {
            // 기존 썸네일 초기화
            previewContainer.innerHTML = '';
            previewContainer.style.display = 'none';

            const files = this.files;

            if (files && files.length > 0) {
                // 1) 텍스트 창에 파일 개수 또는 이름 표시
                if (files.length === 1) {
                    fileNameDisplay.value = files[0].name;
                } else {
                    // HTML에 숨겨둔 다국어 템플릿 가져와서 {0} 갈아 끼우기
                    const msgTemplate = fileNameDisplay.getAttribute('data-multiple-msg');
                    fileNameDisplay.value = msgTemplate.replace('{0}', files.length);
                }

                // 🌟 파일이 들어오면 진하게 보이도록 클래스 추가 (하드코딩 컬러 삭제 완료)
                fileNameDisplay.classList.add('has-file');
                previewContainer.style.display = 'flex';

                // 미리보기 생성
                Array.from(files).forEach(file => {
                    if (file.type.startsWith('image/')) {
                        const reader = new FileReader();
                        reader.onload = (e) => {
                            const img = document.createElement('img');
                            img.src = e.target.result;
                            img.style.width = '80px';
                            img.style.height = '80px';
                            img.style.objectFit = 'cover';
                            img.style.borderRadius = '5px';
                            img.style.border = '1px solid #ddd';
                            previewContainer.appendChild(img);
                        };
                        reader.readAsDataURL(file);
                    } else {
                        // 이미지가 아닌 경우 (PDF 등)
                        const fileBox = document.createElement('div');
                        fileBox.textContent = `📄 ${file.name}`;
                        fileBox.style.padding = '10px';
                        fileBox.style.background = '#f8f9fa';
                        fileBox.style.border = '1px solid #ddd';
                        fileBox.style.borderRadius = '5px';
                        fileBox.style.fontSize = '12px';
                        fileBox.style.maxWidth = '150px';
                        fileBox.style.overflow = 'hidden';
                        fileBox.style.textOverflow = 'ellipsis';
                        fileBox.style.whiteSpace = 'nowrap';
                        previewContainer.appendChild(fileBox);
                    }
                });
            } else {
                // 취소했을 때 원상복구
                fileNameDisplay.value = '';
                fileNameDisplay.setAttribute('placeholder', defaultPlaceholder);

                // 🌟 파일이 없어졌으니 클래스 제거 (잘못된 add 로직 걷어냄)
                fileNameDisplay.classList.remove('has-file');
            }
        });
    }

});