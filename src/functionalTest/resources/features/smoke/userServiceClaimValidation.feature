@Smoke
Feature: User Service Claim Validation

  Scenario: Validate the unique name claim for opal-test@dev.platform.hmcts.net
    Given I am testing as the "opal-test@dev.platform.hmcts.net" user
    Then The response returns the status code 200
    And I validate the unique name claim matches "opal-test@dev.platform.hmcts.net"

  Scenario: Validate the unique name claim for opal-test-2@dev.platform.hmcts.net
    Given I am testing as the "opal-test-2@dev.platform.hmcts.net" user
    Then The response returns the status code 200
    And I validate the unique name claim matches "opal-test-2@dev.platform.hmcts.net"

    Scenario: Validate the unique name claim for opal-test-10@hmcte.net
    Given I am testing as the "opal-test-10@dev.platform.hmcts.net" user
    Then The response returns the status code 200
    And I validate the unique name claim matches "opal-test-10@dev.platform.hmcts.net"
