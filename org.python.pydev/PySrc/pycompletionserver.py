'''
Echoing server.

TODO: THIS IS ONLY A TEST.
'''
import threading
import time
        
from tipper import GenerateTip

class T(threading.Thread):
    
    def run(self):
        # Echo server program
        import socket
        
        HOST = ''                 # Symbolic name meaning the local host
        PORT = 50007              # Arbitrary non-privileged port
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.bind((HOST, PORT))
        s.listen(1)
        conn, addr = s.accept()
        
        #print 'Connected by', addr
        while 1:
            data = conn.recv(1024)
            if not data: 
                break
            
            r = ''
            for d in GenerateTip(data):
                r += d
                r += '|'
            
            #print 'sending data:' , data
            conn.send(r)
            
        conn.close()
        self.ended = True

if __name__ == '__main__':
    t = T()
    t.start()
 
    while(hasattr(t, 'ended') == False):
        time.sleep(1)

        
#  # Echo client program
#    import socket
#    
#    HOST = '127.0.0.1'    # The remote host
#    PORT = 50007              # The same port as used by the server
#    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
#    s.connect((HOST, PORT))
#    s.send('Hello, world')
#    data = s.recv(1024)
#    print 'Received', `data`
#
#
#    s.send('Hello, world again ')
#    data = s.recv(1024)
#    print 'Received', `data`
#
#    s.send('Hello, world once more')
#    data = s.recv(1024)
#    print 'Received', `data`
#
#    s.close()
#    time.sleep(5)
        