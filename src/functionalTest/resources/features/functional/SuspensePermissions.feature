@Opal @JIRA-STORY:PO-9798
Feature: Suspense permissions in user state

  # Requires release-1a enabled and the seeded test user's Suspense roles in BUs 73 and 77.
  # Additional permissions and role assignments do not affect these positive checks.
  Background:
    Given I am testing as the "opal-test@dev.platform.hmcts.net" user

  Scenario: View Suspense permission is returned in the assigned business units
    When I fetch the current V2 user state
    Then The response returns the status code 200
    And The user state contains permission 24 named "View Suspense Items" in business unit 73
    And The user state contains permission 24 named "View Suspense Items" in business unit 77

  Scenario: Manage Suspense permission is returned in the assigned business units
    When I fetch the current V2 user state
    Then The response returns the status code 200
    And The user state contains permission 25 named "Manage Suspense Items" in business unit 73
    And The user state contains permission 25 named "Manage Suspense Items" in business unit 77
