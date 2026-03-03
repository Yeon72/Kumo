package net.kumo.kumo.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.kumo.kumo.domain.enums.JobStatus;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(name = "tokyo_geocoded")
@Getter
@Setter
public class TokyoGeocodedEntity extends BaseEntity {

	@Column(name = "row_no")
	private Integer rowNo;

	@Column(name = "datanum", unique = true)
	private Long datanum;

	@Column(name = "title", length = 200)
	private String title;

	@Column(name = "href", length = 500)
	private String href;

	@Column(name = "write_time")
	private String writeTime;

	@Column(name = "img_urls", length = 1000)
	private String imgUrls;

	@Column(name = "body", columnDefinition = "TEXT")
	private String body;

	@Column(name = "company_name", length = 255)
	private String companyName;

	@Column(name = "address", length = 500)
	private String address;

	@Column(name = "contact_phone", length = 255)
	private String contactPhone;

	@Column(name = "position", length = 100)
	private String position;

	@Column(name = "job_description", columnDefinition = "TEXT")
	private String jobDescription;

	@Column(name = "wage")
	private String wage;

	@Column(name = "wage_jp")
	private String wageJp;
	
	@Column(name = "notes", columnDefinition = "TEXT")
	private String notes;

	@Column(name = "title_jp", length = 200)
	private String titleJp;

	@Column(name = "company_name_jp", length = 255)
	private String companyNameJp;

	@Column(name = "position_jp", length = 100)
	private String positionJp;

	@Column(name = "job_description_jp", columnDefinition = "TEXT")
	private String jobDescriptionJp;

	@Column(nullable = false)
	private Double lat;

	@Column(nullable = false)
	private Double lng;

	@Column(name = "prefecture_jp")
	private String prefectureJp;

	@Column(name = "city_jp")
	private String cityJp;

	@Column(name = "ward_jp")
	private String wardJp;

	@Column(name = "prefecture_kr")
	private String prefectureKr;

	@Column(name = "city_kr")
	private String cityKr;

	@Column(name = "ward_kr")
	private String wardKr;

	@Column(name = "ward_city_jp")
	private String wardCityJp;

	@Column(name = "ward_city_kr")
	private String wardCityKr;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", insertable = false, updatable = false)
	private UserEntity user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "company_id")
	private CompanyEntity company;

	@Enumerated(EnumType.STRING)
	@Column(columnDefinition = "ENUM('RECRUITING', 'CLOSED') DEFAULT 'RECRUITING'")
	private JobStatus status;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	public void prePersist() {
		if (this.status == null)
			this.status = JobStatus.RECRUITING;
	}

	@Column(name = "salary_type")
	private String salaryType;

	@Column(name = "salary_amount")
	private Integer salaryAmount;
}