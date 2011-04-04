Unittest integration
=========================

On PyDev 1.6.4, an improved unittest support was introduced. It allows using different test runners (the default
PyDev test runner, nose or py.test) and allows seeing the results in a view (PyUnit view) with a red/green bar
which also allows re-running tests. 

.. contents:: 


Configuring test runners
----------------------------------

The preferences page to configure the test runner is accessible from the PyUnit view at the dropdown menu > configure test runner preferences
(or at window > preferences > PyDev > PyUnit)

.. image:: images/py_unit/py_unit_preferences.png
   :class: snap
   :align: center

**Note**: the flags to choose the tests in the test runner should not be specified 
(they are properly managed by PyDev in each test run)



Configuring the PyDev test runner
----------------------------------
The options that the PyDev unittest accepts are:

 **--verbosity=number**
    Sets the verbosity level for the run 
    
    
 **--jobs=number** 
    The number of processes to be used to run the tests
    
    
 **--split_jobs=tests|module**
    if **tests** is passed (default), the tests will be split independently to each process
    if **module** is passed, a given job will always receive all the tests from a module


An example of options that can be set in the preferences would be:

**--verbosity=1 --jobs=2 --split_jobs=module**



Configuring the Nose test runner
---------------------------------

Note: the integration was tested with version 1.0

The options that the nose test runner accepts can be found at http://somethingaboutorange.com/mrl/projects/nose

An example of options that can be set in the preferences would be:

**--verbosity=2 --processes=2**

--verbosity=2 (set the verbosity level to 2)

--processes=2 (use 2 processes to run the tests) 



Configuring the Py.test test runner
------------------------------------

Note: the integration was tested with version 2.0

The options that the py.test test runner accepts can be found at http://pytest.org 

An example of options that can be set in the preferences would be:

**-maxfail=2 --tb=native**

-maxfail=2 (stop at 2nd failure)

--tb=native (will show tracebacks in the default standard library formatting)

**Note**: currently when using the xdist plugin, the results won't be properly shown in the PyUnit view.



PyUnit view
------------

The PyUnit view may be used to see the results of tests being run, their output, time, re-running tests, among others.

.. image:: images/py_unit/py_unit_view.png
   :class: snap
   :align: center

The most interesting features related to seeing the tests are:

- The results of tests are shown, along with a green/red bar depending whether all the tests succeeded or not (or if you're with Aptana Studio, following the color theme)
- The time a test took to run is shown (and may be used to sort the tree)
- The errors/output are shown by selecting the test run or just hovering over the items
- A filter to show only errors is available

And the most interesting actions are:

- A test session can be rerun
- A new test session can be created to rerun only the errors of the current run
- The test session can be stopped
- The results of a previously run test session can be seen again


Running and showing results in the PyUnit view
-----------------------------------------------

To show the results in the PyUnit view, a **unittest run** must be done. If running the tests when editing a module, **Ctrl+F9** may be used
to run the tests in that mode (and choosing which tests should be run), otherwise, right-click a folder or python file and choose **Run as > Python Unittest**
