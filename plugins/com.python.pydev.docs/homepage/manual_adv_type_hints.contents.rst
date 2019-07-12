Code Completion
===============

Type hinting with docstrings/comments
======================================

New on PyDev 2.8.0
---------------------

It's possible to provide hints for code-completion with docstrings by commenting types with the Sphinx/Epydoc format.

Below are some examples of how to provide type-hints.


List type with Sphinx (PyDev 4.0 onwards)
------------------------------------------

Note: It works with brackets or parenthesis:

    .. sourcecode:: python

		class MyClass:

		    def method(self, param):
		        ':type param: list(str)'
		        ':type param: list[str]'


Dict type with Sphinx (PyDev 4.0 onwards)
-----------------------------------------

Note: It works with brackets or parenthesis:

    .. sourcecode:: python

		class MyClass:

		    def method(self, param):
		        ':type param: dict(str, MyClass)'
		        ':type param: dict[str, MyClass]'


Return type with Sphinx
-------------------------

    .. sourcecode:: python

		class MyClass:

		    def method(self):
		        ':rtype unittest.TestCase'

Parameter type with Sphinx
-----------------------------

    .. sourcecode:: python

		class MyClass:

		    def method(self, a):
		        ':type a: TestCase'
		        #Note that just the class name is accepted, but in this case,
		        #it'll search for a TestCase class in the whole workspace


Parameter type with Sphinx inline
-----------------------------------

    .. sourcecode:: python

		class MyClass:

		    def method(self, a):
		        ':param TestCase a:'


Local variable with Sphinx
---------------------------

    .. sourcecode:: python

		class MyClass:

		    def method(self, lst):
		    	#Can be on the same line
		        for a in lst: #: :type a: GUITest
		            a.;



Local variable with Sphinx
---------------------------

    .. sourcecode:: python

		class MyClass:

		    def method(self, lst):
		    	#Or on the line before
		        #: :type a: GUITest
		        for a in lst:
		            a.;



Local variable with Sphinx
---------------------------

    .. sourcecode:: python

		class MyClass:

		    def method(self, lst):
		    	#If commented as a docstring must be on the
		    	#line after
		        for a in lst:
		            ': :type a: GUITest'
		            a.;




Return type with Epydoc
-------------------------

    .. sourcecode:: python

		class MyClass:

		    def method(self):
		        '@rtype unittest.TestCase'



Parameter type with Epydoc
-----------------------------

    .. sourcecode:: python

		class MyClass:

		    def method(self, a):
		        '@type a: TestCase'
		        #Note that just the class name is accepted, but in this case,
		        #it'll search for a TestCase class in the whole workspace


