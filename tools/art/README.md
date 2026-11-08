# 美术生成脚本

物品、方块贴图和生物模型都由这里的脚本按代码画出（不是手绘）。改了设计后重新运行对应脚本即可覆盖 `src/main/resources/assets/primordial` 下的文件。

需要 Windows PowerShell 5.1（自带 .NET 的 `System.Drawing` 与 C# 编译器）。在仓库根目录运行，例如：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/art/capsule.ps1 -OutDir src/main/resources/assets/primordial/textures/item
```

| 脚本 | 生成内容 | 参数 |
| --- | --- | --- |
| `capsule.ps1` | 25 个护符（16px 斜卷轴，真理以上带动画与 `.mcmeta`） | `-OutDir <textures/item>`，`-Sheet <预览.png>` |
| `acc.ps1` | 32 个词条饰品（8 帧动画） | `-OutDir`，`-Sheet` |
| `recipes.ps1` | 炼金配方 / 配方碎片 / 神秘配方，各 4 个等级 | `-OutDir`，`-Sheet` |
| `fauna.ps1` | 四种生物的掉落物与萤雷灯、冥灯方块 | `-Assets <assets/primordial>`，`-Sheet` |
| `creatures.ps1` | 毕方、雷萤、鸣金雀、冥灯鱼的 GeckoLib 模型、动画和贴图 | `-Assets <assets/primordial>` |
| `textures.ps1` | 四象群系的方块贴图 | `-OutDir <textures/block>`，`-Sheet` |
| `bossbars.ps1` | 13 个首领的专属像素血条（四周元素、填充、头像框） | `-OutDir <textures/gui/bossbar>`，`-Sheet` |
| `loot.ps1` | 主世界战利品箱新物品：旅人的旧护符、粗铁活力扣（卷轴）、替身草人、残缺的配方页、萤石耳坠、炼金学徒的包裹 | `-OutDir <textures/item>`，`-Sheet` |
| `charms.ps1` | 旧版（平面）护符，已被 `capsule.ps1` 取代，仅作参考 | `-OutDir`，`-Sheet` |

只给 `-Sheet` 时只出一张放大的预览图，不改资源文件。

源文件：
- `charms.cs`：画布 `Cv`（16px 网格、填充、描边、渐变色板）。
- `charm_designs.cs`、`acc_designs.cs`、`recipe_designs.cs`、`fauna_items.cs`、`capsule_designs.cs`：同一个 `partial class Designs` 的各部分；驱动脚本把它们拼在一起编译。
- `creatures.cs`、`creature_designs.cs`：方块式生物模型与贴图展开。

`SinLords.java` 生成暴怒、嫉妒、贪婪、色欲、暴食、懒惰六位罪主及其武器、坐骑的模型、贴图、动画和雕像贴图：在仓库根目录运行 `java tools/art/SinLords.java src/main/resources/assets/primordial`。

`NbtRename.java` 是把结构 `.nbt` 文件中 `angel_charm` 改名为 `primordial` 的一次性工具：`java tools/art/NbtRename.java <目录或文件...>`（递归处理其中的 `.nbt`）。

`bond_book.pl` 从 `AffixBonds.java` 和语言文件生成图鉴里的“羁绊”篇（中英文）：在仓库根目录运行 `perl tools/art/bond_book.pl src/main src/main/resources`。改了羁绊或其文字后重新运行即可。
