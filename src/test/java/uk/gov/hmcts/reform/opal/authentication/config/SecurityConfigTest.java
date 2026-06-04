package uk.gov.hmcts.reform.opal.authentication.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import uk.gov.hmcts.opal.common.user.authentication.exception.CustomAuthenticationExceptions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityConfigTest {

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        CustomAuthenticationExceptions customAuthenticationExceptions = mock(CustomAuthenticationExceptions.class);
        securityConfig = new SecurityConfig(customAuthenticationExceptions);
    }

    @Test
    void isJwtValidationOnlyEndpoint_shouldMatchConfiguredPathAndMethod() {
        assertTrue(securityConfig.isJwtValidationOnlyEndpoint("/users", "POST"));
    }

    @Test
    void isJwtValidationOnlyEndpoint_shouldNotMatchWhenMethodDiffers() {
        assertFalse(securityConfig.isJwtValidationOnlyEndpoint("/users", "GET"));
    }

    @Test
    void isJwtValidationOnlyEndpoint_shouldNotMatchWhenPathDiffers() {
        assertFalse(securityConfig.isJwtValidationOnlyEndpoint("/users/someOther", "POST"));
    }

    @Test
    void isJwtValidationOnlyEndpoint_shouldUseRequestValues() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/users");
        request.setServletPath("/users");

        assertTrue(securityConfig.isJwtValidationOnlyEndpoint(request));
        request.setServletPath("/users/other");
        assertFalse(securityConfig.isJwtValidationOnlyEndpoint(request));
    }

    @Test
    void selectAuthenticationManager_shouldUseStandardResolverForJwtOnlyEndpoints() {
        JwtIssuerAuthenticationManagerResolver opalResolver = mock(JwtIssuerAuthenticationManagerResolver.class);
        JwtIssuerAuthenticationManagerResolver standardResolver = mock(JwtIssuerAuthenticationManagerResolver.class);
        AuthenticationManager standardManager = mock(AuthenticationManager.class);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/users");
        request.setServletPath("/users");

        when(standardResolver.resolve(request)).thenReturn(standardManager);

        AuthenticationManager selectedManager =
            securityConfig.selectAuthenticationManager(request, opalResolver, standardResolver);

        assertSame(standardManager, selectedManager);
        verify(standardResolver).resolve(request);
        verify(opalResolver, never()).resolve(request);
    }

    @Test
    void selectAuthenticationManager_shouldUseOpalResolverForOtherEndpoints() {
        JwtIssuerAuthenticationManagerResolver opalResolver = mock(JwtIssuerAuthenticationManagerResolver.class);
        JwtIssuerAuthenticationManagerResolver standardResolver = mock(JwtIssuerAuthenticationManagerResolver.class);
        AuthenticationManager opalManager = mock(AuthenticationManager.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v2/users/0/state");
        request.setServletPath("/v2/users/0/state");

        when(opalResolver.resolve(request)).thenReturn(opalManager);

        AuthenticationManager selectedManager =
            securityConfig.selectAuthenticationManager(request, opalResolver, standardResolver);

        assertSame(opalManager, selectedManager);
        verify(opalResolver).resolve(request);
        verify(standardResolver, never()).resolve(request);
    }
}
