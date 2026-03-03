package net.kumo.kumo.domain.dto;


import lombok.*;
import net.kumo.kumo.domain.entity.Enum;
import net.kumo.kumo.domain.entity.ProfileImageEntity;
import net.kumo.kumo.domain.entity.UserEntity;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter // <-- [이게 필수입니다!] 타임리프가 값을 꺼내갈 수 있게 해줌
@Setter
@Builder
public class SeekerMyPageDTO {
	private Long id;                // PK
	private String email;           // 이메일
	private String name;            // 실명
	private String nickname;        // 닉네임
	private String fileUrl;    // 프로필 이미지 URL
	private String address;         // 주소
	private LocalDate birthDate;    // 생년월일
	private  String contact;
	
	
	// 소셜 연동 여부 (화면의 토글 스위치용)
	private Enum.SocialProvider SocialProvider;
	
	
	
	// 엔티티 -> DTO 변환 메서드 (편의상 추가)
	public static SeekerMyPageDTO EntityToDto (UserEntity userEntity) {
		return SeekerMyPageDTO.builder()
				.id(userEntity.getUserId())
				.email(userEntity.getEmail())
				.contact(userEntity.getContact())
				.name(userEntity.getNameKanjiSei()+ " " +userEntity.getNameKanjiSei())
				.nickname(userEntity.getNickname())
				.fileUrl(userEntity.getProfileImage() != null ? userEntity.getProfileImage().getFileUrl() : null) // 없으면 null
				.address(userEntity.getAddressMain() + " " + userEntity.getAddressDetail())
				.birthDate(userEntity.getBirthDate())
				// 예시: provider 필드나 별도 테이블을 통해 연동 여부 확인 로직 필요
				.SocialProvider(userEntity.getSocialProvider())
				.build();
	
		}
	}
