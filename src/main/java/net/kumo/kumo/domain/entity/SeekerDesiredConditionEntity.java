package net.kumo.kumo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seeker_desired_conditions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeekerDesiredConditionEntity {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long conditionId;
	
	@OneToOne(fetch = FetchType.LAZY) // 1:1 관계
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private UserEntity user;
	
	@Column(length = 50)
	private String locationPrefecture; // tokyo, osaka 등
	
	@Column(length = 50)
	private String locationWard;       // chiyoda, minato 등
	
	@Column(length = 100)
	private String desiredJob;
	
	@Column(length = 20)
	private String salaryType;         // HOURLY, DAILY 등
	
	private String desiredSalary;
	
	@Column(length = 50)
	private String desiredPeriod;
	
}
