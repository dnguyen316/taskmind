const { draftPullRequest, validateIssue } = require("./lib/issue");

async function validate({ core, context }) {
  const issue = validateIssue(context.payload);
  core.setOutput("issue-number", issue.number);
  core.setOutput("issue-title", issue.title);
  core.setOutput("issue-body", issue.body);
}

async function claim({ github, context, issueNumber }) {
  const common = {
    owner: context.repo.owner,
    repo: context.repo.repo,
    issue_number: Number(issueNumber),
  };
  const issue = await github.rest.issues.get(common);
  const labels = new Set(issue.data.labels.map((label) => label.name));
  if (labels.has("codex-ready")) {
    await github.rest.issues.removeLabel({ ...common, name: "codex-ready" });
  }
  await github.rest.issues.addLabels({
    ...common,
    labels: ["codex-in-progress"],
  });
}

async function createDraftPullRequest({
  github,
  context,
  issueNumber,
  issueTitle,
}) {
  const numericIssueNumber = Number(issueNumber);
  const pullRequest = draftPullRequest(
    numericIssueNumber,
    issueTitle,
    context.payload.repository.default_branch,
  );
  const common = {
    owner: context.repo.owner,
    repo: context.repo.repo,
  };
  const existing = await github.rest.pulls.list({
    ...common,
    state: "open",
    head: `${context.repo.owner}:${pullRequest.head}`,
  });
  if (existing.data.length === 0) {
    await github.rest.pulls.create({ ...common, ...pullRequest });
  }
}

async function block({ github, context }) {
  const common = {
    owner: context.repo.owner,
    repo: context.repo.repo,
    issue_number: context.payload.issue.number,
  };
  const issue = await github.rest.issues.get(common);
  const labels = new Set(issue.data.labels.map((label) => label.name));
  for (const name of ["codex-ready", "codex-in-progress"]) {
    if (labels.has(name)) {
      await github.rest.issues.removeLabel({ ...common, name });
    }
  }
  if (!labels.has("codex-blocked")) {
    await github.rest.issues.addLabels({
      ...common,
      labels: ["codex-blocked"],
    });
  }
}

module.exports = { block, claim, createDraftPullRequest, validate };
