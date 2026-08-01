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
  await github.rest.issues.removeLabel({ ...common, name: "codex-ready" });
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
  const pullRequest = draftPullRequest(
    Number(issueNumber),
    issueTitle,
    context.payload.repository.default_branch,
  );
  await github.rest.pulls.create({
    owner: context.repo.owner,
    repo: context.repo.repo,
    ...pullRequest,
  });
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
