package net.kumo.kumo.domain.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seeker_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeekerProfileEntity {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long seekerProfileId;
	
	@OneToOne(fetch = FetchType.LAZY) // 1:1 관계
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private UserEntity user;
	
	@Column(length = 20)
	private String careerType; // EXPERIENCED / NEWCOMER
	
	@Column(columnDefinition = "TEXT")
	private String selfPr;
	
	@Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
	private Boolean contactPublic;
	
	@Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
	private Boolean isPublic;
	
	@Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
	private Boolean scoutAgree;
}
