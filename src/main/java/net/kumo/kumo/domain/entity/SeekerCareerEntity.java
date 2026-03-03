package net.kumo.kumo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.apache.catalina.User;

import java.time.LocalDate;

@Entity
@Table(name = "seeker_careers")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SeekerCareerEntity {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long careerId;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;
	
	@Column(length = 100, nullable = false)
	private String companyName;
	
	@Column(length = 100)
	private String department;
	
	private LocalDate startDate;
	private LocalDate endDate;
	
	@Column(columnDefinition = "TEXT")
	private String description;
}
