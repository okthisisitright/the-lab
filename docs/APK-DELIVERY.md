# Current delivery — 0.3.0

[Download WidgetLab-0.3.0.apk](https://github.com/okthisisitright/the-lab/releases/download/widget-lab-0.3.0-test/WidgetLab-0.3.0.apk), 9,322,660 bytes. Install over the existing optimized app, open Widget Lab once, then Settings → Refresh installed widgets. The signature matches earlier optimized releases. [Verification and changes](REVISION-0.3.0.md).

[Release notes/checksum](https://github.com/okthisisitright/the-lab/releases/tag/widget-lab-0.3.0-test) · [Exact Actions artifact](https://github.com/okthisisitright/the-lab/actions/runs/38925000635/artifacts/12055943506)

Published through `publish-apk.yml` using actions/upload-artifact and GitHub Releases. The release download was verified byte-for-byte. The regular `android.yml` workflow still builds and uploads a debug APK. Prefer the signed release asset for preserving the existing installation.

The following sections are historical delivery records.

---

# Current delivery — Edition II / 0.2.0

The current verified download is [WidgetLab-0.2.0.apk](https://github.com/okthisisitright/the-lab/releases/download/widget-lab-0.2.0-test/WidgetLab-0.2.0.apk), 8,650,612 bytes. Open it on the phone and allow installation from the browser/file manager when prompted. No Codex file viewer or development server is required.

[Release notes and checksum](https://github.com/okthisisitright/the-lab/releases/tag/widget-lab-0.2.0-test) · [Exact Actions artifact](https://github.com/okthisisitright/the-lab/actions/runs/38860986027/artifacts/12028814522) · [Verification and remaining phone checks](EDITION-II-VERIFICATION.md)

The successful publish workflow is `publish-edition-ii.yml`; it verifies the committed package checksum, uploads with `actions/upload-artifact@v4`, and publishes the same signed APK on GitHub Releases. The regular `android.yml` workflow still builds and uploads a debug APK on demand or source pushes. Prefer the release package for phone updates because its development signing identity matches the earlier optimized 0.1.0 installation.

The material below records the earlier 0.1.0 delivery investigation.

---

# Installing Widget Lab without the Codex file viewer

Delivery verified: 16 October 2026. The existing optimized APK was published without rebuilding or changing the application. A separate GitHub debug build was started to verify future build delivery.

## Existing APK verification

Original APK: `/workspace/widget-lab-delivery/WidgetLab-0.1.0-dev.apk`

- Size: **30,139,671 bytes / 28.74 MiB**.
- ZIP CRC/integrity check passed; Android manifest and executable DEX are present.
- Android package: `dev.widgetlab`, version `0.1.0`, minimum API 29, target API 35.
- `apksigner verify` passed: APK Signature Scheme v2, Android development signing identity.
- SHA-256: `7de46041893fd981d14741e5b3ac686eebc65938041234e79d63dd1c76f46fbb`.

A smaller existing optimized release output was already available. It was signed with the **same** development certificate, without recompiling or changing the application:

`/workspace/shared/downloads/WidgetLab-0.1.0-small.apk`

- Size: **3,480,563 bytes / 3.32 MiB** (about nine times smaller).
- ZIP integrity, package/SDK information, alignment and APK signature verified.
- APK Signature Scheme v3; supported by every OS version this app targets (Android 10+).
- Same certificate SHA-256 as the original: `e1eac9a80df5a56dba2f65e2cf23f1c7449dd33fbcd658172a31acdad5c30368`.
- File SHA-256: `20f55a91c2df94b323317e0fb0141105f85df08be6adfc7bbf3b4fd03e2b169e`.

## Verified GitHub download

Repository: https://github.com/okthisisitright/the-lab . Source and workflows were pushed successfully. The owner authorized proceeding with its current public visibility and will manage visibility separately.

- **Direct APK:** https://github.com/okthisisitright/the-lab/releases/download/widget-lab-0.1.0-test/WidgetLab-0.1.0-small.apk
- **Release page:** https://github.com/okthisisitright/the-lab/releases/tag/widget-lab-0.1.0-test
- **Actions artifact:** https://github.com/okthisisitright/the-lab/actions/runs/38839324832/artifacts/12020540843
- **Successful publishing run:** https://github.com/okthisisitright/the-lab/actions/runs/38839324832
- **Repository download fallback:** https://github.com/okthisisitright/the-lab/raw/refs/heads/main/prebuilt/WidgetLab-0.1.0-small.apk

The publishing run verified the committed APK checksum, uploaded **Widget-Lab-existing-APK** with `actions/upload-artifact@v4`, and published the APK plus checksum as release assets. GitHub reports the APK asset as uploaded, with exactly **3,480,563 bytes**. Both the repository download and the release-asset download were fetched independently without the Codex viewer. Their SHA-256 matched the verified local APK. Android `apksigner` also verified the downloaded release asset and its development certificate.

### Install on an Android phone

1. Open the direct APK link above in the phone browser. If the repository becomes private, sign in to GitHub and use the release page if a direct download requests authentication.
2. Open **WidgetLab-0.1.0-small.apk** from Downloads. This is an APK, so there is no ZIP to extract.
3. If prompted, allow **Install unknown apps** for the browser or file manager used to open it, then tap **Install**.
4. Open Widget Lab. Choose a design → **Add to Home Screen**, or long-press the launcher → Widgets → Widget Lab.

The existing APK is development-signed. Physical Vivo and third-party-player acceptance remain unverified; see [the device checklist](DEVICE-TESTING.md).

## Build and download through Actions

The workflow is `.github/workflows/android.yml`, named **Android APK**. It supports manual `workflow_dispatch`, pushes to `main`, and pull requests. It builds `:app:assembleDebug`, immediately uploads `app/build/outputs/apk/debug/app-debug.apk` using **actions/upload-artifact@v4**, and writes the exact artifact URL to the run summary. Test/lint/release tasks follow the upload. Artifacts are retained for 30 days; debug builds need no release-signing secrets.

The SDK setup explicitly installs `platform-tools` rather than the action's obsolete default `tools` package. All workflow files pass actionlint. The initial GitHub run exposed this SDK setup failure; the corrected run is:

https://github.com/okthisisitright/the-lab/actions/runs/38839317610

The corrected run successfully compiled and uploaded its debug APK:

- **Debug artifact:** https://github.com/okthisisitright/the-lab/actions/runs/38839317610/artifacts/12020019952
- **Artifact name:** Widget-Lab-development-APK
- **ZIP contents:** app-debug.apk

The separate tests/lint/release step runs after this upload; its result does not affect availability of this already-uploaded APK.

To start another debug build:

1. Open https://github.com/okthisisitright/the-lab/actions/workflows/android.yml .
2. Select **Run workflow → main → Run workflow**.
3. Open the run; once **Development APK** succeeds, follow its summary link or choose **Artifacts → Widget-Lab-development-APK**. On mobile, enable **Desktop site** if needed.
4. Download the artifact ZIP, extract **app-debug.apk**, open it, and allow installation as described above.

Equivalent CLI commands:

```sh
gh workflow run android.yml --repo okthisisitright/the-lab --ref main
gh run list --repo okthisisitright/the-lab --workflow android.yml --limit 5
gh run download RUN_ID --repo okthisisitright/the-lab --name Widget-Lab-development-APK --dir widgetlab-apk
```

CI generates/caches its own development signing key. That differs from the locally signed existing APK above. If Android refuses an update because of the signature, uninstalling the old app permits installation but deletes its local settings. Stay with the existing release for initial testing; configure a stable private signing key for future updates.

## Republish the existing APK without a build

`.github/workflows/publish-existing-apk.yml`, **Publish existing APK**, is manual-only. Its source APK, checksum and notes live in `prebuilt/`. It uses the GitHub runner's `GITHUB_TOKEN` to upload the artifact and release assets; no developer token or signing key is committed.

```sh
gh workflow run publish-existing-apk.yml --repo okthisisitright/the-lab --ref main
```

This path was used successfully because the local CLI's release-upload host rejected its injected credentials even after Git push/API access worked. No APK was uploaded to an unrelated file host, and no application rebuild was required for this release.
