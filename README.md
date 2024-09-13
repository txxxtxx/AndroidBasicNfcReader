# Android Basic NFC Reader

This is a simple app showing how to detect and read some data from an NFC tag tapped to the Android's NFC reader.

As there are a lot of questions on Stackoverflow.com that use an **Intent-based** NFC detection system I'm showing here how to use the more 
modern **Reader Mode** NFC communication.

This is from an answer by *[Andrew](https://stackoverflow.com/users/2373819/andrew)* regarding the two modes:

*Also note that using enableForegroundDispatch is actually not the best way to use NFC using enableReaderMode is a newer and much better API 
to use NFC.enableReaderMode does not use Intent's and gives you more control, it is easy to do NFC operations in a background Thread (which 
is recommended), for writing to NFC Tag's it is much more reliable and leads to less errors.*

There are 3 simples steps to follow to run the Reader mode:

1) in `AndroidManifest.xml` add one line:
2) in your activity or fragment expand your class definition by `asd`
3) for minimum implement an `onTagDetected` method where all the work with the tag is done.

Note: the `onTagDetected` method is not running on the User Interface (UI) thread, so you are not allowed to write directly to any UI elements like 
e.g. TextViews or Toasts - you need to encapsulate them in a `run onUiTHread` construct. This method is running in an background thread.

## Example outputs for some tag types

Below you find outputs for some tags with different technologies involved:

### Example for an **NTAG216** tag with **NfcA** technology:
```plaintext
NFC tag detected
Tag UID length: 7 UID: 04be7982355b80
--------------------
The TechList contains 3 entry/ies:
Entry 0: android.nfc.tech.NfcA
Entry 1: android.nfc.tech.MifareUltralight
Entry 2: android.nfc.tech.NdefFormatable
--------------------
TAG: Tech [android.nfc.tech.NfcA, android.nfc.tech.MifareUltralight, android.nfc.tech.NdefFormatable]
--------------------
-= NfcA Technology =-
ATQA: 4400
SAK: 00
maxTransceiveLength: 253
--------------------
Connected to the tag using NfcA technology
--------------------
This tag is NOT supporting the NfcV class
--------------------
This tag is NOT supporting the NDEF class
--------------------
```

### Example for an **NTAG216** tag containing an **NDEF message**:
```plaintext
NFC tag detected
Tag UID length: 7 UID: 04be7982355b80
--------------------
The TechList contains 3 entry/ies:
Entry 0: android.nfc.tech.NfcA
Entry 1: android.nfc.tech.MifareUltralight
Entry 2: android.nfc.tech.Ndef
--------------------
TAG: Tech [android.nfc.tech.NfcA, android.nfc.tech.MifareUltralight, android.nfc.tech.Ndef]
--------------------
-= NfcA Technology =-
ATQA: 4400
SAK: 00
maxTransceiveLength: 253
--------------------
Connected to the tag using NfcA technology
--------------------
This tag is NOT supporting the NfcV class
--------------------
Connected to the tag using NDEF technology
--------------------
NDEF message: NdefMessage [NdefRecord tnf=1 type=54 payload=02656E416E64726F696443727970746F]
NDEF message: d101105402656e416e64726f696443727970746f
NDEF message: enAndroidCrypto
```

### Example for a **MIFARE Classic** tag:
```plaintext
NFC tag detected
Tag UID length: 4 UID: 641a35cf
--------------------
The TechList contains 3 entry/ies:
Entry 0: android.nfc.tech.NfcA
Entry 1: android.nfc.tech.MifareClassic
Entry 2: android.nfc.tech.NdefFormatable
--------------------
TAG: Tech [android.nfc.tech.NfcA, android.nfc.tech.MifareClassic, android.nfc.tech.NdefFormatable]
--------------------
-= NfcA Technology =-
ATQA: 0400
SAK: 08
maxTransceiveLength: 253
--------------------
Connected to the tag using NfcA technology
--------------------
This tag is NOT supporting the NfcV class
--------------------
This tag is NOT supporting the NDEF class
--------------------
```

### Example for an NFC enabled **Credit Card**:
```plaintext
NFC tag detected
Tag UID length: 4 UID: b58fcc6d
--------------------
The TechList contains 2 entry/ies:
Entry 0: android.nfc.tech.IsoDep
Entry 1: android.nfc.tech.NfcA
--------------------
TAG: Tech [android.nfc.tech.IsoDep, android.nfc.tech.NfcA]
--------------------
-= NfcA Technology =-
ATQA: 0400
SAK: 20
maxTransceiveLength: 253
--------------------
Connected to the tag using NfcA technology
--------------------
This tag is NOT supporting the NfcV class
--------------------
This tag is NOT supporting the NDEF class
--------------------
```

### Example for an **ICODE SLIX** tag with **NfcV** technology:
```plaintext
NFC tag detected
Tag UID length: 8 UID: 18958608530104e0
--------------------
The TechList contains 2 entry/ies:
Entry 0: android.nfc.tech.NfcV
Entry 1: android.nfc.tech.NdefFormatable
--------------------
TAG: Tech [android.nfc.tech.NfcV, android.nfc.tech.NdefFormatable]
--------------------
This tag is NOT supporting the NfcA class
--------------------
-= NfcV Technology =-
DsfId: 00
maxTransceiveLength: 253
--------------------
Connected to the tag using NfcV technology
--------------------
This tag is NOT supporting the NDEF class
--------------------
```
