/**
 * companyInfo.js
 * 구인자의 회사 정보 관리 페이지에서 구글 맵스 API를 활용한 주소 검색 및
 * 등록된 회사 정보의 비동기 삭제(Fetch) 기능을 담당합니다.
 */

/**
 * 사용자가 입력한 우편번호를 기반으로 Google Maps Geocoder API를 호출하여
 * 위도/경도 좌표 및 행정구역(도/도/부/현, 시/구, 동 등) 주소 정보를 추출해 각 입력 폼에 자동 반영합니다.
 */
function searchAddress() {
    const zip = document.getElementById('zipcode').value;
    if (!zip || zip.length < 7) {
        alert(msgZipcodeAlert);
        return;
    }

    const geocoder = new google.maps.Geocoder();
    geocoder.geocode({ address: zip, region: 'jp', language: 'ja' }, (results, status) => {
        if (status === "OK") {
            const addrResult = results[0];
            document.getElementById('latitude').value = addrResult.geometry.location.lat();
            document.getElementById('longitude').value = addrResult.geometry.location.lng();

            let pref = "", city = "", town = "", rest = "";
            addrResult.address_components.forEach(comp => {
                const t = comp.types;
                if (t.includes("administrative_area_level_1")) pref = comp.long_name;
                if (t.includes("locality")) city = comp.long_name;
                if (t.includes("sublocality_level_1")) town = comp.long_name;
                if (t.includes("sublocality_level_2") || t.includes("premise")) rest += comp.long_name + " ";
            });

            document.getElementById('address_main').value = `${pref} ${city} ${town} ${rest}`.trim();
            document.getElementById('addr_prefecture').value = pref;
            document.getElementById('addr_city').value = city;
            document.getElementById('addr_town').value = town;
            document.getElementById('address_detail').focus();
        } else {
            alert(msgNotFoundAlert);
        }
    });
}

/**
 * 등록된 회사 정보를 삭제하기 전 사용자에게 확인 창(SweetAlert2)을 띄우고,
 * 승인 시 Fetch API를 통해 서버에 데이터 삭제를 비동기 요청합니다.
 * 삭제 불가(상태 409) 시 경고를 띄우며, 성공 시 페이지를 새로고침(리다이렉트) 합니다.
 * * @param {number|string} id 삭제할 회사 정보의 고유 식별자(PK)
 */
function deleteCompany(id) {
    Swal.fire({
        title: msgDelTitle,
        text: msgDelText,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#ff4d4f',
        cancelButtonColor: '#6c757d',
        confirmButtonText: msgConfirm,
        cancelButtonText: msgCancel
    }).then((result) => {
        if (result.isConfirmed) {
            fetch(deleteCompanyBaseUrl + id, { method: 'DELETE' })
                .then(res => {
                    if (res.status === 409) {
                        return res.text().then(msg => {
                            Swal.fire({ icon: 'warning', title: msgCantDelete, text: msg });
                        });
                    } else if (res.ok) {
                        Swal.fire({ icon: 'success', title: msgSuccess, showConfirmButton: false, timer: 1200 })
                            .then(() => location.href = '/Recruiter/CompanyInfo');
                    } else {
                        Swal.fire({ icon: 'error', title: msgErrorTitle, text: msgErrorText });
                    }
                })
                .catch(err => {
                    console.error(err);
                    Swal.fire({ icon: 'error', title: msgCommErrorTitle, text: msgCommErrorText });
                });
        }
    });
}