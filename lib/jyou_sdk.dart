
import 'dart:async';

import 'package:flutter/services.dart';

class JyouSdk {
  static const MethodChannel _channel = MethodChannel('jyou_sdk/method');

  // static Future<String?> get bind async {
  //   final String? version = await _channel.invokeMethod('bind');
  //   return version;
  // }

  ///bind(BluetoothDevice device)
  Future<dynamic> bind() =>
      _channel.invokeMethod('bind');

  ///unbind(BluetoothDevice device)
  Future<dynamic> unbind() =>
      _channel.invokeMethod('unbind');

  ///scan(BluetoothDevice device)
  Future<dynamic> scan() =>
      _channel.invokeMethod('scan');

  ///disconnect(BluetoothDevice device)
  Future<dynamic> disconnect() =>
      _channel.invokeMethod('disconnect');


}
