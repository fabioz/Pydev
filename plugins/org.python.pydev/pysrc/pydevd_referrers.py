def print_referrers(obj):
    import sys
    del sys.modules['pydevd_referrers']
    
    import gc
    referrers = gc.get_referrers(obj)
    print 'referrers for', obj
    for r in referrers:
        print type(r)
    
    return 'ok'
