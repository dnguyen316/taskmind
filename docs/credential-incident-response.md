# Credential Exposure Incident Procedure

Treat any credential committed to Git or otherwise exposed as compromised. Deleting the
text from a later commit is not sufficient because the credential may already have been
copied and remains available in Git history.

## Immediate response

1. **Revoke or rotate the credential at its provider first.** Do this before editing the
   repository. If direct revocation is unavailable, disable the affected account,
   integration, or signing key and escalate to the service owner. Preserve only
   non-secret evidence needed for the incident record.
2. Determine the credential's owner, permissions, environments, exposure window, and
   whether logs show unauthorized use. Reduce permissions or isolate affected systems
   while the investigation continues.
3. Remove the credential from the current tree. Replace it with an environment variable
   or approved secret-manager reference, and verify that generated files, logs, build
   artifacts, caches, and open pull requests do not contain another copy.
4. When appropriate, rewrite the affected Git history with a tool such as `git filter-repo`
   and coordinate the force-push with repository administrators. History rewriting is
   especially appropriate for sensitive material, but it does **not** replace provider
   revocation or rotation. Notify collaborators to discard old clones and branches, then
   verify the rewritten history with the repository's secret scanner.
5. Record the incident timeline, affected systems, containment, audit findings, and the
   new credential's owner and rotation date. Never place the old or replacement secret in
   the incident ticket.

## Credentials covered

This procedure applies to all credentials, including:

- AWS IAM access keys and secret access keys;
- GitHub personal, fine-grained, OAuth, and GitHub App tokens;
- JWT signing keys and secrets;
- internal and third-party service tokens;
- database usernames, passwords, and connection strings;
- Slack bot, app, user, and webhook tokens; and
- LLM provider API keys and tokens.

After containment, rotate any related credentials that could have been derived from or
accessed with the exposed credential, and add only narrowly scoped scanner exceptions for
confirmed non-secret fixtures.
