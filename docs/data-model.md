# Data model

## Recipe

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `id` | string | no | UUID. Generated on import when missing. |
| `title` | string | yes | Non-empty. |
| `emoji` | string | yes | Any emoji; the dialog offers a food grid. |
| `note` | string | no | Up to 200 characters. |
| `durationSeconds` | int | yes | Greater than 0. |

## Stored / backup format

Recipes are stored under the `recipes` key of the `little_chef` SharedPreferences, and the exported backup uses the same format:

```json
[
  { "id": "…", "title": "Huevo duro", "emoji": "🥚", "note": "", "durationSeconds": 600 },
  { "id": "…", "title": "Arroz", "emoji": "🍚", "note": "2 tazas de agua por una de arroz.", "durationSeconds": 1200 }
]
```

On first launch the two examples above are written; after that they are regular recipes that can be edited or deleted.

## Timer state

One active timer at a time, stored in the same preferences: `timer_recipe`, `timer_total` (ms), and either `timer_end` (epoch ms, while running) or `timer_remaining` (ms, while paused). `-1` means unset.
