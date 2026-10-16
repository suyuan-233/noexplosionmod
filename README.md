# NoExplosionMod

一个 Fabric 模组：把苦力怕的爆炸换成烟花，并让试炼刷怪笼与宝库可以采集。

**Minecraft 26.3 · Fabric Loader ≥ 0.19.5 · Java 25**

## 功能

### 1. 苦力怕的爆炸 → 原地释放烟花

苦力怕引爆时不再产生爆炸，而是在它所在的位置爆开一发彩色烟花。

- **不破坏方块** —— 整次爆炸被替换；烟花自身也不会改动方块（已核对原版 `FireworkRocketEntity#explode` 的实现，它只做粒子广播与实体伤害）。
- **原地爆开、不升空** —— 烟花实体加入世界后立即引爆，飞行 tick 数为 0。
- **保留原版烟花的伤害** —— 这是刻意保留的行为。若要让烟花完全无伤害，需要另外处理 `FireworkRocketEntity#dealExplosionDamage`。
- 使用原版烟花模型：`SMALL_BALL` 形状，每次随机 3 种鲜艳颜色、白色收尾，带拖尾与闪烁。形状与配色可在 `CreeperExplosionToFireworkMixin` 顶部调整。

### 2. 精准采集的下界合金镐可采集试炼刷怪笼与宝库

这两个方块在原版里硬度 50、本来就能挖，但战利品表是空的 —— 挖掉什么都不掉。

- **条件**：手持**下界合金镐**且带**精准采集**。
- **不祥宝库掉落的仍是不祥宝库** —— 保留 `ominous` 与 `facing` 方块状态，同时保留方块实体数据（战利品表、钥匙等），放回去还是原来那个宝库。
- 其余情况维持原版行为（方块照常被破坏，但什么也不掉）。创造模式不会掉落，这由原版 `playerDestroy` 的调用条件决定。

### 3. 宝库可以被同一个玩家反复开启

原版宝库对每个玩家只发一次奖励，这份记录就是 `rewarded_players`。它在**两处**各自生效，本模组两处都处理了：

1. `VaultBlockEntity.Server#tryInsertKey` 中的 `hasRewardedPlayer`；
2. `VaultSharedData#updateConnectedPlayersWithinRange` 中**绕过前者直接读集合**的那处判断 —— 它会把已领取过的玩家从"附近玩家"里剔除，导致宝库被判为 `INACTIVE`，而 `VaultBlock#useItemOn` 对非 `ACTIVE` 状态根本不理会钥匙。

只堵第 1 处是不够的。钥匙依旧照常消耗，这是原版行为。

### 4. 试炼刷怪笼与宝库的挖掘速度与远古残骸一致

这两个方块**不在 `minecraft:mineable/pickaxe` 标签里**，所以镐子的速度加成完全不生效，用下界合金镐挖也按空手速度计算，非常慢。本模组改为直接用远古残骸的方块状态来计算挖掘进度，于是硬度、工具速度加成、"工具不对时更慢"这些细节全部与远古残骸一致。

## 环境要求

| 项目 | 版本 |
| --- | --- |
| Minecraft | 26.3 |
| Fabric Loader | ≥ 0.19.5 |
| Java | 25 |
| Fabric API | 必需 |

## 构建

```bash
./gradlew build
```

产物在 `build/libs/`：

- `noexplosionmod-<版本>.jar` —— 模组本体
- `noexplosionmod-<版本>-sources.jar` —— 源码包，**不要**放进 mods

版本号由 `gradle.properties` 中的 `version=` 决定，产物名会自动跟随。

## 安装

把 `noexplosionmod-<版本>.jar` 放进游戏实例的 `mods` 文件夹。该实例需要装好 Fabric Loader（对应 26.3）、Fabric API，并使用 Java 25。

## 实现说明

所有改动都在 `src/main/java/com/suyuan/noexplosion/mixin/` 下：

| Mixin | 目标 | 作用 |
| --- | --- | --- |
| `CreeperExplosionToFireworkMixin` | `Creeper` | 把爆炸换成原地烟花 |
| `FireworkRocketEntityInvoker` | `FireworkRocketEntity` | `@Invoker`，用于立即引爆烟花 |
| `SilkTouchHarvestMixin` | `Block` | 精准采集掉落，并保留宝库的状态与数据 |
| `VaultRepeatOpenMixin` | `VaultServerData` | 解除 `rewarded_players` 的玩家限制 |
| `AncientDebrisMiningSpeedMixin` | `BlockBehaviour` | 挖掘速度对齐远古残骸 |

## 许可

CC0-1.0
