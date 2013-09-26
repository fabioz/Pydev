About jpathwatch
================

jpatchwatch is a Java library for monitoring directories for changes.  It uses
 the host platform’s native OS functions to achieve this to avoid polling.

The following events on a directory can be monitored:

    * File creation and deletion
    * File modification
    * File renaming*
    * Changes in subdirectories* (recursive monitoring)

(*) selected platforms only, see Features.

Currently the following platforms are supported natively:

    * Windows (Windows 2000, XP, Vista, 7)
    * Linux
    * Mac OS X (x86, 10.6)
    * FreeBSD (x86)

Minimum required Java Platform:

    * Java 5

Because jpathwatch’s native libraries are packaged within it’s JAR file, there
is no setup required that’s common to most Java libraries leveraging native
code. It works just like any other pure Java library: Drop it into your IDE,
and it just works. For this reason, jpathwatch can also be easily integrated
into Java WebStart applications.


Version History
===============

0.95
----
* Fixed: On Windows, files with single-character file names did not generate
  events (bug in Windows native library).
* Fixed: On BSD/MacOSX, there was an issue with stale watch keys where a watch
  was registered, removed, and then re-registered.

0.94
----
* 64 bit support for Windows added
* WatchService.poll() was interpreting the timeout value wrong for units other than milliseconds.
* On Windows, jpathwatch now supports waiting on more than 63 directories simultaneously (other platforms never had such a limit)
* ENTRY_MODIFY is now reported more frequently on Windows (but note there is no guarantee on any platform how often modify events are reported)
* Fixed bug where non-ascii characters in directory names made these directories unmonitorable
* Overflow is now correctly detected and reported on Linux (was ignored prevously rare high-load cases)

0.93
----
* Windows version now uses a monitor thread, which fixes glitches and missed events on that platform
* Fix for Linux so that 'unknown watch descriptor: -1' like warnings won't occur any more on calling cancel()
* jpathwatch now uses JDK Logger, so users can configure whatever blurb is left of jpathwatch (there shouldn't be any, actually, unless something's wrong...)
* Class-Path problem in Manifest is fixed now, so no annoying problems with some build tools
* KEY_INVALID event added. This event will be issued when the key becomes invalid by an external event, such as that the watched directory becomes unavailable.
* Fixed problems with WatchKey.reset() on BSD/OSX
* 64 bit support on Linux added
* Fixed ENTRY_MODIFY events on BSD/OSX and polling fallback. ENTRY_MODIFY should now work more accurtately.

0.92
----
* Fixed major bug: file names in libraries were wrong on all platforms except
Linux

0.91
----

* Fixed a bug in the linux path watch service
* Added polling fallback for platforms for which we have no native library

0.90
----

Initial revision. Did not have polling fallback

