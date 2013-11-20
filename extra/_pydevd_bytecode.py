"""
Utilities for extracting information from Python byte code.
"""
import dis
import opcode
import sys

# Is the Python version at least 3.2?
# This approach is not verified for lower versions (although it might work).
# TODO: This should be rewritten to use the ast module instead of
# looking at bytecode for better version independence.
IS_PY_VERSION_32 = False
try:
    if sys.version_info[0] == 3 and sys.version_info[1] >= 2:
        IS_PY_VERSION_32 = True
except AttributeError:
    pass #Not all versions have sys.version_info

if IS_PY_VERSION_32:
    ALL_EXCEPTIONS = '*'
    
    OP_COMPARE = opcode.opmap['COMPARE_OP']
    OP_DUP_TOP = opcode.opmap['DUP_TOP']
    OP_LOAD_GLOBAL = opcode.opmap['LOAD_GLOBAL']
    OP_LOAD_ATTR = opcode.opmap['LOAD_ATTR']
    OP_POP_JMP_IF_FALSE = opcode.opmap['POP_JUMP_IF_FALSE']
    OP_POP_TOP = opcode.opmap['POP_TOP']
    
    OP_SETUP_FINALLY = opcode.opmap['SETUP_FINALLY']
    OP_SETUP_EXCEPT = opcode.opmap['SETUP_EXCEPT']
    OP_SETUP_WITH = opcode.opmap['SETUP_WITH']
    OP_END_FINALLY = opcode.opmap['END_FINALLY']
    
    CMP_EXCEPTION_MATCH = opcode.cmp_op.index('exception match')
    
    # Exception filters in CPython 3.2 follow this pattern:
    #  4     >>   15 DUP_TOP
    #             16 LOAD_GLOBAL              0 (RuntimeError)
    #             19 COMPARE_OP              10 (exception match)
    #             22 POP_JUMP_IF_FALSE       37
    #             25 POP_TOP
    #             26 POP_TOP
    #             27 POP_TOP
    # For exceptions defined within a module, there are one or more
    # LOAD_ATTR instructions between LOAD_GLOBAL and COMPARE_OP.
    PATTERN_EX_HANDLER = [
        (OP_DUP_TOP,                             ),
        (OP_LOAD_GLOBAL,      'global_index'     ),
        (OP_LOAD_ATTR,        'attr_indices'     ), '*',
        (OP_COMPARE,          CMP_EXCEPTION_MATCH),
        (OP_POP_JMP_IF_FALSE, 'else_target'      ),
        (OP_POP_TOP,                             ),
        (OP_POP_TOP,                             ),
        (OP_POP_TOP,                             )]
    
    BLOCK_OPS = {OP_SETUP_EXCEPT: OP_END_FINALLY,
                 OP_SETUP_FINALLY: OP_END_FINALLY,
                 OP_SETUP_WITH: OP_END_FINALLY,}
    
    QUANTIFIERS = {'*': (0, None), '+': (1, None), '?': (0, 1)}
    
    class HandlerInfo:
        """
        A Handler Info represents the exceptions that are handled by a given except block.
        """
        def __init__(self, handlers):
            """
            Construct a HandlerInfo.
            
            handlers: A set of exception names, or ALL_EXCEPTIONS.
            """
            self.exceptions = handlers
    
        def will_handle(self, exc):
            """
            Return True if this block will handle the given exception.
            
            exc: May be an exception type or instance.
            """
            if self.exceptions == ALL_EXCEPTIONS:
                return True
    
            if isinstance(exc, BaseException):
                exc_type = type(exc)
            elif issubclass(exc, BaseException):
                exc_type = exc
            else:
                raise ValueError("Unexpected exc '{}' (type: {})".format(exc, type(exc)))
    
            for sub_exc_type in exc_type.__mro__:
                if sub_exc_type.__name__ in self.exceptions:
                    return True
    
            return False
    
        def will_handle_name(self, exc_name):
            """
            Return True if this block will handle the given exception.
            
            exc_name: The type name of an exception, as a string.
            """
            assert isinstance(exc_name, str)
    
            if self.exceptions == ALL_EXCEPTIONS:
                return True
    
            return exc_name in self.exceptions
    
        def __repr__(self):
            return "HandlerInfo({})".format(self.exceptions)
    
    def get_handled_exceptions(co, line, include_finally=False, include_with=True):
        """
        Return a HandlerInfo describing the exceptions handled by
        the given code object at the given line.
    
        co: A code object.
        line: The line number.
        """
        offset = get_offset_for_line(co, line)
    
        handled = set()
    
        code = co.co_code
        for op, index, handler, end in get_exception_blocks(code):
            if offset <= index or offset >= handler:
                continue
    
            # Check for blocks that can handle exceptions 
            if op == OP_SETUP_FINALLY and include_finally:
                # Finally blocks effectively handle all exceptions
                return HandlerInfo(ALL_EXCEPTIONS)
            elif op == OP_SETUP_WITH and include_with:
                # The context manager may handle the exception.
                # It is difficult to get more detail at this point.
                return HandlerInfo(ALL_EXCEPTIONS)
            elif op == OP_SETUP_EXCEPT:
                # Except blocks contain a potentially repeated list
                # of filters followed by an optional "default" handler.
                # We need to parse the block to extract the filters.  
                filters = get_exception_filters(co, begin=handler, end=end)
                
                if filters == ALL_EXCEPTIONS:
                    return HandlerInfo(ALL_EXCEPTIONS)
                
                handled.update(filters)
    
        return HandlerInfo(handled or ())
    
    def get_offset_for_line(co, line):
        """
        Return the offset associated with the given line in a code object.
        Return -1 if there is no byte code associated with this line.
        """
        for offset, l in dis.findlinestarts(co):
            if l == line:
                return offset
            elif l > line:
                break
        return -1
    
    def get_exception_filters(co, begin, end):
        """
        Return a list of the names of exceptions that are handled by the given
        exception block.
        
        Return ALL_EXCEPTIONS instead if all exceptions are handled
        (for example, if there is an unfiltered 'except:' block.)
        
        co: A code object (e.g., func.__code__)
        begin: The first bytecode offset of the exception handler block.
        end: The end of the exception handler block.
        """
        code = co.co_code
        names = co.co_names
    
        filters = []
    
        # Starting at begin, scan for repeated exception filter patterns.
        # Each pattern branches to 'else_target' for exceptions that it does
        # not handle.  If our pattern matches we chain to this branch target.
        index = begin
        while index < end:
            # Attempt to match an exception filter at the current index.
            match = match_bytecode(code, PATTERN_EX_HANDLER, begin=index, end=end)
            if match is None:
                break
            index = match['else_target']
            
            # The exception name is a composite of an initial global symbol and
            # an optional series of attributes.
            components = [match['global_index']]
            if 'attr_indices' in match:
                components.extend(match['attr_indices'])
    
            ex_filter = ".".join([names[i] for i in components])
            filters.append(ex_filter)
    
        # Check whether there is any additional content that will run after the
        # last matching exception filter.  This can happen in several cases:
        # try:
        #     pass
        # except function_returning_exception(): # Match fails due to function call
        #     pass
        # except: # No filter, match of subsequent block fails
        #     pass
        #
        # If the final 'else_target' did not jump to the end of the exception block,
        # we have to assume that all exceptions are handled. 
        if index != end - 1:
            return ALL_EXCEPTIONS
    
        return filters
    
    def get_exception_blocks(code, begin=0, end=None):
        """
        Return a list of (op, begin, except, end) tuples for each exception handler in code.
        Raise ValueError if there are dangling exception blocks.
        
        code: An array of bytecode.
        begin: An optional offset in code to start searching
        end: An optional end to stop searching. 
        """
        block_stack = []
        blocks = []
    
        bytecode = bytecode_gen(code, begin=begin, end=end)
        for index, next_ip, op, _oparg, dest in bytecode:
            if op in BLOCK_OPS:
                block_stack.append((index, op, dest))
            elif block_stack:
                open_index, open_op, open_dest = block_stack[-1]
                if op == BLOCK_OPS[open_op]:
                    block_stack.pop()
                    blocks.append((open_op, open_index, open_dest, next_ip))
        if block_stack:
            raise ValueError("Block stack contains {} additional items".format(len(block_stack)))
        return blocks
    
    def match_bytecode(code, pattern, begin=0, end=None):
        """
        Attempt to match a pattern in the given code object.
        If the pattern matches, return a dictionary of {token: value}
        pairs for each named token in pattern.
        Return None if the pattern does not match.
    
        code: An array of bytecode.
        pattern: A list of opcode or (opcode, argument) tuples. Each
                 opcode or argument may be a string or a number.  A list
                 entry may be followed by an optional '?', '*' or '+'
                 quantifier, in which case result will contain lists of matches.
                 Entries with quantifiers are matched greedily.
        begin: An optional offset in code to start searching
        end: An optional end to stop searching. 
        """
        bytecode = bytecode_gen(code, begin=begin, end=end)
        pg = pattern_gen(pattern)
    
        current_match = 0
        min_match, max_match = 0, 0
    
        results = {}
        p_op = None
        p_oparg = None
        for _index, _next_ip, op, oparg, dest in bytecode:
            try:
                # In order to handle quantifiers, we keep track of how many times the
                # current pattern entry has matched.  If it does not match, and
                # the current match count is less than the minimum matches, we advance.
                # When the current match count exceeds the maximum we also advance.
                match = False
                while not match:
                    advance = False
                    if max_match is not None and current_match >= max_match:
                        advance = True
                    else:
                        match = pattern_entry_match(op, oparg, p_op, p_oparg)
                        if not match and current_match >= min_match:
                            advance = True
                    if not match and not advance:
                        # This pattern cannot match
                        return None
                    if advance:
                        p_op, p_op_token, p_oparg, p_oparg_token, min_match, max_match = next(pg)
                        current_match = 0
            except StopIteration:
                break
            current_match = current_match + 1
    
            # If we have a token associated with op, store it either as a single element,
            # or in a list if we have a repeating quantifier.
            if p_op_token is not None:
                if max_match is None or max_match > 1:
                    results.setdefault(p_op_token, []).append(op)
                else:
                    results[p_op_token] = op
            
            if p_op is not None and p_op != op:
                return None
            
            if oparg is None:
                if p_oparg is not None or p_oparg_token is not None:
                    raise ValueError("Opcode {} cannot have an argument".format(opcode.opname[op]))
            else:
                if p_oparg is None and p_oparg_token is None:
                    raise ValueError("Opcode {} requires an argument".format(opcode.opname[op]))
    
                # If we have a token associated with oparg, store it either as a single element,
                # or in a list if we have a repeating quantifier.
                if p_oparg_token is not None:
                    # Capture the absolute offset for relative jumps
                    value = dest or oparg
                    if max_match is None or max_match > 1:
                        results.setdefault(p_oparg_token, []).append(value)
                    else:
                        results[p_oparg_token] = value
                        
                if p_oparg is not None and p_oparg != oparg:
                    return None
    
        return results
    
    def pattern_gen(pattern):
        """
        Generate (op, op_token, oparg, oparg_token, min_match, max_match) tuples for the given pattern.
        """
        i = 0
        n = len(pattern)
        while i < n:
            entry = pattern[i]
            i = i + 1
            l = len(entry)
            if l == 1:
                op = entry[0]
                oparg = None
            elif l == 2:
                op, oparg = entry
            else:
                raise AssertionError("Entry {} does not contain 1 or 2 elements".format(entry))
    
            if i < n and pattern[i] in QUANTIFIERS:
                q = pattern[i]
                i = i + 1
                min_match, max_match = QUANTIFIERS[q]
            else:
                min_match, max_match = 1, 1
    
            op, op_token = _to_token(op)
            oparg, oparg_token = _to_token(oparg)
    
            yield (op, op_token, oparg, oparg_token, min_match, max_match)
    
    def _to_token(entry):
        """
        Return a (pattern, token) tuple for an entry.
        
        If entry is a string, the pattern is assumed to be a wildcard, and
        the entry is a symbolic token.
        
        Otherwise, entry is the symbol to match and token is None.
        """
        if isinstance(entry, str):
            return None, entry
        else:
            return entry, None
          
    def pattern_entry_match(op, oparg, p_op, p_oparg):
        """
        Return whether the given op and optional oparg match
        the given pattern and optional pattern oparg.
        """
        if p_op is not None and p_op != op:
            return False
        if p_oparg is not None and p_oparg != oparg:
            return False
        return True
        
    def bytecode_gen(code, begin=0, end=None):
        """
        Generate (index, next_ip, op, oparg, dest) pairs for all bytecode instructions in the given code object.
    
        For each result, arg will be None for no-argument ops and dest will be None
        for non-jump instructions.
    
        code: An array of bytecode.
        begin: An optional offset in code to start searching
        end: An optional end to stop searching. 
        """
        i = begin
        if end is None:
            n = len(code)
        else:
            n = end
    
        while i < n:
            index = i
            op = code[i]
            i = i + 1
            
            if op < dis.HAVE_ARGUMENT:
                yield (index, i, op, None, None)
            else:
                oparg = code[i] + code[i + 1] * 256
                i = i + 2
                
                dest = None
                if op in dis.hasjrel:
                    dest = i + oparg
                elif op in dis.hasjabs:
                    dest = oparg
                    
                yield (index, i, op, oparg, dest)


