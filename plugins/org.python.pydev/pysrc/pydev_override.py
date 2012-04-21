def overrides(method):
    def wrapper(func):
        if func.__name__ != method.__name__:
            msg = "Wrong @override: %r expected, but overwriting %r."
            msg = msg % (func.__name__, method.__name__)
            raise AssertionError(msg)

        if func.__doc__ is None:
            func.__doc__ = method.__doc__

        return func

    return wrapper

def implements(method):
    def wrapper(func):
        if func.__name__ != method.__name__:
            msg = "Wrong @implements: %r expected, but implementing %r."
            msg = msg % (func.__name__, method.__name__)
            raise AssertionError(msg)

        if func.__doc__ is None:
            func.__doc__ = method.__doc__

        return func

    return wrapper