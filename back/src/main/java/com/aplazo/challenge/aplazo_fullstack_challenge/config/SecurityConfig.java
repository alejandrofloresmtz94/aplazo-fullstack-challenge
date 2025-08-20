package com.aplazo.challenge.aplazo_fullstack_challenge.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import com.aplazo.challenge.aplazo_fullstack_challenge.dto.ErrorResponse;
import com.aplazo.challenge.aplazo_fullstack_challenge.exception.custom_exception.JwtDecoderInitializationException;
import com.aplazo.challenge.aplazo_fullstack_challenge.infra.logging.service.ErrorLogService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String UNAUTHORIZED_CODE = "APZ000007";
    private static final String UNAUTHORIZED_ERROR = "UNAUTHORIZED";
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    private final ErrorLogService errorLogService;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/customers").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/customers").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(nimbusJwtDecoder())))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            logger.warn("SecurityCOnfig - Unauthorized access: {}", authException.getMessage());
                            errorLogService.logError(UNAUTHORIZED_CODE, UNAUTHORIZED_ERROR, authException.getMessage(),
                                    request.getRequestURI());

                            response.setStatus(401);
                            response.setContentType("application/json");

                            ErrorResponse error = ErrorResponse.builder()
                                    .code(UNAUTHORIZED_CODE)
                                    .error(UNAUTHORIZED_ERROR)
                                    .timestamp(System.currentTimeMillis() / 1000)
                                    .message(authException.getMessage())
                                    .path(request.getRequestURI())
                                    .build();

                            OBJECT_MAPPER.writeValue(response.getWriter(), error);
                        }));

        return http.build();
    }

    @Bean
    public JwtDecoder nimbusJwtDecoder() {
        try {
            InputStream is = getClass().getResourceAsStream("/public_key.pem");
            String publicKeyPEM = new String(is.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] decoded = Base64.getDecoder().decode(publicKeyPEM);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
            RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(keySpec);

            return NimbusJwtDecoder.withPublicKey(publicKey).build();
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new JwtDecoderInitializationException("Error initializing JwtDecoder", e);
        }
    }

}
