# API 102 迁移与新版国际版 B 站适配 TODO

更新日期：2026-09-06。

本文记录迁移和适配的实施计划及进度。2026-09-06：API 102 源码迁移已完成，用户反馈实机测试无明显问题（本任务未独立复核设备日志）。P3 静态调查已核对；P4 已按 6.4.0 反编译证据落地代码与本地测试，设备主链仍待验收。复选框只代表该项写明的验证层级，不代表整体功能验收。

## 1. 目标、范围与完成定义

### 1.1 两个需求

1. 将现有传统 Xposed 模块完整迁移到 libxposed API 102，包括入口、Hook、配置通信、日志、激活状态检测及发布元数据。
2. 适配用户提供的新版国际版 B 站 APKs，打通发评捕获、字段转换、启动反诈、评论检查、历史记录和评论定位，并核查现有国际版附加功能。

“API 102”指 libxposed 的模块接口版本，不是 Android API 等级，也不能直接等同于某个 LSPosed 产品版本。设备上的框架必须实际支持所使用的接口与远程配置能力。

### 1.2 范围约定

- 本次规划以纯 API 102 模块为目标；不默认保留 legacy/modern 双入口，也不新增 API 100/101 兼容层。
- 保留模块应用包名、现有账号、历史评论、待检查记录和第三方 Intent 接入契约。
- 国内版已有功能随共用 Hook 一并迁移并回归；新版国际版是新增适配重点。
- 手动发评、查询、历史记录等独立 App 功能在未安装框架时仍应可用。
- 油猴脚本、已停用的弹幕检查、UI 全面重做、Native Hook、自动热重载不属于本次默认交付范围。
- API 仓库、APK 样本、反编译目录属于参考材料；不直接修改第三方源码来迁就主工程，不把它们作为主 App 源集。
- 不把迁移中的局部顺手修改扩大为整个仓库重构。与主链直接相关的异常分支缺口须处理或明确阻断验收。

### 1.3 完成定义

- [x] 主工程不再依赖或引用旧 `de.robv.android.xposed` API，停用源码也不会造成编译残留。
- [x] APK 只保留现代入口和正确的 `META-INF/xposed` 配置，API 类不被打进模块 DEX。
- [ ] API 102 框架下加载、包/进程过滤、Hook 注册、回调命中均有证据。
- [ ] App 侧配置、Hook 侧配置、激活状态在首次安装、升级和框架断连时行为明确。
- [ ] 指定国际版样本的根评论、楼中楼、含图片评论主链通过设备验证；不存在重复弹窗、重复记录或宿主发送异常。
- [ ] 国内版回归与独立 App 回归完成；缺少设备或样本的项目保持待验收。
- [ ] 新包版本、哈希、签名、框架版本、B 站版本及验证记录可以对应，不能用构建成功代替适配成功。

## 2. 当前基线与本地资料

### 2.1 路径

| 标识 | 完整路径 | 用途 |
| --- | --- | --- |
| 仓库根目录 | `D:\Users\Andrea-TB\Desktop\biliSendCommAntifraud-fix` | 文档与 Git 操作起点 |
| Android 工程 | `D:\Users\Andrea-TB\Desktop\biliSendCommAntifraud-fix\biliSendCommAntifraud` | Gradle Wrapper 所在目录 |
| API 源码 | `D:\Users\Andrea-TB\Desktop\biliSendCommAntifraud-fix\BiliSource\api` | API 102 接口事实来源，独立 Git 仓库 |
| 国际版样本 | `D:\Users\Andrea-TB\Desktop\biliSendCommAntifraud-fix\BiliSource\bilibili_6.4.0.apks` | 新版国际版适配输入 |

用户消息中的 `bilibili\_6.4.0.apks` 按实际存在的文件名 `bilibili_6.4.0.apks` 处理，不创建名为 `bilibili` 的额外子目录。

以下源码表中，`S/` 表示 Android 工程下的 `app/src/main/java/icu/freedomIntrovert/biliSendCommAntifraud/`；路径缩写仅用于本文阅读。

### 2.2 已确认事实

- [x] 主仓库基线提交：`b03290a`，提交日期 2025-05-12，版本 `6.3.5` / `635`。
- [x] 现有工程是单 `app` 模块，Java 8，AGP 8.1.1，Gradle Wrapper 8.0，compileSdk 35、targetSdk 33、minSdk 21。
- [x] 旧 API 依赖为 `app/libs/XposedBridgeApi-54.jar`；入口为 `assets/xposed_init` → `xposed.XposedInit`。
- [x] 旧 scope 为 `tv.danmaku.bili`、`com.bilibili.app.in`；新版样本的实际包名仍须读取 Manifest 确认。
- [x] API 本地提交：`79b75b49255a257d38f67bce2e928649dc2fc6c9`，提交日期 2026-08-27；读取时无工作区修改。
- [x] 本地 API 的 `LIB_API` 为 `API_102`，构建文件版本为 `102.0.0`，源码库声明 minSdk 26、Java 17、compileSdk 37、AGP 9.2.1。
- [x] 本地 `XposedModule` 继承 `XposedInterfaceWrapper`，实现 `XposedModuleInterface`；初始化使用生命周期回调，不照搬旧构造器注入模板。
- [x] 本地 `ExceptionMode` **存在** `DEFAULT`、`PROTECTIVE`、`PASSTHROUGH`。若旧参考资料声称没有 `PROTECTIVE`，以本地接口声明为准。
- [x] APKs SHA-256：`47034c8bc2994ef75c941996dacc20b7c1c405f49fe2c7b8157139d5109b2597`。
- [x] APKs 内包含 `base.apk`、`split_config.arm64_v8a.apk`、`split_config.xxxhdpi.apk`。
- [x] 当前没有发现工程的单元测试或 instrumentation 测试源集。
- [x] 编写计划前主仓库只有 `.serena/` 和 `BiliSource/` 未跟踪，没有已跟踪文件修改。

规划初稿时**未确认**：样本 Manifest 中的包名、versionCode、真实 versionName、签名、发评方法、响应字段、Cookie 来源、运行进程，以及 API/service 发布制品是否与本地源码完全一致。文件名不能替代这些证据。初稿阶段没有解包反编译、构建、连接设备或执行线上发评；后续进展以第 13 节执行记录为准。

### 2.3 事实来源索引

