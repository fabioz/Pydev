def holder():
    def nestedFunc():
        print "nested foo"
    
    nestedFunc()

holder()
