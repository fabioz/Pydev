#
# -*- coding: UTF-8 -*-

'''Python 3 PEP3131 Test

This code is given to check PyDev (or other
checkers) working correctly.
The names of class, function, and keyword 
are written in Japanese.
Certainly, this goes fine with Python 3.'''

class 日本語の名前が付いたクラス:
    def 英語でこんにちはと言う(self, 相手="PyDev users"):
        "Say Hello to given target(s)."
        print ("Hello, {0}.".format(相手))

def 日本語の名前が付いた関数(出力内容):
    "Simply print argument."
    print (出力内容)

if __name__ == "__main__":
    実体 = 日本語の名前が付いたクラス()
    実体.英語でこんにちはと言う("Everyone")
    日本語の名前が付いた関数("The function named with Japanese called.")

    # Result: Following 2 lines are printed on console.
    # Hello, Everyone.
    # The function named with Japanese called.
