@Opal
Feature: User Service release-1a enabled coverage

  @JIRA-STORY:PO-3763 @JIRA-EPIC:PO-3685
  Scenario: Explicit user token and parse endpoints remain available when release-1a is enabled
    When I request the release-1a token for the "opal-test@dev.platform.hmcts.net" user
    Then The response returns the status code 200
    And The response contains a non-empty "access_token" field

    When I parse the last returned release-1a access token
    Then The response returns the status code 200
    And The response body is "opal-test@dev.platform.hmcts.net"
