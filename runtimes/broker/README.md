# Broker

Standalone runtime Java DSA broker.

# Setting up SSL

## Using a certificate you already have

The X.509 certificate must be in PEM format. The private key must be in pkcs8
format. Encryption is optional in the server configuration.

## Creating a self-signed certificate

- openssl genrsa -out tmp.key 4096
- openssl req -new -key tmp.key -out server.csr
- openssl x509 -req -days 365 -in server.csr -signkey tmp.key -out server.pem -outform PEM
- openssl pkcs8 -topk8 -in tmp.key -out server.key
