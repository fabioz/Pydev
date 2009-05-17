import sys
import weakref

def SetUp():
    observable = Observable()
    observer = Observer()
    observable.AddObserver(observer)
    return observable


class Observable(object):
    def __init__(self):
        self.observers = []
        
    def AddObserver(self, observer):
        print 'observer', observer
        ref = weakref.ref(observer)
        self.observers.append(ref)
        print 'weakref:', ref()
        
    def Notify(self):
        for o in self.observers:
            o = o()
            
            
            try:
                import gc
            except ImportError:
                o = None #jython does not have gc, so, there's no sense testing this in it
            
            if o is not None:
                print 'still observing', o
                print 'number of referrers:', len(gc.get_referrers(o))
                frame = gc.get_referrers(o)[0]
                frame_referrers = gc.get_referrers(frame)
                print 'frame referrer', frame_referrers
                referrers1 = gc.get_referrers(frame_referrers[1])
                print referrers1
                print >> sys.stderr, 'TEST FAILED: The observer should have died, even when running in debug'
            else:
                print 'TEST SUCEEDED: observer died'
                
            sys.stdout.flush()
            sys.stderr.flush()
                
class Observer(object):
    pass

    
def main():
    observable = SetUp()
    observable.Notify()
    
    
if __name__ == '__main__':
    main()
