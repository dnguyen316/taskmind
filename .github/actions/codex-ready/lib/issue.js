const EXPECTED_SECTIONS = [
  "Objective",
  "Scope",
  "Acceptance criteria",
  "Verification",
  "Restrictions",
];

const CONFLICTING_LABELS = [
  "codex-in-progress",
  "codex-review",
  "human-review",
  "codex-blocked",
  "codex-done",
];

function validateIssue(event) {
  const issue = event.issue;
  if (event.label?.name !== "codex-ready") {
    throw new Error(
      "This run was not triggered by the exact codex-ready label.",
    );
  }
  if (issue.state !== "open") {
    throw new Error("The triggering issue must be open.");
  }
  if (issue.pull_request) {
    throw new Error("Pull requests cannot enter the Codex issue queue.");
  }
  if (!issue.title.startsWith("[Codex]: ")) {
    throw new Error(
      "The issue must use the Codex task issue form title prefix.",
    );
  }

  const labels = new Set(issue.labels.map((label) => label.name));
  const conflicting = CONFLICTING_LABELS.filter((label) => labels.has(label));
  if (conflicting.length > 0) {
    throw new Error(
      `Issue already has terminal/active label(s): ${conflicting.join(", ")}`,
    );
  }

  const body = issue.body || "";
  const headings = [...body.matchAll(/^### (.+)$/gm)];
  const names = headings.map((match) => match[1].trim());
  if (
    names.length !== EXPECTED_SECTIONS.length ||
    !EXPECTED_SECTIONS.every((section, index) => names[index] === section)
  ) {
    throw new Error(
      `Issue must use the Codex task form sections in order: ${EXPECTED_SECTIONS.join(", ")}`,
    );
  }

  for (let index = 0; index < headings.length; index += 1) {
    const start = headings[index].index + headings[index][0].length;
    const end =
      index + 1 < headings.length ? headings[index + 1].index : body.length;
    const value = body.slice(start, end).trim();
    if (!value || value === "_No response_") {
      throw new Error(`${EXPECTED_SECTIONS[index]} must be non-empty.`);
    }
  }

  return { number: issue.number, title: issue.title, body };
}

function branchName(issueNumber) {
  return `codex/issue-${issueNumber}`;
}

function draftPullRequest(issueNumber, issueTitle, defaultBranch) {
  const branch = branchName(issueNumber);
  const title = issueTitle.replace(/^\[Codex\]:\s*/, "");
  const body = `## Summary

Implements the validated scope from issue #${issueNumber}.

## Verification

- Automated CI checks are not started for this draft pull request.
- Run the repository verification commands manually before marking this draft ready for review.

## Agent report

- Codex generated the patch in a read-only job; the apply job validated and applied it.
- Branch: \`${branch}\`.

## Risks or limitations

- Human review and manual verification are required before this draft is ready to merge.

## Linked issue

**Closes #${issueNumber}**`;

  return {
    head: branch,
    base: defaultBranch,
    title: `[Codex #${issueNumber}] ${title}`,
    body,
    draft: true,
  };
}

module.exports = { branchName, draftPullRequest, validateIssue };
