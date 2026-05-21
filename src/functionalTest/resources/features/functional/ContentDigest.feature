@Opal @JIRA-LABEL:content-digest
Feature: Content-Digest handling

  Scenario: Missing request Content-Digest succeeds and returns a response Content-Digest
    When I make a content digest request without a Content-Digest header
    Then The response returns the status code 200
    And The response has a valid Content-Digest header

  Scenario: Valid request Content-Digest succeeds and returns a response Content-Digest
    When I make a content digest request with a valid Content-Digest header
    Then The response returns the status code 200
    And The response has a valid Content-Digest header

  Scenario: Invalid request Content-Digest returns a problem response with a response Content-Digest
    When I make a content digest request with an invalid Content-Digest header
    Then The response returns the status code 400
    And The response contains the following
      | title  | Digest validation failed                      |
      | detail | Body hash did not match for algorithm: sha-512 |
    And The response has a valid Content-Digest header

  Scenario: Malformed request Content-Digest returns a problem response with a response Content-Digest
    When I make a content digest request with a malformed Content-Digest header
    Then The response returns the status code 400
    And The response contains the following
      | title  | Invalid Content-Digest header       |
      | detail | No valid digest entries found in header. |
    And The response has a valid Content-Digest header
