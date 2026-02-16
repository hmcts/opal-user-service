@Opal
Feature: Just In Time user provisioning

  @PO-2364
  Scenario: The application returns an error when creating a user with a subject token that already exists
    Given I am testing as the "opal-test-2@hmcts.net" user
    When I create a user using the stored token
    Then The response returns the status code 409
    And The response contains the following
      | title  | Conflict                                             |
      | detail | Data integrity violation with the requested resource |
