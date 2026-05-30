package uk.gov.hmcts.reform.opal.authentication.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;
import uk.gov.hmcts.opal.common.config.OpalCommonConfiguration;
import uk.gov.hmcts.opal.common.user.authentication.exception.CustomAuthenticationExceptions;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;
import uk.gov.hmcts.reform.opal.authentication.config.internal.InternalAuthConfigurationProperties;
import uk.gov.hmcts.reform.opal.authentication.config.internal.InternalAuthProviderConfigurationProperties;
import uk.gov.hmcts.reform.opal.mappers.UserStateMapper;
import uk.gov.hmcts.reform.opal.repository.UserRepository;

import java.time.Clock;
import java.util.Map;

@Slf4j(topic = "opal.SecurityConfig")
@Configuration
@EnableWebSecurity
//@EnableWebSecurity(debug = true)
@RequiredArgsConstructor
@Profile("!integration")
public class SecurityConfig {

    private final CustomAuthenticationExceptions userCustomAuthenticationExceptions;

    private static final String[] AUTH_WHITELIST = {
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/swagger-resources/**",
        "/v3/**",
        "/favicon.ico",
        "/health/**",
        "/mappings",
        "/info",
        "/metrics",
        "/metrics/**",
        "/internal-user/login-or-refresh",
        "/internal-user/logout",
        "/internal-user/handle-oauth-code",
        "/testing-support/**",
        "/s2s/**",
        "/"
    };

    @Bean
    @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "squid:S4502"})
    public SecurityFilterChain filterChain(
        HttpSecurity http,
        JwtIssuerAuthenticationManagerResolver jwtIssuerAuthenticationManagerResolver
    ) {
        log.info(":filterChain: http security: {}", http);
        applyCommonConfig(http)
            .authorizeHttpRequests(authorize ->
                authorize.requestMatchers(PathRequest.toStaticResources().atCommonLocations())
                    .permitAll()
                    .requestMatchers(AUTH_WHITELIST)
                    .permitAll()
                    .anyRequest().authenticated()
            )
            .exceptionHandling(exceptionHandling ->
                exceptionHandling
                    .authenticationEntryPoint(userCustomAuthenticationExceptions)
                    .accessDeniedHandler(userCustomAuthenticationExceptions)
            )
            .oauth2ResourceServer(oauth2 ->
                oauth2.authenticationManagerResolver(jwtIssuerAuthenticationManagerResolver)
            );

        return http.build();
    }

    private HttpSecurity applyCommonConfig(HttpSecurity http) {
        return http
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(FormLoginConfigurer::disable)
            .logout(LogoutConfigurer::disable);
    }

    @Bean
    JwtIssuerAuthenticationManagerResolver jwtIssuerAuthenticationManagerResolver(
        InternalAuthConfigurationProperties internalAuthConfigurationProperties,
        UserOpalJwtAuthenticationProvider userOpalJwtAuthenticationProvider
    ) {
        AuthenticationManager manager = userOpalJwtAuthenticationProvider::authenticate;
        Map<String, AuthenticationManager> managers =
            Map.of(internalAuthConfigurationProperties.getIssuerUri(), manager);
        return new JwtIssuerAuthenticationManagerResolver(managers::get);
    }

    @Bean
    UserOpalJwtAuthenticationProvider userOpalJwtAuthenticationProvider(
        NimbusJwtDecoder internalJwtDecoder,
        UserRepository userRepository,
        UserStateMapper userStateMapper,
        Clock clock,
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter,
        OpalCommonConfiguration commonConfiguration) {

        Domain domain = Domain.findByDisplayName(commonConfiguration.getDomain());

        return new UserOpalJwtAuthenticationProvider(
            internalJwtDecoder,
            userRepository,
            userStateMapper,
            clock,
            jwtGrantedAuthoritiesConverter,
            domain
        );
    }

    @Bean
    NimbusJwtDecoder internalJwtDecoder(
        InternalAuthProviderConfigurationProperties providerProps,
        InternalAuthConfigurationProperties authProps) {

        var jwtDecoder = NimbusJwtDecoder.withJwkSetUri(providerProps.getJwkSetUri())
            .jwsAlgorithm(SignatureAlgorithm.RS256)
            .build();

        OAuth2TokenValidator<Jwt> jwtValidator =
            JwtValidators.createDefaultWithIssuer(authProps.getIssuerUri());
        jwtDecoder.setJwtValidator(jwtValidator);
        return jwtDecoder;
    }

    @Bean
    JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter() {
        return new JwtGrantedAuthoritiesConverter();
    }
}
