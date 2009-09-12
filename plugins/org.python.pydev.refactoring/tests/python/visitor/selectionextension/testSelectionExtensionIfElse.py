from twisted.internet import protocol
from twisted.internet import reactor

class Echo(protocol.DatagramProtocol):

    a = 5
    def startProtocol(self):
        return AbstractDatagramProtocol.startProtocol(self)


    def connectionRefused(self):
        return DatagramProtocol.connectionRefused(self)


    def makeConnection(self, transport):
        print "got a connection"

    
    def datagramReceived(self, data, (host, port)):
        if (port > 80):
            print "bigger than 80"
            print "received %r from %s:%d" % (data, host, port)
        elif(port == 9999):
    ##|        print "is 9999"
            print "cool hm"
       ##| else:
            print "wow it works"
            var = a*a
        self.transport.write(data, (host, port))
        
reactor.listenUDP(9999, Echo())
reactor.run()

##r selection starts somewhere before the "while"-node, still have to normalize selected code in order to parse it

print "is 9999"
print "cool hm"