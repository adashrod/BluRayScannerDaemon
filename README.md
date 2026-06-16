# BluRayScannerDaemon
a daemon that wakes up periodically to scan a directory that might contain Blu-ray directories itself, and demuxes them into individual tracks

Use case: you have a directory (e.g. c:\blurays) with some ripped Blu-ray disc images in it, (e.g. a dir named AWESOME_ACTION_FLICK). You set up the BRSD config file, run the jar, and the demuxed Blu-ray tracks will go into same dir as they are created.

### Example daemon.properties (this file will be created by BRSD if it doesn't already exist)
~~~~
eac3toExecutable=c:\\Program Files\\eac3to\\eac3to.exe
dirToScan=c:\\blurays
languages=English,Spanish,Undetermined
sleepTimeMinutes=360
maxRetries=4
~~~~

`languages` is a comma-separated list of track languages to include. Each can be a language name or 3-letter ISO-639-2/T code.

1. run `ant deploy` from `BluRayScannerDaemon/daemon-module`

`BluRayScannerDaemon.jar` should now be in `BluRayScannerDaemon/target`, and can be run using `java -jar BluRayScannerDaemon.jar`
