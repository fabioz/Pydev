# This program is free software; you can redistribute it and/or modify it under
# the terms of the GNU General Public License as published by the Free Software
# Foundation; either version 2 of the License, or (at your option) any later
# version.
#
# This program is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details
#
# You should have received a copy of the GNU General Public License along with
# this program; if not, write to the Free Software Foundation, Inc.,
# 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
""" Copyright (c) 2002-2003 LOGILAB S.A. (Paris, FRANCE).
 http://www.logilab.fr/ -- mailto:contact@logilab.fr
"""

__revision__ = "$Id: configuration.py,v 1.1 2004-10-26 12:52:29 fabioz Exp $"

import sys
import re
from os import linesep
from os.path import exists
from copy import copy
from ConfigParser import ConfigParser, NoOptionError, NoSectionError

from logilab.common.optik_ext import OptionParser, OptionGroup, Values, \
     OptionValueError, check_yn, check_csv
from logilab.common.textutils import normalize_text, unquote

class UnsupportedAction(Exception):
    """raised by set_option when it doesn't know what to do for an action"""
    
def choice_validator(opt_dict, name, value):
    """validate and return a converted value for option of type 'choice'
    """
    if not value in opt_dict['choices']:
        msg = "option %s: invalid value: %r, should be in %s"
        raise OptionValueError(msg % (name, value, opt_dict['choices']))
    return value

def csv_validator(opt_dict, name, value):
    """validate and return a converted value for option of type 'csv'
    """
    return check_csv(None, name, value)

def yn_validator(opt_dict, name, value):
    """validate and return a converted value for option of type 'yn'
    """
    return check_yn(None, name, value)
   
VALIDATORS = {'string' : unquote,
              'int' : int,
              'float': float,
              'regexp': re.compile,
              'csv': csv_validator,
              'yn': yn_validator,
              'choice': choice_validator
              }

def convert(value, opt_dict, name = ''):
    """return a validated value for an option according to its type
    
    optional argument name is only used for error message formatting
    """
    _type = opt_dict['type']
    if not VALIDATORS.has_key(_type):
        raise Exception('Unsupported type "%s"' % _type)
    try:
        return VALIDATORS[_type](opt_dict, name, value)
    except TypeError:
        try:
            return VALIDATORS[_type](value)
        except:
            raise OptionValueError('%s value (%s) should be of type %s' %
                                   (name, value, _type))

def comment(string, marker='# '):
    """return string as a comment"""
    return marker + string.replace(linesep, linesep + marker)
                
def format_section(stream, section, options, doc=None):
    """format an options section using the INI format"""
    if doc:
        print >> stream, comment(doc)
    print >> stream, '[%s]' % section.upper()
    section = {}
    for opt_name, value, opt_dict in options:
        if type(value) in (type(()), type([])):
            value = ','.join(value)
        elif hasattr(value, 'match'):
            # compiled regexp
            value = value.pattern
        elif opt_dict.get('type') == 'yn':
            value = value and 'yes' or 'no'
        else:
            value = repr(value)
        help_msg = opt_dict.get('help')
        if help_msg:
            print >> stream, normalize_text(help_msg, indent='# ')
        print >> stream, '%s=%s\n' % (opt_name, value)
    print >> stream, '\n'

