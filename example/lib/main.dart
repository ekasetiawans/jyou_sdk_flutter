import 'package:flutter/material.dart';
import 'package:jyou_sdk/jyou_sdk.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  JyouSdk jyou=JyouSdk();


  @override
  void initState() {
    jyou=JyouSdk();
    super.initState();
  }



  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: ListView(
            shrinkWrap: true,
            primary: true,
            physics: const ScrollPhysics(),
            children: [
                ElevatedButton(
                  child: const Text("Bind",style: TextStyle(color: Colors.black),),
                  onPressed: (){
                    jyou.bind();
                },),
              ElevatedButton(
                child: const Text("UnBind",style: TextStyle(color: Colors.black),),
                onPressed: (){
                  jyou.unbind();
                },),
              ElevatedButton(
                child: const Text("Scan",style: TextStyle(color: Colors.black),),
                onPressed: (){
                  jyou.scan();
                },),
              ElevatedButton(
                child: const Text("Disconnect",style: TextStyle(color: Colors.black),),
                onPressed: (){
                  jyou.disconnect();
                },),
            ],
          ),
        ),
      ),
    );
  }
}
