"""test undefined global
"""

__revision__ = 0

def function():
    """use an undefined global
    """
    global AAAA
    print AAAA

