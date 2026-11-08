# 原初之始 · Primordial Genesis

Minecraft 1.20.1（Forge）的炼金冒险模组：在炼金仪上炼成护符与饰品，挑战七罪之主、四象与最初的贤者，深入灵界，追寻“原初”。

> A Forge 1.20.1 alchemy & adventure mod: craft charms and accessories at alchemy altars, face the Seven Sin Lords, the Four Symbols and the First Sage, and journey into the Spirit Realm.

## 内容一览

- **炼金体系**：四阶炼金仪（基础、嬗变、真理、原初），配方需要学习后才能炼成；配方可从战利品、配方残片、神秘配方（附魔台）获得。
- **护符与饰品**：炼金护符栏与炼金饰品栏（由贤者之心开启）。数十种护符分攻击、法术、物理防御、法术防御、攻速、弹射物几条路线；词条饰品之间可组成羁绊。
- **词条生物**：怪物会带上词条（分裂、反伤、限伤、诅咒……），击败后掉落对应的词条材料。
- **首领**：
  - 七罪之主（傲慢、暴怒、嫉妒、贪婪、色欲、暴食、懒惰）、原初之主、最初的贤者；
  - 四象（青龙、白虎、朱雀、玄武），各有自己的群系与象宫；
  - 每个首领都有专属血条，并显示各自的战斗机制。
- **灵界**：独立维度，有浮空岛、圣所与专属生物。
- **探索辅助**：寻灵罗盘指向下一个目标；炼金图鉴（需 Patchouli）记录配方、词条与首领。

## 安装

| 依赖 | 版本 | |
| --- | --- | --- |
| Minecraft | 1.20.1 | 必需 |
| Forge | 47.4.10 | 必需 |
| Curios API | 5.14.1 或更高 | 必需 |
| GeckoLib | 4.4.9 | 必需 |
| JEI | 15.20.0.106 | 可选（配方查看） |
| Patchouli | 1.20.1-85 | 可选（炼金图鉴） |

成品 jar 见本仓库的 Releases。服务端与客户端必须使用同一版本。

## 从源码构建

需要 JDK 17。

```bash
./gradlew build
```

Windows 下也可以运行 `gradlew.bat build`，或双击 `build-jar.bat`（会自动寻找 JDK 17）。构建结果在 `build/libs/`。首次构建需要联网下载 ForgeGradle、Minecraft 与各依赖。

运行游戏测试（约 160 项，覆盖护符、词条、首领、战利品等）：

```bash
./gradlew runGameTestServer -Psmoke
```

## 目录

| 路径 | 内容 |
| --- | --- |
| `src/main` | 模组本体（发行内容） |
| `src/smoke` | 游戏测试，不打包进 jar |
| `src/shot` | 开发用的截图脚本，不打包进 jar |
| `tools/art` | 生成贴图、模型与动画的脚本（护符、饰品、首领模型、血条等），见其中的 README |

## 更新日志

见 [CHANGELOG.md](CHANGELOG.md)（从 1.0.75 起）。

## 许可

本模组以 [GNU LGPL-3.0](LICENSE) 发布（它所补充的 GPL-3.0 全文见 [COPYING](COPYING)）。
