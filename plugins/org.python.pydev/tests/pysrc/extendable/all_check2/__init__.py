class ThisGoes:
    pass

class ThisGoesToo:
    pass

class ThisDoesnt:
    pass

__all__ = ['ThisGoes']
__all__.append('ThisGoesToo') #When it's defined in runtime, any addition will make it accept anything (at least for now).    