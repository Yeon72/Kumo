package net.kumo.kumo.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kumo.kumo.domain.entity.UserEntity;
import net.kumo.kumo.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security 인증 과정에서 사용자가 입력한 식별자(Email)를 기반으로
 * DB에서 계정 정보를 조회하여 UserDetails 객체로 래핑(Wrapping)하는 서비스 클래스입니다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthenticatedUserDetailsService implements UserDetailsService {

    private final UserRepository ur;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        log.debug("로그인 시도 이메일 : {}", email);

        UserEntity userEntity = ur.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email + " : 존재하지 않는 이메일입니다."));

        log.debug("조회된 회원 정보 : {}", userEntity);

        AuthenticatedUser user = AuthenticatedUser.builder()
                .email(userEntity.getEmail())
                .password(userEntity.getPassword())
                .nameKanjiSei(userEntity.getNameKanjiSei())
                .nameKanjiMei(userEntity.getNameKanjiMei())
                .nickname(userEntity.getNickname())
                .role(userEntity.getRole().name())
                .enabled(userEntity.isActive())
                .build();

        log.debug("생성된 인증 객체 : {}", user);

        return user;
    }
}