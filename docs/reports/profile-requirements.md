# Profile, settings and address requirements (FR-05)

Approved issue [#54](https://github.com/Hooman1996/RagBot-Assistant/issues/54).
This report separates recovered frontend requirements, implemented local preview
behavior and future service proposals. It does not define a banking API or bank
database, and it does not authorize another frontend batch.

## Identity and evidence boundaries

FR-05 covers app settings menu `S-025`, additional information `S-064`, edit
user `S-065`, invitation `S-069`, biometric settings `S-070`, notification
settings `S-071`, financial services `S-072`, storefront services `S-073`,
changelog `S-074`, add address `S-463` and address list `S-464`. The Profile
tab is hosted content of `S-026`.

**Recovered (PF-TAB, PF-ROUTES, PF-PAGES, PF-RULES):**

- **Profile tab:** shows the profile header, the server profile menu, last login and version.
- **Profile router:** opens `S-064`, `S-065`, `S-069`, `S-074`, `S-464`, `S-025`, account management `S-063` and card management `S-126`. A section of the settings key opens card display status `S-130`. Limits, club, referral and signature keys belong to other batches.
- **Settings menu `S-025`:** opens `S-070` (biometric), `S-072` and `S-073`. No static caller of `S-071` was found.
- **`S-065`:** expandable credential sections, each confirmed through an OTP.
- **`S-072`/`S-073`:** facility switches confirmed with the login password.
- **`S-464`:** list, delete confirmation, and add via `S-463`.
- **`S-463`:** title, province, city, postal code and address.

**Designed for mock:** menu rows and order, profile values, facilities,
notification items, versions, provinces/cities, invite code, fixed OTP,
password-not-checked confirmation, and local persistence.

## Per-destination requirements

| Existing node / family | Purpose; user inputs | Required data and fixture source | Prerequisites and navigation result | Supported API/model references | Proposed future mock records/services | Unknowns; effort/confidence |
|---|---|---|---|---|---|---|
| Profile tab in `S-026`; `F-021` | View profile; open menu rows | Profile header, menu | Profile tab handler → rows | Profile read (encrypted) | Profile summary read and menu configuration | Server menu set **Unknown**. 0.5 day / high |
| `S-064`; `F-021` | Complete English names, birth details, marital, education, job, income, tax code | Local profile map | Profile row; save → local state | Identical-info and lookup reads, update write | Additional-info read/update with lookup lists | Country step, age rule and lookup lists **Unknown**. 1 day / medium |
| `S-065`; `F-021` | Change password, username, mobile, email | None stored | Section → OTP (`12345`) → labelled result | Credential change + OTP writes | Credential-change challenge, no secret storage | OTP limits, email verification **Unknown**. 1 day / high rules |
| `S-069`; `F-021` | Copy invite code; share; history | Invite code | Share/history unavailable (history `S-013`, FR-45) | Profile/invite reads | Invite code read | Reward rules belong to FR-25. 0.5 day / high |
| `S-025`; `F-022` | Choose a settings section | Settings menu | Opens `S-070`, `S-072`, `S-073`, `S-130` | Server menu | Settings menu configuration | Section order **Unknown**. 0.5 day / high |
| `S-070`; `F-022` | Enable biometric login | — | Unavailable (platform) | Biometric registration write | None | Device prompts **Unknown**. 0.5 day / medium |
| `S-071`; `F-022` | Toggle notification kinds | Local switch state | Review entry; off asks first | Notification activate/deactivate writes | Notification preference read/write | Caller and permission flow **Unknown**. 0.5 day / medium |
| `S-072`, `S-073`; `F-022`/`F-023` | Toggle financial services (normal / biometric login) | Local switch state | Each change asks for the password (not checked) | Facility enable/disable writes | Facility preference read/write | Storefront meaning beyond the recovered title **Unknown**. 1 day / medium |
| `S-074`; `F-024` | Read version history | Version list | Profile row | Paged version read | Version list read | Item detail **Unknown**. 0.5 day / high |
| `S-464`; `F-062` | List and delete addresses; add | Local address list | Profile row; delete asks first | Address list read, delete write | Saved-address read/delete | Address limits **Unknown**. 0.5 day / high |
| `S-463`; `F-062` | Title, province, city, postal code, address | Local address list | From `S-464`, `S-129` and the wallet address step; save → back | Province/city reads, postal lookup, add write | Address add with lookups | Postal-code lookup behavior **Unknown**. 1 day / high fields |

Requests and responses travel in the encrypted envelope. The shapes above are
caller parse types, not proven HTTP schemas. Proposed services are our designs,
not recovered bank tables.

## Validation and local state

- Password: 8–20 English characters with upper, lower, digit and one of `!@$^&*`. Username: 6–20 English letters, digits or dots, not starting or ending with a dot. Mobile: 11 digits starting `09`. Email: a basic address check. These rules come from the recovered hints.
- English names use Latin letters; the issue date must be a valid Persian date; every picker is required; the tax code is optional.
- Address: title, province, city and address are required; the postal code is 10 digits.
- Local state survives Back and recreation. Secrets are never saved or sent.
