# When prompted for password, enter "pokrio" (4 times).
# At the end, type "yes" to overwrite the keystore.

set -euo pipefail

ALIAS="callingstation"
KEYSTORE_P12="/etc/letsencrypt/live/callingstation.net/keystore.p12"
KEYSTORE_JKS="/etc/letsencrypt/live/callingstation.net/keystore.jks"
PRIVKEY="/etc/letsencrypt/live/callingstation.net/privkey.pem"
FULLCHAIN="/etc/letsencrypt/live/callingstation.net/fullchain.pem"

echo "Stopping the server"

systemctl stop pokrio.service

echo "Renewing the certificate"

sudo letsencrypt renew

# TODO: Not sure if the next line is necessary, remove it if it isn't
# sudo certbot certonly --standalone -d callingstation.net -d www.callingstation.net

echo "Doing some magic with the keys"

openssl pkcs12 -export -out $KEYSTORE_P12 -inkey $PRIVKEY -in $FULLCHAIN -name $ALIAS

keytool -importkeystore -alias $ALIAS -destkeystore $KEYSTORE_JKS -srcstoretype PKCS12 -srckeystore $KEYSTORE_P12

echo "Starting the server again"

systemctl start pokrio.service

echo "Done!"
