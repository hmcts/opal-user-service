@Smoke
Feature: User Service Claim Validation

  Scenario: Validate the unique name claim for opal-test@hmcts.net
    Given I am testing as the "opal-test@hmcts.net" user
    Then The response returns the status code 200
    And I validate the unique name claim matches "opal-test@HMCTS.NET"

  Scenario: Validate the unique name claim for opal-test-2@hmcts.net
    Given I am testing as the "opal-test-2@hmcts.net" user
    Then The response returns the status code 200
    And I validate the unique name claim matches "opal-test-2@HMCTS.NET"

    Scenario: Validate the unique name claim for opal-test-10@hmcte.net
    Given I am testing as the "opal-test-10@hmcts.net" user
    Then The response returns the status code 200
    And I validate the unique name claim matches "opal-test-10@HMCTS.NET"