| 内容 | 本地文件与入口 |
| --- | --- |
| API 集成、R8 | `BiliSource/api/README.md` |
| API 工具链和版本 | `BiliSource/api/api/build.gradle.kts`、`BiliSource/api/gradle/libs.versions.toml` |
| 生命周期、ClassLoader、进程 | `BiliSource/api/api/src/main/java/io/github/libxposed/api/XposedModuleInterface.java` |
| Chain、异常策略、Remote Preferences | `BiliSource/api/api/src/main/java/io/github/libxposed/api/XposedInterface.java`，其中异常枚举在第 366 行附近 |
| 现代元数据、scope | `BiliSource/api/api/src/main/java/io/github/libxposed/api/package-info.java` |
| 现有构建 | `biliSendCommAntifraud/app/build.gradle`、根构建文件与 Wrapper 配置 |
| 现有 Hook 路由 | `S/xposed/XposedInit.java:24` |
| 发评捕获 | `S/xposed/hooks/PostCommentHook.java:50`，网络回调 Hook 在第 80 行附近 |
| Intent 接收与检查分发 | `S/ByXposedLaunchedActivity.java:32`、`:85` |
| 检查引擎 | `S/async/commentcheck/CommentCheckTask.java:40`、`:133` |
| 旧跨进程配置 | `S/Config.java:26`、`:99`，以及 `S/xposed/InAppXConfig.java`、`InHookXConfig.java`、`XConfig.java` |
| 评论定位 | `S/CommentLocator.java`、`S/xposed/hooks/IntentTransferStationHook.java` |

行号是编写本文时的定位提示，实施后按符号重新确认。

## 3. 实施顺序和交付物

| 阶段 | 依赖 | 交付物 | 通过条件 |
| --- | --- | --- | --- |
| P0 基线与样本核实 | 无 | 环境表、样本身份、现有功能清单 | 输入明确，参考材料与业务源码区分清楚 |
| P1 API 集成和基础设施 | P0 | 现代入口、元数据、Hook 支撑、配置通信 | 可构建、可被框架识别、配置可读 |
| P2 旧 Hook 全量迁移 | P1 | 纯 API 102 的国内版和旧国际版逻辑 | 旧 API 零残留，主调用链语义保持 |
| P3 新国际版静态适配研究 | P0，可与 P1/P2 分时推进 | 版本适配表、字段映射、调用链证据 | 新样本 Hook 点有源码/DEX 依据 |
| P4 新国际版实现 | P2 + P3 | 新版适配器、Intent/定位/Cookie 衔接 | 端到端数据契约完整、异常可回退 |
| P5 集成与设备验收 | P4 | 本地测试结果、设备矩阵与日志 | 两项需求分别验收、国内版无回归 |
| P6 交付整理 | P5 | 文档、版本、制品元数据、回退说明 | 结论与实际验证范围一致 |

建议按阶段提交最小变更，迁移与国际版业务差异尽量分开，便于定位回归。本计划不要求额外创建多个工作区或同时运行多个代理。

## 4. P0：基线、工具链与资料准备

- [ ] 再次检查主仓库和 API 子仓库状态，记录 HEAD；保留既有未提交内容。
- [ ] 记录已安装 JDK、Android SDK、Gradle 缓存、可用 build-tools、JADX/ADB 路径。
- [ ] 在 Android 工程目录尝试一次当前 Debug 构建；如果基线构建失败，记录首个真实错误及其归属，不先大规模升级依赖。
- [ ] 记录设备型号、Android 版本、框架名称/版本/支持 API、目标包安装版本及签名。
- [ ] 建立国内版基准用例；没有国内版样本或设备时明确列为待回归，不推断新版国际版通过即国内版通过。
- [ ] 为 APK 解包、反编译和设备证据选择独立目录，防止覆盖 API 仓库或原始 APKs；生成物不进入业务源集。
- [ ] 核实 `.gitignore` 对后续分析目录、日志、设备数据、构建输出的覆盖；调整时只增加必要规则。
- [ ] 保存升级前账号、历史记录和配置的可恢复副本；不在报告或 Git 中存储 Cookie、令牌和用户私有数据。

## 5. P1：API 102 工程和基础设施

### 5.1 依赖与构建兼容

- [x] 移除 `compileOnly files('libs/XposedBridgeApi-54.jar')`，在旧引用清理完成后移除不再使用的 JAR。
- [x] 优先验证 `compileOnly 'io.github.libxposed:api:102.0.0'` 与本地源码的签名一致性，记录解析到的制品版本和来源。
- [ ] 若必须从本地 API 源码构建，使用明确的本地制品或独立构建流程，记录提交/hash；不把 API Java 源码复制进主 App。
- [ ] 核查 API AAR 元数据、class 文件版本和消费者工具链要求；API 仓库使用 SDK 37/AGP 9.2.1，不代表消费它的 App 必须无条件复制整套工具链。
- [ ] 采用兼容 API 102 的 JDK，计划基线为 JDK 17；按实际依赖要求调整 Java 编译选项、AGP/Wrapper/SDK，避免无依据全量升级。
- [ ] 明确最低系统支持：计划默认纯现代模块至少 Android 8.0/API 26；最终 minSdk 取 API/service 制品与实际实现要求的最大值，并在发布说明中说明旧 Android 支持变化。保留 Android 5–7 独立 App 支持需另行设计，不默认承诺。
- [x] 不顺带提高 targetSdk；如依赖或发布要求确实需要提高，补齐对应后台启动、通知、前台服务、包可见性回归。
- [x] 核实并引入匹配的 `libxposed/service` App 侧依赖，查实际源码/制品确认绑定、配置和状态接口；本地 `BiliSource/api` 不包含 service 实现，不能把两者混为一谈。
- [x] 保留现有应用包名及发布签名连续性，不改变数据库身份。

### 5.2 现代入口与打包元数据

- [x] 改造 `XposedInit` 为 `XposedModule` 子类，提供符合本地 API 的可实例化入口。
- [x] 新增 `app/src/main/resources/META-INF/xposed/java_init.list`，只注册实际入口。
- [x] 新增同目录的 `module.prop`，计划值如下：

```properties
minApiVersion=102
targetApiVersion=102
staticScope=true
exceptionMode=protective
autoHotReload=false
```

- [x] 新增 `scope.list`：保留国内版包名；国际版包名以样本 Manifest 为准。若仍是 `com.bilibili.app.in`，保留该项；不因名称相似加入无关 B 站产品。
- [x] 去掉 `assets/xposed_init` 及旧 `xposedmodule/xposedsharedprefs/xposeddescription/xposedminversion/xposedscope` 元数据，检查无其他消费者后清理旧 scope 数组。
- [x] 使用标准 `android:label`、`android:description` 表示模块名称与描述。
- [x] 根据本地 API README 更新 R8 入口保留和资源内容适配规则，核查 Release APK 中入口仍可加载。
- [x] 先保留与本次无关的现有 R8 策略；不把迁移扩大为整体混淆优化。
- [x] APK 中不包含 API 接口实现、不保留旧入口；无 Native Hook 需求，不创建空的 native 清单。

