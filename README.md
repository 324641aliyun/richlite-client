# RichLite 客户端 · 我的世界.cc

公开说明更新：2026-11-02T13:30:00+09:00（Asia/Tokyo）。

[官网与下载说明](https://xn--rhqx00c95nv9a.cc/download) · [RichLite 0.4.0 正式发布](https://github.com/324641aliyun/richlite-client/releases/tag/v0.4.0)

Minecraft Java 1.21.1 / NeoForge 21.1.251 / Java 21。ZIP 及备用下载由 GitHub Releases 和原始公开 CDN 提供，玩家大文件下载不经过官网笔记本。

| 版本 | 使用设备 | 启动方式 | 默认画面 |
| --- | --- | --- | --- |
| [低配导入 ZIP · 216 MiB](https://github.com/324641aliyun/richlite-client/releases/download/v0.4.0/RichLite-0.4.0-Lite-Launcher-import.zip) | 约 8 GB 整机内存的电脑、Android | 导入兼容启动器 | 无光影、无 ReGlass，视距和模拟距离 4 |
| [常规一键 ZIP · 270 MiB](https://github.com/324641aliyun/richlite-client/releases/download/v0.4.0/RichLite-0.4.0-Regular-Windows-macOS-Linux-oneclick.zip) | Windows x64、macOS Intel / Apple Silicon、Linux x86_64 | 完整解压后双击对应入口，无需第三方启动器 | 启用光影与 ReGlass，默认不支持 Android |

两个包保留相同的必需服务器玩法。ZIP 大小不等于首次下载总流量或安装后大小；保持联网并预留至少 5 GB 空间。首次启动会下载游戏和必需模组等资源。

低配版：电脑使用[官方 HMCL](https://hmcl.huangyuhui.net/)，Android 使用[官方 Zalith Launcher 2](https://github.com/ZalithLauncher/ZalithLauncher2/releases)等兼容启动器导入新实例。若选择器只接受 `.mrpack`，将 ZIP 副本改为该后缀。8 GB 整机建议 Java 堆先设为 4096 MiB，为系统和图形运行预留空间。添加 LittleSkin 外置认证 `https://littleskin.cn/api/yggdrasil`，选择自己的角色。Android 的渲染器适配取决于 GPU 和驱动。

常规版：Windows 双击 `Start-Windows.cmd`；macOS 双击 `Start-macOS.command`；Linux 使用 `Start-Linux.desktop` 或 `Start-Linux.sh`。无需预装 Java、Python 或其他游戏启动器；内置程序自动准备对应平台 Java、NeoForge 与资源。首次登录 LittleSkin 并选择已获白名单资格的角色，随后自动加入服务器；会话有效时后续双击直接启动进服。运行同平台 `CreateShortcut-*` 可创建桌面入口；创建后保持原解压目录位置。系统首次要求信任文件时请核对来源并自行确认。

Windows / Linux 保留服主定制 Eclipse 光影和预设；仅 macOS 启动入口选择 Sildur’s Enhanced Default 兼容光影，Eclipse 原文件保留。常规包自动修复缺失模组：优先 Modrinth，失败后切换 GitHub 固定缓存；只安装 SHA512 完全一致的模组，不覆盖玩家设置。备用缓存可能额外下载约 902 MiB。

游戏地址：`s.我的世界.cc`；备用 ASCII：`s.xn--rhqx00c95nv9a.cc`，无需端口。玩家须使用自己的 LittleSkin 账号与白名单角色。官网 OAuth 与游戏登录分别完成。每日维护 03:40–05:30 Asia/Tokyo（UTC+09:00），资源检查通过后恢复。

文件校验：

- 低配 ZIP SHA256：`4f5099c3ce8cbd0b68980de4109aa7f04c7de465f537cc3579da180ba1e4f161`
- 常规 ZIP SHA256：`b4d250d29ef906fc06cd0c9b8921e898600cb2fbdcfd88e111042d0e80c7362d`
- 发布页含同名 `.sha256` 文件、安装步骤、逐文件清单、第三方许可及本服修改源码。包内不含个人账号、登录令牌、玩家存档或 Minecraft 基础文件。第三方内容仍遵守各自许可证。

已完成的验证：Windows 常规包通过真实脚本启动、LittleSkin 加密会话刷新、Eclipse 预设加载及自动公网进服；低配内容在 Windows 4096 MiB Java 堆限制下进服，测试电脑整机为 32 GiB，不冒充 8 GiB 或 Android 实机。官方 HMCL 3.16.4 直接导入低配 ZIP 成功，358 个模组逐 SHA512 匹配；最终仅将客户端内存提示阈值同步为 4096 MiB。两版公开资产的 GitHub SHA256、校验文件和 HTTP 206 下载均核对通过。

[四平台一次性验收记录](https://github.com/324641aliyun/richlite-client/actions/runs/41387957904)：最终常规包文件树在 Windows、Ubuntu、Intel Mac 和 Apple Silicon Mac 上逐文件核对一致，对应启动入口、完整资源准备及凭据存储检查均通过。macOS / Linux 的实际图形渲染、Android 实机与持续游玩仍待设备验证。这些 CI 检查只手动触发，没有定时任务。
