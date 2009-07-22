
Download (Current release: **LAST_VERSION_TAG**)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. contents::

Notes
~~~~~~

* The Pydev Extensions version must always be accompanied by the same version of the Pydev "Open Source" version.
* If you already have the matching Open Source version installed, it will not appear in the Eclipse update manager.



Requirements
~~~~~~~~~~~~~

.. _Python: http://www.python.org
.. _Jython: http://www.jython.org
.. _Iron Python: http://www.codeplex.com/Wiki/View.aspx?ProjectName=IronPython
.. _Eclipse 3.2, 3.3, 3.4 or 3.5: http://www.eclipse.org
.. _Java: http://www.javasoft.com
.. _JDT: http://www.eclipse.org/jdt/
.. _Platform Runtime Binary: http://download.eclipse.org/eclipse/downloads/

At least one of:

* Python_ **(2.1 or newer)**
* Jython_ **(2.1 or newer)**
* `Iron Python`_ **(2.6 or newer)**

and 

* `Eclipse 3.2, 3.3, 3.4 or 3.5`_ -- Python_ and `Iron Python`_ require only the `Platform Runtime Binary`_ (download around 45-50 MB), and Jython_ also requires JDT_.
* Java_ 1.4 or higher


.. _http://pydev.sourceforge.net/updates: http://pydev.sourceforge.net/updates
.. _http://update.aptana.com/update/pydev/3.2: http://update.aptana.com/update/pydev/3.2
.. _http://pydev.sourceforge.net/updates_old: http://pydev.sourceforge.net/updates_old
.. _http://nightly.aptana.com/pydev/site.xml: http://nightly.aptana.com/pydev/site.xml
.. _SourceForge download: http://sourceforge.net/project/showfiles.php?group_id=85796
.. _http://www.fabioz.com/pydev/updates: http://www.fabioz.com/pydev/updates
.. _http://www.fabioz.com/pydev/updates_old: http://www.fabioz.com/pydev/updates_old
.. _http://nightly.aptana.com/pydev-pro/site.xml: http://nightly.aptana.com/pydev-pro/site.xml
.. _http://update.aptana.com/update/pydev-pro/3.2: http://update.aptana.com/update/pydev-pro/3.2
.. _http://www.fabioz.com/pydev/zips: http://www.fabioz.com/pydev/zips

Urls to use when updating with the Eclipse update manager
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

:Mirrors for 1.4.3 onwards:

    
    :Containing only Pydev:        
        * `http://pydev.sourceforge.net/updates`_
        * `http://update.aptana.com/update/pydev/3.2`_

    :Containing Pydev and Pydev Extensions:
        * `http://www.fabioz.com/pydev/updates`_
    
    :Containing only Pydev Extensions:    
        * `http://update.aptana.com/update/pydev-pro/3.2`_
    
:Nightly builds: 
    
    :Containing only Pydev:
        * `http://nightly.aptana.com/pydev/site.xml`_
    
    :Containing only Pydev Extensions:
        * `http://nightly.aptana.com/pydev-pro/site.xml`_

:Before 1.4.3: 
    
    :Containing only Pydev:        
        * `http://pydev.sourceforge.net/updates_old`_
    
    :Containing Pydev and Pydev Extensions:    
        * `http://www.fabioz.com/pydev/updates_old`_        
        
        


Get zip releases
~~~~~~~~~~~~~~~~~~
:Containing only Pydev:        
    * `SourceForge download`_

:Containing Pydev and Pydev Extensions:    
    * `http://www.fabioz.com/pydev/zips`_
    

Fast Install Instructions
~~~~~~~~~~~~~~~~~~~~~~~~~~

**Update Manager**

    Go to the update manager (inside the help menu) and add one of the urls 
    specified above and follow the wizard (Eclipse should do the rest)

**Zip File**

    An alternative is just getting the zip file and extracting it yourself in eclipse. 
    
    For **Eclipse 3.2 and 3.3**, you have to make sure the plugins folder 
    is extracted on top of the Eclipse plugins folder and **restart with '-clean'**.
    
    For **Eclipse 3.4 and 3.5**, you can extract it in the '**dropins**' folder (and restart Eclipse).


Complete Install
~~~~~~~~~~~~~~~~~~

.. _http://www.fabioz.com/pydev/manual_101_root.html: http://www.fabioz.com/pydev/manual_101_root.html

There is a 'complete install guide' at `http://www.fabioz.com/pydev/manual_101_root.html`_
so, if you have any problems in the install, that's the place you should check 
(it also guides you through configuring pydev correctly).