class OptionsManagerMixIn:
    """MixIn to handle a configuration from both a configuration file and
    command line options
    """
    
    def __init__(self, usage, config_file=None, version=None, quiet=0):
        self._config_file = config_file
        # configuration file parser
        self._config_parser = ConfigParser()
        # command line parser
        self._optik_parser = OptionParser(usage=usage, version=version)
        # list of registered options providers
        self.options_providers = []
        # dictionary assocating option name to checker
        self._all_options = {}
        self._nocallback_options = {}
        # verbosity
        self.quiet = quiet
        
    def register_options_provider(self, provider, own_group=1):
        """register an options provider """
        assert provider.priority <= 0, "provider's priority can't be >= 0"
        for i in range(len(self.options_providers)):
            if provider.priority > self.options_providers[i].priority:
                self.options_providers.insert(i, provider)
                break
        else:
            self.options_providers.append(provider)
        non_group_spec_options = [option for option in provider.options
                                  if not option[1].has_key('group')]
        groups = getattr(provider, 'option_groups', None)
        if own_group:
            self.add_option_group(provider.name.upper(), provider.__doc__,
                                  non_group_spec_options, provider)
        else:
            for opt_name, opt_dict in non_group_spec_options:
                args, opt_dict = self.optik_option(provider, opt_name, opt_dict)
                self._optik_parser.add_option(*args, **opt_dict)
                self._all_options[opt_name] = provider                
        if groups:
            for group_name, doc in groups:
                self.add_option_group(
                    group_name, doc,
                    [option for option in provider.options
                     if option[1].get('group') == group_name],
                    provider)
                    
    def add_option_group(self, group_name, doc, options, provider):
        """add an option group including the listed options
        """
        # add section to the config file
        self._config_parser.add_section(group_name)
        # add option group to the command line parser
        group = OptionGroup(self._optik_parser,
                            title=group_name.capitalize(),
                            description=doc)
        self._optik_parser.add_option_group(group)
        # add provider's specific options
        for opt_name, opt_dict in options:
            args, opt_dict = self.optik_option(provider, opt_name, opt_dict)
            group.add_option(*args, **opt_dict)
            self._all_options[opt_name] = provider
            
    def optik_option(self, provider, opt_name, opt_dict):
        """get our personal option definition and return a suitable form for
        use with optik/optparse
        """
        opt_dict = copy(opt_dict)
        if opt_dict.has_key('action'):
            self._nocallback_options[provider] = opt_name
        else:
            opt_dict['action'] = 'callback'
            opt_dict['callback'] = self.cb_set_provider_option
        for specific in ('default', 'group'):
            if opt_dict.has_key(specific):
                del opt_dict[specific]
        args = ['--' + opt_name]
        if opt_dict.has_key('short'):
            args.append('-' + opt_dict['short'])
            del opt_dict['short']
        return args, opt_dict
            
    def cb_set_provider_option(self, option, opt_name, value, parser):
        """optik callback for option setting"""
        # remove --
        opt_name = opt_name[2:]
        # trick since we can't set action='store_true' on options
        if value is None:
            value = 1
        self.global_set_option(opt_name, value)
        
    def global_set_option(self, opt_name, value):
        """set option on the correct option provider"""
        self._all_options[opt_name].set_option(opt_name, value)

    def generate_config(self, stream=None):
        """write a configuration file according to the current configuration
        into the given stream or stdout
        """
        stream = stream or sys.stdout
        for provider in self.options_providers:
            default_options = []
            sections = {}
            for opt_name, opt_dict in provider.options:
                if opt_dict.get('type') is None:
                    continue                
                attr = provider.option_name(opt_name)
                try:
                    value = getattr(provider.config, attr)
                except AttributeError:
                    continue
                if value is None:
                    continue
                if opt_dict.get('group'):
                    sections.setdefault(opt_dict['group'], []).append(
                        (opt_name, value, opt_dict))
                else:
                    default_options.append((opt_name, value, opt_dict))
            if default_options:
                format_section(stream, provider.name, default_options,
                               provider.__doc__)
            for section, options in sections.items():
                format_section(stream, section, options)

    # initialization methods ##################################################

    def load_file_configuration(self, config_file=None):
        """load the configuration from file
        """
        if config_file is None:
            config_file = self._config_file
        parser = self._config_parser
        if config_file and exists(config_file):
            parser.read([config_file])
        elif not self.quiet:
            msg = 'No config file found, using default configuration'
            print >> sys.stderr, msg
            return
        for provider in self.options_providers:
            default_section = provider.name.upper()
            for opt_name, opt_dict in provider.options:
                section = opt_dict.get('group', default_section)
                try:
                    value = parser.get(section, opt_name)
                    # type casting
                    value = convert(value, opt_dict, opt_name)
                    provider.set_option(opt_name, value)
                except (NoSectionError, NoOptionError):
                    continue

    def load_configuration(self, **kwargs):
        """override configuration according to given parameters
        """
        for opt_name, opt_value in kwargs.items():
            opt_name = opt_name.replace('_', '-')
            provider = self._all_options[opt_name]
            provider.set_option(opt_name, opt_value)
            
            
    def load_command_line_configuration(self, args=None):
        """override configuration according to command line parameters

        return additional arguments
        """
        if args is None:
            args = sys.argv[1:]
        else:
            args = list(args)
        (options, args) = self._optik_parser.parse_args(args=args)
        for provider in self._nocallback_options.keys():
            config = provider.config
            for attr in config.__dict__.keys():
                value = getattr(options, attr, None)
                if value is None:
                    continue
                setattr(config, attr, value)
        return args


    # help methods ############################################################

    def add_help_section(self, title, description):
        """add a dummy option section for help purpose """
        group = OptionGroup(self._optik_parser,
                            title=title.capitalize(),
                            description=description)
        self._optik_parser.add_option_group(group)

        
    def help(self):
        """return the usage string for available options """
        return self._optik_parser.print_help()
    

        
