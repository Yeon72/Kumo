package net.kumo.kumo.security;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/*
회원 인증 정보 객체
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
	
	/**
	 * 비밀번호 반환
	 */
	@Override
	public String getPassword() {
		return this.password;
	}
	
	/**
	 * ★ 중요: 로그인 아이디(Username) 반환
	 * 우리는 Email을 아이디로 쓰므로 email을 리턴해야 함!
	 */
	@Override
	public String getUsername() {
		return this.email;
	}
	
	/**
	 * 계정 만료 여부 (true: 만료 안 됨)
	 * 별도 로직 없으면 true 리턴
	 */
	@Override
	public boolean isAccountNonExpired() {
		return true;
	}
	
	/**
	 * 계정 잠금 여부 (true: 잠기지 않음)
	 * 별도 로직 없으면 true 리턴
	 */
	@Override
	public boolean isAccountNonLocked() {
		return true;
	}
	
	/**
	 * 비밀번호 만료 여부 (true: 만료 안 됨)
	 * 별도 로직 없으면 true 리턴
	 */
	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}
	
	/**
	 * 계정 활성화 여부 (true: 활성화 됨)
	 * DB에서 가져온 enabled 값을 그대로 리턴
	 */
	@Override
	public boolean isEnabled() {
		return this.enabled;
	}
	
	
	
}
	
	
	
	
	

