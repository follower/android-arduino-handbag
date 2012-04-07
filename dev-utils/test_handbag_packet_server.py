# Used during development to serve up Handbag-format packets over TCP/IP
# before the app or library are expanded to do so.

import SocketServer

import socket

from generate_handbag_packet import createPacket

widgetId = 1

class PacketServerHandler(SocketServer.BaseRequestHandler):
    """
    """
    
    def handle(self):
        """
        """
        global widgetId

        print "Client: %s" % str((self.request.getpeername()))

        data = ["widget", "label", widgetId, 0, 0, "My Label;\nHere, forever."]

        widgetId+=1

        return self.request.sendall(createPacket(data))

if __name__ == "__main__":

    # Note: This method of getting the local IP address may not work
    #       in all situations.
    HOST, PORT = socket.gethostbyname(socket.gethostname()), 0xba9

    print "Host: %s Port: 0x%x" % (HOST, PORT)

    # Create the server, binding to localhost on port 2985 (0xba9)
    server = SocketServer.TCPServer((HOST, PORT), PacketServerHandler)

    # Avoid "socket.error: [Errno 48] Address already in use" errors.
    server.socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    server.serve_forever()


    
