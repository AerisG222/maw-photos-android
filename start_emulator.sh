#!/bin/bash
SDK=/home/mmorano/Android/Sdk
EMU="${SDK}/emulator/emulator"
ADB="${SDK}/platform-tools/adb"
#DNS_SERVER_IP=$(hostname -I | cut -d ' ' -f 1)
DNS_SERVER_IP=127.0.0.1

DEVICES=(
  "Pixel_8a_API_35"
  "Pixel_7a_API_34"
  "Pixel_2XL_API_26"
)

select DEVICE in "${DEVICES[@]}"
do
    case $REPLY in
        1) break;;
        2) break;;
        3) break;;
        *) echo "invalid!";;
    esac
done

echo "*** Starting ${DEVICE} Emulator with DNS server: ${DNS_SERVER_IP} ***"
$EMU -avd "${DEVICE}" -no-snapshot -dns-server "${DNS_SERVER_IP}" -feature -Vulkan &

echo '*** Wait for Emulator to Start ***'
sleep 60

echo '*** Certificate Authority ***'
echo '  - copying public cert to emulator'
$ADB push ~/maw-ca/maw-ca.crt /sdcard/maw-ca.crt

echo '  - Add certificate to emulator (must be performed manually within the emulator):'
echo '    1. Settings > Security > Credential Storage'
echo '    2. Click Install from SD card'
echo '    3. Select mawdev.public.crt in the resulting file explorer'
