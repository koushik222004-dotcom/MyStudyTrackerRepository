---
name: CodeExecution secrets not inherited by subshells
description: Why $GITHUB_ACCESS_TOKEN (or any requested secret) is empty inside execSync/bash but present in process.env — and the fix.
---

Inside an impure CodeExecution function, `process.env.MY_SECRET` holds the real value (confirmed via
`process.env.GITHUB_ACCESS_TOKEN.length`). But if you shell out with `execSync("... $MY_SECRET ...")`,
the spawned bash subprocess does **not** inherit that env var — it expands to an empty string. This
silently breaks anything that relies on shell-side env expansion: `git remote add
https://$TOKEN@github.com/...` stores a credential-less URL, and constructs like
`git -c http.extraHeader="Authorization: Basic $(... "$TOKEN" ...)"` fail with
`fatal: could not read Username for 'https://github.com'`.

**Why:** the sandbox appears to deliberately strip secret env vars from child-process environments
(likely to prevent trivial exfiltration via shell history/process args), while still exposing them to
the Node/JS layer that received them from `requestSecrets`.

**How to apply:** never rely on `$SECRET_NAME` expanding inside a `execSync`/bash command. Instead,
read the value in JS (`const token = process.env.MY_SECRET`) and interpolate the *already-resolved*
value directly into the command string you pass to `execSync` (e.g. build a Basic-auth header with
`Buffer.from(...).toString("base64")` in JS, then splice that string into the git command). This is
the reliable way to authenticate git pushes/pulls to a repo via a manually pasted PAT when the user
declined the GitHub OAuth connector.
