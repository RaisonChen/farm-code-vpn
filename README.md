# 农场取code - 单开关代理 App

## 功能说明
- 页面只有一个"农场取code"开关
- 打开开关 → 只有 QQ 走代理（574530266.iok.la:8451）
- 关闭开关 → 全部恢复直连
- 首次打开 App 显示"安装CA证书"按钮，点击自动弹出系统证书安装
- 安装成功后按钮永久隐藏
- 节点信息拆段隐藏，页面不暴露任何 IP/端口

## 文件清单
```
lib/main.dart                      ← 覆盖原文件
lib/ui/single_switch_page.dart     ← 新建
pubspec.yaml                       ← 编辑依赖部分
assets/ca.crt                      ← 放入你的 CA 证书
.github/workflows/build-apk.yml    ← 新建
```

## GitHub Actions 编译步骤
1. Fork https://github.com/ys1231/appproxy
2. 把上面 5 个文件按路径覆盖/新建到你的 fork
3. 进 Actions → 选 "Build Single Switch APK" → Run workflow
4. 等 ~20 分钟变绿 → 下载 Artifacts 里的 APK

## 替换 CA 证书
把你的 CA 证书文件重命名为 `ca.crt`，替换 `assets/ca.crt`

## 修改代理节点
编辑 `lib/ui/single_switch_page.dart` 顶部：
- `_h1` / `_h2` ：域名拆成的两段
- `_port` ：端口号
- `_apps` ：要代理的 App 包名列表

## License
基于 ys1231/appproxy (GPL-3.0)，本 fork 同样以 GPL-3.0 开源
