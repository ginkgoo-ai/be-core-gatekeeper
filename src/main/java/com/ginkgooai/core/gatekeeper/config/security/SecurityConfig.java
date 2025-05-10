package com.ginkgooai.core.gatekeeper.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/api/gatekeeper/v3/api-docs/**", "/api/gatekeeper/swagger-ui/**", "/webjars/**",
						"/health", "/admin/**", "/css/**", "/js/**", "/images/**", "/api/gatekeeper/swagger-ui.html",
						"/api/gatekeeper/v3/api-docs/**")
				.permitAll()
				// .anyRequest().authenticated()
				.anyRequest()
				.permitAll())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(new ProblemDetailsAuthenticationEntryPoint())
                        .accessDeniedHandler(new ProblemDetailsAuthenticationEntryPoint()))
                .oauth2ResourceServer(oauth2 -> oauth2
				.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();

        jwtConverter.setPrincipalClaimName("email");
        jwtConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            List<String> roles = getClaimAsList(jwt, "role");
            if (roles != null) {
                for (String role : roles) {
                    authorities.add(new SimpleGrantedAuthority(role.toUpperCase()));
                }
            }

            List<String> scopes = getClaimAsList(jwt, "scope");
            if (scopes != null) {
                for (String scope : scopes) {
                    authorities.add(new SimpleGrantedAuthority(scope));
                }
            }

            return authorities;
        });

        return jwtConverter;
    }

    private List<String> getClaimAsList(Jwt jwt, String claimName) {
        Object claimValue = jwt.getClaim(claimName);

        if (claimValue == null) {
            return null;
        }

        if (claimValue instanceof List) {
            return (List<String>) claimValue;
        }

        if (claimValue instanceof String) {
            return List.of(((String) claimValue).split(" "));
        }

        return null;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return JwtDecoders.fromIssuerLocation(issuerUri);
    }

}
