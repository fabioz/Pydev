def holder():
    def nested_func():
        print "nested foo"
    
    nested_func()

holder()
