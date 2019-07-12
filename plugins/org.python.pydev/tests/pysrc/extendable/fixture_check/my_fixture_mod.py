class B(object):
    
    def method1(self):
        pass

    def method2(self):
        pass
    
class C(object):
    
    def method3(self):
        pass

    def method4(self):
        pass


@pytest.fixture
def my_fixture_b():
    return B()

@pytest.yield_fixture
def my_fixture_c():
    yield C()