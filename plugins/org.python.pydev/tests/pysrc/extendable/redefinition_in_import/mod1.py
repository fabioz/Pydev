try:
    from unittest import TestCase
except ImportError:
    from package_that_does_not_exist import TestCase
