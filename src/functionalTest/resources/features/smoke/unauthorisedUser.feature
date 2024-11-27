@Smoke
Feature: Attempt to get a token as an unauthorised user

  Scenario: Attempt to get a token as an unauthorised user
    Given I am testing as the "stealing-your-data@webmail.com" user
    Then The response returns the status code 500
