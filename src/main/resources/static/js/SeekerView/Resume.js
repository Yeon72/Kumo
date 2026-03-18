/**
 * Resume.js
 * 이력서 작성 및 수정 페이지의 프론트엔드 비즈니스 로직을 담당합니다.
 * 주요 기능:
 * 1. 동적 지역(도/현 및 구/시) 선택 연동
 * 2. 경력/신입 상태에 따른 UI 제어
 * 3. 학력, 경력, 자격증, 어학 능력 등 폼 필드 동적 복제 및 삭제
 * 4. 증빙 서류 다중 업로드 및 미리보기 생성
 * 5. 근무 기간(시작일/종료일) 유효성 검사
 */
document.addEventListener('DOMContentLoaded', () => {

    /**
     * 사용자가 선택한 상위 지역(도/현)에 따라
     * 하위 지역(구/시) 셀렉트 박스의 옵션을 동적으로 필터링하여 렌더링합니다.
     */
    const location1 = document.getElementById('location1');
    const location2 = document.getElementById('location2');

    if (location1 && location2) {
        const allOptions = Array.from(location2.querySelectorAll('option'));

        const initialValue2 = location2.value;

        location1.addEventListener('change', function() {
            const selectedPrefecture = this.value;
            const targetClass = 'ward-' + selectedPrefecture;

            location2.innerHTML = '';
            allOptions.forEach(option => {
                if (option.classList.contains(targetClass)) {
                    const clone = option.cloneNode(true);
                    if (clone.value === initialValue2) {
                        clone.selected = true;
                    }
                    location2.appendChild(clone);
                }
            });
        });

        location1.dispatchEvent(new Event('change'));
    }

    /**
     * 초기 로딩 시 경력/신입 토글 상태를 확인하여
     * 신입인 경우 경력 사항 입력 필드를 비활성화하고 숨깁니다.
     */
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

    /**
     * 항목이 모두 삭제되었을 때 복제를 위한 대체 HTML 템플릿 구조입니다.
     * @constant {Object}
     */
    const i = typeof resumeI18n !== 'undefined' ? resumeI18n : {};
    const TEMPLATES = {
        certFields: `
            <div class="form-group row cert-item clonable-item">
                <label>${i.certLabel || '자격증'}</label>
                <div class="input-group cert-group">
                    <input type="text" name="certName" class="custom-input" placeholder="${i.certName || ''}" style="flex:1">
                    <input type="text" name="certPublisher" class="custom-input" placeholder="${i.certPublisher || ''}" style="flex:1">
                    <select name="certYear" class="custom-select" style="flex:1">
                        <option value="" selected disabled>${i.certYear || '취득연도'}</option>
                        ${Array.from({length: 57}, (_, i) => 2026 - i).map(y => `<option value="${y}">${y}</option>`).join('')}
                    </select>
                    <button type="button" class="btn-delete-item"><i class="fa-solid fa-xmark"></i></button>
                </div>
            </div>`,
        langFields: `
            <div class="form-group row mt-5 lang-item clonable-item">
                <label>${i.langLabel || '어학 능력'}</label>
                <div class="input-group cert-group">
                    <input type="text" name="languageName" class="custom-input" placeholder="${i.langName || ''}" style="flex:1">
                    <div class="toggle-group multi-segment" style="flex:2">
                        <button type="button" class="toggle-btn active" data-value="ADVANCED">${i.advanced || '상급'}</button>
                        <button type="button" class="toggle-btn" data-value="INTERMEDIATE">${i.intermediate || '중급'}</button>
                        <button type="button" class="toggle-btn" data-value="BEGINNER">${i.beginner || '초급'}</button>
                        <input type="hidden" name="languageLevel" value="ADVANCED">
                    </div>
                    <button type="button" class="btn-delete-item"><i class="fa-solid fa-xmark"></i></button>
                </div>
            </div>`
    };

    /**
     * 특정 컨테이너 내의 폼 필드를 동적으로 복제하여 추가합니다.
     * 컨테이너가 비어있을 경우 TEMPLATES 객체에서 마크업을 가져와 생성합니다.
     *
     * @param {string} containerId 복제할 아이템이 속한 부모 컨테이너의 식별자
     */
    function cloneField(containerId) {
        const container = document.getElementById(containerId);
        if (!container) return;

        const firstItem = container.querySelector('.clonable-item');

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

    document.getElementById('btnAddCareer')?.addEventListener('click', () => cloneField('careerFields'));
    document.getElementById('btnAddCert')?.addEventListener('click', () => cloneField('certFields'));
    document.getElementById('btnAddLang')?.addEventListener('click', () => cloneField('langFields'));

    /**
     * 동적으로 생성된 요소들의 삭제(X) 버튼 및 상태 토글 버튼에 대한 클릭 이벤트를 위임하여 처리합니다.
     * 토글 상태 변경 시 연동된 hidden input의 값을 동기화합니다.
     */
    document.addEventListener('click', function(e) {

        const deleteBtn = e.target.closest('.btn-delete-item');
        if (deleteBtn) {
            const itemToRemove = deleteBtn.closest('.clonable-item');
            const container = itemToRemove.parentElement;
            if (container.id === 'careerFields' && container.querySelectorAll('.clonable-item').length <= 1) return;
            itemToRemove.remove();
            return;
        }

        if (e.target.classList.contains('toggle-btn')) {
            const btn = e.target;
            const group = btn.closest('.toggle-group');

            group.querySelectorAll('.toggle-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            const hiddenInput = group.querySelector('input[type="hidden"]');
            if (hiddenInput) {
                hiddenInput.value = btn.getAttribute('data-value');

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

    /**
     * 경력 입력란의 시작 연월과 종료 연월 변경 시 유효성 검사를 수행하는 이벤트 리스너입니다.
     */
    const careerContainer = document.getElementById('careerFields');
    if (careerContainer) {
        careerContainer.addEventListener('change', function(e) {
            const target = e.target;
            if (target.tagName === 'SELECT' && (target.name.includes('Year') || target.name.includes('Month'))) {
                const careerItem = target.closest('.career-item');
                if (careerItem) {
                    validateCareerDates(careerItem);
                }
            }
        });
    }

    /**
     * 선택된 시작일과 종료일을 비교하여 종료일이 시작일보다 과거인지 검사합니다.
     * 유효하지 않을 경우 경고창을 띄우고 종료일을 시작일과 동일하게 자동 수정합니다.
     *
     * @param {HTMLElement} item 검사할 연월 Select 요소가 포함된 부모 DOM 요소
     */
    function validateCareerDates(item) {
        const sYear = item.querySelector('select[name="startYear"]').value;
        const sMonth = item.querySelector('select[name="startMonth"]').value;
        const eYear = item.querySelector('select[name="endYear"]').value;
        const eMonth = item.querySelector('select[name="endMonth"]').value;

        if (sYear && sMonth && eYear && eMonth) {
            const startDateNum = (parseInt(sYear) * 100) + parseInt(sMonth);
            const endDateNum = (parseInt(eYear) * 100) + parseInt(eMonth);

            if (startDateNum > endDateNum) {
                alert("종료일이 시작일보다 빠를 수 없습니다.\n근무 기간을 확인해주세요.");

                item.querySelector('select[name="endYear"]').value = sYear;
                item.querySelector('select[name="endMonth"]').value = sMonth;
            }
        }
    }

    /**
     * 증빙 서류 업로드 폼의 파일 선택 이벤트와 다중 파일 미리보기 UI를 제어합니다.
     * 첨부된 파일의 종류(이미지 여부)에 따라 렌더링 방식을 분기합니다.
     */
    const evidenceFile = document.getElementById('evidenceFile');
    const btnUpload = document.getElementById('btnUpload');
    const fileNameDisplay = document.getElementById('fileNameDisplay');
    const previewContainer = document.getElementById('previewContainer');

    if (evidenceFile && btnUpload && fileNameDisplay && previewContainer) {
        const defaultPlaceholder = fileNameDisplay.getAttribute('placeholder') || '선택된 파일 없음';

        btnUpload.addEventListener('click', () => {
            evidenceFile.click();
        });

        evidenceFile.addEventListener('change', function() {
            previewContainer.innerHTML = '';
            previewContainer.style.display = 'none';

            const files = this.files;

            if (files && files.length > 0) {
                if (files.length === 1) {
                    fileNameDisplay.value = files[0].name;
                } else {
                    const msgTemplate = fileNameDisplay.getAttribute('data-multiple-msg');
                    fileNameDisplay.value = msgTemplate.replace('{0}', files.length);
                }

                fileNameDisplay.classList.add('has-file');
                previewContainer.style.display = 'flex';

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
                fileNameDisplay.value = '';
                fileNameDisplay.setAttribute('placeholder', defaultPlaceholder);

                fileNameDisplay.classList.remove('has-file');
            }
        });
    }

});