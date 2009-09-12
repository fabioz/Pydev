class Test(object):

    def method(self):##|
        print "method"##|
        def nested():
            print "nested"
        nested()

##r

class Test(object):

    def method(self):
        '''
        ##|
        '''
        print "method"
        def nested():
            print "nested"
        nested()
