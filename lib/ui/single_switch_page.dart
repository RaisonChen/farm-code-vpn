import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:share_plus/share_plus.dart';

class SingleSwitchPage extends StatefulWidget {
  const SingleSwitchPage({super.key});
  @override
  State createState() => _SingleSwitchPageState();
}

class _SingleSwitchPageState extends State<SingleSwitchPage>
    with SingleTickerProviderStateMixin {
  bool _on = false;
  bool _caInstalled = false;
  late AnimationController _animCtrl;
  late Animation<double> _scaleAnim;

  // 必须与 MainActivity.kt 中 CHANNEL_VPN 一致
  static const platform = MethodChannel('cn.ys1231/appproxy/vpn');
  static const _prefsKey = 'ca_installed';

  // 节点信息拆分隐藏
  static const String _h1 = '574530266';
  static const String _h2 = '.iok.la';
  static const int _port = 8451;
  static const String _type = 'http';
  static const List<String> _apps = ['com.tencent.mobileqq'];

  @override
  void initState() {
    super.initState();
    _loadCaFlag();
    _animCtrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 300),
    );
    _scaleAnim = CurvedAnimation(parent: _animCtrl, curve: Curves.easeOutBack);
  }

  @override
  void dispose() {
    _animCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadCaFlag() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() => _caInstalled = prefs.getBool(_prefsKey) ?? false);
  }

  Future<void> _markCaInstalled() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_prefsKey, true);
    setState(() => _caInstalled = true);
  }

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
        _animCtrl.forward();
      } else {
        await platform.invokeMethod('stopVpn');
        _animCtrl.reverse();
      }
      setState(() => _on = v);
    } catch (e) {
      setState(() => _on = !v);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('操作失败: $e')),
        );
      }
    }
  }

  /// 安装CA证书 —— 方案2：写私有目录 + 分享让用户保存到Download
  Future<void> _installCA() async {
    try {
      final byteData = await rootBundle.load('assets/ca.cer');

      // 写 App 外部私有目录（不需要任何额外权限）
      final dir = await getExternalStorageDirectory();
      if (dir == null) throw Exception('无法获取存储目录');
      final file = File('${dir.path}/ca.cer');
      if (await file.exists()) await file.delete();
      await file.writeAsBytes(byteData.buffer.asUint8List());

      // 弹出提示
      if (!mounted) return;
      showDialog(
        context: context,
        builder: (ctx) => AlertDialog(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(20),
          ),
          title: const Row(
            children: [
              Icon(Icons.shield_outlined, color: Colors.green, size: 24),
              SizedBox(width: 8),
              Text('安装CA证书'),
            ],
          ),
          content: const Text(
            '接下来会弹出"分享"菜单，请选择：\n'
            '「保存到手机」或「保存到下载」\n\n'
            '保存成功后，再去：\n'
            '设置 → 安全 → 加密与凭据\n'
            '→ 安装证书 → CA证书\n'
            '选择刚才保存的 ca.cer\n\n'
            '安装完成后点击下方"我已安装"。',
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('稍后'),
            ),
            ElevatedButton(
              onPressed: () async {
                Navigator.pop(ctx);
                // 调起系统分享，让用户自己保存到 Download
                await Share.shareXFiles(
                  [XFile(file.path, mimeType: 'application/x-x509-ca-cert')],
                  subject: 'CA证书',
                );
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.green,
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(10),
                ),
              ),
              child: const Text('去保存证书'),
            ),
          ],
        ),
      );
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('保存证书失败：$e')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.of(context).size;
    final cardWidth = (size.width * 0.85).clamp(300.0, 380.0);

    return Scaffold(
      backgroundColor: const Color(0xFFF0F2F5),
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFFE8EAF6), Color(0xFFF0F2F5), Color(0xFFE0F7FA)],
          ),
        ),
        child: SafeArea(
          child: Center(
            child: Container(
              width: cardWidth,
              padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 36),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(28),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.05),
                    blurRadius: 30,
                    offset: const Offset(0, 12),
                  ),
                ],
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  // 图标 + 动画
                  ScaleTransition(
                    scale: _scaleAnim,
                    child: Container(
                      width: 72,
                      height: 72,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        gradient: RadialGradient(
                          colors: _on
                              ? [Colors.green.shade300, Colors.green.shade600]
                              : [Colors.grey.shade300, Colors.grey.shade500],
                        ),
                        boxShadow: _on
                            ? [
                                BoxShadow(
                                  color: Colors.green.withOpacity(0.4),
                                  blurRadius: 20,
                                  spreadRadius: 2,
                                ),
                              ]
                            : [],
                      ),
                      child: const Icon(
                        Icons.shield_outlined,
                        size: 36,
                        color: Colors.white,
                      ),
                    ),
                  ),
                  const SizedBox(height: 20),

                  // 标题
                  const Text(
                    '农场取code',
                    style: TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                      color: Color(0xFF1A1A2E),
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    '代理开关',
                    style: TextStyle(
                      fontSize: 14,
                      color: Colors.grey.shade500,
                    ),
                  ),
                  const SizedBox(height: 28),

                  // 自定义开关
                  _buildCustomSwitch(),
                  const SizedBox(height: 12),

                  // 状态文字
                  AnimatedSwitcher(
                    duration: const Duration(milliseconds: 200),
                    child: Text(
                      _on ? '● 运行中' : '○ 已停止',
                      key: ValueKey(_on),
                      style: TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w500,
                        color: _on ? Colors.green : Colors.grey.shade400,
                      ),
                    ),
                  ),

                  // CA 证书按钮
                  if (!_caInstalled) ...[
                    const SizedBox(height: 28),
                    const Divider(height: 1),
                    const SizedBox(height: 16),
                    SizedBox(
                      width: double.infinity,
                      child: OutlinedButton.icon(
                        onPressed: _installCA,
                        icon: const Icon(Icons.lock_outline, size: 18),
                        label: const Text('安装CA证书（首次需手动）'),
                        style: OutlinedButton.styleFrom(
                          foregroundColor: Colors.grey.shade700,
                          side: BorderSide(color: Colors.grey.shade300),
                          padding: const EdgeInsets.symmetric(vertical: 12),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12),
                          ),
                        ),
                      ),
                    ),
                  ],

                  const SizedBox(height: 16),
                  Text(
                    '仅代理QQ端',
                    style: TextStyle(
                      fontSize: 11,
                      color: Colors.grey.shade400,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildCustomSwitch() {
    return GestureDetector(
      onTap: () => _toggle(!_on),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 250),
        curve: Curves.easeInOut,
        width: 68,
        height: 38,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(20),
          color: _on ? Colors.green : Colors.grey.shade400,
          boxShadow: [
            BoxShadow(
              color: (_on ? Colors.green : Colors.grey).withOpacity(0.3),
              blurRadius: 8,
              offset: const Offset(0, 2),
            ),
          ],
        ),
        child: Stack(
          children: [
            AnimatedPositioned(
              duration: const Duration(milliseconds: 250),
              curve: Curves.easeInOut,
              left: _on ? 32 : 2,
              top: 2,
              child: Container(
                width: 34,
                height: 34,
                decoration: const BoxDecoration(
                  shape: BoxShape.circle,
                  color: Colors.white,
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black12,
                      blurRadius: 4,
                      offset: Offset(0, 1),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
