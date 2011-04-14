Code coverage
================

Requisites
-----------

To use the code coverage integration in PyDev, the coverage module is needed. It may
be gotten from: http://pypi.python.org/pypi/coverage (the integration is tested with version 3.4, so, this is the 
recommended version).

After installing it (which may be done through easy_install coverage), don't forget to refresh your interpreter
configuration so that the coverage module is properly recognized by PyDev.

Usage
------

To use the code coverage view, first open it (window > show view > code coverage)

Then, drag the folder which should have coverage info obtained and drop it over the code coverage view.

.. image:: images/codecoverage/coverage_view.png
   :class: snap
   :align: center
   

Check the 'enable code coverage for new launches' (after this step, any launch, regular or unit-test, will be launched
with flags so that code coverage information is obtained).
   
.. image:: images/codecoverage/enable_new_launches.png
   :class: snap
   :align: center
   
   
Then, do a new launch and inspect the new coverage results (clicking on the link will open an editor that allows
opening the file with markers indicating the lines that weren't executed -- note that editing the file or closing it will
remove those markers, but you can always click on the link again to see them, although they may already be unsynchronized at 
that point). 

   
.. image:: images/codecoverage/results.png
   :class: snap
   :align: center
   
   


.. _Video PyDev 2.0: video_pydev_20.html

A video showing code coverage information is available at: `Video PyDev 2.0`_
 
(Note that code coverage is shown towards the end of the video)
   
   