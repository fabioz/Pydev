'''
Echoing server.

TODO: THIS IS ONLY A TEST.
'''
import threading
import time
        

END_MSG = "@END@"

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
        data = ''
        
        #print 'Connected by', addr
        while 1:
            data = conn.recv(1024)
            while not data.endswith(END_MSG):
                data += conn.recv(1024)

            if not data: 
                break
            
            else:
                conn.send('other command')

        conn.close()
        self.ended = True

if __name__ == '__main__':
    t = T()
    t.start()
 
    while(hasattr(t, 'ended') == False):
        time.sleep(1)
