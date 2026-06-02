# 战斗力计算器资源

这里维护实验室“战斗力计算器Beta”使用的源表和同步脚本。

## 目录

- `source/data/`：源工作簿副本
- `selection_rules.json`：角色系数表筛选规则
- `sync_battle_power_assets.py`：将工作簿同步为应用资产 JSON

## 更新流程

1. 用新版文档替换 `source/data/` 里的工作簿。
2. 如果角色系数表的筛选边界有变化，更新 `selection_rules.json`。
3. 运行 `python .\tools\battle_power\sync_battle_power_assets.py`。
4. 检查 `app/src/main/assets/battle_power/character_coefficients.json` 的 diff 是否符合预期。

当前默认规则：

- 只保留 `UID < 10088`
- 额外排除 `UID = 10054`
