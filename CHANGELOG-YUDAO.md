# 芋道兼容版变更记录

## 2.1.0-preview-20261120 · 2026-11-20

[Git tag](https://github.com/yudaocode/yudao-agentscope-java/tree/v2.1.0-preview-20261120) · [Maven Central BOM](https://central.sonatype.com/artifact/cn.iocoder.agentscope/agentscope-bom/2.1.0-preview-20261120)

- 修复 Alpine BusyBox tar 在非 root 沙箱恢复工作区时丢失组和其他用户权限。解包子进程使用专用 umask，应用进程的 umask 保持原样。
- JDK 8、17、25 用真实非 root 容器验证冷恢复后目录、文本文件和脚本权限，并在恢复后的容器里成功执行脚本。

适用于 Spring Boot 3／4 和 JDK 17／25。

上游提交 `21be74a8a`，兼容分支源码 `a9aa17bef`。

公开制品沿用上一版范围。开发中的 Durable 模块继续从源码安装。
