rem = """-*-Python-*- script
@echo off
rem -------------------- DOS section --------------------
rem You could set PYTHONPATH or TK environment variables here
python %0 %1 %2 %3 %4 %5 %6 %7 %8 %9
goto exit
 
"""
# -------------------- Python section --------------------
import sys
from logilab.pylint.checkers import similar
similar.run()
 

DosExitLabel = """
:exit
rem """


