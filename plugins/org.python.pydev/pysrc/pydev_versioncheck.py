import sys

def versionok():
    ''' Return True if running Python is suitable for GUI Event Integration and deeper IPython integration '''
    # We require Python 2.6+ ...
    if sys.hexversion < 0x02060000:
        return False
    # Or Python 3.2+
    if sys.hexversion >= 0x03000000 and sys.hexversion < 0x03020000:
        return False
    # Not supported under Jython
    if sys.platform.startswith("java"):
        return False

    return True

