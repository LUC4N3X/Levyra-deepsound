# Contributing to Levyra

Thanks for taking the time to contribute.

Levyra moves quickly, so the most useful contributions are usually the ones that solve one clear problem without changing unrelated parts of the project. You do not need to know the whole codebase before helping — just keep the scope understandable and make it easy to review.

## Before you start

- Check the existing issues and pull requests to avoid duplicating work.
- Small bug fixes and focused improvements can usually go straight to a pull request.
- For larger features, architectural changes, or anything that affects several parts of the app, open an issue first so the approach can be discussed before a lot of work is done.
- Security issues should **not** be reported in a public issue. Follow [SECURITY.md](SECURITY.md) instead.

## Setting up the project

1. Fork the repository and clone your fork.
2. Create a branch from the latest `main`.
3. Use the repository's Gradle wrapper instead of a system Gradle installation.
4. Keep your change focused on the problem you are solving.

For Android development, the usual local build is:

```bash
./gradlew :app:assembleDebug
```

On Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

Before submitting a meaningful code change, run the project's fast validation gate:

```bash
python scripts/ai_quality_gate.py --profile fast
```

If a check cannot run because of a missing SDK, JDK, device, signing setup, network dependency, or another local requirement, say so in the pull request instead of presenting it as passed.

## What makes a good contribution

A good pull request is easy to understand without guessing what the author intended.

Please try to:

- solve one concern at a time;
- follow the structure and patterns already used in the surrounding code;
- avoid unrelated refactors, dependency upgrades, formatting sweeps, or file moves;
- preserve existing playback, downloads, playlists, favorites, history, settings, localization, and user data unless your change intentionally affects them;
- add or update tests when the change has behavior that can be tested reasonably;
- test the part of the app you actually changed;
- include screenshots or a short screen recording for visible UI changes when that helps the review;
- keep user-facing text clear and consistent with the rest of Levyra;
- never commit secrets, API keys, cookies, signing files, `.env`, `local.properties`, or private URLs.

## Pull requests

Use a descriptive title and explain the change in plain language.

A useful pull request description should cover:

- what was wrong or missing;
- what you changed;
- anything users might notice;
- what you tested;
- anything you could not test.

Do not inflate the description with claims that were not actually verified. A small, accurate PR description is more useful than a long one.

Please keep review fixes in the same pull request unless the scope genuinely changes. If feedback points to a separate problem, it is usually better handled in another issue or PR.

## Bug reports

When reporting a bug, include enough detail for someone else to reproduce it:

- Levyra version;
- Android or Windows version, depending on where it happened;
- device or PC details when relevant;
- clear reproduction steps;
- expected behavior;
- actual behavior;
- logs, screenshots, or recordings when useful.

Remove personal data, tokens, account information, and other sensitive information before posting logs publicly.

## Feature ideas

Feature requests are welcome. The most useful ones explain the user problem first and the proposed solution second.

For larger ideas, mention how the feature should fit with existing Levyra behavior instead of assuming the surrounding architecture needs to change.

## AI-assisted contributions

Using coding assistants is fine, but the person opening the pull request is still responsible for the result.

Please review the generated changes, understand what they do, remove unnecessary churn, and verify the behavior before submitting them. A pull request should never rely on "the tool said it works" as validation.

## Style and review

There is no need to rewrite working code just to match a personal preference. Follow the style already present in the file or module you are touching.

Reviews may ask for changes when a contribution introduces unnecessary complexity, weakens reliability or privacy, duplicates an existing owner, or makes future maintenance harder. That is about protecting the project, not about making every contribution look identical.

## License

By submitting a contribution to Levyra, you agree that your contribution will be licensed under the same license as the project. See [LICENSE](../LICENSE).

## Community

Be respectful to maintainers, contributors, and users. Technical disagreement is normal; personal attacks are not.

Please read the [Code of Conduct](CODE_OF_CONDUCT.md) before participating in project discussions.