### 5.3 生命周期、ClassLoader 与 Hook 支撑

- [x] `onModuleLoaded()` 保存进程名并记录模块版本、框架信息；该阶段不解析目标 B 站业务类。
- [x] `onPackageReady()` 按包名、进程、ClassLoader 路由，以 `getClassLoader()` 查找宿主类。
- [ ] 依据样本及运行证据确定需要 Hook 的进程，不沿用“所有进程都装一次”的逻辑。
- [x] 注意 `onPackageReady()` 时 Application 尚待创建；需要 Context 时从适当生命周期获取，不假设入口已经有 Activity/Application 实例。
- [ ] 替换 `systemContext()` 中反射 `ActivityThread` 的隐式依赖；确定宿主版本读取时机、来源和长整型版本号处理。
- [x] `BaseHook`、`HookStater` 接收明确的 API 实例/Hook 上下文，包含版本、包、进程、ClassLoader 和配置访问，不依赖旧静态 `XposedBridge`。
- [x] 按 Hook 功能保存注册状态和 `HookHandle`；只有注册成功才标记完成，防止部分失败后错误地跳过必要 Hook。
- [x] 给独立功能隔离安装异常；附加功能失败不阻断发评捕获。
- [x] 精确替换 `findAndHookMethod`、`XC_MethodHook`、`XC_MethodReplacement`：反射得到 Method/Constructor → `hook(executable).intercept(...)`。
- [x] 观测型回调只执行一次 `chain.proceed()` 并返回原结果；原调用异常照常传播，模块后处理失败不得重发网络请求或吞掉宿主异常。
- [x] 修改参数时复制 `chain.getArgs()` 为数组再 `proceed(newArgs)`；该列表不可变，Chain 不跨线程、不缓存。
- [ ] 原本替换返回值的 Hook 逐个核对副作用；对需要保留原调用的 getter 先取得原结果，再做必要转换。
- [ ] 反射工具覆盖父类成员、重载、基本类型/装箱类型、可访问性和空值；缓存按 ClassLoader 隔离，不仅按类名缓存。
- [ ] 若使用 Invoker，显式核对是否调用完整链或原方法，避免递归进入自身 Hook。
- [x] `XB` 和安装日志迁到现代日志接口；App 普通运行路径不要求加载框架提供的类。
- [x] 本期关闭自动 Hot Reload；配置同步独立实现。后续要启用热重载时另补线程、监听器、旧 Hook 和对象引用清理方案。

### 5.4 配置通信和旧数据迁移

现有 `Config` 同时服务 App 业务和 Hook，并包含旧 `MODE_WORLD_READABLE` / `XSharedPreferences` 逻辑；另有 `InAppXConfig/InHookXConfig/XConfig` 路径，不能只搜索当前入口。

- [x] 搜索全部配置读写调用，区分正在使用与遗留未使用的配置封装。
- [x] App 私有设置保留私有存储；Hook 只读取明确需要的远程配置组。
- [x] 至少梳理 `use_client_cookie`、`post_picture_hook`、`fuck_fold_pictures_hook` 的共享需求、默认值与现有开关行为。
- [x] Cookie、账号数据库、历史评论、申诉状态不发布到共享配置；旧 `cookie/deputy_cookie` 键尤其不能跟随 `getAll()` 整体复制。
- [x] App 侧建立一次注册的 service 连接管理，处理首次未绑定、迟到绑定、断连、重连与多个框架回调。
- [x] Hook 使用 API 的 `getRemotePreferences(group)`；核查只读行为和框架能力缺失时的降级。
- [x] App 未绑定时仍保存用户设置，绑定后发布最新有效值；不得用启动默认值覆盖用户刚修改的值。
- [ ] 明确单一配置主来源、发布方向和配置版本；迁移标志只能在必要数据成功保存后写入，失败可重试。
- [x] 如采用监听器，保持强引用并更新线程安全快照；如只在目标重启后应用，UI 明确说明，不声称即时生效。
- [x] 对安装时启用的 Hook，确定动态关闭时采用回调内开关还是 unhook；支持再次打开且不重复注册。
- [ ] 核实老版本 `xposedsharedprefs` 重定向路径中的配置如何安全迁出。升级前导出或框架支持的读取方式优先；不能假设新 App 能直接读 `/data/misc/` 旧文件。
- [ ] 迁移正常私有 `config` 与旧重定向配置时规定冲突优先级、缺失字段默认值和重复执行行为。
- [ ] 检查历史 `bili_anti_fraud_config/hook_picture_select` 是否仍有消费者；有则显式映射，无则在确认后清理封装。
- [x] 删除对 `SharedPreferences` 实现私有字段、构造器和世界可读模式的常态运行依赖。
- [ ] 验证首次安装、覆盖升级、框架不存在、service 断连、远程能力不支持、配置缺失等场景。

### 5.5 模块激活状态

- [x] 用 service 连接与实际框架信息替代 `XPCheckHook` 修改 `MainActivity.isXposedEnabled()` 的自 Hook 做法。
- [x] UI 至少区分“框架连接中/不可用”“框架可用”“目标 scope 是否启用”；不能把绑定成功直接显示为目标 Hook 已命中。
- [x] 检查 `MainActivity` 中依赖 `isXposedEnabled()` 的开关初始化，支持状态异步更新。
- [x] 不再为了自测把模块自己的包名加入 scope。
- [ ] 验证无框架时 App 首页、账号管理、手动检查及历史记录不会因缺少 API 类而崩溃。

## 6. P2：现有 Hook 迁移清单

