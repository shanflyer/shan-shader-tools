# Shan HLSL for CLion

版本 0.1.2，适配 Windows x64 上的 CLion 2026.2.x（262.*）。
独立于 Shan GLSL，可同时安装。引擎源码和 CMake 无需修改。

## 安装

CLion → Settings → Plugins → 齿轮 → Install Plugin from Disk，选择
`build/distributions/ShanHlsl-0.1.2.zip`，按提示重启。
安装包包含语言服务器及 .NET 运行时，使用插件不需要另外安装 .NET SDK、DXC 或 Vulkan SDK。
如已有其他 HLSL 插件，先停用，避免文件关联冲突。
默认支持 `.hlsl`、`.hlsli`、`.fx`、`.fxh`。

## 实现的功能

- 基础高亮和 HlslTools 语义分类：结构、函数、参数、字段、变量、宏及语义。
- 基于原 HlslTools 引擎的语义补全，包括结构成员和内置函数。
- 函数参数提示、悬停类型说明、定义跳转。
- 文件符号 / Structure，以及当前文件引用高亮。
- 实时语法与语义错误，编辑器波浪线和 Problems 列表；分析未保存的编辑内容。
- 调用原 HlslTools 格式化器执行 Reformat Code。
- 预处理排除区域置灰、include 与预处理宏配置。
- 本地代码折叠、括号匹配、行注释及块注释。

常用动作：Ctrl+Space 补全，Ctrl+P 参数提示，Ctrl+B 定义跳转，
Ctrl+Q 文档，Ctrl+Alt+L 格式化，Alt+7 文件结构。
具体快捷键以所选 CLion Keymap 为准。

设置入口：Languages & Frameworks → Shan HLSL。
可以停用语言服务、停用语义配色、指定其他语言服务器程序及启动参数。
颜色入口：Editor → Color Scheme → Shan HLSL。
文件关联入口：Editor → File Types → Shan HLSL。

## 宏与 include 配置

沿用参考插件的 `shadertoolsconfig.json` 格式，例如：

```json
{
    "root": true,
    "hlsl.preprocessorDefinitions": { "USE_LIGHTING": 1 },
    "hlsl.additionalIncludeDirectories": [".", "include"],
    "hlsl.virtualDirectoryMappings": { "/Project": "." }
}
```

配置文件从 shader 所在目录向父目录查找，更近的配置优先。
相对 include 目录按配置文件位置解析。修改配置或磁盘 include 后，关闭再打开受影响的 shader。
包含文件的内容读取行为沿用上游实现，修改公共头文件后应先保存。
`examples` 中提供了顶点/像素 shader、纹理采样、计算 shader、include 和错误示例。

## 与 Visual Studio 参考插件的边界

语义分析使用同一 HlslTools 项目的语言服务器，固定源码版本为
`3cb3e76a8a5a35daf6f8e6fa5c32da7facf7175a`。
CLion 的菜单、弹窗、字体、颜色主题和导航界面由 IDE 绘制，不能做到 Visual Studio 像素级一致。
导航栏通过 CLion 的 Structure / 符号导航呈现，没有复制 VS 的语法树可视化窗口。
符号搜索范围沿用语言服务器已打开文档的工作区，不承诺全项目离线索引。
新 HLSL 语法的支持范围取决于此版本的 HlslTools；实时分析结果不替代最终 DXC 编译。
本版只验证 Windows x64 / CLion 262.*。

## 构建

需要本地 CLion 2026.2.3、.NET 10 SDK、Python 3 和首次构建的网络连接。

```powershell
.\build.ps1
```

默认执行：下载并校验固定 SHA256 的上游源码 → 应用明确的后端补丁 →
发布自带运行时的语言服务器 → 编译 Java 前端 → 运行前端与 LSP 测试 → 打包。
`-SkipBackend` 可复用已经编译的服务器；`-SkipTests` 仅用于本地排错。
其他 CLion 安装位置可通过 `-ClionHome` 指定。

```powershell
.\sandbox.ps1 -SmokeTest
.\sandbox.ps1
```

沙盒位于 `build/sandbox`，需要有效的 CLion 授权。脚本不复制或分发授权。
`-ConfigDirectory` 可指定已有的独立开发配置目录；不要与运行中的 IDE 共用配置目录。
测试插件只在沙盒中使用，不包含在发布包内。

## 源码与许可

`language/` 负责文件和词法；`editor/` 负责本地编辑能力；`lsp/` 负责 CLion 语言服务与分类桥接；
`settings/` 负责配置；`backend/extensions/` 为原引擎增加格式化和分类协议。
`backend/prepare.ps1` 固定上游提交、校验下载哈希并记录全部构建补丁。

参考项目：https://github.com/tgjones/HlslTools

沿用 Apache 2.0 许可，保留上游版权及第三方声明，见 LICENSE 和 THIRD-PARTY-NOTICES.txt。