class OptionsProviderMixIn:
    """Mixin to provide options to an OptionsManager
    """
    
    # those attributes should be overriden
    priority = -1
    name = 'default'
    options = ()

    def __init__(self):
        self.config = Values()
        for option in self.options:
            try:
                opt_name, opt_dict = option
            except ValueError:
                raise Exception('Bad option: %r' % option)
            action = opt_dict.get('action')
                
            if action != 'callback':
                # callback action have no default
                self.set_option(opt_name, opt_dict.get('default'),
                                action, opt_dict)

    def option_name(self, opt_name, opt_dict=None):
        """get the config attribute corresponding to opt_name
        """
        if opt_dict is None:
            opt_dict = self.get_option_def(opt_name)
        return opt_dict.get('dest', opt_name.replace('-', '_'))
        
    def set_option(self, opt_name, value, action=None, opt_dict=None):
        """method called to set an option (registered in the options list)
        """
        if action is None:
            if opt_dict is None:
                opt_dict = self.get_option_def(opt_name)
            action = opt_dict.get('action', 'store')
        if action == 'store':
            setattr(self.config, self.option_name(opt_name), value)
        elif action in ('store_true', 'count'):
            setattr(self.config, self.option_name(opt_name), 0)
        elif action == 'store_false':
            setattr(self.config, self.option_name(opt_name), 1)
        elif action == 'append':
            opt_name = self.option_name(opt_name)
            _list = getattr(self.config, opt_name, None)
            if _list is None:
                if type(value) in (type(()), type([])):
                    _list = value
                elif value is not None:
                    _list = []
                    _list.append(value)
                setattr(self.config, opt_name, _list)
            elif type(_list) is type(()):
                setattr(self.config, opt_name, _list + (value,))
            else:
                _list.append(value)
        else:
            raise UnsupportedAction(action)
            
    def get_option_def(self, opt_name):
        """return the dictionary defining an option given it's name"""
        for opt in self.options:
            if opt[0] == opt_name:
                return opt[1]
        else:
            raise OptionValueError('No such option %s in section %s' % (
                opt_name, self.name))
        
class ConfigurationMixIn(OptionsManagerMixIn, OptionsProviderMixIn):
    
    def __init__(self, *args, **kwargs):
        not args and kwargs.setdefault('usage', '')
        kwargs.setdefault('quiet', 1)
        OptionsManagerMixIn.__init__(self, *args, **kwargs)
        OptionsProviderMixIn.__init__(self)
        self.register_options_provider(self)
