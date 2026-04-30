@Opal
Feature: Seeded users are active

  @JIRA-STORY:PO-3845 @JIRA-EPIC:2352
  Scenario Outline: Seeded test users return active status
    Given I am testing as the "<user>" user
    When I fetch the current user state
    Then The response returns the status code 200
    And The response contains the following
      | status | active |

    Examples:
      | user                                |
      | opal-test@dev.platform.hmcts.net    |
      | opal-test-2@dev.platform.hmcts.net  |
      | opal-test-10@dev.platform.hmcts.net |
