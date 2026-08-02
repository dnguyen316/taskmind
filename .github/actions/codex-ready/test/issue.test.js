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

function event(overrides = {}, eventOverrides = {}) {
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
    ...eventOverrides,
  };
}

test("validates and extracts a Codex task form", () => {
  assert.deepEqual(validateIssue(event()), {
    number: 42,
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

test("rejects a different triggering label", () => {
  assert.throws(
    () => validateIssue(event({}, { label: { name: "bug" } })),
    /exact codex-ready label/,
  );
});

test("rejects a closed issue", () => {
  assert.throws(() => validateIssue(event({ state: "closed" })), /must be open/);
});

test("rejects a pull request payload", () => {
  assert.throws(
    () => validateIssue(event({ pull_request: { url: "https://example.test" } })),
    /Pull requests cannot/,
  );
});

test("rejects an issue without the Codex title prefix", () => {
  assert.throws(
    () => validateIssue(event({ title: "Refactor workflow" })),
    /title prefix/,
  );
});

test("rejects task form sections that are out of order", () => {
  const body = validBody.replace(
    "### Objective\nBuild it.\n\n### Scope\nOnly this.",
    "### Scope\nOnly this.\n\n### Objective\nBuild it.",
  );
  assert.throws(() => validateIssue(event({ body })), /sections in order/);
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
    assert.ok(pullRequest.body.includes(section), `missing section: ${section}`);
  }
  assert.match(
    pullRequest.body,
    /Automated CI checks are not started for this draft pull request/,
  );
  assert.match(pullRequest.body, /Run the repository verification commands manually/);
});
