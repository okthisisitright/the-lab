# Installing Widget Lab without the Codex file viewer

Delivery investigation: 15 October 2026. No Android source changes or new app build were required.

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

The smaller file and shared-downloads path may avoid the large-file timeout, but successful local verification does not prove that the Codex download UI will transfer it. GitHub delivery below bypasses that UI entirely.

## GitHub status

Target repository: https://github.com/okthisisitright/the-lab . GitHub authentication is working and repository write access is available. The repository is currently public; the owner explicitly instructed proceeding with upload and will change its visibility later. Repository creation and visibility changes are not allowed by this connection, but those are no longer prerequisites for delivery.

The workflow is committed and validated with actionlint. Exact release and Actions URLs must be confirmed after their respective uploads complete; creating a local APK or committing a workflow does not itself publish an artifact.

## Build and download through Actions

The workflow is `.github/workflows/android.yml`, named **Android APK**. It supports manual `workflow_dispatch` and pushes to `main`.

It now runs `:app:assembleDebug`, immediately uploads `app/build/outputs/apk/debug/app-debug.apk` with **actions/upload-artifact@v4**, and writes the actual artifact URL to the run summary. Test/lint/release tasks follow the upload, so a later release/check failure does not prevent an already-built debug APK being delivered. Artifacts are retained for 30 days. No release-signing secrets are required for the debug artifact.

After connecting the intended repository and authorizing GitHub:

```sh
# Only if origin has not already been configured; replace okthisisitright/the-lab.
git remote add origin https://github.com/okthisisitright/the-lab.git
git push -u origin main
gh workflow run android.yml --repo okthisisitright/the-lab --ref main
gh run list --repo okthisisitright/the-lab --workflow android.yml --limit 5
```

A push to main also starts the workflow; a manual dispatch is optional if a push run is already active. If the remote already contains commits, reconcile the histories normally before pushing; do not force-push over it.

Phone/browser steps:

1. Sign in to GitHub and open the repository.
2. Open **Actions → Android APK → Run workflow → main → Run workflow**.
3. Open the new run. The APK is available once the **Development APK** step completes, even if later checks are still running.
4. Follow **Download Widget-Lab-development-APK** in the run summary, or scroll to **Artifacts → Widget-Lab-development-APK**. On mobile, enable the browser’s **Desktop site** if Actions/Artifacts are hidden.
5. Download the artifact ZIP and extract **app-debug.apk** with the Android Files app or another ZIP extractor.
6. Open the APK. If prompted, allow **Install unknown apps** for the browser/file manager you are using, then choose **Install**.
7. Open Widget Lab. Choose a design → **Add to Home Screen**, or use your launcher’s Widgets picker.

Artifact location after an actual run:

- Run page: `https://github.com/okthisisitright/the-lab/actions/runs/RUN_ID`
- Artifact name: **Widget-Lab-development-APK**
- File inside its ZIP: **app-debug.apk**
- The upload step provides the exact artifact download-page URL in the run summary. Its artifact ID does not exist before a successful upload, so an exact URL cannot be supplied in advance.

Terminal download, once the run/artifact exists:

```sh
gh run download RUN_ID --repo okthisisitright/the-lab --name Widget-Lab-development-APK --dir widgetlab-apk
```

The locally signed APK and a newly generated CI development key may differ. If Android refuses to update an existing Widget Lab installation because of its signature, uninstall the old installation before installing the new one (this removes its local settings), or configure a stable private signing key for later updates.

## Deliver this existing APK without any rebuild

With a connected private repository and working GitHub authorization, the already-signed small APK can be uploaded as a private GitHub Release asset:

```sh
gh release create widget-lab-0.1.0-test \
  /workspace/shared/downloads/WidgetLab-0.1.0-small.apk \
  --repo okthisisitright/the-lab \
  --title 'Widget Lab 0.1.0 device test' \
  --notes 'Existing optimized APK, development-signed. No new application build.'
```

Then use the repository’s **Releases → Widget Lab 0.1.0 device test → Assets → WidgetLab-0.1.0-small.apk** from a signed-in phone browser. This downloads an APK directly, without the Codex viewer or an artifact ZIP. This command has **not** been executed in this session until the source is pushed and the release asset upload is verified. No APK was sent to an unrelated public file host.