| 现有文件（相对 S） | 当前职责/依赖 | 迁移任务与验收点 |
| --- | --- | --- |
| `xposed/XposedInit.java` | 国内/国际/自身包分发 | 现代生命周期与明确进程路由，移除旧激活自 Hook |
| `xposed/BaseHook.java`、`HookStater.java` | Hook 安装抽象及异常捕获 | 传递现代 API、独立注册、失败可诊断 |
| `xposed/XB.java` | 旧框架日志 | 现代日志适配，独立 App 不加载框架依赖 |
| `xposed/hooks/PostCommentHook.java` | Activity 跟踪、BiliCall 响应、字段提取、Cookie、Intent | 保持原调用语义，Activity 有效性、去重、线程与异常隔离 |
| `xposed/hooks/PostCommentHookByMaster.java` | 国内版固定类/方法/Cookie 路径 | 保留既有映射并验证国内版；避免国际版变动污染国内版 |
| `xposed/hooks/PostCommentHookByGlobal.java` | 旧国际版动态返回类型定位、`a/h` 方法、Invisible | 先迁移 API；新版签名由 P3/P4 明确替换或分版本处理 |
| `xposed/hooks/ShowInvisibleCommentHook.java` | getInvisible/getLocation 与底层字段 | 保留原始 Invisible 状态供标记使用，不能从已强制 false 的 getter 再判断 |
| `xposed/hooks/FuckFoldPicturesHook.java` | getFoldPictures 返回值 | 开关生效；方法缺失只停用该功能 |
| `xposed/hooks/PostPictureHook.java` | 国内版相册替换拍照、结果处理 | 参数复制、结果调用顺序、权限与 URI 处理回归；不默认向国际版扩展 |
| `xposed/hooks/IntentTransferStationHook.java` | MainActivityV2 onCreate/onNewIntent 中转 | 生命周期、重入控制、目标版本路由匹配 |
| `xposed/hooks/Utils.java` | 图片反射、启动 App、字段验证 | 移除旧反射 API、数字类型归一、主线程跳转、日志脱敏 |
| `xposed/hooks/XPCheckHook.java` | 修改自身激活结果 | service 状态替代后删除 |
| `xposed/hooks/PostDanmakuHook.java` | 已停用，但仍在 Java 源集中 | 不重新启用；移除编译依赖或清理无效类，避免阻止 legacy JAR 删除 |
| `Config.java`、`xposed/*XConfig.java` | 旧跨进程配置 | 按 P1 配置任务逐项迁移 |

- [ ] 把表内每一行标记为已迁移/已清理/不适用，并附具体提交和验证结果。
- [ ] 清理大段过期注释时只处理迁移范围内内容，不误删仍需要的旧版兼容路径。
- [x] 对 Activity 跟踪采用不长期保留已销毁 Activity 的方式，明确前后台切换和宿主切屏时的跳转行为。
- [x] 保留“发评成功后检查”的语义，不在发送前自动再发一次评论。
- [ ] 核查 sourceId 网络回退：目前辅助方法中的 `FutureTask.run()` 仍在调用线程同步执行；不得把方法名称当成异步保障。
- [ ] 把磁盘 Cookie 读取、网络补充和 UI 调度从高频网络 Hook 中合理分离；只提取实际需要且可安全传递的数据。
- [x] 全源集扫描 `de.robv.android.xposed`、`XC_MethodHook`、`XposedHelpers`、`XSharedPreferences`、`MODE_WORLD_READABLE`，逐一确认零运行依赖。

## 7. P3：新版国际版 APKs 静态研究

### 7.1 样本身份与拆包

- [ ] 重新核对 APKs hash，解出 base 和必要 splits，保存各 APK hash。
- [ ] 从 Manifest/包分析工具读取真实包名、versionName、versionCode、minSdk、targetSdk、Application、Launcher、进程和动态加载配置。
- [ ] 核查 base/splits 签名与包身份一致性，记录 ABI；本样本带 arm64 split，测试设备须兼容。
- [ ] 用 JADX 分析 base 与带代码的必要 split；保留真实混淆类名，首次不使用会造成 Hook 名称误认的重命名输出。
- [ ] 反编译失败的方法回查 DEX/smali，不以 JADX 伪代码中的缺失分支作最终结论。
- [ ] 建立独立的新版适配证据文件，例如后续新增 `docs/compat/international-6.4.0.md`；本文不声称该文件已存在。

### 7.2 发评调用链

- [ ] 从评论发送按钮/提交业务或 `/x/v2/reply/add` 字符串定位请求构造、网络调用、成功/失败回调和 UI 更新链。
- [ ] 核实新版本使用 Retrofit/BiliCall 同步 execute、异步回调、协程、gRPC 或其他路径；旧 `execute()` 只是候选，不是已确认 Hook 点。
- [ ] 检查 `BiliCommentApiService.postComment(Map)`、`GeneralResponse`、`BiliCommentAddResult` 是否存在及实际运行时名称。
- [ ] 如仍从接口返回类型推导调用类，确认返回类型是否为接口/包装类型、真正实现类与方法所在父类，不能直接假设返回类型可 Hook。
- [ ] 记录候选方法完整签名：声明类、方法名、参数、返回值、静态/实例、继承关系、所在 DEX、加载进程及 ClassLoader 来源。
- [ ] 成功条件核实 code/action/reply 的真实语义，区分已发出、精选待审、发布失败、网络失败、敏感内容拒绝和响应缺字段。
- [ ] 检查一条评论是否经历多个回调或重试，选择一个权威交接点，避免 execute 与 callback 同时重复捕获。
- [ ] 如证据表明架构已变化，新增明确的新版适配路径；不堆叠多个猜测类名/方法名逐个碰运气。
- [ ] 按稳定业务特征、字段结构和签名验证匹配；缓存绑定包版本/样本身份，多个候选同时匹配时不盲选第一个。

### 7.3 数据字段映射

| 反诈字段 | 现有契约 | 新样本必须确认 |
| --- | --- | --- |
| `action` | int，0 检查、2 恢复检查 | 模块 Intent action 与宿主响应 action 不是同一枚举，不能直接混用 |
| `oid` | long，评论区对象 ID | 视频/专栏/动态来源；全链保留 64 位 |
| `type` | int，现有 1/12/11/17 | 新版实际类型；未知类型处理 |
| `rpid` | long，评论 ID | 最终服务器 ID，不能用临时发送 ID |
| `root` / `parent` | long，根楼与父评论 | 根评论与楼中楼层级，不互换 |
| `source_id` | String，BV/cv/动态 ID | type 11 的动态 ID 与 oid 区别；无法获取时的可恢复行为 |
| `comment_text` | String | 回复前缀、表情、换行、Unicode 与服务器文本对应关系 |
| `ctime` | long，秒 | App 当前会乘 1000，防止毫秒再乘一次 |
| `uid` | long，作者 ID | 来自最终响应作者，与可用 Cookie 账号匹配 |
| `pictures` | 可空 JSON 字符串 | 列表结构、URL、宽高/大小实际类型，保持现有存储格式 |
| `cookies` | 可空 String 列表 | 只有用户启用时提取；多来源与多账号匹配规则 |

