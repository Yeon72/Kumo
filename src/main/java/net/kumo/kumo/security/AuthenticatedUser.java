package net.kumo.kumo.security;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security 컨텍스트(Security Context) 내부에서 관리되는 인증된 사용자 객체입니다.
 * DB의 UserEntity 정보를 기반으로 생성되며, 세션 유지 및 권한 검증에 사용됩니다.
 */
@Slf4j
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class AuthenticatedUser implements UserDetails {

    private String email;
    private String password;
    private String nameKanjiSei;
    private String nameKanjiMei;
    private String nickname;
    private String role;
    private boolean enabled;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + this.role));
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }
}