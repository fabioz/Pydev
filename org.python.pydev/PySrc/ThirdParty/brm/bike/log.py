import sys
class SilentLogger:
    def write(*args):
        pass

progress = SilentLogger()
warning = SilentLogger()
#warning = sys.stderr
