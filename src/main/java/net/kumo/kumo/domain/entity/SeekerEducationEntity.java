package net.kumo.kumo.domain.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seeker_educations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeekerEducationEntity {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long eduId;
	
	@ManyToOne(fetch = FetchType.LAZY) // 한 유저가 여러 학력(고졸, 대졸 등) 가질 수 있음
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;
	
	@Column(length = 50, nullable = false)
	private String educationLevel;
	
	@Column(length = 100, nullable = false)
	private String schoolName;
	
	@Column(length = 100)
	private String major;
	
	@Column(length = 20)
	private String status; // GRADUATED, EXPECTED 등
}
