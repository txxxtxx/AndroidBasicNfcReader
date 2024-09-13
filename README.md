# Android Basic NFC Reader

This is a simple app showing how to detect and read some data from an NFC tag tapped to the Android's NFC reader.

As there are a lot of questions on Stackoverflow.com that use an **Intent-based** NFC detection system I'm showing here how to use the more 
modern **Reader Mode** NFC communication.

This is from an answer by *Andrew* (https://stackoverflow.com/users/2373819/andrew) regarding the two modes:

*Also note that using enableForegroundDispatch is actually not the best way to use NFC using enableReaderMode is a newer and much better API 
to use NFC.enableReaderMode does not use Intent's and gives you more control, it is easy to do NFC operations in a background Thread (which 
is recommended), for writing to NFC Tag's it is much more reliable and leads to less errors.*

There are 3 simples steps to follow the Reader mode:

1) in `AndroidManifest.xml` add one line:
2) in your activity or fragment expand your class definition by `asd`
3) for minimum implement an `onTagDetected` method where all the work with the tag is done.

Note: the `onTagDetected` method is not running on the User Interface (UI) thread, so you are not allowed to write directly to any UI elements like 
e.g. TextViews or Toasts - you need to encapsulate them in a `run onUiTHread` construct. This method is running in an background thread.


