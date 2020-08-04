class Endpoint(object):
    def notify(self):
        pass

class Bar(object):
    def __init__(self):
        self._endpoint = Endpoint()