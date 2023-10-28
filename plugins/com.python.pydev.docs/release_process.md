##	Update the version:

Open /dev.py, update the version and then run:

cdd X:\liclipsews\liclipsews\Pydev\rootproject
activate py311_64
python -m dev update_version
python -m dev update_typeshed
python -m dev update_pydevd_bins

## Update homepage

- index.rst
- download.contents.rst
- history_pydev.rst (move contents from index.rst if needed)

python -m dev update_version_in_homepage
python x:\liclipsews\pydev.page\build_homepage.py

## Commit everything and merge with master (homepage at: x:\liclipsews\pydev.page and X:\liclipsews\liclipsews\Pydev repo)

mu acp "PyDev release 11.0.2"

## Do build:

python -m dev build_pydev_in_build_dir

## Put things in the proper places and create zips to distribute

python -m dev copy_and_zips

## TODO: Finish automating steps below this line!

## Submit feature and sources .zip in folder X:\pydev_build\build_dir\pydev\features\org.python.pydev.p2-repo\target\runnable to:

"C:\Program Files\FileZilla FTP Client\filezilla.exe" sftp://fabioz,pydev@frs.sourceforge.net/home/pfs/project/p/py/pydev/pydev/ --local="X:\pydev_build\build_dir\pydev\features\org.python.pydev.p2-repo\target\runnable"

Note: check pageant if it does not connect.

Check if the main download button points to the proper place (not to the sources) --
https://sourceforge.net/projects/pydev/files/pydev/PyDev 11.0.1/ -- choose file > file info > set default.


## Add contents to the update site


cd /D X:\pydev_build\build_dir\pydev\features\org.python.pydev.p2-repo\target\

mkdir org.python.pydev.p2-repo-11.0.1-SNAPSHOT

cd org.python.pydev.p2-repo-11.0.1-SNAPSHOT

"C:\Program Files\7-Zip\7z" x ..\org.python.pydev.p2-repo-11.0.1-SNAPSHOT.zip

cdd X:\pydev_build\build_dir\pydev\features\org.python.pydev.p2-repo\target\org.python.pydev.p2-repo-11.0.1-SNAPSHOT
C:\Users\fabio\AppData\Roaming\npm\surge.cmd --domain pydev-11-0-1.surge.sh

## Add update site to SourceForge (create directory with version and push it).

"C:\Program Files\FileZilla FTP Client\filezilla.exe" sftp://fabioz,pydev@frs.sourceforge.net/home/project-web/pydev/htdocs/pydev_update_site --local="X:\pydev_build\build_dir\pydev\features\org.python.pydev.p2-repo\target\org.python.pydev.p2-repo-11.0.1-SNAPSHOT"


## Tag repository:

git tag pydev_11_0_1 -a -m "PyDev 11.0.1"
git push --tags

## Create release in Github Releases

SET CONVERT_SOURCE=X:\pydev_build\build_dir\pydev\features\org.python.pydev.p2-repo\target\org.python.pydev.p2-repo-11.0.1-SNAPSHOT
SET CONVERT_FINAL_ZIP=X:\pydev_build\build_dir\pydev\features\org.python.pydev.p2-repo\target\runnable\PyDev 11.0.1.zip
SET CONVERT_TARGET_DIR=X:\pydev_build\build_dir\pydev\features\org.python.pydev.p2-repo\target\github
python X:\release_tools\convert_to_github.py 11.0.1


## Goto:

https://github.com/fabioz/Pydev/releases/new?tag=pydev_11_0_1
Contents in: X:\pydev_build\build_dir\pydev\features\org.python.pydev.p2-repo\target\github

### Title:
PyDev 11.0.1

### Message:

This release contains PyDev 11.0.1

It's possible to add it as an Eclipse update site using the url:

https://github.com/fabioz/Pydev/releases/download/pydev_11_0_1/

Or get a .zip to install manually by unzipping it in the dropins:

https://github.com/fabioz/Pydev/releases/download/pydev_11_0_1/PyDev.11.0.1.zip


## Update homepage:

Update version in build_homepage.py
cd /D x:\liclipsews\pydev.page
python deploy.py


## update version in eclipse marketplace: http://marketplace.eclipse.org/

## Add news in forum (same as e-mail)

## Send e-mail (use contents from sf e-mail -- change title sizes)

## Add blog post

## Add to reddit: http://www.reddit.com/r/Python/submit

## Twitter
