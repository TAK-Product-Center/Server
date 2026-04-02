// server.js

const express = require('express');
const https = require('https');
const fs = require('fs');
const forge = require('node-forge');
const { URL } = require('url');

// Target server
const targetBase = 'https://localhost:9100';
const port = 5200;

const p12Path = '/opt/tak/certs/files/admin.p12';
const trustPath = '/opt/tak/certs/files/truststore-root.p12';
const p12Password = 'atakatak';

function extractFromP12(p12Path, password) {
  const p12Buffer = fs.readFileSync(p12Path);
  const p12Asn1 = forge.asn1.fromDer(p12Buffer.toString('binary'));
  const p12 = forge.pkcs12.pkcs12FromAsn1(p12Asn1, password);

  let key, cert;
  const bags = p12.getBags({ bagType: forge.pki.oids.pkcs8ShroudedKeyBag });
  try {
    key = forge.pki.privateKeyToPem(bags[forge.pki.oids.pkcs8ShroudedKeyBag][0].key);
  } catch(e){}

  const certBags = p12.getBags({ bagType: forge.pki.oids.certBag });
  try {
    cert = forge.pki.certificateToPem(certBags[forge.pki.oids.certBag][0].cert);
  } catch(e){}

  return { key, cert };
}

// Load client cert and truststore
const client = extractFromP12(p12Path, p12Password);
const truststore = extractFromP12(trustPath, p12Password);

const agent = new https.Agent({
    key: client.key,
    cert: client.cert,
    ca: truststore.cert,
    rejectUnauthorized: false
});

const app = express();

app.use('/fig', (req, res) => {

  // Build the full target URL
  const targetUrl = new URL(req.originalUrl, targetBase);

  // Set up options for the proxied request
  const options = {
    method: req.method,
    headers: { ...req.headers },
    agent, // important for mTLS
  };

  // Create proxied request
  const proxyReq = https.request(targetUrl, options, (proxyRes) => {
    res.writeHead(proxyRes.statusCode, proxyRes.headers);
    proxyRes.pipe(res, { end: true });
  });

  // Handle proxy errors
  proxyReq.on('error', (err) => {
    console.error('Proxy request error:', err);
    res.status(502).send('Proxy error');
  });

  // Pipe the incoming request body into the proxied request
  req.pipe(proxyReq, { end: true });
});

// Middleware to parse JSON bodies (this must come AFTER the proxy request)
app.use(express.json());


// Start the server
app.listen(port, () => {
  console.log(`Server is running on http://localhost:${port}`);
});
