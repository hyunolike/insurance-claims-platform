package com.insurance.claims.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 보안 설정 (Phase 0 골격).
 *
 * <p>v1은 Spring Security 의존성조차 없어 <b>모든 엔드포인트가 무인증</b>이었다.
 * 청구번호만 알면 남의 건강정보를 조회할 수 있는 상태였다.
 * 재설계에서는 Phase 0부터 기본 차단으로 시작한다 — 나중에 "보안을 붙이는" 것은
 * 늘 미뤄지기 때문이다.
 *
 * <p>현재는 인증 수단이 없으므로 실제 API가 없다. Phase 2에서 JWT 리소스 서버와
 * 서비스 토큰 인증을 붙이고, 고객이 <b>자기 계약의 청구만</b> 조회하도록
 * 메서드 수준 인가를 추가한다.
 *
 * @see docs/design/07-architecture.md §9
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // REST API는 상태를 세션에 두지 않는다.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 세션이 없으므로 CSRF 토큰도 의미가 없다.
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 컨테이너 오케스트레이터가 인증 없이 볼 수 있어야 한다.
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        // 지표는 내부망에서만. 운영 배포 시 네트워크 정책으로 한 번 더 막는다.
                        .requestMatchers("/actuator/prometheus").hasAuthority("SCOPE_metrics.read")
                        // 나머지는 전부 차단이 기본값이다.
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
