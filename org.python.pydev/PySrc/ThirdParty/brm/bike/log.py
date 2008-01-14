import sys
class SilentLogger:
    def write(*args): #@NoSelf
        pass

progress = SilentLogger()
warning = SilentLogger()
#warning = sys.stderr
