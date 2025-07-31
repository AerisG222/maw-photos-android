#!/bin/bash

#DNS_SERVER_IP=$(hostname -I | cut -d ' ' -f 1)
DNS_SERVER_IP=127.0.0.1

echo "*** Starting Dev DNS server on ${DNS_SERVER_IP} ***"
sudo dnsmasq \
    --no-hosts \
    --no-daemon \
    --no-resolv \
    --log-debug \
    --log-queries \
    --log-facility - \
    --listen-address "${DNS_SERVER_IP}" \
    --server 8.8.8.8 \
    --host-record dev-media.mikeandwan.us,10.0.2.2

#    --address /mikeandwan.us/10.0.2.2
#    --domain mikeandwan.us \