- [ ] 对每个字段填写新版本源对象/字段/getter、类型、空值条件和静态证据位置。
- [ ] 图片数值不能继续假设全部是 `Double`；按 `Number` 等真实契约归一，缺失图片元数据不丢弃整个评论。
- [ ] 显式覆盖大于 32 位的 ID，避免经过 int、浮点数或不精确 JSON 转换。
- [ ] 先确认现有存储和 Intent 能容纳新样本；只有无法表达时才扩展 DTO/数据库，并提供迁移。

### 7.4 Cookie、评论定位与附加功能

- [ ] 核查登录 Cookie 的真实来源，确认 WebView 数据目录后缀、进程与 SQLite schema；旧硬编码目录不是新版事实。
- [ ] 确认相关字段是否可直接读取、是否存在加密值、WAL/锁占用/空库；使用只读方式，失败可回退手动账号。
- [ ] 不把“不具备 SESSDATA/buvid3 字段”直接等同于所有登录态失效；先核对现有 API 验证要求及样本。
- [ ] 验证多 Cookie 候选：单个无效/UID 不匹配时是否应继续查找；只使用作者 UID 匹配且验证有效的账号。
- [ ] 核实视频、专栏、动态详情与评论详情的 Activity/deep link/参数；覆盖冷启动及 `onNewIntent` 热启动。
- [ ] 核查 `ReplyControl.getInvisible/getFoldPictures` 和 `getLocation` 是否仍存在、位于实际渲染路径及受何种配置控制。
- [ ] 给缺失功能写清楚“目标不支持”“尚未找到”“待设备确认”，不能统称适配完成。
- [ ] 国际版此前未接入相册替代拍照功能，本期保持范围；如需新增，独立提出并补专门样本。

## 8. P4：新版国际版实现与端到端衔接

### 8.1 适配结构

- [x] 共用层负责现代 Hook 注册、日志、配置、去重、数据交接；国内/国际版本层负责宿主签名、字段、页面路由和 Cookie 来源。
- [x] 根据 P3 结果决定扩展 `PostCommentHookByGlobal` 或增加新版适配类；避免把所有版本分支塞进公共 `PostCommentHook`。
- [x] 保留仍有证据支持的旧国际版逻辑；兼容范围写到明确版本，不能把只测 6.4.0 写成支持所有国际版。
- [x] 输出轻量的功能支持结果：发评捕获、获取 Cookie、评论定位、Invisible、图片展开分别判定。
- [x] 注册失败输出包/进程/版本、目标签名及原因；不因一个缺失功能中断所有功能。

### 8.2 捕获、去重与启动反诈

- [x] 把宿主响应转换为稳定的评论事件，再构建现有 Intent Extras；不跨 App 传宿主私有 Parcelable/类实例。
- [x] 以包/账号/评论 ID 等稳定身份做有界去重，不按评论文本去重；相同文本的不同评论都应记录。
- [x] 记录一次事件从捕获、字段转换、交接、入待检查、检查结果、入历史的状态；区分“已经发起交接”和“App 已接收”。
- [x] App 缺失、Activity 已销毁、后台启动受限、连续发送等情况下保住宿主流程；失败不能默默标记成已完成。
- [x] UI 跳转在主线程执行；异步后处理保留必要快照，不持有 Chain 或长期保留宿主响应对象。
- [x] `ByXposedLaunchedActivity` 验证字段存在、类型、合理值和支持的 action；错误提示只展示脱敏摘要。
- [x] 第三方显式 Intent 接口保持兼容，不直接加上会排除第三方客户端的签名级权限。
- [x] 默认等待规则保持现状：普通评论 5000 ms，图片额外 15000 ms，即默认含图共 20000 ms；对应用户自定义值继续生效。
- [x] 检查失败后待检查记录可恢复，成功后历史/待检查状态一致；恢复 action 不再次插入重复记录。

### 8.3 主链相关的已知缺口

以下是现有源码中已确认的衔接问题，需在迁移时纳入验证，不代表已在新样本复现。

- [x] `PostCommentHook` 可发出 `ACTION_SAVE_CONTAIN_SENSITIVE_CONTENT=3`，但 `ByXposedLaunchedActivity.checkIntentExtras()` 与分发只接受 0/2。补齐 action 3 的校验、必要数据、保存和提示，或明确收敛该路径并同步文档；不继续发送接收端必拒绝的 action。
- [x] 同一敏感内容分支循环读取请求表单 `name/value` 时未传索引；先确认新版真实请求体，再按准确方法签名提取，不能照抄到现代 API。
- [x] 该分支把 oid 解析为 int；统一到 long，并验证拒绝评论没有有效 rpid/uid 时的记录规则，不伪造正常成功事件。
- [x] `Utils.picturesObjToString()` 当前硬转 `Double`，按 P3 的实际数值类型完善转换。
- [x] `CommentLocator` 与 `IntentTransferStationHook` 当前使用固定 Activity/参数；新版按静态签名和设备实际定位结果更新。
- [x] 现有 `LoggerInterceptor` 会写完整请求头和表单；在新增适配调试前对 Cookie、认证、CSRF 等脱敏，捕获日志默认不输出评论全文和原始 Cookie 数据库内容。

### 8.4 检查和存储

- [x] 尽量保持 `CommentCheckTask` / `CommentManipulator` 的业务判断，适配器把新版数据归一到现有模型。
- [ ] 若游客列表、回复定位接口发生变化，用响应和调用链证明后再调整；框架迁移不能作为随意重写状态分类的依据。
- [ ] 游客/登录双视角、根楼/楼中楼、Invisible 与疑似审核分别检查。
- [ ] 网络失败、限流、验证码、Cookie 失效、权限问题显示为检查失败或待复查，不能仅因没找到就断言秒删。
- [ ] 图片缓存失败、sourceId 补充失败等与评论状态独立处理，避免无关错误导致全部记录丢失。
- [ ] 首次检查、手动复查、后台监控共用字段语义，更新同一个 rpid 的历史记录。
- [x] 未发生数据模型变化时不提升数据库版本；确需迁移时覆盖旧库升级及数据保留用例。

## 9. P5：验证计划

### 9.1 验证层级

