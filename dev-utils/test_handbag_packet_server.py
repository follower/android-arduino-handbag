# Used during development to serve up Handbag-format packets over TCP/IP
# before the app or library are expanded to do so.

import SocketServer

import select

import errno
import socket

import random

import time

from generate_handbag_packet import createPacket

from parse_handbag_packet import PacketParser


class PacketServerHandler(SocketServer.StreamRequestHandler):
    """
    """

    _parser = None

    _widgetId = None


    def _getNumBytesAvailable(self):
        """

        (Non-blocking)

        Returns: number of bytes available or -1 when connection has been closed
        """

        numBytesAvailable = 0

        if select.select([self.request],[],[],0.1)[0]:
            try:
                numBytesAvailable = len(self.request.recv(1024, socket.MSG_PEEK))
            except socket.timeout:
                # TODO: Treat timeout as an error?
                pass
            else:
                if numBytesAvailable == 0:
                    # When this happens it means the connection has closed
                    # TODO: Throw exception on connection closed?
                    numBytesAvailable = -1

        return numBytesAvailable


    def _getNextPacket(self):
        """

        Returns: A packet or [] (timeout/no data) or None (connection closed)
        """

        packet = []

        # Non-blocking check for data available
        numBytesAvailable = self._getNumBytesAvailable()

        if numBytesAvailable == -1:

            # TODO: Throw exception on connection closed?
            packet = None

        elif numBytesAvailable > 0:

            try:
                packet = self._parser.nextPacket()
            except socket.timeout:
                # TODO: Treat timeout as an error?
                pass

        return packet


    def setupUI(self):
        """
        """
        # TODO: Override this when subclassed.

        # TODO: Remove this test code

        # TODO: Use helper methods
        data = ["widget", "label", self._widgetId, 30, 0, "My Label;\nHere, forever."]
        self._widgetId+=1

        self.wfile.write(createPacket(data))


        data = ["widget", "button", self._widgetId, 0, 0, "Push It!"]
        self._widgetId+=1

        self.wfile.write(createPacket(data))


    def loop(self):
        """
        """


    def handle(self):
        """
        """

        self._widgetId = 1

        print "Client: %s" % str((self.request.getpeername()))

        print self.__dict__

        self._parser = PacketParser(self.rfile)

        # TODO: Do handshake/version check?

        try:

            self.setupUI()

            while True:

                ## Idle (shouldn't be needed once we use non-blocking I/O on Android)
                self.wfile.write(createPacket(["idle"])) # TODO: properly

                ## Get and Process next packet
                packet = self._getNextPacket()

                if packet is None:

                    # Remote disconnected
                    break

                elif packet:

                    # TODO: Process packet
                    print packet

                ## Call user code
                self.loop()

        except socket.error, e:

            print e

            if e[0] in [errno.EPIPE, errno.ECONNRESET]:
                print "Remote disconnect"
                try:
                    self.wfile.close()
                except socket.error, e:
                    print "Error on remote disconnect cleanup: " + e
            else:
                raise e

        finally:
            # problem with borken pipe?
            try:
                self.request.shutdown(socket.SHUT_RDWR)
                self.request.close()
            except socket.error, e:
                if e[0] == errno.ENOTCONN: # "Socket is not connected"
                    print "Can't shutdown/close remote already disconnected"
                else:
                    raise e

        print "bye"

        return

        for i in range(4):
            data = ["widget", "label", widgetId, random.randint(0, 40), 0, "My Label;\nHere, forever."]

            widgetId+=1

            self.wfile.write(createPacket(data))

        # TODO: Keep connection open and handle this in an "event loop".
        while 1:
            try:
                packet = parser.nextPacket()
            except socket.timeout:
                break
            else:
                print packet

        try:
            for i in range(21):
                data = ["widget", "label", widgetId, 50, 0x01, i]
                self.wfile.write(createPacket(data))
                time.sleep(0.1)
        except socket.error, e:
            print e

            try:
                self.wfile.close()
            except socket.error, e:
                print e

            return

        widgetId+=1

        for i in range(0, 101, 5):
            data = ["widget", "progress", widgetId, i]
            self.wfile.write(createPacket(data))
            time.sleep(0.05)

        widgetId+=1

        data = ["feature", "speech", "speak", "Terrain! Terrain! Pull up! Pull up!", 1.0, 1.0]

        self.wfile.write(createPacket(data))

        # Note: Because of the lack of TTS connection reuse this doesn't
        #       really work currently.
        time.sleep(2)

        data = ["feature", "speech", "speak", "Just kidding! Hello from Handbag.", 1.0, 1.0]
        self.wfile.write(createPacket(data))

        data = ["widget", "dialog", "I dialogued with you!"]

        self.wfile.write(createPacket(data))

        # TODO: Attach this to a button
        data = ["feature", "sms", "send", "Me", "Hello from Handbag!"]

        # Don't send this by default
        #self.wfile.write(createPacket(data))

        self.request.shutdown(socket.SHUT_RDWR)
        self.request.close()



if __name__ == "__main__":

    socket.setdefaulttimeout(0.5)

    # Note: This method of getting the local IP address may not work
    #       in all situations.
    HOST, PORT = socket.gethostbyname(socket.gethostname()), 0xba9

    print "Host: %s Port: 0x%x" % (HOST, PORT)

    # Create the server, binding to localhost on port 2985 (0xba9)
    server = SocketServer.TCPServer((HOST, PORT), PacketServerHandler)

    # Avoid "socket.error: [Errno 48] Address already in use" errors.
    server.socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    server.serve_forever()


    
