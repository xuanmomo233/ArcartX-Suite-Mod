# ArcartX-Suite-Mod

ArcartX Suite 客户端辅助 mod，为 ArcartX 自定义 UI 提供 TACZ 枪械和 Apotheosis 神化词条的 tooltip 桥接。

## 功能

- **TACZ 枪属性 tooltip 注入**：将 TACZ 枪械的伤害、穿甲、爆头倍率、移速惩罚、弹药容量等属性文本行注入 `ItemTooltipEvent`，使 ArcartX 自定义 UI 能显示这些信息。
- **Apotheosis affix 支持**：Apotheosis 的 affix 描述行已在 `ItemTooltipEvent` 中，本 mod 确保它们被正确传递到 ArcartX 渲染流程。

## 依赖

| 依赖 | 类型 | 版本 |
|---|---|---|
| Minecraft | 必需 | 1.20.1 |
| Forge | 必需 | 47.3.0+ |
| ArcartX | 必需 | 2.5.36+ |
| TACZ | 可选 | 1.1.8+ |
| Apotheosis | 可选 | 7.4.8+ |
| Apotheosis Modern Ragnarok | 可选 | 7.0.0+ |

## 构建

```bash
gradle build
```

需要 JDK 17。CI 构建见 `.github/workflows/build.yml`。

## 发布

推送 `v*` 格式的 tag 即可触发自动构建和 Release 发布：

```bash
git tag v1.0.0
git push origin v1.0.0
```
