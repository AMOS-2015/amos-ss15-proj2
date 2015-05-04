#!/bin/bash
rm keys.tar.enc
cp client/src/main/res/values/keys.xml client/
tar cfv keys.tar client/debug.keystore client/keys.xml
travis login
travis encrypt-file keys.tar
rm client/keys.xml
rm keys.tar

echo
echo !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
echo !!! DO NOT FORGET TO UPDATE .travis.yml WITH THE NEW DECRYPTION COMMAND !!!
echo !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
echo
