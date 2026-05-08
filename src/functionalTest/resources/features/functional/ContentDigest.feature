@Opal @JIRA-LABEL:content-digest
Feature: Content-Digest handling

  Scenario: Missing request Content-Digest succeeds when content digest is disabled
    When I make a content digest request without a Content-Digest header
    Then The response returns the status code 200

  Scenario: Valid request Content-Digest succeeds
    When I make a content digest request with a valid Content-Digest header
    Then The response returns the status code 200

  Scenario: Invalid request Content-Digest returns a problem response
    When I make a content digest request with an invalid Content-Digest header
    Then The response returns the status code 400
    And The response contains the following
      | title  | Digest validation failed                      |
      | detail | Body hash did not match for algorithm: sha-512 |

  Scenario: Malformed request Content-Digest returns a problem response
    When I make a content digest request with a malformed Content-Digest header
    Then The response returns the status code 400
    And The response contains the following
      | title  | Invalid Content-Digest header       |
      | detail | No valid digest entries found in header. |
