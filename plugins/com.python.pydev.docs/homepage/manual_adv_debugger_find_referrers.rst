Debugger Find Referrers
========================

During a debug session it's possible to find referrers to a given instance and
see them in a view (and then in that view it's possible to iteratively go
deeper by seeing referrers of referrers).

This is especially useful when trying to debug leaks to a variable in a program.

To get the referrers of a given object, it's possible to go to the variables view,
right-click the variable for which you want the referrers and then select **Get Referrers**
as in the image below:

.. figure:: images/referrers/referrers.png
   :align: center

Afterwards, it's possible to right-click a variable in the referrers view and
get the referrers of that instance.

Note that the process of getting referrers can sometimes be slow depending on the
number of live instances in your program and how many referrers are found.