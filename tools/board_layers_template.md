# Board Layers CSV Template

Use `board_layers_template.csv` as the working table.

Fastest workflow:
1. Open `tools/board_layers_template.csv` in Excel.
2. Modify character data.
3. Save the file directly, or export as CSV.
4. Double-click `tools/sync_board_data.bat`.
5. The script will update `app/src/main/assets/character_boards.json` automatically.

You can also run it manually:
```powershell
python tools/convert_board_layers_csv.py
```

Avatar:
- `avatar_key` should match a drawable resource name exactly, without file extension
- recommended naming: `avatar_<character_id>`, for example `avatar_ed`, `avatar_aiko`
- put avatar files in `app/src/main/res/drawable/`
- recommended format: `png` or `webp`
- if the resource is missing, the app will fall back to the built-in placeholder avatar automatically

Personality:
- CSV column name is `Personality`
- allowed values are `光 / 暗 / 冰 / 火 / 草`
- the app maps them to the following drawable resource names:
  - `personality_light`
  - `personality_dark`
  - `personality_ice`
  - `personality_fire`
  - `personality_grass`
- the same character must keep the same Personality across all 3 tiers

Allowed symbols:
- `.` empty
- `W` white cell
- `P` generic purple cell
- `S` start cell
- `E` end cell
- `A/K/H/D/R` unlockable attack / crit / health / defense / crit resist
- `a/k/h/d/r` purple display-only attack / crit / health / defense / crit resist

Rules:
- each character must provide tier `1`, `2`, `3`
- each character must provide a valid `Personality`
- each layer can use up to 13 rows and 7 columns
- all non-empty rows in the same layer must have the same width
- each layer must contain at least one uppercase attribute symbol
- tier `1` must contain exactly `2` uppercase attribute symbols
- tier `2` must contain exactly `3` uppercase attribute symbols
- tier `3` must contain exactly `4` uppercase attribute symbols
- lowercase purple attribute symbols are display-only and do not participate in unlock, filter, or statistics

If conversion fails:
- check whether the same character has exactly 3 rows
- check whether tier is exactly `1`, `2`, `3`
- check whether all non-empty `row_XX` values in the same layer have the same length
- check whether only supported symbols are used
- check whether uppercase attribute symbol counts are `2 / 3 / 4` for tier `1 / 2 / 3`
