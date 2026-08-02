const assert = require("node:assert/strict");
const test = require("node:test");
const { claim, createDraftPullRequest } = require("../handlers");

const context = {
  repo: { owner: "taskmind", repo: "taskmind" },
  payload: { repository: { default_branch: "main" } },
};

test("claim skips removal when codex-ready is already absent", async () => {
  const calls = [];
  const github = {
    rest: {
      issues: {
        get: async () => ({ data: { labels: [] } }),
        removeLabel: async (input) => calls.push(["remove", input]),
        addLabels: async (input) => calls.push(["add", input]),
      },
    },
  };

  await claim({ github, context, issueNumber: "42" });

  assert.deepEqual(calls, [
    [
      "add",
      {
        owner: "taskmind",
        repo: "taskmind",
        issue_number: 42,
        labels: ["codex-in-progress"],
      },
    ],
  ]);
});

test("claim removes codex-ready when it is present", async () => {
  const calls = [];
  const github = {
    rest: {
      issues: {
        get: async () => ({ data: { labels: [{ name: "codex-ready" }] } }),
        removeLabel: async (input) => calls.push(["remove", input.name]),
        addLabels: async () => calls.push(["add"]),
      },
    },
  };

  await claim({ github, context, issueNumber: "42" });

  assert.deepEqual(calls, [["remove", "codex-ready"], ["add"]]);
});

test("draft creation reuses an existing open pull request", async () => {
  let createCalls = 0;
  let listInput;
  const github = {
    rest: {
      pulls: {
        list: async (input) => {
          listInput = input;
          return { data: [{ number: 7 }] };
        },
        create: async () => {
          createCalls += 1;
        },
      },
    },
  };

  await createDraftPullRequest({
    github,
    context,
    issueNumber: "42",
    issueTitle: "[Codex]: Refactor workflow",
  });

  assert.deepEqual(listInput, {
    owner: "taskmind",
    repo: "taskmind",
    state: "open",
    head: "taskmind:codex/issue-42",
  });
  assert.equal(createCalls, 0);
});

test("draft creation creates a pull request when no open match exists", async () => {
  let createInput;
  const github = {
    rest: {
      pulls: {
        list: async () => ({ data: [] }),
        create: async (input) => {
          createInput = input;
        },
      },
    },
  };

  await createDraftPullRequest({
    github,
    context,
    issueNumber: "42",
    issueTitle: "[Codex]: Refactor workflow",
  });

  assert.equal(createInput.head, "codex/issue-42");
  assert.equal(createInput.base, "main");
  assert.equal(createInput.draft, true);
});
