import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'ui/single_switch_page.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  // 锁定竖屏
  SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
  ]);
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '农场取code',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        primarySwatch: Colors.green,
        useMaterial3: true,
      ),
      home: const SingleSwitchPage(),
    );
  }
}
