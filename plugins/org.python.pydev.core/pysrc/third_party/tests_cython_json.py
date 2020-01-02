import Cython
from cython_json import source_to_dict
import pytest
import json


def test_dump_ast_error():
    as_dict = source_to_dict("x = [a  10]")
    assert as_dict['is_error']
    assert as_dict['name'] == 'CompileError'
    assert as_dict['line'] == 1
    assert as_dict['col'] == 8
    assert 'Expected' in as_dict['message_only']


def test_dump_error():
    contents = '''
from distutils import sysconfig
'''
    if isinstance(contents, bytes):
        contents = contents.decode('utf-8')
    source_to_dict(contents)

def test_global():
    contents = '''
def method():
  global b
  b = 10
'''
    if isinstance(contents, bytes):
        contents = contents.decode('utf-8')
    source_to_dict(contents)

# def test_dump_custom():
#     with open(r'X:\cython\tests\compile\buildenv.pyx', 'r') as stream:
#         contents = stream.read().decode('utf-8')
#     source_to_dict(contents)


def test_dump_ast():
    assert source_to_dict("x = [a, 10]") == {
        "__version__": Cython.__version__,
        "name": "StatList",
        "line": 1,
        "col": 0,
        "stats": [
            {
                "name": "SingleAssignment",
                "line": 1,
                "col": 4,
                "lhs": {
                    "name": "x",
                    "line": 1,
                    "col": 0
                },
                "rhs": {
                    "name": "List",
                    "line": 1,
                    "col": 4,
                    "args": [
                        {
                            "name": "a",
                            "line": 1,
                            "col": 5
                        },
                        {
                            "name": "Int",
                            "line": 1,
                            "col": 8,
                            "is_c_literal": "None",
                            "value": "10",
                            "unsigned": "",
                            "longness": "",
                            "constant_result": "10",
                            "type": "long"
                        }
                    ]
                }
            }
        ]
    }


if __name__ == '__main__':
    pytest.main()
