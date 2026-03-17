/**
 * SeekerProfileEdit.js
 * 구직자 프로필 수정 페이지의 프론트엔드 비즈니스 로직을 담당합니다.
 * 닉네임 중복 확인, 외부 API(zipcloud)를 이용한 우편번호 검색,
 * Google Maps API를 활용한 주소 좌표 변환(Geocoding) 및 폼 제출 검증 기능을 수행합니다.
 * (글로벌 변수 msgs 객체 및 Google Maps API 로드에 의존합니다.)
 */
document.addEventListener('DOMContentLoaded', () => {
    initNicknameEvents();
    initAddressEvents();
    initFormSubmit();
});

/**
 * 닉네임 입력 필드의 값 변경 이벤트와 중복 확인 버튼의 클릭 이벤트를 바인딩합니다.
 * 비동기 통신(Fetch)을 통해 서버에 닉네임 중복 여부를 질의하고 검증 상태를 갱신합니다.
 */
function initNicknameEvents() {
    const nicknameInput = document.getElementById('nickname');
    const btnCheck = document.getElementById('btnCheckNickname');
    const errorEl = document.getElementById('error_nickname');
    const checkStatus = document.getElementById('nicknameChecked');

    if (nicknameInput) {
        nicknameInput.addEventListener('input', () => {
            checkStatus.value = "false";
            errorEl.style.display = 'none';
            errorEl.innerText = '';
        });
    }

    if (btnCheck) {
        btnCheck.addEventListener('click', () => {
            const nickname = nicknameInput.value.trim();

            if (!nickname) {
                showError(errorEl, msgs.nickname_empty, true);
                return;
            }

            fetch('/api/check/nickname', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ nickname })
            })
                .then(res => {
                    if (!res.ok) throw new Error('Network response was not ok');
                    return res.json();
                })
                .then(isDuplicate => {
                    if (isDuplicate) {
                        showError(errorEl, msgs.nickname_duplicate, true);
                        checkStatus.value = "false";
                        nicknameInput.focus();
                    } else {
                        showError(errorEl, msgs.nickname_ok, false);
                        checkStatus.value = "true";
                    }
                })
                .catch(err => {
                    console.error('Error:', err);
                    alert(msgs.network_error);
                });
        });
    }
}

/**
 * 주소 검색 버튼 클릭 이벤트와 상세 주소 입력 필드의 블러(blur) 이벤트를 바인딩합니다.
 * 상세 주소 입력이 완료되면 전체 주소를 바탕으로 정밀한 좌표 갱신을 트리거합니다.
 */
function initAddressEvents() {
    const btnSearch = document.getElementById('btnSearchAddress');
    const detailInput = document.getElementById('address_detail');

    if (btnSearch) {
        btnSearch.addEventListener('click', searchAddressFromZipcloud);
    }

    if (detailInput) {
        detailInput.addEventListener('blur', function() {
            const mainAddr = document.getElementById('address_main').value;
            const detailAddr = this.value;
            if (mainAddr) {
                getGeocode(mainAddr + " " + detailAddr);
            }
        });
    }
}

/**
 * 입력된 우편번호를 바탕으로 zipcloud API를 호출하여 행정구역 주소 정보를 가져옵니다.
 * 응답받은 주소 데이터를 각 입력 필드에 분배하고 Google Maps 좌표 변환을 요청합니다.
 */
function searchAddressFromZipcloud() {
    const zipcode = document.getElementById('zipcode').value.trim();

    if (!zipcode || zipcode.length < 7) {
        alert(msgs.zipcode_empty);
        document.getElementById('zipcode').focus();
        return;
    }

    fetch(`https://zipcloud.ibsnet.co.jp/api/search?zipcode=${zipcode}`)
        .then(res => res.json())
        .then(data => {
            if (data.status === 200 && data.results) {
                const result = data.results[0];
                const fullAddress = result.address1 + result.address2 + result.address3;

                document.getElementById('address_main').value = fullAddress;

                document.getElementById('addr_prefecture').value = result.address1;
                document.getElementById('addr_city').value = result.address2;
                document.getElementById('addr_town').value = result.address3;

                getGeocode(fullAddress);

                document.getElementById('address_detail').focus();
            } else {
                alert(msgs.search_fail);
            }
        })
        .catch(err => {
            console.error(err);
            alert(msgs.search_fail);
        });
}

/**
 * 완전한 주소 문자열을 Google Maps Geocoder API에 전달하여 위도(Latitude)와 경도(Longitude)를 추출합니다.
 *
 * @param {string} address 좌표로 변환할 대상 전체 주소 문자열
 */
function getGeocode(address) {
    if (!window.google || !window.google.maps) {
        console.warn("Google Maps API is not loaded.");
        return;
    }

    const geocoder = new google.maps.Geocoder();
    geocoder.geocode({ 'address': address }, function(results, status) {
        if (status === 'OK' && results[0]) {
            const loc = results[0].geometry.location;

            document.getElementById('latitude').value = loc.lat();
            document.getElementById('longitude').value = loc.lng();

            console.log(`좌표 갱신 완료: ${loc.lat()}, ${loc.lng()}`);
        } else {
            console.error('Geocode failed: ' + status);
        }
    });
}

/**
 * 프로필 수정 폼의 제출(submit) 이벤트를 가로채어 최종 유효성 검사를 수행합니다.
 * 닉네임 중복 확인이 정상적으로 완료되지 않았을 경우 폼 제출을 차단합니다.
 */
function initFormSubmit() {
    const form = document.getElementById('editForm');
    if (form) {
        form.addEventListener('submit', function(e) {
            const isChecked = document.getElementById('nicknameChecked').value;

            if (isChecked !== "true") {
                e.preventDefault();
                const errorEl = document.getElementById('error_nickname');
                showError(errorEl, msgs.check_dup, true);
                document.getElementById('nickname').focus();
            }
        });
    }
}

/**
 * 대상 DOM 요소에 검증 결과 메시지를 지정된 스타일(성공/실패 색상)로 화면에 노출합니다.
 *
 * @param {HTMLElement} element 메시지를 렌더링할 대상 DOM 요소
 * @param {string} message 화면에 출력할 다국어 텍스트 메시지
 * @param {boolean} isError 에러 여부 (true: 에러 색상 적용, false: 성공 색상 적용)
 */
function showError(element, message, isError) {
    element.innerText = message;
    element.style.color = isError ? '#EA4335' : '#4285F4';
    element.style.display = 'block';
}