import unittest
from logilab.common import testlib

import tempfile
import os
from cStringIO import StringIO

from logilab.common.configuration import Configuration, OptionValueError

options = [('dothis', {'type':'yn', 'default': True, 'metavar': '<y or n>'}),
           ('value', {'type': 'string', 'metavar': '<string>', 'short': 'v'}),
           ('multiple', {'type': 'csv', 'default': ('yop',),
                         'metavar': '<comma separated values>',
                         'help': 'you can also document the option'}),
           ('number', {'type': 'int', 'default':2, 'metavar':'<int>'}),
           ('choice', {'type': 'choice', 'default':'yo', 'choices': ('yo', 'ye'),
                       'metavar':'<yo|ye>'}),
           ('multiple-choice', {'type': 'multiple_choice', 'default':('yo', 'ye'),
                                'choices': ('yo', 'ye', 'yu', 'yi', 'ya'),
                                'metavar':'<yo|ye>'}),
           ]

class ConfigurationTC(testlib.TestCase):
    
    def setUp(self):
        self.cfg = Configuration(name='test', options=options, usage='Just do it ! (tm)')

    def test_default(self):
        cfg = self.cfg
        self.assertEquals(cfg['dothis'], True)
        self.assertEquals(cfg['value'], None)
        self.assertEquals(cfg['multiple'], ('yop',))
        self.assertEquals(cfg['number'], 2)
        self.assertEquals(cfg['choice'], 'yo')
        self.assertEquals(cfg['multiple-choice'], ('yo', 'ye'))

    def test_base(self):
        cfg = self.cfg
        cfg.set_option('number', '0')
        self.assertEquals(cfg['number'], 0)
        self.assertRaises(OptionValueError, cfg.set_option, 'number', 'youpi')
        self.assertRaises(OptionValueError, cfg.set_option, 'choice', 'youpi')
        self.assertRaises(OptionValueError, cfg.set_option, 'multiple-choice', ('yo', 'y', 'ya'))
        cfg.set_option('multiple-choice', 'yo, ya')
        self.assertEquals(cfg['multiple-choice'], ['yo', 'ya'])
        self.assertEquals(cfg.get('multiple-choice'), ['yo', 'ya'])
        self.assertEquals(cfg.get('whatever'), None)

    def test_load_command_line_configuration(self):
        cfg = self.cfg
        args = cfg.load_command_line_configuration(['--choice', 'ye', '--number', '4',
                                                    '--multiple=1,2,3', '--dothis=n',
                                                    'other', 'arguments'])
        self.assertEquals(args, ['other', 'arguments'])
        self.assertEquals(cfg['dothis'], False)
        self.assertEquals(cfg['multiple'], ['1', '2', '3'])
        self.assertEquals(cfg['number'], 4)
        self.assertEquals(cfg['choice'], 'ye')
        self.assertEquals(cfg['value'], None)
        args = cfg.load_command_line_configuration(['-v', 'duh'])
        self.assertEquals(args, [])
        self.assertEquals(cfg['value'], 'duh')
        self.assertEquals(cfg['dothis'], False)
        self.assertEquals(cfg['multiple'], ['1', '2', '3'])
        self.assertEquals(cfg['number'], 4)
        self.assertEquals(cfg['choice'], 'ye')
        
    def test_load_configuration(self):
        cfg = self.cfg
        args = cfg.load_configuration(choice='ye', number='4',
                                      multiple='1,2,3', dothis='n',
                                      multiple_choice=('yo', 'ya'))
        self.assertEquals(cfg['dothis'], False)
        self.assertEquals(cfg['multiple'], ['1', '2', '3'])
        self.assertEquals(cfg['number'], 4)
        self.assertEquals(cfg['choice'], 'ye')
        self.assertEquals(cfg['value'], None)
        self.assertEquals(cfg['multiple-choice'], ('yo', 'ya'))
        
    def test_generate_config(self):
        stream = StringIO()
        self.cfg.generate_config(stream)
        self.assertLinesEquals(stream.getvalue().strip(), """# class for simple configurations which don't need the
#     manager / providers model and prefer delegation to inheritance
# 
#     configuration values are accessible through a dict like interface
#     
[TEST]
dothis=yes

# you can also document the option
multiple=yop

number=2

choice=yo

multiple-choice=yo,ye
""")
        
    def test_generate_config_with_space_string(self):
        self.cfg['value'] = '    '
        stream = StringIO()
        self.cfg.generate_config(stream)
        self.assertLinesEquals(stream.getvalue().strip(), """# class for simple configurations which don't need the
#     manager / providers model and prefer delegation to inheritance
# 
#     configuration values are accessible through a dict like interface
#     
[TEST]
dothis=yes

value='    '

# you can also document the option
multiple=yop

number=2

choice=yo

multiple-choice=yo,ye
""")
        

    def test_loopback(self):
        cfg = self.cfg
        f = tempfile.mktemp()
        stream = open(f, 'w')
        try:
            cfg.generate_config(stream)
            stream.close()
            new_cfg = Configuration(name='testloop', options=options)
            new_cfg.load_file_configuration(f)
            self.assertEquals(cfg['dothis'], new_cfg['dothis'])
            self.assertEquals(cfg['multiple'], new_cfg['multiple'])
            self.assertEquals(cfg['number'], new_cfg['number'])
            self.assertEquals(cfg['choice'], new_cfg['choice'])
            self.assertEquals(cfg['value'], new_cfg['value'])
            self.assertEquals(cfg['multiple-choice'], new_cfg['multiple-choice'])
        finally:
            os.remove(f)
        
    def test_help(self):
        self.cfg.add_help_section('bonus', 'a nice additional help')
        self.assertLinesEquals(self.cfg.help().strip(), """usage: Just do it ! (tm)

options:
  -h, --help            show this help message and exit
  --dothis=<y or n>     
  -v<string>, --value=<string>
  --multiple=<comma separated values>
                        you can also document the option
  --number=<int>        
  --choice=<yo|ye>      
  --multiple-choice=<yo|ye>

  Bonus:
    a nice additional help
""".strip())
        
if __name__ == '__main__':
    unittest.main()
