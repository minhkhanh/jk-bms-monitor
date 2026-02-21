#!/bin/bash

read -p "Enter Keystore Password (storePassword): " storepass
read -p "Enter Key Password (keyPassword): " keypass

echo "Generating JK BMS Monitor Release Keystore..."

keytool -genkey -v \
  -keystore release.keystore \
  -alias jkbms_alias \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass "$storepass" \
  -keypass "$keypass" \
  -dname "CN=JK BMS Monitor, OU=App Dev, O=TmKhanh, L=Ho Chi Minh City, S=HCMC, C=VN"

echo "Creating keystore.properties..."

cat << PROP > keystore.properties
storePassword=$storepass
keyPassword=$keypass
keyAlias=jkbms_alias
storeFile=../release.keystore
PROP

echo "Done! Ensure 'release.keystore' and 'keystore.properties' are kept safe and NOT committed to Git."
