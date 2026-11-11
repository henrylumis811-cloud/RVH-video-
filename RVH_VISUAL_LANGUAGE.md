# RVH visual language — restrained dark theme

The RVH family shares a visual foundation, not identical screens. Each app may keep its own information architecture and personality while using the same principles:

- Deep charcoal backgrounds and subtly stepped charcoal surfaces.
- Soft-white primary text and neutral-grey secondary text.
- A muted blue-cyan family accent (`#76AEB7`) for controls, selected states and progress.
- A pale cool-cyan companion (`#B4CDD1`) for secondary accent text.
- A deeper blue-teal (`#456F78`) for subdued tracks and supporting accents.
- Bright, high-saturation colour is exceptional: reserve it for a critical state or brief feedback, not routine decoration.
- Prefer low-alpha borders and fills over persistent neon outlines and glows.

## RVH Video implementation

The central Compose palette lives in `app/src/main/java/com/rvh/video/ui/theme/Color.kt`. Existing `RvhGold*` names remain as compatibility aliases; they now resolve to the restrained RVH blue-cyan palette. Movie-card outlines and artwork-loading shimmer were also brought into the same restrained treatment.

This is a foundation pass, not a wholesale redesign: player architecture, gesture behavior, app navigation, artwork, and the established RVH Video icon are intentionally left intact.
