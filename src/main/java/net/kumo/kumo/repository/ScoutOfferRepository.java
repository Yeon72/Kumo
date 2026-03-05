package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.ScoutOfferEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScoutOfferRepository extends JpaRepository<ScoutOfferEntity, Long> {
    List<ScoutOfferEntity> findBySeekerOrderByCreatedAtDesc(UserEntity seeker);
    boolean existsByRecruiterAndSeekerAndStatus(UserEntity recruiter, UserEntity seeker, ScoutOfferEntity.ScoutStatus status);
	
	void deleteByRecruiter(UserEntity user);
	
	void deleteBySeeker(UserEntity user);
}