| 层级 | 能证明什么 | 不能据此声称什么 |
| --- | --- | --- |
| S：静态 | API 签名、元数据、候选调用链、分支和字段映射有依据 | 实际设备会加载或 Hook 会命中 |
| L：本地 | 编译、必要测试、APK 结构和签名检查通过 | 新国际版实际可用 |
| D：设备 | 指定 ROM/框架/宿主版本下完整操作与日志一致 | 未测版本、账号和设备都兼容 |

### 9.2 本地检查

- [x] 运行必要的 Debug 编译和相关单元测试；对纯数据转换、ID/时间转换、去重、配置迁移、分支判定补针对性测试。
- [x] 使用脱敏/合成响应样本，不把真实用户 Cookie 和完整私有评论保存为测试 fixture。
- [ ] 验证 Hook 封装的原调用次数与异常传播，尤其是 `proceed()` 后处理失败时不能重复调用原方法。
- [x] 验证 Release 编译/R8 后的现代入口、资源清单、scope、module.prop 和 API 类排除情况。
- [x] 检查 Manifest 合并结果、安装 minSdk、versionCode、签名和产物 hash。
- [x] 只对相关 lint/构建问题处理；不通过重新生成整个 lint baseline 隐藏迁移新增问题。

在 `D:\Users\Andrea-TB\Desktop\biliSendCommAntifraud-fix\biliSendCommAntifraud` 下，按实施进度执行，例如：

```powershell
.\gradlew.bat :app:assembleDebug
# 添加相关测试后执行；没有测试源集时 NO-SOURCE 不算测试通过。
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug :app:assembleRelease
```

命令是后续执行入口，不表示本轮已运行。不要求每个小修改都重跑全部检查；发生相关改动或新失败时再补对应验证。

### 9.3 设备矩阵

每项记录结果、模块 hash、框架版本、B 站包名/versionCode、复现步骤、日志时间窗；未测项填“待设备验证”，不勾选。

| 编号 | 场景 | 预期 |
| --- | --- | --- |
| D01 | 新国际版 + 支持 API 102 的框架，冷启动 | 正确包/进程加载，必要 Hook 注册成功 |
| D02 | 目标无关进程、非目标包 | 不安装业务 Hook，无多余弹窗 |
| D03 | 无框架/未启用模块启动反诈 | 独立 App 功能可用，状态提示准确 |
| D04 | 国内版文本根评论 | 发送正常，单次捕获/检查/记录 |
| D05 | 新国际版视频根评论 | oid/rpid/uid/ctime/sourceId 正确 |
| D06 | 新国际版楼中楼、多级回复 | root/parent 正确，不误当根楼 |
| D07 | 动态 11/17 与专栏 | 按样本支持范围检查 ID 和定位；缺入口写不适用理由 |
| D08 | 图片、表情、换行、Unicode、长评论 | 无类型转换崩溃，文本图片数据完整，等待时间正确 |
| D09 | 成功、精选/审核、敏感拒绝、网络失败 | 事件类型正确，失败不写成正常成功 |
| D10 | 连续多评、同内容不同 rpid、重复回调 | 不错抑制不同评论，也不重复记录同一事件 |
| D11 | 切屏、返回、Activity 销毁、后台发送结果 | 不闪退、不打开错误页面，有可理解的失败/恢复路径 |
| D12 | 手动 Cookie、自动获取关/开、多个 Cookie | 作者 UID 匹配；读取失败不影响宿主发评 |
| D13 | 登录失效/切账号/WebView 未初始化 | 提示具体原因，保留待检查入口，不误报秒删 |
| D14 | 游客可见、登录仅自己可见、删除、Invisible | 模块分类与 API/人工观察证据对应 |
| D15 | 疑似审核状态后续变化 | 复查和监控更新正确，无法复现的状态用例保留待验收 |
| D16 | 5000 ms 等待、含图额外等待、后台等待 | 进度、通知、恢复 action、待检查记录一致 |
| D17 | API 限流/验证码/离线/异常 JSON | 检查错误可恢复，不误写状态 |
| D18 | 从历史记录定位根评论/回复 | 国内/国际、冷启动/热启动均落到正确评论 |
| D19 | Invisible 显示、图片展开及开关 | 各功能单独验证，缺方法只影响自身 |
| D20 | 国内版相册替代拍照 | 结果和取消流程正常，权限/URI 不回归 |
| D21 | 旧版覆盖升级、配置迁移重试 | 账号、历史、待检查、开关值保留，迁移不重复覆盖 |
| D22 | service 迟到、断连、重连、缺少远程能力 | App 不崩溃，配置不倒退，提示准确 |
| D23 | 第三方 Intent、错误字段/action | 正常接入兼容，异常输入被拒绝且日志脱敏 |
| D24 | 正式构建的 Release 包 | R8 后加载和主链再次冒烟通过 |

### 9.4 日志和验收证据

- [x] 日志事件至少覆盖 `module_loaded`、`route_skipped`、`hook_registered`、`hook_failed`、`comment_captured`、`dispatch_failed`、`check_started`、`check_finished`。
- [ ] 用脱敏事件标识关联同一条评论，不把 Cookie 或评论全文当关联键。
- [ ] 发现问题从“模块是否加载 → scope/进程 → 方法是否注册 → 是否命中 → 字段是否齐 → 是否交接 → 检查 API → 存储”找到首个断点。
- [ ] 未确认内联问题前不盲目 deoptimize，不通过额外 Hook 掩盖前序断点。
- [ ] 真实发评、删评、申诉、转发等测试限于用户指定账号和测试评论区，并明确测试产生的内容；本计划不是自动批量操作线上账号的指令。
- [ ] 不能稳定制造的审核状态，用静态/本地测试补覆盖但仍标记设备未复现，不构造虚假设备通过结论。

## 10. P6：文档与制品交付

- [x] 更新 README：框架/API 最低要求、Android 支持范围、国际版已验证版本、开关生效方式、Cookie 获取限制、第三方 Intent action。
- [ ] 文档中区分“框架已连接”“目标已启用”“Hook 已命中”和“评论检查完成”。
- [ ] 更新版本号与更新说明；最终版本名在完成变更范围后确定，不在计划阶段伪造发布版本。
- [ ] 记录 APK SHA-256、签名摘要、versionCode/versionName、构建 JDK/AGP/Gradle 和代码提交。
- [ ] 若提供 Debug 签名的 Release 构建，标注为本地测试包；不覆盖混淆为现有正式签名发布包。
- [ ] 验证旧包覆盖升级的签名条件；不能覆盖时先说明，不通过卸载牺牲用户数据来跳过验证。
- [ ] 给出模块禁用、强停后重启目标 App、恢复旧包/配置的回退步骤；涉及降级安装时明确签名和数据库兼容条件。
- [ ] 检查暂存范围，只提交自主源码、必要配置、测试和文档；不误提交整个 `BiliSource/api` 嵌套仓库、原始 APKs、反编译输出或敏感日志。
- [ ] 发布/推送作为单独交付动作，根据届时用户要求执行；写 TODO 不等于已授权发布。

