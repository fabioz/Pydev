from static import TestStatic
print TestStatic.static1
class TestStaticExt(TestStatic):
    def __init__(self):
        print self.static1