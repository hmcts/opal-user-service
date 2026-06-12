@Opal
Feature: User Service release-1a feature toggles

  @JIRA-STORY:PO-3763 @JIRA-EPIC:PO-3685
  Scenario Outline: Release-1a gated endpoint <endpoint> returns the feature-disabled response when disabled
    When I call the release-1a gated "<endpoint>" endpoint
    Then The response returns the status code 404
    And The response contains the following
      | title  | Feature Disabled                                  |
      | detail | The requested feature is not currently available |

    Examples:
      | endpoint                            |
      | get test user token                 |
      | get user token                      |
      | parse token                         |
      | add user                            |
      | update current user                 |
      | update user by id                   |
      | get current user state              |
      | get user state by id                |