## 11. 风险、未决项与处理方式

| 项目 | 当前状态 | 实施时处理 |
| --- | --- | --- |
| 新国际版真实身份 | 只确认文件与容器结构 | P3 先读 Manifest，再选择包/进程路由 |
| API/service 制品一致性 | API 本地源码已读，service 尚未核实 | P1 核查发布制品签名/元数据，不按技能模板猜接口 |
| Android 最低版本变化 | 当前 App 21，API 源码 26 | 默认现代版本至少 26，按最终制品核定并明确说明 |
| API 工具链与主工程差距 | API 仓库 SDK 37/AGP 9.2.1，主工程 35/8.1.1 | 区分“构建 API 源码”和“消费 API 制品”，按真实错误最小升级 |
| 旧配置在框架重定向目录 | 现有 Config 有迁移私有反射逻辑 | 先设计可恢复迁出路径，不能直接删除后假设值仍在 |
| 新版绕过旧 BiliCall.execute | 未知 | P3 建立实际发送链，必要时新增适配器 |
| Cookie 路径/格式变化 | 未知 | 只读探测并支持手动账号回退 |
| B 站服务端响应/规则变化 | 现有算法已读，新样本未实测 | Hook 兼容与状态判定分别验证 |
| 国内版没有当前设备证据 | 待准备 | 共用代码回归不能省略，明确待测版本 |
| action 3 接收缺口 | 现有源码静态确认 | P4 明确实现/收敛并同步 Intent 文档 |
| 热重载 | 不是本期目标 | 默认关闭，不把 Remote Preferences 与热重载混用 |

遇到资料不足时，记录缺口和下一条可执行验证步骤；不把猜测写成已适配，也不因一个可独立处理的缺口停止其余阶段。

## 12. 执行记录模板

每完成一个阶段，追加以下记录，并回填对应复选框：

```text
阶段/任务编号：
日期与提交：
涉及文件：
改动目的与最终行为：
静态证据（文件/符号/行号）：
本地验证（命令/结果/产物 hash）：
设备验证（设备/Android/框架/宿主版本/日志时间窗）：
未完成项、失败原因与下一步：
兼容范围及回退方式：
```

当前总状态：API 102 源码迁移已完成；用户反馈实机测试无明显问题，但本任务未独立复核设备日志。P3 静态证据已按 `BiliSource/bilibili_6.4.0_analysis` 核对。P4 代码已按 6.4.0 落地并通过本地测试，国际版主链仍待设备验收。P5 设备矩阵及正式发布尚未完成。

## 13. API 102 本轮执行记录（2026-09-06）

- 版本：`6.3.5-api102-dev` / `636`，未提交、未推送、未发布。
- 依赖：`compileOnly io.github.libxposed:api:102.0.0`，`implementation io.github.libxposed:service:102.0.0`；读取实际 AAR 和 javap 确认入口、枚举、service 方法。
- 工具链：AGP 9.2.1 / Gradle 9.5.1 / compileSdk 37 / Java 源码与目标字节码 17；本机执行 JDK 为 Temurin 21。targetSdk 保留 33，minSdk 为 26。
- 升级依据：原 Gradle 8.0 在本机 JDK 21 下失败（class file major version 65）；API/service AAR 元数据要求 minCompileSdk 37。不是为适配新版国际版而全面升级业务依赖。
- 入口：`XposedInit extends XposedModule`，`onModuleLoaded/onPackageReady`，包名与主进程过滤；不自 Hook App、不启用 Hot Reload。
- Hook：使用真实 Method、现代 Chain、注册 handle 与局部安装回滚；旧 JAR、legacy 入口、旧配置封装、自 Hook 激活类、停用弹幕 Hook 已移除，均可从 Git 基线恢复。
- 配置：App 私有 config 保留；service 只发布三个布尔开关及发布标记，Hook 读取监听后的快照。未发布/远程读取失败时可选开关关闭；手动检查仍可使用。
- UI：菜单增加“API 102 框架状态”“导入旧版设置”；开关可在未绑定时保存；状态区分框架连接、scope 与配置同步，不将其冒充 Hook 命中。
- 旧配置恢复：原 App 私有 config 可直接继续使用；旧框架重定向目录不会自动读取，需用户导出 config.xml 后显式导入。导入会验证已知键/类型、保留导入前私有副本，再覆盖对应设置。账号/历史数据库未改动。此恢复流程仍需设备升级验收。
- 国际版边界：`PostCommentHookByGlobal` 暂保留旧 postComment(Map)、a()/h() 映射。新版报告指出这些点已有变化，因此不能用本轮 API 迁移结果宣称 6.4.0 已适配。下一阶段需读取实际反编译源码逐项落地。
- 连带小修正：敏感分支表单索引/oid long、图片数值 Number 转换、Cookie 数据库只读、主线程跳转和 Activity 弱引用。action 3 接收缺口仍留在 P4，未声称修复。
- 本地验证：`:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease` 最终全部成功；6 个测试、0 失败；Lint 0 错误、302 警告。未改 lint baseline。R8 有第三方 META-INF/services/javax.ws.rs.ext.Providers 缺失引用警告，构建成功。
- APK 结构：Debug 与 Release 均包含三个现代元数据文件，没有 assets/xposed_init；使用 apkanalyzer 的 DEX **类定义**检查确认未打包现代 API 或旧 Xposed 类，入口类存在（签名引用 API 类型属于正常情况）。
- Debug 产物：`biliSendCommAntifraud/app/build/outputs/apk/debug/app-debug.apk`，Debug 签名，v2 验证通过；SHA-256 `a7609ee5732984ddf3d22265c121763fb441f3157924e6d760937a7fe09d706a`。
- Release 产物：`biliSendCommAntifraud/app/build/outputs/apk/release/app-release-unsigned.apk`，未签名，只用于 R8/打包验证；SHA-256 `a31d6fb635209575c3b3c92f5af91bcb5128020d043bee95a935b15328f61737`。
- 设备验证：本轮未安装 APK、未操作 B 站账号。仍需验证框架注入、远程配置、无框架启动、旧版设置恢复、国内版及旧国际版主链。
- 下一步：优先设备冒烟确认 API 102 注入与配置，再根据 `BiliSource/bilibili_6.4.0_analysis/INTERNATIONAL_6.4.0_STATIC_ANALYSIS.md` 和实际 DEX 源码完成新版国际版适配。

