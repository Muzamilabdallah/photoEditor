# Releasing PhotoForge

Releases go to **Maven Central** as `io.github.photoforge:photoforge-core` and
`io.github.photoforge:photoforge-ui`, published by
[`.github/workflows/release.yml`](.github/workflows/release.yml) when a version
tag is pushed. The workflow also creates the GitHub Release from the matching
`CHANGELOG.md` section.

## One-time setup

Everything here needs your own accounts, so it can't be automated.

### 1. Claim the `photoforge` name on GitHub

Maven Central only lets you publish under `io.github.photoforge` if you
control `github.com/photoforge`.

1. Create a free organization named **`photoforge`**:
   <https://github.com/account/organizations/new>. Do this soon; the name is
   only free until someone takes it.
2. Optional: transfer this repository into the organization
   (*Settings → General → Transfer ownership*) and update the `url`/`scm`
   values in both module `build.gradle.kts` files. GitHub redirects the old
   URL, so nothing breaks if you leave it where it is.

### 2. Register the namespace on the Central Portal

1. Sign in at <https://central.sonatype.com> (signing in with GitHub is fine).
2. **Namespaces → Add Namespace →** `io.github.photoforge`.
3. The portal shows a verification key. Create a **public, empty repository
   with that exact name** under `github.com/photoforge`, then click
   **Verify** in the portal. Delete the repository once it shows *Verified*.

### 3. Create a publishing token

On the Central Portal: **Account → Generate User Token**. It gives a
username and a password (not your login).

### 4. Create a signing key

Central requires every file to be signed with a PGP key whose public half is
on a public key server.

```bash
brew install gnupg
```

```bash
gpg --full-generate-key
```

Pick the default key type, no expiry (or a long one), your name and an email,
and a passphrase. Then publish the public key and export the private key:

```bash
gpg --list-secret-keys --keyid-format=long
```

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
```

```bash
gpg --armor --export-secret-keys <KEY_ID> > signing-key.asc
```

### 5. Add the repository secrets

Store the four values as GitHub Actions secrets (each command prompts for the
value; nothing ends up in your shell history):

```bash
gh secret set MAVEN_CENTRAL_USERNAME -R Muzamilabdallah/photoEditor
```

```bash
gh secret set MAVEN_CENTRAL_PASSWORD -R Muzamilabdallah/photoEditor
```

```bash
gh secret set SIGNING_KEY -R Muzamilabdallah/photoEditor < signing-key.asc
```

```bash
gh secret set SIGNING_KEY_PASSWORD -R Muzamilabdallah/photoEditor
```

Then delete `signing-key.asc` and keep the key itself backed up somewhere
safe. The workflow fails early with a clear error if any secret is missing.

## Each release

1. **Version:** set `VERSION_NAME` in `gradle.properties` (e.g. `0.2.0`).
2. **Changelog:** rename `## [Unreleased]…` in `CHANGELOG.md` to
   `## [0.2.0] — YYYY-MM-DD`. The workflow uses that exact heading for the
   GitHub Release notes. Start a fresh `## [Unreleased]` above it.
3. **README:** update the version in the install snippet.
4. Merge to `master`, then tag that commit and push the tag:

   ```bash
   git tag 0.2.0
   ```

   ```bash
   git push origin 0.2.0
   ```

5. Watch the **Release** workflow under *Actions*. It checks that the tag
   equals `VERSION_NAME`, runs the unit tests, publishes, and creates the
   GitHub Release. Artifacts usually show up on Maven Central within 10–30
   minutes.
6. **Check it:** in a fresh project, depend on
   `io.github.photoforge:photoforge-ui:<version>` with only `google()` and
   `mavenCentral()` and build.

If publishing fails after the upload, the deployment may still be listed on
the Central Portal under **Deployments**; drop it there before re-tagging.

## Checking locally

```bash
./gradlew :photoeditor-core:publishToMavenLocal :photoeditor-ui-default:publishToMavenLocal
```

This writes unsigned artifacts to `~/.m2/repository/io/github/photoforge/`.
Run it with `JITPACK=true` set to see the legacy JitPack coordinates instead.
