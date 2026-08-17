---
name: minimal-zdon-ui
description: Minimalist Material 3 design system for the Zdon Android app. Use when editing any @Composable UI, theme tokens, or adding screens/components. Enforces neutral monochrome surfaces, a single blue accent, flat rows with hairline dividers, and quiet outlined metadata chips.
---

# Zdon Minimal UI Design System

Apply these rules to every Compose change in this repo. The design was refined
in core/designsystem + the feature modules; do not regress toward filled-card
or colorful-chrome patterns.

## 1. Color usage
- Surfaces are neutral monochrome: light = pure white surface on `#16161A` dark.
  Only `primary` (blue `#00639B` light / `#9ECDFF` dark) is an accent.
- `secondary`/`tertiary` roles are mapped to neutral grays — never use them for
  decoration, chips, or icons.
- Color is reserved for semantics: `error` for failures, `primary` only for the
  active/interactive element (running status, primary buttons, selected format).
- Completed/neutral states use `onSurfaceVariant`, not a color.
- Prefer `MaterialTheme.colorScheme.*`; never hardcode hex in components.
- Dynamic color support lives in `Theme.kt` (opt-in via Settings, default off).

## 2. Type
- `ZdonTypography` in core/designsystem: system font, standard-weight headings,
  no letter-spacing. Do not reintroduce bold headings or tracked uppercase text.

## 3. Layout & components
- Lists use flat rows with `HorizontalDivider(color = outlineVariant)`, never
  filled Card containers. See DownloadRow/HistoryRow/filter lists.
- Spacing and radius come from `ZdonDimensions` (4/8/12/16/24/32dp grid;
  thumbnail radius 8dp, cards 12dp). No arbitrary inline dp values.
- `ZdonInfoChip` defaults to an outlined, transparent style. Filled chips
  (error/selected) are allowed only for real emphasis.
- Metadata that isn't critical at a glance is a muted `labelSmall`/`bodySmall`
  text line joined with " · " — not a chip cloud.
- Buttons: one contained `Button` for the primary action, `OutlinedButton`/
  text for the rest. Utility actions like "paste" belong to a trailing icon,
  not a full-width button.
- Alerts (warnings, analysis errors) are slim single-row notice bars
  (Surface + icon + title + description [+ text action]), not filled cards
  with nested empty-state layouts.
- No shadows/elevation beyond M3 defaults; no gradients.

## 4. Where things live
- Theme tokens: `core/designsystem/.../theme/` (Color.kt, Type.kt, Shape.kt,
  Dimension.kt, Theme.kt).
- Shared components: `core/designsystem/.../component/` (ZdonInfoChip,
  ZdonThumbnail, ZdonProgressBar, ZdonEmptyState, ...).
- Screens: `feature/home`, `feature/downloads`, `feature/history`,
  `feature/settings`; shared shell/navigation in `:app`.

## 5. Review checklist
- [ ] No filled Card in a list where a flat row + divider is possible.
- [ ] No decorative secondary/tertiary color usage.
- [ ] No chip cloud; secondary details are muted text.
- [ ] Only one contained button per action group.
- [ ] Uses `ZdonDimensions` tokens rather than new inline dp values.
- [ ] Dark theme looks intentional, not an afterthought (check both schemes).