#!/usr/local/bin/python

import SimpleHTTPServer
import SocketServer

PORT = 4502

Handler = SimpleHTTPServer.SimpleHTTPRequestHandler

httpd = SocketServer.TCPServer(("", PORT), Handler)

print "serving at port", PORT
httpd.serve_forever()
