class C(A, B):
    def __init__(self):
        print("C")
        
    def kwarg(self, **kwarg):
        print(kwarg)

    def seek(self, pos: int, whence: int, *args, **kwargs) -> int:
        print(kwarg)

