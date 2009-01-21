from static import TestStatic
print TestStatic.static1
class TestStaticExt(TestStatic):
    def __init__(self):
        print self.static1
        from extendable.dependencies.file2 import Test
        import extendable.dependencies.file2.Test
        