""" Copyright (c) 2002-2003 LOGILAB S.A. (Paris, FRANCE).
http://www.logilab.fr/ -- mailto:contact@logilab.fr

Distutils extensions for twisted framework.

This module enables the installation of plugins.tml files using standard
distutils syntax. It adds the following commands to the standard
setup.py commands:
* build_twisted_plugins: build (i.e. copy) plugins
* install_twisted_plugins: install plugins

Additionally, the following commands have been modified to deal with
plugins files:
 * sdist
 * build
 * install

To use these extenstion, you should import the setup fonction from this
module, and use it normally. To list the plugins.tml files, use the
twisted_plugins keyword argument to the setup function:

from twisted_distutils import setup # you can also import Extension if needed

if __name__ == '__main__':
    setup(name='my_twisted_app',
          version='1.0',
          author='me',
          packages=['my_package'],
          twisted_plugins = ['my_package/plugins.tml'])

Note that you can use this to install files that are not twisted plugins in any
package directory of your application.
"""
#
# (c) 2002 Alexandre Fayolle <alexandre.fayolle@free.fr>
# This module is heavily based on code copied from the python distutils
# framework, especially distutils.command.build_script,
# distutils.command.install_script. Many thanks to the authors of these
# modules.
# This module is provided as is, I'm not responsible if anything bad
# happens to you or your python library while using this module. You may
# freely copy it, distribute it and use it in your library or to distribute
# your applications. I'd appreciate if you could drop me an email if you plan
# to do so <wink>.
#
# Happy twisting!
#
__revision__ = "$Id: twisted_distutils.py,v 1.4 2005-02-16 16:45:43 fabioz Exp $"

from distutils.core import Distribution, Command
from distutils.command.install import install
from distutils.command.build import build
from distutils.command.sdist import sdist
from distutils.dep_util import newer
from distutils.util import convert_path
import os

class twisted_sdist(sdist):
    def add_defaults(self):
        sdist.add_defaults(self)
        if self.distribution.has_twisted_plugins():
            plugins = self.get_finalized_command('build_twisted_plugins')
            self.filelist.extend(plugins.get_source_files())

class twisted_install(install):
    def initialize_options (self):
        install.initialize_options(self)
        self.twisted_plugins = None
        
    def has_twisted_plugins(self):
        return self.distribution.has_twisted_plugins()
    
    sub_commands = []
    sub_commands.extend(install.sub_commands)
    sub_commands.append(('install_twisted_plugins', has_twisted_plugins))
                   

class twisted_build(build):
    def initialize_options (self):
        build.initialize_options(self)
        self.twisted_plugins = None
        
    def has_twisted_plugins(self):
        return self.distribution.has_twisted_plugins()
    
    sub_commands = []
    sub_commands.extend(build.sub_commands)
    sub_commands.append(('build_twisted_plugins', has_twisted_plugins))

class build_twisted_plugins (Command):

    description = "\"build\" twisted plugins (copy)"

    user_options = [
        ('build-dir=', 'd', "directory to \"build\" (copy) to"),
        ('force', 'f', "forcibly build everything (ignore file timestamps"),
        ]

    boolean_options = ['force']


    def initialize_options (self):
        self.build_dir = None
        self.twisted_plugins = None
        self.force = None
        self.outfiles = None

    def get_source_files(self):
        return self.twisted_plugins

    def finalize_options (self):
        self.set_undefined_options('build',
                                   ('build_lib', 'build_dir'),
                                   ('force', 'force'))
        self.twisted_plugins = self.distribution.twisted_plugins


    def run (self):
        if not self.twisted_plugins:
            return
        self.copy_twisted_plugins()


    def copy_twisted_plugins (self):
        """Copy each plugin listed in 'self.twisted_plugins'.
        """
        self.mkpath(self.build_dir)
        for plugin in self.twisted_plugins:
            adjust = 0
            plugin = convert_path(plugin)
            outfile = os.path.join(self.build_dir, plugin)
            if not self.force and not newer(plugin, outfile):
                self.announce("not copying %s (up-to-date)" % plugin)
                continue

            # Always open the file, but ignore failures in dry-run mode --
            # that way, we'll get accurate feedback if we can read the
            # plugin.
            try:
                f = open(plugin, "r")
            except IOError:
                if not self.dry_run:
                    raise
                f = None
            else:
                f.close()
                self.copy_file(plugin, outfile)


class install_twisted_plugins(Command):

    description = "install twisted plugins"

    user_options = [
        ('install-dir=', 'd', "directory to install scripts to"),
        ('build-dir=','b', "build directory (where to install from)"),
        ('force', 'f', "force installation (overwrite existing files)"),
        ('skip-build', None, "skip the build steps"),
    ]

    boolean_options = ['force', 'skip-build']


    def initialize_options (self):
        self.install_dir = None
        self.force = 0
        self.build_dir = None
        self.skip_build = None

    def finalize_options (self):
        self.set_undefined_options('build', ('build_lib', 'build_dir'))
        self.set_undefined_options('install',
                                   ('install_lib', 'install_dir'),
                                   ('force', 'force'),
                                   ('skip_build', 'skip_build'),
                                  )

    def run (self):
        if not self.skip_build:
            self.run_command('build_twisted_plugins')
        self.outfiles = self.copy_tree(self.build_dir, self.install_dir)

    def get_inputs (self):
        return self.distribution.twisted_plugins or []

    def get_outputs(self):
        return self.outfiles or []
        
    

class TwistedDistribution(Distribution):
    def __init__(self,attrs=None):
        self.twisted_plugins = None
        Distribution.__init__(self, attrs)
        self.cmdclass = {'install':twisted_install,
                         'install_twisted_plugins':install_twisted_plugins,
                         'build':twisted_build,
                         'build_twisted_plugins':build_twisted_plugins,
                         'sdist':twisted_sdist,
                         }

    def has_twisted_plugins(self):
        return self.twisted_plugins and len(self.twisted_plugins) > 0


def setup(**attrs):
    from distutils import core
    attrs['distclass'] = TwistedDistribution
    core.setup(**attrs)
