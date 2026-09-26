package com.taskmanager.taskapi.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

	@Bean
	@ConditionalOnProperty(name = "app.security.enabled", havingValue = "true")
	SecurityFilterChain productionSecurityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/", "/index.html", "/styles.css", "/app.js", "/favicon.ico").permitAll()
						.requestMatchers("/h2-console/**").denyAll()
						.requestMatchers("/api/**").authenticated()
						.anyRequest().denyAll())
				.httpBasic(Customizer.withDefaults())
				.build();
	}

	@Bean
	@ConditionalOnProperty(name = "app.security.enabled", havingValue = "false", matchIfMissing = true)
	SecurityFilterChain localSecurityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
				.build();
	}

	@Bean
	@ConditionalOnProperty(name = "app.security.enabled", havingValue = "true")
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	@ConditionalOnProperty(name = "app.security.enabled", havingValue = "true")
	UserDetailsService userDetailsService(
			@Value("${app.security.username:}") String username,
			@Value("${app.security.password:}") String password,
			PasswordEncoder passwordEncoder) {
		if (username.isBlank() || password.length() < 16) {
			throw new IllegalStateException("Configure APP_SECURITY_USERNAME e uma APP_SECURITY_PASSWORD com ao menos 16 caracteres");
		}
		return new InMemoryUserDetailsManager(User.withUsername(username)
				.password(passwordEncoder.encode(password))
				.roles("USER")
				.build());
	}
}