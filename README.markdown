Introduction
------------

Pydev is a plugin for Eclipse based IDEs, enabling you to do Python programming the proper way.  
In the humble opinion of the author of this document, it is _the_ best IDE for Python development.  

Best of all: **it's free**!  

And thanks to Java and Eclipse it is cross-platform and contains a slew of rich additional features.

You can find out all about it, by visiting the official [homepage](http://pydev.org). 

Changes
-------

I'll try to keep up with documenting any and all changes from this fork in the [wiki](http://github.com/andreberg/Pydev/wiki/Changelog).


Building
--------

If you plan to contribute back, your first stop should be the [developers info page](http://www.pydev.org/developers.htm).  

However, know that the instructions in section `Creating a distribution locally` may be slightly outdated.  
You will have to inspect carefully all the files involved in the Ant build system.

Specifically, look inside the `builders` folder for a file called `build_local.properties`.  
Open it and adjust paths and settings to match your system. 

I have tried to put additional comments where the intended purpose seemed clear.

In some cases it may be neccessary to fully grasp the mechanisms of the Ant build file, `build.xml`.
This file also references the other two XML files in the folder,  namely `allElements.xml` and `customTargets.xml`.

In most cases you will want to perform a local build since you most likely do not have access to the internal Aptana update site.

Currently, for a complete build you will need to build against Aptana Studio 3 (because of the Django-Template editor feature). 
 
To do this download a fresh copy of Eclipse, 3.5 or later, and then install the Aptana Studio 3 Eclipse plugin.
Or download a fresh copy of standalone Aptana Studio 3. You then specify the path where you downloaded/installed it inside the `build_local.properties` file.

If you want to sign the deployed plugins you will need to have a proper keystore identity in place.
For this fork you will then need to set three environment variables, at least temporarily, to hold your `KEYSTORE` location (e.g. `$HOME/.keystore`), your `STOREPASS` and your `KEYPASS`.   
It is recommended that you unset these variables once the build is complete.

_Tip: on *nix (incl. OS X) you can pass the variables in front of invoking the `ant` executable from a shell._

Installing
----------

If you want to install the plugin from this fork, please go to the [downloads page](http://github.com/andreberg/Pydev/downloads) and download the latest version, usually a file called `pydev-latest.zip` ([direct link](http://github.com/andreberg/Pydev/downloads/pydev-latest.zip)).

The zip contains a compressed update site which you can use to perform an offline install.

To do that, unzip the downloaded archive and add the directory it produces as local software site in Eclipse/Aptana's preferences (under `Install/Update` → `Available Software Sites`)
Once the local software site is in place, you can install Pydev from it via the normal route through the `Help` menu.

If, for whatever reason, you want to install from an online update site, use the following URL:

`http://www.bergmedia.de/remote/github/Pydev/install/pydev_deploy`


Attribution
-----------

Pydev was created by Fabio Zadrozny.  
It is now owned by [Appcelerator, Inc](http://www.appcelerator.com).

License
-------

[EPL (Eclipse Public License)](http://www.eclipse.org/legal/epl-v10.html)