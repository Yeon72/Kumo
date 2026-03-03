document.addEventListener('DOMContentLoaded', () => {

    // ==========================================
    // 1. 도쿄/오사카 지역 변경 로직
    // ==========================================
    const location1 = document.getElementById('location1');
    const location2 = document.getElementById('location2');

    if (location1 && location2) {
        const allOptions = Array.from(location2.querySelectorAll('option'));

        location1.addEventListener('change', function() {
            const selectedPrefecture = this.value;
            const targetClass = 'ward-' + selectedPrefecture;
            location2.innerHTML = '';
            allOptions.forEach(option => {
                if (option.classList.contains(targetClass)) {
                    location2.appendChild(option);
                }
            });
        });
        location1.dispatchEvent(new Event('change'));
    }

    // ==========================================
    // 2. 공통 폼 복사 함수 (Clone 마법)
    // ==========================================
    function cloneField(containerId) {
        const container = document.getElementById(containerId);
        if (!container) return;

        const firstItem = container.querySelector('.clonable-item');
        if (!firstItem) return;

        const clone = firstItem.cloneNode(true);

        // 텍스트 입력창 초기화
        clone.querySelectorAll('input[type="text"], textarea').forEach(input => {
            input.value = '';
        });

        // 셀렉트박스 초기화
        clone.querySelectorAll('select').forEach(select => {
            select.selectedIndex = 0;
        });

        // 토글 버튼 그룹 초기화
        clone.querySelectorAll('.toggle-group').forEach(group => {
            group.querySelectorAll('.toggle-btn').forEach((btn, index) => {
                if (index === 0) btn.classList.add('active');
                else btn.classList.remove('active');
            });
            const hiddenInput = group.querySelector('input[type="hidden"]');
            const firstBtn = group.querySelector('.toggle-btn');
            if (hiddenInput && firstBtn) {
                hiddenInput.value = firstBtn.getAttribute('data-value');
            }
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

        // A. X 버튼 클릭 시 삭제 (경력, 자격증, 어학 등)
        const deleteBtn = e.target.closest('.btn-delete-item');
        if (deleteBtn) {
            const itemToRemove = deleteBtn.closest('.clonable-item');
            const container = itemToRemove.parentElement;
            // 원본 1개는 무조건 남기기
            if (container.querySelectorAll('.clonable-item').length > 1) {
                itemToRemove.remove();
            }
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