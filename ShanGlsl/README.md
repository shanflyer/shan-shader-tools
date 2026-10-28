# Shan GLSL for JetBrains IDEs

为 IntelliJ Platform 2026.2 系列 IDE 编写的 GLSL 编辑插件。以 Daniel Scherzer 的
[GLSL language integration](https://marketplace.visualstudio.com/items?itemName=DanielScherzer.GLSL2022)
为功能与配色参照，采用 IntelliJ Platform 原生扩展实现。

当前版本：`0.2.0`。目标：IntelliJ Platform `2026.2.x / 262.10968–262.*`，Java 25。
已在本机 CLion `2026.2.3 / 262.10968.117` 编译和运行验证。

## IDE 兼容性

最低构建为 `262.10968`，最高为 `262.*`（2026.2 系列），不要求安装 CLion 或 C/C++ 支持插件。
只依赖通用平台和语言模块，可用于同一构建范围内的 IntelliJ IDEA、CLion、PyCharm、WebStorm、PhpStorm、Rider、GoLand、RubyMine、DataGrip、RustRover 等 IntelliJ 平台 IDE。
GLSL 编译器由用户在本地安装或配置。

## 安装

1. 在 IDE 打开 **Settings → Plugins → 齿轮 → Install Plugin from Disk**。
2. 选择本目录下的 `build/distributions/ShanGlsl-0.2.0.zip`，无需解压。
3. 按 IDE 提示重启。
4. 打开 `examples/triangle.vert` 或自己的 shader。

若已安装其他 GLSL 插件，先停用它，避免争用文件类型。
若文件没有高亮，在 **Settings → Editor → File Types → Shan GLSL** 检查扩展名关联，
并移除 Text / C/C++ / 其他 GLSL 类型中重复的关联。

## 功能

| 功能 | 当前行为 |
| --- | --- |
| 语法高亮 | 关键字、内置函数、内置变量、数字、字符串、注释、预处理指令 |
| 默认配色 | 关键字 `#5fafff`，内置函数 `#ffaf5f`，内置变量 `#5fffff`，对齐参考插件 |
| 补全 | 参考插件的 GLSLhelper 词库、当前文件标识符、两组自定义词；Ctrl+Space |
| 折叠 | 多行大括号块、块注释、`#if / #ifdef / #ifndef` 块 |
| 编辑 | 括号匹配、行注释和块注释 |
| 实时错误 | 后台调用 glslang；编辑器波浪线与 CLion Problems → File 列表 |
| 未保存内容 | 主文件与已打开且有未保存修改的 include 均读取编辑器快照 |
| include | 相对路径、绝对路径、额外搜索目录、嵌套 include、错误来源行号 |
| 阶段识别 | 根据后缀确定阶段；`.glsl` 根据阶段特征推断，否则使用设置中的回退阶段 |
| 特殊注释 | `//!` 始终替换该行前部；`//?` 仅在直接检查该文件时替换 |
| 自定义 | 编译器、参数、目标环境、include 搜索目录、超时、两组自定义词、颜色和后缀 |

默认后缀：`glsl vert frag geom comp tesc tese mesh task rgen rint rmiss rahit rchit rcall`。
也识别 `triangle.vert.glsl` 这样的复合文件名。

词库沿用参考项目的分类，额外包含其 Vulkan / ray tracing / subgroup 扩展条目。
补全是词库与当前文件标识符补全，不宣称具备完整的作用域、类型推导或函数重载解析。

## 编译器设置

入口：**Settings → Languages & Frameworks → Shan GLSL**。

Compiler 留空时，依次查找 `VULKAN_SDK` 和 `PATH` 中的 `glslangValidator` / `glslang`。
本机可使用：

```text
C:\VulkanSDK\1.4.363.0\Bin\glslangValidator.exe
```

- **Vulkan**：默认使用 `-V --target-env vulkan1.3`；额外参数可指定其他目标环境。
- **OpenGL**：校验 GLSL 源码，不强制生成 OpenGL SPIR-V。
- **Custom**：仅传入源码文件与自定义参数；自定义编译器需要输出 glslang、NVIDIA 或 GCC 风格的诊断。

支持参数占位符 `{file}`、`{stage}`、`{output}`。没有 `{file}` 时，把临时源码文件作为第一个参数。
路径包含空格时用双引号。保留 Windows 反斜杠，支持 `%VULKAN_SDK%` 与 `${VULKAN_SDK}` 环境变量。
通过进程参数数组调用编译器，不通过 shell。

include 搜索目录每行一个，相对路径以当前项目目录为基准。编译先搜索被引用文件所在目录。
错误发生在 include 中时，在主文件对应 `#include` 行显示提示，消息包含真实 include 路径与行号。
GLSL、`.inc`、`.glslinc`、`.h` 的编辑和磁盘变更会触发已打开 shader 的重新检查。

编译器默认最多运行 5 秒；可以调整到 250–60000 毫秒。超时、取消时结束子进程并清理临时目录。
找不到编译器、启动失败、异常退出会显示配置提示，不会伪装为检查通过。
生成的 SPIR-V 仅用于校验，保存在临时目录中；不会更新引擎的 shader 产物。

### 特殊注释示例

```glsl
//! #version 460
#include "/runtime/common.glsl" //! #include "common.glsl"
```

检查时等价于：

```glsl
#version 460
#include "common.glsl"
```

在公共文件中使用 `//? #version 460`，直接打开检查时启用，作为 include 时保持注释。
替换保持行数，编译器错误可以映射回原始文件。

## 与参考插件的差异与边界

- 补全弹窗、Problems 列表、工具提示、字体和编辑器背景由 CLion 绘制。GLSL 分类颜色对齐，
  IDE 整体界面不会与 Visual Studio 像素级一致。
- 使用外部编译器路径，不提供参考插件的独立 OpenGL 显卡驱动编译线程。
  驱动特有的报错文本与 glslang 的结果可能不同。
- Problems → File 展示当前编辑文件的诊断；没有添加全项目批量编译扫描器。
- `.glsl` 阶段推断是启发式规则；存在歧义时请使用明确的阶段后缀。
  光追的 miss / closest-hit 等阶段尤其应使用 `.rmiss` / `.rchit`。
- include 支持字面量路径；不实现宏形式的 `#include HEADER`。
  正常 include guard 可保护递归引用；无保护的递归引用会报错。
- 每个源文件最多 1 MiB，展开累计最多 4 MiB，嵌套深度最多 32。
  自行使用 `#line` 的生成式 shader 会改变编译器行号语义。
- 本版兼容范围仅声明为 `262.*`，不对尚未验证的 CLion 大版本放开兼容标记。

## 构建与验证

插件是独立 Java 工程，不参与引擎 CMake 构建；引擎源码和 CMakeLists 无需修改。
首选构建脚本直接使用本地 CLion 的编译 API 和 JDK，无需下载 Gradle / IDE SDK：

```powershell
.\build.ps1
# 其他安装位置：
.\build.ps1 -ClionHome 'C:\Program Files\JetBrains\CLion 2026.2.3'
```

脚本自动编译、执行核心检查并打包。`-SkipTests` 仅用于本地排错。
另提供常规 Gradle 构建文件，可用 Gradle 9 与 JDK 25 执行：

```powershell
gradle buildPlugin -PclionHome='C:/Program Files/JetBrains/CLion 2026.2.3'
```

### 独立 CLion 沙盒

```powershell
.\sandbox.ps1 -SmokeTest
.\sandbox.ps1
```

沙盒的配置、插件、缓存和日志均位于 `build/sandbox`。
沙盒需要有效的 CLion 授权；脚本不分发或自动复制授权。
测试用插件位于 `src/smoke`，仅放入沙盒，**不包含在交付安装包中**。
退出可视化沙盒之后再运行无界面测试，避免同一配置目录被两个 IDE 占用。

验证覆盖词法边界、注释替换、include 展开与映射、阶段推断、参数处理、
真实 Vulkan/OpenGL 编译、未保存内容、超时、取消及进程清理。
沙盒检查实际插件加载、各阶段文件关联、PSI、高亮注册、补全、折叠和设置 UI。
视觉验收检查编辑器配色、红色波浪线与 Problems 列表。

## 源码组织

```text
src/main/java/dev/shanflyer/glsl/
    language/       语言、文件类型、词法、轻量 PSI、词库
    editor/         高亮、颜色设置、补全、折叠、括号与注释
    diagnostics/    源码转换、阶段、编译进程、诊断映射、依赖变更
    settings/       持久设置与配置界面
src/main/resources/ 插件注册、图标、词库
src/test/           核心及外部进程验证
src/smoke/          真实 CLion 沙盒验证插件
examples/           可打开验证的 shader
```

新实现采用 MIT 许可证；第三方词库与参考来源见 `THIRD-PARTY-NOTICES.txt`。
