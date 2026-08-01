const assert = require("node:assert/strict");
const test = require("node:test");
const { draftPullRequest, validateIssue } = require("../lib/issue");

const validBody = `### Objective
Build it.

### Scope
Only this.

### Acceptance criteria
- [ ] It works.

### Verification
Run tests.

### Restrictions
Do not deploy.`;

function event(overrides = {}) {
  return {
    label: { name: "codex-ready" },
    issue: {
      number: 42,
      state: "open",
      title: "[Codex]: Refactor workflow",
      body: validBody,
      labels: [{ name: "codex-ready" }],
      ...overrides,
    },
  };
}

test("validates and extracts a Codex task form", () => {
  assert.deepEqual(validateIssue(event()), {
    number: "42",
    title: "[Codex]: Refactor workflow",
    body: validBody,
  });
});

test("rejects empty required sections", () => {
  assert.throws(
    () =>
      validateIssue(
        event({ body: validBody.replace("Build it.", "_No response_") }),
      ),
    /Objective must be non-empty/,
  );
});

test("rejects conflicting lifecycle labels", () => {
  assert.throws(
    () =>
      validateIssue(
        event({
          labels: [{ name: "codex-ready" }, { name: "codex-in-progress" }],
        }),
      ),
    /codex-in-progress/,
  );
});

test("builds a template-complete issue-linked draft pull request", () => {
  const pullRequest = draftPullRequest(
    42,
    "[Codex]: Refactor workflow",
    "main",
  );
  assert.equal(pullRequest.head, "codex/issue-42");
  assert.equal(pullRequest.base, "main");
  assert.equal(pullRequest.draft, true);
  for (const section of [
    "## Summary",
    "## Verification",
    "## Agent report",
    "## Risks or limitations",
    "## Linked issue",
    "**Closes #42**",
  ]) {
    assert.match(
      pullRequest.body,
      new RegExp(section.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")),
    );
  }
});
