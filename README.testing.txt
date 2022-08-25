Noteworthy details when testing PyDev:
=======================================

For running the tests the file:

org.python.pydev.core/tests/org.python.pydev.core/TestDependent.<OS>.properties must
have the values set regarding to the computer that'll execute the tests.

Note that to make sure that PyDev keeps working on the long run, usually tests
are required for pull requests (unless it's a really trivial change).


Caches during testing
=======================

The test setup is usually the slow part of the test because it may have to
rescan the whole Python sources to rebuild index information.

It's possible to set org.python.pydev.plugin.PydevTestUtils.ERASE_TEST_DATA_CACHES
to False to prevent caches from being erased between runs.


Disabling tests
================

If for some reason a test has to be disabled (for instance because it's too brittle
or because it's a work in progress or a TODO), it should do so checking the value of:

SharedCorePlugin.skipKnownFailures()

-- Thus it's possible to check for references of that method to see skipped tests.