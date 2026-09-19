# Privacy Policy — Workshop Manual Organiser

_Last updated: 2026-09-19_

## Summary

Workshop Manual Organiser stores everything locally on your device. It does not
have a user account, does not transmit your manuals, notes or VINs, and contains
no analytics, advertising or crash-reporting SDKs.

## What the app stores

- The manuals you create: title, category, notes and optional VIN.
- The page files you capture, import or generate — stored in the app's private
  storage (`filesDir/library/`).
- A small JSON index of that library (`filesDir/manuals.json`).

This data never leaves the device unless you explicitly export it and choose a
destination yourself.

## Permissions

| Permission | Why it is requested |
|---|---|
| `INTERNET` | Used **only** if you configure an optional AI endpoint for AI Technical Help. With no endpoint configured, no network request is ever made. |

The app deliberately does **not** request the camera permission. Capturing a page
delegates to the system camera app, which handles its own permission. It does not
request storage or media permissions: file access goes through the system Photo
Picker and the Storage Access Framework, and the selected content is copied into
app-private storage.

## Backup

User documents are excluded from cloud backup by `res/xml/backup_rules.xml` and
`res/xml/data_extraction_rules.xml`. They are included in a direct device-to-device
transfer, which you control.

## Deleting your data

Deleting a manual removes its pages from app-private storage. Uninstalling the app
removes everything the app stored.

## Third parties

None. The app bundles no advertising, analytics or attribution SDK.

## Contact

redrum.studios@example.com
