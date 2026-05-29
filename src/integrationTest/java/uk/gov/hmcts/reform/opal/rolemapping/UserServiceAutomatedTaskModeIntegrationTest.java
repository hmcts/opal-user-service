package uk.gov.hmcts.reform.opal.rolemapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;
import uk.gov.hmcts.reform.opal.service.rolemapping.UserRoleMappingRefreshService;

import java.io.IOException;

@ActiveProfiles("integration")
@SpringBootTest(properties = {
    "opal.automated-task=true",
    "spring.main.web-application-type=none"
})
@DisplayName("UserServiceAutomatedTaskModeIntegrationTest")
class UserServiceAutomatedTaskModeIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private MockMvc mockMvc;

    @MockitoBean
    private UserRoleMappingRefreshService userRoleMappingRefreshService;

    @Test
    @DisplayName("Should not create web layer in automated task mode")
    void shouldNotCreateWebLayer() {
        assertThat(applicationContext.containsBean("dispatcherServlet")).isFalse();
        assertThat(applicationContext.getBeansOfType(org.springframework.web.servlet.DispatcherServlet.class))
            .isEmpty();
    }

    @Test
    @DisplayName("Should call refreshMappings on startup")
    void shouldCallRefreshMappingsOnStartup() throws IOException {
        verify(userRoleMappingRefreshService, times(1)).refreshMappings();
    }
}
