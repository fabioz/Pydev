Configuring Eclipse to suit your needs
======================================

Well, Eclipse has lots of things you can configure, and below are some
things that really make a difference...

Configuring auto-refresh
========================

Eclipse by default does not refresh on file changes
automatically, so, if you make changes outside of the workspace, you
will not see the changes until you refresh it (F5 on the navigator).
However, you can change the default setting and ask Eclipse to refresh
automatically.

To set auto-refresh, go to **window > preferences > general >
workspace** and check the **refresh using native hooks or polling** and **refresh on access** check-boxes. 

**NOTE:** not doing so may have some specially strange results with .pyc files not
being deleted, as PyDev will only acknowledge that the .pyc file exists
on a refresh.

Note: This is automatically done in LiClipse

Set workspace to be utf-8 
==========================

The default encoding is possibly not utf-8 (depending on your OS, it can be cp1252, ascii, etc),
so, the suggestion is setting the default encoding to utf-8 in **window > preferences > general > workspace > text file encoding** 
(and you may also want to change the new line delimiter to be unix style on Mac and Windows in that same page).

Note: This is automatically done in LiClipse

