# Pet Dispatch Assets

`tools/pet_dispatch/source/` 是农场宠物派遣计算器的长期维护源目录。

## 目录说明
- `reference/ddl_PetDispatch.py`
  Python 桌面版参考实现，用于行为对照和回归，不参与 Android 运行时。
- `source/data/*.xlsx`
  长期编辑的数据源。
- `source/images/*.png`
  与宠物名一一对应的源图片。
- `sync_pet_dispatch_assets.py`
  从源数据生成 Android 运行时 assets。

## 更新流程
1. 修改 `source/data` 下的 Excel。
2. 新增或替换 `source/images` 下对应名称的 PNG。
3. 运行：

```powershell
python .\tools\pet_dispatch\sync_pet_dispatch_assets.py
```

或：

```powershell
.\tools\pet_dispatch\sync_pet_dispatch_assets.bat
```

4. 提交这两部分文件：
   - `tools/pet_dispatch/source/*`
   - `app/src/main/assets/pet_dispatch/*`

## 生成结果
脚本会生成：
- `app/src/main/assets/pet_dispatch/catalog.json`
- `app/src/main/assets/pet_dispatch/images/*.png`

同步脚本会在失败时直接退出，并校验：
- 宠物名唯一
- 稀有度和技能等级合法
- 每个宠物都有同名 PNG
- 没有多余图片
