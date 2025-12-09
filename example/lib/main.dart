import 'dart:convert';
import 'dart:io';
import 'package:device_info_plus/device_info_plus.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:apk_info/apk_info.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:path_provider/path_provider.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  while (await Permission.manageExternalStorage.request().isDenied) {}

  final info = await DeviceInfoPlugin().androidInfo;
  while (info.version.sdkInt < 33 && await Permission.storage.request().isDenied) {}

  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  /// Copy APK from assets to temporary directory
  Future<String> copyApkFromAssets() async {
    final byteData = await rootBundle.load('assets/apk_info_test.apk');
    final tempDir = await getTemporaryDirectory();
    final tempFile = File('${tempDir.path}/apk_info_test.apk');
    await tempFile.writeAsBytes(byteData.buffer.asUint8List());
    return tempFile.path;
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: Text('APK Info Example')),
        body: Center(
          child: FutureBuilder<ApkInfo>(
            future: copyApkFromAssets().then((path) => ApkInfo.about(path)),
            builder: (context, snapshot) {
              if (snapshot.connectionState == ConnectionState.waiting) {
                return CircularProgressIndicator();
              } else if (snapshot.hasError) {
                return Text('Error: ${snapshot.error}');
              } else {
                final apkInfo = snapshot.data!;
                Uint8List? iconBytes;
                if (apkInfo.icon != null) {
                  iconBytes = base64Decode(apkInfo.icon!);
                }

                return SingleChildScrollView(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      if (iconBytes != null)
                        Padding(
                          padding: const EdgeInsets.all(16.0),
                          child: Image.memory(
                            iconBytes,
                            width: 100,
                            height: 100,
                          ),
                        ),
                      Text('UUID: ${apkInfo.uuid}'),
                      Text('Application ID: ${apkInfo.applicationId}'),
                      Text('App Label: ${apkInfo.applicationLabel}'),
                      Text('Version Code: ${apkInfo.versionCode}'),
                      Text('Version Name: ${apkInfo.versionName}'),
                      Text('Platform Build Version Code: ${apkInfo.platformBuildVersionCode}'),
                      Text('Compile SDK: ${apkInfo.compileSdkVersion}'),
                      Text('Min SDK: ${apkInfo.minSdkVersion}'),
                      Text('Target SDK: ${apkInfo.targetSdkVersion}'),
                    ],
                  ),
                );
              }
            },
          ),
        ),
      ),
    );
  }
}
