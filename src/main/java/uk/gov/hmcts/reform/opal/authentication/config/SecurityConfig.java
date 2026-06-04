package uk.gov.hmcts.reform.opal.authentication.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.AntPathMatcher;
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
        "/testing-support/**",
        "/s2s/**",
        "/"
    };

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final EndpointMethod[] JWT_VALIDATION_ONLY_ENDPOINTS = {
        new EndpointMethod("/users", HttpMethod.POST)
    };

    @Bean
    @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "squid:S4502"})
    public SecurityFilterChain filterChain(
        HttpSecurity http,
        JwtIssuerAuthenticationManagerResolver opalUserJwtIssuerAuthenticationManagerResolver,
        JwtIssuerAuthenticationManagerResolver standardJwtIssuerAuthenticationManagerResolver
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
                oauth2.authenticationManagerResolver(request ->
                    selectAuthenticationManager(
                        request,
                        opalUserJwtIssuerAuthenticationManagerResolver,
                        standardJwtIssuerAuthenticationManagerResolver
                    )
                )
                    .authenticationEntryPoint(userCustomAuthenticationExceptions)
                    .accessDeniedHandler(userCustomAuthenticationExceptions)
            );

        return http.build();
    }

    AuthenticationManager selectAuthenticationManager(
        HttpServletRequest request,
        JwtIssuerAuthenticationManagerResolver opalUserJwtIssuerAuthenticationManagerResolver,
        JwtIssuerAuthenticationManagerResolver standardJwtIssuerAuthenticationManagerResolver
    ) {
        if (isJwtValidationOnlyEndpoint(request)) {
            return standardJwtIssuerAuthenticationManagerResolver.resolve(request);
        }
        return opalUserJwtIssuerAuthenticationManagerResolver.resolve(request);
    }

    boolean isJwtValidationOnlyEndpoint(HttpServletRequest request) {
        return isJwtValidationOnlyEndpoint(request.getServletPath(), request.getMethod());
    }

    boolean isJwtValidationOnlyEndpoint(String servletPath, String method) {
        for (EndpointMethod endpoint : JWT_VALIDATION_ONLY_ENDPOINTS) {
            if (endpoint.method().name().equalsIgnoreCase(method)
                && PATH_MATCHER.match(endpoint.path(), servletPath)) {
                return true;
            }
        }
        return false;
    }

    private record EndpointMethod(String path, HttpMethod method) {
    }

    private HttpSecurity applyCommonConfig(HttpSecurity http) {
        return http
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(FormLoginConfigurer::disable)
            .logout(LogoutConfigurer::disable);
    }

    @Bean
    JwtIssuerAuthenticationManagerResolver opalUserJwtIssuerAuthenticationManagerResolver(
        InternalAuthConfigurationProperties internalAuthConfigurationProperties,
        UserOpalJwtAuthenticationProvider userOpalJwtAuthenticationProvider
    ) {
        return jwtIssuerAuthenticationManagerResolver(
            internalAuthConfigurationProperties,
            userOpalJwtAuthenticationProvider);
    }

    @Bean
    JwtIssuerAuthenticationManagerResolver standardJwtIssuerAuthenticationManagerResolver(
        InternalAuthConfigurationProperties internalAuthConfigurationProperties,
        JwtAuthenticationProvider standardOpalJwtAuthenticationProvider
    ) {
        return jwtIssuerAuthenticationManagerResolver(
            internalAuthConfigurationProperties,
            standardOpalJwtAuthenticationProvider);
    }


    private JwtIssuerAuthenticationManagerResolver jwtIssuerAuthenticationManagerResolver(
        InternalAuthConfigurationProperties internalAuthConfigurationProperties,
        AuthenticationProvider authenticationProvider
    ) {
        AuthenticationManager manager = authenticationProvider::authenticate;
        Map<String, AuthenticationManager> managers =
            Map.of(internalAuthConfigurationProperties.getIssuerUri(), manager);
        return new JwtIssuerAuthenticationManagerResolver(managers::get);
    }

    @Bean
    JwtAuthenticationProvider standardOpalJwtAuthenticationProvider(
        NimbusJwtDecoder internalJwtDecoder) {
        return new JwtAuthenticationProvider(internalJwtDecoder);
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