## 14. 国际版 6.4.0 适配执行记录（2026-09-06）

- 阶段/任务编号：P3 核对 + P4 实现。版本：`6.3.5-in640-dev` / `637`，未提交、未推送、未发布。
- 目标样本：`com.bilibili.app.in` 6.4.0 / versionCode `9100300`。包名未变，主进程发评。
- 静态证据（已对照 JADX 源码，非仅报告摘要）：
  - `BiliCommentApiService.postComment(Map, String)`，`classes11.dex`
  - `CB0.a.execute()` -> `retrofit2.z`，body 为字段 `b`；请求为 `CB0.a.request()` / 字段 `b`
  - `okhttp3.z.d` 为 FormBody（`okhttp3.r`），参数名/值在字段 `a`/`b`
  - 视频详情页改为 `UnitedBizDetailsActivity`，extras 读取 `aid`/`bvid`/`comment_root_id`/`comment_secondary_id`/`tab_index`
  - Cookie 路径与 `bili.account.storage` 仍存在；`ReplyControl.getInvisible/getFoldPictures/getLocation` 仍在渲染路径
- 代码改动：
  - 新增 `HostCallAdapter`：6.4.0 反射提取 + 旧 `body()/name(i)/value(i)` 回退
  - `PostCommentHookByGlobal` 识别 `postComment(Map, String)`，按 URL `/x/v2/reply/add` 过滤 `CB0.a.execute()`
  - 国内版仍走 `PostCommentHookByMaster` 的 `body()/request()`，不改宿主类名
  - `CommentLocator.lunchGlobal` 视频页改为 `UnitedBizDetailsActivity`，同时写 `id` 和 `aid`，BV 时写 `bvid`
  - `IntentTransferStationHook` 在新旧视频 Activity 类名之间回退
  - `ByXposedLaunchedActivity` 接收 action=3，写入敏感历史，错误提示不再 dump 评论文本
  - `LoggerInterceptor` 对 Cookie/Authorization/CSRF/SESSDATA/access_key 脱敏
- 功能支持（静态/本地，非设备）：
  - 发评捕获：已改 6.4.0 Hook 点，待设备 T01/T02
  - Cookie：路径未改，仍待设备锁/WAL 验证
  - 评论定位：视频页类名/aid 已改，待设备 T04
  - Invisible / 折叠图片：静态仍成立，未改 Hook 点
- 兼容范围：明确针对国际版 6.4.0。旧国际版保留 `postComment(Map)`、`a()`/`h()`、`name/value` 回退，但未用旧 APK 验证。国内版未改发评签名。
- 本地验证：`:app:testDebugUnitTest` 17 tests / 0 failures；`:app:assembleDebug` 成功。Debug APK SHA-256 `E585062C3DEEE9CB98C19C7842ADEB9E4ACA05173FEBB091F9A15D334E9699C5`。设备验证：本轮未安装到国际版 6.4.0，不宣称适配成功。
- 下一步：在 API 102 框架 + `com.bilibili.app.in` 6.4.0 上跑 D01/D05/D06/D09/D18，确认 `comment_captured` 与定位 extras。
- 设备日志（2026-09-06，用户提供 `log/logs_2026-09-06_13-43-40.zip`）：这是反诈 App 的 OkHttp 检查日志，不是 LSPosed 注入日志。13:40 会话 Cookie 已脱敏为 `[redacted]`，对应 in640 构建。一次视频根评论（type=1，64 位 oid/rpid）在发评约 7 秒后出现在 `/x/v2/reply/main` 最新列表首位，`state=0`、`root=0`、`invisible=false`，登录 Cookie 可用且 UID 匹配检查继续。这支持 D05 的一条成功根评主链，不覆盖楼中楼、敏感拦截、定位或 LSPosed `hook_registered`。13:09 会话是更早的历史复查（含游客 12022 删除判定），Cookie 尚未脱敏。
- 收口（versionCode 638）：进程内按 rpid 去重；敏感按 oid/type/messageHash 去重；SQLite 失败仍读 `bili.account.storage`；只要有 SESSDATA 就传递 Cookie；视频 source_id 优先 Intent `bvid`，Hook 线程不再同步请求 BV；动态 ID 覆盖 fragment_args / deep link / inject；`route_skipped` 与 `hook_failed` 带包/进程；App 侧 `check_started`/`check_finished`；检查日志不再写完整 JSON 与 csrf 查询参数。Debug SHA-256 `A9A5E14246DF0670DD5C6C49F713E87F1239BEF299E51FF40A03B1931330E7AC`。21 tests / 0 failures。楼中楼、敏感拦截、定位仍待设备专项。

- 图文动态漏捕获（versionCode 641，6.3.5-in641-dev）：
  - 设备证据：log/LSPosed_20260906_145158.zip。14:51:02 主进程已加载 6.3.5-in640-dev，CB0.a.execute() 与 Java ReplyMoss.addReply/executeAddReply 均 hook_registered。14:51:09 打开 kntr.common.compose.launcher.VertexActivity，14:51:20 再开 TranslucentVertexActivity。直到 14:51:58 提取日志，没有 comment_captured / comment_skipped / ByXposedLaunchedActivity。
  - 静态确认：图文页发评走 KMP CommentPostServiceImpl（运行时类 kntr.common.comment.publish.service.c）到 CommentPostApiKt.requestAddReply，再到 KReplyMoss.addReply(KAddReplyReq, Continuation)。这条链使用 KAddReplyReq / KAddReplyResp，不经过 Java ReplyMoss，也不经过 CB0.a.execute() 的 /x/v2/reply/add。
  - 代码：MossAddReplyHook 增加 KReplyMoss.addReply 的 Zq1.i 回调包装；KReplyCapture 解析 rpid/oid/type；VertexActivity/TranslucentVertexActivity 纳入动态 ID 注入，缺失时不再 Toast。
  - 本地验证：`:app:testDebugUnitTest` 26 tests / 0 failures；`assembleDebug` 成功。Debug SHA-256 `A3108E300B399565AF8255138E3B23E164E5B541996E04BC4A04650E6F9325F1`。设备验证：需强停国际版后在 https://b23.tv/MJOhhIX 再发一条图文评论，确认 LSPosed modules 日志出现 kind=kmoss 的 comment_captured 并弹出检查页。
