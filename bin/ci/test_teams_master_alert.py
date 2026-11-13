"""Focused result and attribution checks for user-service master Teams alerts."""

import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch


spec = importlib.util.spec_from_file_location(
    "teams_master_alert", Path(__file__).with_name("teams-master-alert.py"))
alert = importlib.util.module_from_spec(spec)
spec.loader.exec_module(alert)


def status(number, state, identifier, sha="head"):
    return {
        "id": identifier,
        "state": state,
        "sha": sha,
        "context": "continuous-integration/jenkins/branch",
        "target_url": ("https://sds-build.hmcts.net/job/HMCTS/job/opal-user-service/"
                       f"job/master/{number}/display/redirect"),
    }


class MasterAlertTest(unittest.TestCase):
    def test_green_messages_distinguish_rebuild_new_code_and_recovery(self):
        green = status(420, "success", 20)
        previous_green = status(419, "success", 19, "old")
        previous_red = status(419, "failure", 19, "old")

        self.assertIn("Rebuild remains GREEN",
                      alert.message_for(green, green, previous_green, report_all=True))
        self.assertIn("remains GREEN after new code",
                      alert.message_for(green, previous_green, previous_green, report_all=True))
        self.assertIn("Rebuild is GREEN; master has recovered",
                      alert.message_for(green, green, previous_red, report_all=True))
        self.assertIn("GREEN again after new code",
                      alert.message_for(green, previous_red, previous_red, report_all=True))

    def test_red_messages_distinguish_first_failure_and_still_red(self):
        red = status(420, "failure", 20)
        previous_green = status(419, "success", 19, "old")
        previous_red = status(419, "failure", 19, "old")

        self.assertIn("turned RED on unchanged code",
                      alert.message_for(red, red, previous_green, report_all=True))
        self.assertIn("turned RED after new code",
                      alert.message_for(red, previous_green, previous_green, report_all=True))
        self.assertIn("Rebuild remains RED",
                      alert.message_for(red, red, previous_red, report_all=True))
        self.assertIn("remains RED after new code",
                      alert.message_for(red, previous_red, previous_red, report_all=True))

    def test_aborted_build_has_own_alert_and_does_not_claim_recovery(self):
        aborted = status(746, "error", 20)
        aborted["description"] = "The build of this commit was aborted"
        previous_red = status(745, "failure", 19, "old")

        message = alert.message_for(aborted, previous_red, previous_red)
        self.assertIn("ABORTED; no completed result", message)
        self.assertNotIn("after new code", message)
        card = alert.card_for(message, aborted)
        self.assertIn("#746 · ABORTED", card["body"][0]["items"][0]["text"])
        self.assertEqual("attention", card["body"][0]["style"])
        self.assertIn("Console output", [action["title"] for action in card["actions"]])

        next_green = status(747, "success", 21)
        self.assertIn("GREEN following an aborted build",
                      alert.message_for(next_green, aborted, aborted))

    def test_other_github_errors_are_not_called_aborted(self):
        error = status(746, "error", 20)
        error["description"] = "Could not report the build result"

        self.assertFalse(alert.is_aborted(error))
        self.assertIn("#746 · ERROR", alert.card_for("Master is RED.", error)["body"][0]["items"][0]["text"])

    def test_transition_mode_only_sends_first_red_and_recovery(self):
        red = status(420, "error", 20)
        green = status(419, "success", 19, "old")
        previous_red = status(419, "failure", 19, "old")

        self.assertIsNotNone(alert.message_for(red, green, green))
        self.assertIsNone(alert.message_for(red, red, red))
        self.assertIn("remains RED after new code", alert.message_for(red, previous_red, previous_red))
        self.assertIsNotNone(alert.message_for(green, green, red))
        self.assertIsNone(alert.message_for(green, green, green))

    def test_previous_builds_keep_numbered_and_completed_results_separate(self):
        previous_pending = status(419, "pending", 19)
        previous_red = status(418, "failure", 18, "old")
        commits = {"head": {"sha": "head", "parents": [{"sha": "old"}]},
                   "old": {"sha": "old", "parents": []}}

        def fake_api(path):
            sha = path.split("/commits/")[1].split("/")[0]
            if "/statuses?" in path:
                return [previous_pending] if sha == "head" else [previous_red]
            return commits[sha]

        with patch.object(alert, "api", side_effect=fake_api):
            numbered, completed = alert.previous_builds("head", 420)
        self.assertEqual(419, int(alert.MASTER_BUILD.match(numbered["target_url"]).group(1)))
        self.assertEqual("head", numbered["sha"])
        self.assertEqual("failure", completed["state"])
        self.assertEqual("old", completed["sha"])

    def test_pending_previous_build_does_not_imply_unchanged_from_last_green(self):
        current = status(420, "failure", 20)
        previous_pending = status(419, "pending", 19)
        last_green = status(418, "success", 18, "old")

        message = alert.message_for(current, previous_pending, last_green, report_all=True)
        self.assertIn("preceding build has no final GitHub status", message)
        self.assertNotIn("unchanged code", message)
        self.assertNotIn("after new code", message)

    def test_only_exact_merge_commit_is_attributed_to_a_pr(self):
        associated = [{"number": 346, "merged_at": "2026-11-13T15:19:21Z",
                       "merge_commit_sha": "head", "base": {"ref": "master"}}]
        detail = {"number": 346, "html_url": "https://github.com/hmcts/opal-user-service/pull/346",
                  "merged_by": {"login": "RustyHMCTS"}}
        with patch.object(alert, "api", side_effect=[associated, detail]) as api:
            self.assertEqual(detail, alert.merged_pr_for_commit("head"))
        self.assertEqual(2, api.call_count)

        associated[0]["merge_commit_sha"] = "some-other-commit"
        with patch.object(alert, "api", return_value=associated):
            self.assertIsNone(alert.merged_pr_for_commit("head"))

    def test_card_keeps_jenkins_links_and_adds_pr_or_commit_context(self):
        green = status(420, "success", 20)
        red = status(421, "failure", 21)
        pr = {"number": 346, "html_url": "https://github.com/hmcts/opal-user-service/pull/346",
              "merged_by": {"login": "RustyHMCTS"}}

        green_card = alert.card_for("Master remains GREEN.", green, pr, new_code=True)
        self.assertIn("User Service master · #420", green_card["body"][0]["items"][0]["text"])
        self.assertEqual("good", green_card["body"][0]["style"])
        self.assertIn("Merged PR #346 by @RustyHMCTS", green_card["body"][2]["text"])
        self.assertEqual([alert.MASTER_JOB, f"{alert.MASTER_JOB}420/", pr["html_url"]],
                         [action["url"] for action in green_card["actions"]])

        red_card = alert.card_for("Master is RED.", red, new_code=True)
        self.assertIn("User Service master · #421", red_card["body"][0]["items"][0]["text"])
        self.assertEqual("attention", red_card["body"][0]["style"])
        self.assertEqual([alert.MASTER_JOB, f"{alert.MASTER_JOB}421/",
                          f"{alert.MASTER_JOB}421/console",
                          "https://github.com/hmcts/opal-user-service/commit/head"],
                         [action["url"] for action in red_card["actions"]])

    def test_completed_merge_build_includes_verified_pr_and_merger(self):
        current = status(420, "success", 20)
        previous = status(419, "success", 19, "old")
        associated = [{"number": 346, "merged_at": "2026-11-13T15:19:21Z",
                       "merge_commit_sha": "head", "base": {"ref": "master"}}]
        detail = {"number": 346, "html_url": "https://github.com/hmcts/opal-user-service/pull/346",
                  "merged_by": {"login": "RustyHMCTS"}}

        def fake_api(path):
            if path.endswith("/commits/head/statuses?per_page=100"):
                return [current]
            if path.endswith("/commits/old/statuses?per_page=100"):
                return [previous]
            if path.endswith("/commits/head"):
                return {"sha": "head", "parents": [{"sha": "old"}]}
            if path.endswith("/commits/old"):
                return {"sha": "old", "parents": []}
            if path.endswith("/commits/head/pulls?per_page=100"):
                return associated
            if path.endswith("/pulls/346"):
                return detail
            self.fail(f"Unexpected API path: {path}")

        environment = {"STATUS_ID": "20", "STATUS_URL": current["target_url"],
                       "STATUS_SHA": "head", "REPORT_ALL_BUILDS": "true",
                       "TEAMS_WEBHOOK_URL": "https://example.invalid/webhook"}
        with patch.dict("os.environ", environment), patch.object(alert, "api", side_effect=fake_api), \
                patch.object(alert, "send_card") as send:
            alert.main()

        card = send.call_args.args[0]
        self.assertIn("remains GREEN after new code", card["body"][1]["text"])
        self.assertIn("Merged PR #346 by @RustyHMCTS", card["body"][2]["text"])
        self.assertEqual(detail["html_url"], card["actions"][-1]["url"])

    def test_rebuild_does_not_repeat_pr_attribution_when_previous_status_is_pending(self):
        current = status(748, "success", 23)
        previous_pending = status(747, "pending", 22)
        last_completed = status(746, "success", 21, "old")

        def fake_api(path):
            if path.endswith("/commits/head/statuses?per_page=100"):
                return [current, previous_pending]
            if path.endswith("/commits/old/statuses?per_page=100"):
                return [last_completed]
            if path.endswith("/commits/head"):
                return {"sha": "head", "parents": [{"sha": "old"}]}
            if path.endswith("/commits/old"):
                return {"sha": "old", "parents": []}
            self.fail(f"Unexpected API path: {path}")

        environment = {"STATUS_ID": "23", "STATUS_URL": current["target_url"],
                       "STATUS_SHA": "head", "REPORT_ALL_BUILDS": "true",
                       "TEAMS_WEBHOOK_URL": "https://example.invalid/webhook"}
        with patch.dict("os.environ", environment), patch.object(alert, "api", side_effect=fake_api), \
                patch.object(alert, "send_card") as send:
            alert.main()

        card = send.call_args.args[0]
        self.assertIn("preceding build has no final GitHub status", card["body"][1]["text"])
        self.assertEqual(2, len(card["body"]))
        self.assertEqual([alert.MASTER_JOB, f"{alert.MASTER_JOB}748/"],
                         [action["url"] for action in card["actions"]])

    def test_report_all_ignores_pending_and_superseded_statuses(self):
        pending = status(418, "pending", 18)
        complete = status(418, "success", 19)
        with patch.object(alert, "api", return_value=[complete, pending]):
            self.assertIsNone(alert.status_for_event("sha", 18, pending["target_url"]))
            self.assertEqual(complete, alert.status_for_event("sha", 19, complete["target_url"]))

    def test_build_number_wins_over_late_older_completion(self):
        newer = status(418, "failure", 18)
        older = status(417, "success", 19)
        commits = {
            "master": {"sha": "head", "parents": [{"sha": "parent"}]},
            "head": {"sha": "head", "parents": [{"sha": "parent"}]},
            "parent": {"sha": "parent", "parents": []},
        }

        def fake_api(path):
            if "/statuses?" in path:
                return [newer] if "/head/" in path else [older]
            return commits[path.rsplit("/", 1)[1]]

        with patch.object(alert, "api", side_effect=fake_api):
            self.assertEqual([18, 19], [s["id"] for s in alert.completed_builds()])


if __name__ == "__main__":
    unittest.main()
