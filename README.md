# nfc_host_card_emulation

The Flutter plugin implementing Host Card Emulation (HCE) with the APDU communication interface. Supported only on Android now.

## Features

Implemented an interface for working with SELECT (by AID) APDU commands. The structure of APDU commands is as follows:

| CLA | INS | P1  | P2 (port) | AID Length | AID | Additional data |
| --- | --- | --- | --- | ---------- | --- | --- |
| 0x00| 0xA4| 0x04| 0 - 255   | 5 - 16 | ByteArray with defined length | ByteArray |

The P2 parameter is adjustable and is used in the plugin as a port for registering the APDU response. The structure of APDU response is as follows:

| DATA  | STATUS  |
| --- | --- |
| User-defined response or nothing | Two status bytes defined in ISO7816-4 standart |

## Android Setup

Add NFC permissions to Android manifest file locating at `android/app/src/main/AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.NFC" />
<uses-feature android:name="android.hardware.nfc" android:required="false" />
<uses-feature android:name="android.hardware.nfc.hce" android:required="false" />
```

Add host APDU service to the < application > block in the same Android manifest file:
```xml
<service 
	 android:name="io.flutter.plugins.nfc_host_card_emulation.AndroidHceService"
	 android:exported="true"
	 android:permission="android.permission.BIND_NFC_SERVICE">
	 <intent-filter>
		 <action android:name="android.nfc.cardemulation.action.HOST_APDU_SERVICE" />
	 </intent-filter>
	 <meta-data
		 android:name="android.nfc.cardemulation.host_apdu_service"
		 android:resource="@xml/apduservice" />
 </service>
```

The host APDU service refers to a meta-data file `apduservice.xml `. It must be created in the `android/app/src/main/res/xml` folder. If you don't have an `xml` folder, just create one. This file contains information about the available AIDs for the host APDU service. So add the following text to `apduservice.xml`.
```xml
<host-apdu-service xmlns:android="http://schemas.android.com/apk/res/android"
	android:requireDeviceUnlock="false">
	<aid-group android:category="other">
		<aid-filter android:name="A000DADADADADA"/>
	</aid-group>
</host-apdu-service>
```
This aid-filter means that the application will only accept ( 0xA0, 0x00, 0xDA, 0xDA, 0xDA, 0xDA, 0xDA ) AID. But you can change it to any other with length from 5 to 16 or add more aid-filters.

## Flutter Setup

Add to pubspec.yaml:
```yaml
dependencies:
  nfc_host_card_emulation: <latest version>
```
And import it to your code:
```dart
import 'package:nfc_host_card_emulation/nfc_host_card_emulation.dart';
```

## Usage

Check if your device supports NFC and if the NFC-module is enabled:
```dart
final nfcState = await NfcHce.checkDeviceNfcState();
```

If nfcState is `NfcState.enabled`, then you can use other NfcHce functions. Otherwise, the functions will work, but will have no effect and you won't be able to use APDU.

So after you have checked that Nfc is enabled, initialize the HCE service with the following command:
```dart
await NfcHce.init(
    // AID that match at least one aid-filter in apduservice.xml.
    // In my case it is A000DADADADADA.
    aid: Uint8List.fromList([0xA0, 0x00, 0xDA, 0xDA, 0xDA, 0xDA, 0xDA]),

    // next parameter determines whether APDU responses from the ports
    // on which the connection occurred will be deleted.
    // If `true`, responses will be deleted, otherwise won't.
    permanentApduResponses: true,

    // next parameter determines whether APDU commands received on ports
    // to which there are no responses will be added to the stream.
    // If `true`, command won't be added, otherwise will.
    listenOnlyConfiguredPorts: false,
  );
```

Subscribe to NfcHce stream changes:
```dart
NfcHce.stream.listen((command) {
	// some action here
});
```

Add or remove APDU responses to desired ports:
```dart
// this line adds [0,1,2,3,4,5] response to 0 port
await NfcHce.addApduResponse(0, [0,1,2,3,4,5]);

// this line remove response from 0 port
await NfcHce.removeApduResponse(port);
```

Bring your device to the NFC module, which can send APDU commands.