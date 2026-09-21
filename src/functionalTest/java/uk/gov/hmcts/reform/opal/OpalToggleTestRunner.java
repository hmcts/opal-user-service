package uk.gov.hmcts.reform.opal;

import static io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/toggle")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@Opal and not @Ignore")
public class OpalToggleTestRunner {
}
