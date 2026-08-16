[![GitHub license](https://img.shields.io/github/license/mashape/apistatus.svg)](https://github.com/AerisG222/maw_photos_android/blob/master/LICENSE.md)

# maw_photos_android

Android version of the photos section of mikeandwan.us

# Running in Emulator

There are a few steps required to get this running in an emulator on a developer machine.

The first is related to getting a copy of the Certificate Authority (CA) public cert for the
development sites.  There are 2 parts to this - one so the Android system recognizes these self
signed certs as valid (so things like the browser trust it).  The other is to add the CA cert
so the application itself will trust this certificate.

1. Getting the CA configured on the emulator
  - Run start_dev_emulator.sh
    - this should start and copy the CA file to the emulator, please review output to confirm that
      copy succeeds.  If not, manually run the adb push as in the script
    - follow the steps written to the console after running start_dev_emulator.sh to install the
      CA cert into the system.  Note that you likely will also need to create a PIN.
  - Configure the CA for the app
    - Open the test CA public certificate and copy the contents
    - Paste this into res/raw/debug_cas (that is configured via android:networkSecurityConfig in the
      main application manifest)

## Updated DNS Instructions

Please see the original notes about DNS below.  Those were not fully working, so streamlining the
details for DNS in the follow bullets.  These are ugly, but allowed me to finally get the
emulator working locally against the dev site.

note: if dnsmasq below fails because port is already in use, update /etc/dnsmasq.conf by uncommenting
the 'listen-address' line and add the ip for the machine (192.168.x.x) then restart.  This is needed
as it listens by default to 0.0.0.0 and we need to have localhost/127.0.0.1 listen to support the
emulator.  Additionally, you also need to uncomment the bind-interfaces line.

- start dev dns server (dnsmasq)
  - `./start_dev_dns.sh`
- start dev emulator
  - `./start_dev_emulator.sh`
- make sure you started the dev sites
  - `systemctl --user start pod-dev-www-pod.service`
  - `start_dev_services.sh`
- verify you can access dev site via chrome in emulator by going to https://10.0.2.2:5021 (should bring up www site)
- try to see if dns resolution works by going to https://dev.www.mikeandwan.us:5021 (this likely will not work)
- manually configure network settings
  - go to `Settings > Network & internet > Internet (Android wifi)`
  - click gear next to Android wifi
  - click pencil icon / edit in upper right
  - change IP Settings from 'DHCP' to 'Static', and specify the following:
    - IP Address: 192.168.10.10
    - Gateway: 10.0.2.2
    - Network Prefix Length: 16
    - DNS1: 10.0.2.2

With the above steps, this seems to work pretty reliably, though I don't understand at this point
why
simply setting the dns server on the emulator command line is not sufficient to get this to work anymore.

## Original DNS Instructions
The second step is to adjust the DNS configuration so that the emulator can find the dev sites on
your local pc.  By default, the Android emulator will use your local DNS server to access the
internet, but unfortunately does not reference the local hosts file.  As such, one quick way to get
this to work (which will not work for all scenarios) is to add DNS entries on your local DNS server
to point to the dev sites (authdev.mikeandwan.us, apidev.mikeandwan.us, and wwwdev.mikeandwan.us).
For Android to see those, you need to specify an ip address of 10.0.2.2 as that is how the emulator
maps the localhost that is running the emulator.

While the above steps will fix DNS for the emulator, you should still create similar entries in the
hosts file, but referencing 127.0.0.1 - as this is what the sites will use to resolve the proper
ips, magically allowing both emulator and local development scenarios to work!  Confusing, hacky -
of course!

2. DNS Entries
  - Using your local router, manually add the DNS entries pointing to 10.0.2.2 - this is what the
    emulator will see
  - You should already have a known working dev environment before trying this app, but if not,
    make sure to add the entries to the /etc/hosts file for the dev sites pointing to 127.0.0.1

** Be sure to start the emulator using the start_dev_emulator.sh script in the root of this project!
   That includes an argument to override the default DNS server to point at the router where you
   manually added the DNS entry, which likely needs to override the default DNS on the host.

Note - In the past, we worked around this by pushing a hosts file to the emulator.  However, this
does not seem to work anymore, as any Google API system image does not allow writing system files.
If you can figure out how to do that, it would be a preferable solution as you no longer need to
touch the DNS where you plan on testing the app - which sucks.

Once the above steps are complete, you should now be able to debug the application while all
services are running on a single machine.

## License

MIT
