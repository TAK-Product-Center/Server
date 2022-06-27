## Changes from previous scripts:
##   - massive cleanup, actually easy to set certificate metadata now
##   - add in V3 attributes to limit usage of certificates appropriately
##     (e.g., a client cert cannot be used as a server cert)
##   - support for multi-tier CA configurations


To use these scripts:
## Step 1: Make the root CA
First edit cert-metadata.sh to have appropriate locality information for the 
CA you want to make.

If desired, you can also edit config.cfg and set different "basicConstraints"
in the "v3_ca" section.  Speficially, you can set the pathlen attribute if you
want to limit the depth of the verification chain (only applies if doing multi-
tier CA configuration).

Finally, run 
 ./makeRootCa.sh

You should now have a 'files' subdirectory with a pem and key file

## Step 1.a: Decide if you have an OCSP server
If you have an OCSP server that the client and server will be able to reach to
check revocation status of certificates, you can edit the 'client' and 
'server' sections of config.cfg to add the authorityInfoAccess attribute.

## Step 2: make server cert(s)
Edit cert-metadata.sh if you want to add more detail to the locality for a
given certificate.  Decide what you want the CommonName for your certificate
to be.  If possible, use the DNS name of the machine running the server. 
In this case, we'll call it foo.bar.com; run the './makeCert.sh' script:
 ./makeCert.sh server "foo.bar.com"
The script will add the appropriate v3 attributes for server name or IP address
so be aware that you may run into trouble if you try to take the same server
cert and move it to another machine.

## Step 3: make client cert(s)
Edit cert-metadata.sh if you want to add more detail to the locality for a
given certificate.  Decide what you want the CommonName for your certificate
to be (in our example, we'll use 'foo@bar.com'), then run the ./makeCert.sh 
script with the client parameter:
  ./makeCert.sh client "foo@bar.com"


## Optional: multi-tier CA:
The ./makeCert.sh script can also be used with the 'ca' parameter to make new
CAs that are signed by the root-CA.  For example:
 ./makeCert.sh ca intermediate-CA

The generated truststore-intermediate-CA.[p12|jks] will have all the CAs in the
validation chain, so that is what you should use for your server and clients.

## Note: steps below can be done automatically by the script when making a CA
To make new server or client certs that are signed my the new intermediate-CA,
go into the 'files' directory, and copy the various files over the base 'ca' 
files:
 cp intermediate-CA.pem ca.pem
 cp intermediate-CA-trusted.pem ca-trusted.pem
 cp intermediate-CA.key ca-do-not-share.key

The Root-CA's version of those files are already saved under a different name 
(e.g., root-ca.pem). Those files are used by the 'makeCert.sh' script.  So in 
this way you can create arbitrarily deep chains of CAs.  

