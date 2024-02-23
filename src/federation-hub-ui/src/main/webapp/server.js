const express = require('express')
const { createProxyMiddleware } = require('http-proxy-middleware');
const app = express();
const port = 3000;
var path = require("path");

// these need to go first:
app.use("/images", express.static(__dirname + "/images"));
app.use("/scripts", express.static(__dirname + "/scripts"));
app.use("/modules", express.static(__dirname + "/modules"));
app.use("/styles", express.static(__dirname + "/styles"));
app.use("/lib", express.static(__dirname + "/lib"));
app.use("/bowerDependencies", express.static(__dirname + "/bowerDependencies"));
app.use("/views", express.static(__dirname + "/views"));
//app.use("/fig/**", createProxyMiddleware({ target: 'https://[::1]:9100', changeOrigin: true, secure: false }));

app.get('/',function(req,res){  
    res.sendFile(path.join(__dirname+'/home.html')); 
 });

app.listen(port, () => console.log(`Example app listening on port ${port}!`))