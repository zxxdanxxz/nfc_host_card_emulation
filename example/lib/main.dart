import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:nfc_host_card_emulation/nfc_host_card_emulation.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  await NfcHce.init(
    aid: Uint8List.fromList([0xA0, 0x00, 0xDA, 0xDA, 0xDA, 0xDA, 0xDA]),
    permanentApduResponses: true,
    listenOnlyConfiguredPorts: false,
  );

  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  bool apduAdded = false;

  // change port here
  final port = 0;
  // change data to transmit here
  final data = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11];

  // this will be changed in the NfcHce.stream listen callback
  NfcApduCommand? nfcApduCommand;

  @override
  void initState() {
    super.initState();

    NfcHce.stream.listen((command) {
      setState(() => nfcApduCommand = command);
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData(useMaterial3: true, colorSchemeSeed: Colors.green),
      home: Scaffold(
        appBar: AppBar(
          title: const Text('NFC HCE example app'),
        ),
        body: Center(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              SizedBox(
                height: 200,
                width: 300,
                child: ElevatedButton(
                  style: ButtonStyle(
                    backgroundColor: MaterialStateProperty.all(
                      apduAdded ? Colors.redAccent : Colors.greenAccent,
                    ),
                  ),
                  onPressed: () async {
                    if (apduAdded == false) {
                      await NfcHce.addApduResponse(port, data);
                    } else {
                      await NfcHce.removeApduResponse(port);
                    }

                    setState(() => apduAdded = !apduAdded);
                  },
                  child: FittedBox(
                    child: Text(
                      apduAdded
                          ? 'remove\n$data\nfrom\nport $port'
                          : 'add\n$data\nto\nport $port',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 26,
                        color: apduAdded ? Colors.white : Colors.black,
                      ),
                    ),
                  ),
                ),
              ),
              if (nfcApduCommand != null)
                Text(
                  'You listened to the stream and received the '
                  'following command on the port ${nfcApduCommand!.port}:\n'
                  '${nfcApduCommand!.command}\n'
                  'with additional data ${nfcApduCommand!.data}',
                  style: const TextStyle(fontSize: 26),
                  textAlign: TextAlign.center,
                ),
            ],
          ),
        ),
      ),
    );
  }
}
