package net.kumo.kumo.domain.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 구인자(Recruiter) 사용자가 등록한 개별 사업장(회사)의
 * 위치, 소개, 연락처 등을 관리하는 엔티티 클래스입니다.
 */
@Entity
@Table(name = "companies")
@Data
public class CompanyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long companyId;

    /** 이 사업장을 등록/소유한 관리자(Recruiter) 계정 매핑 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    private String bizName;
    private String ceoName;

    private String zipCode;
    private String addressMain;
    private String addressDetail;
    private String addrPrefecture;
    private String addrCity;
    private String addrTown;

    /** 정밀한 지도 마커 배치를 위한 위도 (소수점 8자리) */
    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    /** 정밀한 지도 마커 배치를 위한 경도 (소수점 8자리) */
    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(columnDefinition = "TEXT")
    private String introduction;

}