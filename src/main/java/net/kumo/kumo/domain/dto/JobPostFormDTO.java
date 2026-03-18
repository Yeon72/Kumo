package net.kumo.kumo.domain.dto;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 구인자가 새로운 구인 공고를 작성할 때 폼(Form)에서 입력한
 * 기본 정보 및 첨부 이미지 데이터를 수신하는 DTO 클래스입니다.
 */
@Getter
@Setter
@ToString
public class JobPostFormDTO {

    private String title;
    private String position;

    /** 직무 상세 설명 (폼 바인딩 전용) */
    private String positionDetail;

    /** 모집 마감일 ("YYYY-MM-DD" 포맷 자동 변환) */
    private LocalDate deadline;

    /** 급여 지급 유형 (예: HOURLY, DAILY, MONTHLY, SALARY) */
    private String salaryType;

    private Integer salaryAmount;
    private String description;

    /** 공고가 소속된 등록 회사 식별자 */
    private Long companyId;

    /** 클라이언트로부터 업로드된 다중 이미지 파일 목록 */
    private List<MultipartFile> images;

}