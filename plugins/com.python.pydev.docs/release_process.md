##	Update the version:

1. Update version in this file
2. Update version on dev.py

cdd X:\liclipsews\liclipsews\Pydev\rootproject
deactivate
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

mu acp "PyDev release 11.0.3"

## Do build:

Plugin token for signing

python -m dev build_pydev_in_build_dir

## Put things in the proper places and create zips to distribute
## Submit feature and sources .zip in folder X:\pydev_build\build_dir\pydev\features\org.python.pydev.p2-repo\target\runnable to:
## Add contents to the update site mirrors

python -m dev copy_and_zips
python -m dev copy_zips_to_sf
python -m dev add_to_update_site_mirror

## Tag repository (needed so that GitHub can reference it later)

git tag pydev_11_0_3 -a -m "PyDev 11.0.3"
git push --tag origin pydev_11_0_3

## Create release in Github Releases

python -m dev add_to_github

Contents in: X:\pydev_build\build_dir\pydev\features\org.python.pydev.p2-repo\target\github

### Title:
PyDev 11.0.3

### Message:

This release contains PyDev 11.0.3

It's possible to add it as an Eclipse update site using the url:

https://github.com/fabioz/Pydev/releases/download/pydev_11_0_3/

Or get a .zip to install manually by unzipping it in the dropins:

https://github.com/fabioz/Pydev/releases/download/pydev_11_0_3/PyDev.11.0.3.zip


## Update homepage:

cd /D x:\liclipsews\pydev.page
python deploy.py

## update version in eclipse marketplace: http://marketplace.eclipse.org/

## Add news in forum (same as e-mail): https://sourceforge.net/p/pydev/news/new

## Send e-mail (use contents from sf e-mail -- change title sizes)

## Add blog post

## Add to reddit: http://www.reddit.com/r/Python/submit

## Twitter
