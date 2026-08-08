import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:path_provider/path_provider.dart';
import 'package:open_file/open_file.dart';
import 'package:shared_preferences/shared_preferences.dart';

class SingleSwitchPage extends StatefulWidget {
  const SingleSwitchPage({super.key});

  @override
  State<SingleSwitchPage> createState() => _SingleSwitchPageState();
}

class _SingleSwitchPageState extends State<SingleSwitchPage> {
  bool _on = false;
  bool _caInstalled = false;
  static const platform = MethodChannel('cn.ys1231/appproxy/vpn');
  static const _prefsKey = 'ca_installed';

  // ===== 节点信息（拆段隐藏，UI 不暴露）=====
  static const String _h1 = '574530266';
  static const String _h2 = '.iok.la';
  static const int _port = 8451;
  static const String _type = 'http';

  // 只代理 QQ
  static const List<String> _apps = ['com.tencent.mobileqq'];

  @override
  void initState() {
    super.initState();
    _loadCaFlag();
  }

  /// 读取本地标记：之前是否装过 CA
  Future<void> _loadCaFlag() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      _caInstalled = prefs.getBool(_prefsKey) ?? false;
    });
  }

  /// 开关：开 → 启动 VPN（只代理 QQ）；关 → 停止
  Future<void> _toggle(bool v) async {
    try {
      if (v) {
        await platform.invokeMethod('startVpn', {
          'proxyName': '农场取code',
          'proxyType': _type,
          'proxyHost': _h1 + _h2,
          'proxyPort': _port.toString(),
          'proxyUser': '',
          'proxyPass': '',
          'appProxyPackageList': _apps,
        });
      } else {
        await platform.invokeMethod('stopVpn');
      }
      setState(() => _on = v);
    } catch (e) {
      setState(() => _on = !v);
    }
  }

  /// 点击按钮 → 从 assets 提取 ca.crt → 打开系统安装器 → 成功写标记
  Future<void> _installCA() async {
    try {
      // 1. 读取 assets 里的证书
      final byteData = await rootBundle.load('assets/ca.cer');
      final dir = await getTemporaryDirectory();
      final file = File('${dir.path}/ca.cer');
      await file.writeAsBytes(byteData.buffer.asUint8List());

      // 2. 打开文件，触发系统证书安装界面
      final result = await OpenFile.open(file.path);

      if (result.type == ResultType.done) {
        // 3. 安装成功 → 写标记 → 隐藏按钮
        final prefs = await SharedPreferences.getInstance();
        await prefs.setBool(_prefsKey, true);
        setState(() => _caInstalled = true);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('安装失败，请手动前往：设置 → 安全 → 安装证书'),
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text(''),
        elevation: 0,
        backgroundColor: Colors.transparent,
      ),
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // 状态图标
            Icon(
              _on ? Icons.cloud_done : Icons.cloud_off,
              size: 64,
              color: _on ? Colors.green : Colors.grey,
            ),
            const SizedBox(height: 16),

            // 标题
            const Text(
              '农场取code',
              style: TextStyle(fontSize: 22, fontWeight: FontWeight.w500),
            ),
            const SizedBox(height: 8),

            // 状态文字
            Text(
              _on ? '运行中' : '已停止',
              style: const TextStyle(color: Colors.grey, fontSize: 14),
            ),
            const SizedBox(height: 32),

            // 开关
            Switch(
              value: _on,
              onChanged: _toggle,
              activeColor: Colors.green,
            ),

            // 只有没装过 CA 才显示安装按钮
            if (!_caInstalled) ...[
              const SizedBox(height: 40),
              ElevatedButton.icon(
                icon: const Icon(Icons.security, size: 18),
                label: const Text('安装CA证书（首次需手动）'),
                onPressed: _installCA,
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(
                      horizontal: 20, vertical: 12),
                  backgroundColor: Colors.green,
                  foregroundColor: Colors.white,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
