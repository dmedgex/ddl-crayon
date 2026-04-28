from __future__ import annotations

import csv
import json
import sys
from collections import defaultdict
from pathlib import Path

ALLOWED_SYMBOLS = set('.WPSEAKHDRakhdr')
REQUIRED_COLUMNS = [
    'character_id',
    'character_name',
    'avatar_key',
    'Personality',
    'sort_order',
    'tier',
    *[f'row_{index:02d}' for index in range(1, 14)],
    'note',
]
SUPPORTED_ENCODINGS = ('utf-8-sig', 'utf-8', 'gb18030', 'cp936')
EXPECTED_UNLOCKABLE_COUNTS = {
    '1': 2,
    '2': 3,
    '3': 4,
}
PERSONALITY_MAPPING = {
    '光': 'LIGHT',
    '暗': 'DARK',
    '冰': 'ICE',
    '火': 'FIRE',
    '草': 'GRASS',
}


def read_text_with_fallback(csv_path: Path) -> tuple[str, str]:
    last_error: Exception | None = None
    for encoding in SUPPORTED_ENCODINGS:
        try:
            return csv_path.read_text(encoding=encoding), encoding
        except UnicodeDecodeError as error:
            last_error = error
    raise ValueError(
        f'无法读取文件 {csv_path.name}。请使用 UTF-8、GB18030 或 Excel 默认 ANSI/GBK 编码保存。'
    ) from last_error


def read_rows(csv_path: Path) -> list[dict[str, str]]:
    raw_text, encoding = read_text_with_fallback(csv_path)
    reader = csv.DictReader(raw_text.splitlines())
    missing = [column for column in REQUIRED_COLUMNS if column not in (reader.fieldnames or [])]
    if missing:
        raise ValueError(f'缺少列：{", ".join(missing)}')

    rows = []
    for row_index, row in enumerate(reader, start=2):
        normalized = {key: (value or '').strip() for key, value in row.items() if key is not None}
        if not normalized['character_id']:
            continue
        normalized['_row_number'] = str(row_index)
        normalized['_source_encoding'] = encoding
        rows.append(normalized)
    return rows


def normalize_layer_rows(record: dict[str, str]) -> list[str]:
    label = row_label(record)
    rows = [record[f'row_{index:02d}'] for index in range(1, 14)]
    normalized = [row for row in rows if row]
    if not normalized:
        raise ValueError(f'{label} 没有填写任何棋盘行数据')
    if len(normalized) > 13:
        raise ValueError(f'{label} 超过 13 行')

    widths = {len(row) for row in normalized}
    if len(widths) != 1:
        detail = ', '.join(f'row_{index:02d}={len(value)}' for index, value in enumerate(rows, start=1) if value)
        raise ValueError(f'{label} 的棋盘每行长度不一致：{detail}')

    width = widths.pop()
    if width < 1 or width > 7:
        raise ValueError(f'{label} 超过 7 列，当前列数为 {width}')

    invalid_symbols = sorted({symbol for row in normalized for symbol in row if symbol not in ALLOWED_SYMBOLS})
    if invalid_symbols:
        raise ValueError(f'{label} 包含非法字符：{" ".join(invalid_symbols)}')

    unlockable_count = sum(1 for row in normalized for symbol in row if symbol in 'AKHDR')
    if unlockable_count == 0:
        raise ValueError(f'{label} 至少要包含 1 个大写属性格（A/K/H/D/R）')
    expected_count = EXPECTED_UNLOCKABLE_COUNTS.get(record['tier'])
    if expected_count is not None and unlockable_count != expected_count:
        raise ValueError(
            f'{label} 的大写属性格数量不正确：'
            f'第 {record["tier"]} 层必须为 {expected_count} 个，当前为 {unlockable_count} 个'
        )

    return normalized


def build_payload(records: list[dict[str, str]]) -> dict[str, list[dict[str, object]]]:
    grouped: dict[str, list[dict[str, str]]] = defaultdict(list)
    for record in records:
        grouped[record['character_id']].append(record)

    characters = []
    errors: list[str] = []

    for character_id, layers in grouped.items():
        try:
            tier_values = [layer['tier'] for layer in layers]
            if len(set(tier_values)) != len(tier_values):
                raise ValueError(f'角色 {character_id} 存在重复层级')
            if set(tier_values) != {'1', '2', '3'}:
                raise ValueError(f'角色 {character_id} 必须完整提供 1、2、3 三层')

            first = layers[0]
            name = first['character_name']
            avatar_key = first['avatar_key'] or f'avatar_{character_id}'
            personality = normalize_personality(first['Personality'], f'角色 {character_id}')
            sort_order = int(first['sort_order'])
            for layer in layers[1:]:
                if layer['character_name'] != name:
                    raise ValueError(f'角色 {character_id} 的 character_name 不一致')
                if (layer['avatar_key'] or f'avatar_{character_id}') != avatar_key:
                    raise ValueError(f'角色 {character_id} 的 avatar_key 不一致')
                if normalize_personality(layer['Personality'], f'角色 {character_id}') != personality:
                    raise ValueError(f'角色 {character_id} 的 Personality 不一致')
                if int(layer['sort_order']) != sort_order:
                    raise ValueError(f'角色 {character_id} 的 sort_order 不一致')

            characters.append(
                {
                    'id': character_id,
                    'name': name,
                    'avatarKey': avatar_key,
                    'personality': personality,
                    'sortOrder': sort_order,
                    'layers': [
                        {
                            'tier': int(layer['tier']),
                            'rows': normalize_layer_rows(layer),
                        }
                        for layer in sorted(layers, key=lambda item: int(item['tier']))
                    ],
                }
            )
        except ValueError as error:
            errors.append(str(error))

    if errors:
        raise ValueError('CSV 校验失败：\n- ' + '\n- '.join(errors))

    characters.sort(key=lambda item: (int(item['sortOrder']), item['id']))
    return {'characters': characters}


def row_label(record: dict[str, str]) -> str:
    return f"第 {record['_row_number']} 行（角色 {record['character_id']} / 第 {record['tier']} 层）"


def normalize_personality(value: str, label: str) -> str:
    normalized = value.strip()
    if not normalized:
        raise ValueError(f'{label} 缺少 Personality 配置')
    if normalized not in PERSONALITY_MAPPING:
        raise ValueError(f'{label} 的 Personality 非法：{normalized}，只支持 光 / 暗 / 冰 / 火 / 草')
    return PERSONALITY_MAPPING[normalized]


def main() -> int:
    workspace_root = Path(__file__).resolve().parent.parent
    input_path = Path(sys.argv[1]) if len(sys.argv) > 1 else workspace_root / 'tools' / 'board_layers_template.csv'
    output_path = Path(sys.argv[2]) if len(sys.argv) > 2 else workspace_root / 'app' / 'src' / 'main' / 'assets' / 'character_boards.json'

    if not input_path.exists():
        raise SystemExit(f'找不到输入文件：{input_path}')

    try:
        records = read_rows(input_path)
        if not records:
            raise ValueError('表格里没有可导出的角色数据。')
        payload = build_payload(records)
    except ValueError as error:
        raise SystemExit(str(error)) from error

    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(f'已生成：{output_path}')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
