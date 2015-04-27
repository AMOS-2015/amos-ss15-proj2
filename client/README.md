# CroudTrip Client

This repo contains the client application that runs on Android.
## Google API Setup
To run the application for your own IDE you have to follow several steps:

### 1. Register your keystore
You can find your usual debug.keystore in ~/.android/debug.keystore. You have to
extract the SHA1 fingerprint of this keystore and add this fingerprint at Google's
Developer Console console at: APIs & auth -> Credentials -> Edit allowed Android applications.

### 2. Copy your keystore
Since we had to create a keystore for the travis build it was necessary to tell the gradle
build file which key should be used. So you have to copy your debug.keystore from ~/.android to
./client/debug.keystore. Just paste the file there and gradle should be able to build your project.

### 3. Add the API-Key to your project
To be able to use the google-services in your own project you have to add the API-Key manually
by copying "./client/src/main/templates/keys.xml.template" to "./client/src/main/res/values/keys.xml"
and replace the dummy key with the real key that you get from the Developers Console.

### 4. Do not push the API-Key
Be aware that both debug.keystore and keys.xml are ignored for our
repository (see .gitignore). So we never want to push either of these
files and therefore we want to prevent our keystores and API keys being
visible for public. So please make sure that you do not paste the API
key somewhere in the code.