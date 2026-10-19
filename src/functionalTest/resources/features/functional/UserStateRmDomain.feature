@Opal @JIRA-STORY:PO-10302 @JIRA-EPIC:PO-6506
Feature: Maintenance permissions in User State

  Scenario: Retrieve Draft Casefile permission in User State
    When I request my V2 User State as the "opal-test@dev.platform.hmcts.net" user
    Then The response returns the status code 200
    And my User State belongs to user 500000000
    And my Maintenance Business Unit 44 includes permission 21 named "Create and Manage Draft Casefiles"

  Scenario: Draft Casefile permission is not granted implicitly
    When I request my V2 User State as the "opal-test-4@dev.platform.hmcts.net" user
    Then The response returns the status code 200
    And my User State belongs to user 500000003
    And my Maintenance Business Unit 111 includes permission 3 named "Account Enquiry"
    And no Maintenance Business Unit grants permission 21 named "Create and Manage Draft Casefiles"

  Scenario: User State requires authentication
    When I request V2 User State without authentication
    Then The response returns the status code 401
    And the User State response is an authentication problem without assignment data
