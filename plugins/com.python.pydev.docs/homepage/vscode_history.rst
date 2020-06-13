History For PyDev on VSCode
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


Release 0.2.4 (2020-06-13)
=============================

* Critical bugfix getting quick fixes.
* Critical bugfix for method navigation.


Release 0.2.3 (2020-06-09)
=============================

* Critical bugfix getting the workspace folders.
* Critical bugfix getting settings.


Release 0.2.1 (2020-06-05)
=============================

* Upgraded to PyDev 7.5.0

	* Support for Python 3.8
	* Multiple improvements in type inference
	* Improvements in code-formatting
	* Improvements on auto import location


Release 0.1.5 (2018-05-05)
=============================

* Fixed issue validating license

* Upgraded to PyDev 6.3.3

	* Code-formatter properly working with matrix multiplication **@=**

Release 0.1.4 (2018-03-17)
=============================

* Fixed issue starting language server when no workspace is opened.

* Upgraded to PyDev 6.3.2

	* Improvements in type inference.

Release 0.1.3 (2018-02-26)
=============================

* Upgraded to PyDev 6.3.1

	* Improvements in type inference.

Release 0.1.2 (2018-02-26)
=============================

* Fix parsing f-strings.
* Fix restoring workspace symbols information from cache.
* No longer require **__init__** in folders to consider it a package (Python 3 no longer needs **__init__** files in each package).
* Update code which gets java executable to start the server when unable to find from a variable.
* Show at most 300 items when getting workspace symbols to avoid delay in vscode when returning too many items.

Release 0.1.1 (2018-02-19)
=============================

* Properly register command to show pydev configuration on server

Release 0.1.0 (2018-02-19)
=============================

Initial release.

Features:

* Code-completion
    * Fast
    * Context sensitive
    * Common tokens
    * Context insensitive with auto import

* Code formatter
    * Fast
    * Format ranges
    * Format on type

* Code analysis
    * Real time

* Go to definition

* Symbols for Workspace

* Symbols for open editor

* Find references

* Quick fix for undefined variables (adds missing import)

* Navigate to previous or next class or method (Ctrl+Shift+Up, Ctrl+Shift+Down)
