from twisted.internet.protocol import DatagramProtocol
from twisted.internet import reactor

class Echo(DatagramProtocol):

    def connectionRefused(self):
        return DatagramProtocol.connectionRefused(self)


    def makeConnection(self, transport):
        print "got a connection"

    
    def datagramReceived(self, data, (host, port)):
        if (port > 80):
          ##|  print "bigger than 80"
            pr##|int "received %r from %s:%d" % (data, host, port)
        elif(port == 9999):
            print "is 9999"
            print "cool hm"
        else:
            print "wow it works"
            var = host+":"+port
        self.transport.write(data, (host, port))
        print var

reactor.listenUDP(9999, Echo())
reactor.run()

##r selection starts somewhere before the "while"-node, still have to normalize selected code in order to parse it

from twisted.internet.protocol import DatagramProtocol
from twisted.internet import reactor

class Echo(DatagramProtocol):

    def extracted_method(self, data, host, port):
        print "bigger than 80"
        print "received %r from %s:%d" % (data, host, port)


    def connectionRefused(self):
        return DatagramProtocol.connectionRefused(self)


    def makeConnection(self, transport):
        print "got a connection"

    
    def datagramReceived(self, data, (host, port)):
        if (port > 80):
            self.extracted_method(data, host, port)
        elif(port == 9999):
            print "is 9999"
            print "cool hm"
        else:
            print "wow it works"
            var = host+":"+port
        self.transport.write(data, (host, port))
        print var

reactor.listenUDP(9999, Echo())
reactor.run()