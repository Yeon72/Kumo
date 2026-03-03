package net.kumo.kumo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seeker_languages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeekerLanguageEntity {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long langId;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;
	
	@Column(length = 50, nullable = false)
	private String language;
	
	@Column(length = 50)
	private String level;
}
