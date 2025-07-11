#\!/bin/bash

# Create a ruleset for the main branch requiring test workflows to pass
gh api \
  --method POST \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  /repos/ThornLang/JavaThorn/rulesets \
  -f name='Require Tests to Pass' \
  -f target='branch' \
  -f enforcement='active' \
  -F conditions='
{
  "ref_name": {
    "include": ["~DEFAULT_BRANCH"],
    "exclude": []
  }
}' \
  -F rules='[
  {
    "type": "pull_request",
    "parameters": {
      "required_approving_review_count": 0,
      "dismiss_stale_reviews_on_push": false,
      "require_code_owner_review": false,
      "require_last_push_approval": false,
      "required_review_thread_resolution": false
    }
  },
  {
    "type": "required_status_checks",
    "parameters": {
      "required_status_checks": [
        {
          "context": "test",
          "integration_id": null
        },
        {
          "context": "pr-tests",
          "integration_id": null
        }
      ],
      "strict_required_status_checks_policy": true
    }
  }
]' \
  -F bypass_actors='[]'

echo "Ruleset created successfully\!"
