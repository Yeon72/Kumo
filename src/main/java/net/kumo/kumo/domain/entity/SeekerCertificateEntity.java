package net.kumo.kumo.domain.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "seeker_certificates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeekerCertificateEntity {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long certId;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;
	
	@Column(length = 100, nullable = false)
	private String certName;
	
	@Column(length = 4)
	private String acquisitionYear; // VARCHAR(4)로 수정됨
	
	@Column(length = 100)
	private String issuer;
}
