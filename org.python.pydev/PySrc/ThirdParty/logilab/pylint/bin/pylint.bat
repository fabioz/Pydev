rem = """-*-Python-*- script
@echo off
rem -------------------- DOS section --------------------
rem You could set PYTHONPATH or TK environment variables here
python %*
goto exit
 
"""
# -------------------- Python section --------------------
import sys
from logilab.pylint import lint
lint.Run(sys.argv[1:])
 

DosExitLabel = """
:exit
rem """


