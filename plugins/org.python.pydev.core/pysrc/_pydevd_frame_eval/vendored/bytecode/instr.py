import dis
import enum
import opcode as _opcode
import sys
from abc import abstractmethod
from dataclasses import dataclass
from marshal import dumps as _dumps
from typing import Any, Callable, Dict, Generic, Optional, Tuple, TypeVar, Union

try:
    from typing import TypeGuard
except ImportError:
    from typing_extensions import TypeGuard  # type: ignore

from _pydevd_frame_eval.vendored import bytecode as _bytecode

# --- Instruction argument tools and

MIN_INSTRUMENTED_OPCODE = getattr(_opcode, "MIN_INSTRUMENTED_OPCODE", 256)

# Instructions relying on a bit to modify its behavior.
# The lowest bit is used to encode custom behavior.
BITFLAG_INSTRUCTIONS = (
    ("LOAD_GLOBAL", "LOAD_ATTR")
    if sys.version_info >= (3, 12)
    else ("LOAD_GLOBAL",)
    if sys.version_info >= (3, 11)
    else ()
)

BITFLAG2_INSTRUCTIONS = ("LOAD_SUPER_ATTR",) if sys.version_info >= (3, 12) else ()

# Intrinsic related opcodes
INTRINSIC_1OP = (
    (_opcode.opmap["CALL_INTRINSIC_1"],) if sys.version_info >= (3, 12) else ()
)
INTRINSIC_2OP = (
    (_opcode.opmap["CALL_INTRINSIC_2"],) if sys.version_info >= (3, 12) else ()
)
INTRINSIC = INTRINSIC_1OP + INTRINSIC_2OP


# Used for COMPARE_OP opcode argument
@enum.unique
class Compare(enum.IntEnum):
    LT = 0
    LE = 1
    EQ = 2
    NE = 3
    GT = 4
    GE = 5
    if sys.version_info < (3, 9):
        IN = 6
        NOT_IN = 7
        IS = 8
        IS_NOT = 9
        EXC_MATCH = 10

    if sys.version_info >= (3, 12):

        def _get_mask(self):
            if self == Compare.EQ:
                return 8
            elif self == Compare.NE:
                return 1 + 2 + 4
            elif self == Compare.LT:
                return 2
            elif self == Compare.LE:
                return 2 + 8
            elif self == Compare.GT:
                return 4
            elif self == Compare.GE:
                return 4 + 8


# Used for BINARY_OP under Python 3.11+
@enum.unique
class BinaryOp(enum.IntEnum):
    ADD = 0
    AND = 1
    FLOOR_DIVIDE = 2
    LSHIFT = 3
    MATRIX_MULTIPLY = 4
    MULTIPLY = 5
    REMAINDER = 6
    OR = 7
    POWER = 8
    RSHIFT = 9
    SUBTRACT = 10
    TRUE_DIVIDE = 11
    XOR = 12
    INPLACE_ADD = 13
    INPLACE_AND = 14
    INPLACE_FLOOR_DIVIDE = 15
    INPLACE_LSHIFT = 16
    INPLACE_MATRIX_MULTIPLY = 17
    INPLACE_MULTIPLY = 18
    INPLACE_REMAINDER = 19
    INPLACE_OR = 20
    INPLACE_POWER = 21
    INPLACE_RSHIFT = 22
    INPLACE_SUBTRACT = 23
    INPLACE_TRUE_DIVIDE = 24
    INPLACE_XOR = 25


@enum.unique
class Intrinsic1Op(enum.IntEnum):
    INTRINSIC_1_INVALID = 0
    INTRINSIC_PRINT = 1
    INTRINSIC_IMPORT_STAR = 2
    INTRINSIC_STOPITERATION_ERROR = 3
    INTRINSIC_ASYNC_GEN_WRAP = 4
    INTRINSIC_UNARY_POSITIVE = 5
    INTRINSIC_LIST_TO_TUPLE = 6
    INTRINSIC_TYPEVAR = 7
    INTRINSIC_PARAMSPEC = 8
    INTRINSIC_TYPEVARTUPLE = 9
    INTRINSIC_SUBSCRIPT_GENERIC = 10
    INTRINSIC_TYPEALIAS = 11


@enum.unique
class Intrinsic2Op(enum.IntEnum):
    INTRINSIC_2_INVALID = 0
    INTRINSIC_PREP_RERAISE_STAR = 1
    INTRINSIC_TYPEVAR_WITH_BOUND = 2
    INTRINSIC_TYPEVAR_WITH_CONSTRAINTS = 3
    INTRINSIC_SET_FUNCTION_TYPE_PARAMS = 4


# This make type checking happy but means it won't catch attempt to manipulate an unset
# statically. We would need guard on object attribute narrowed down through methods
class _UNSET(int):
    instance = None

    def __new__(cls):
        if cls.instance is None:
            cls.instance = super().__new__(cls)
        return cls.instance

    def __eq__(self, other) -> bool:
        return self is other


for op in [
    "__abs__",
    "__add__",
    "__and__",
    "__bool__",
    "__ceil__",
    "__divmod__",
    "__float__",
    "__floor__",
    "__floordiv__",
    "__ge__",
    "__gt__",
    "__hash__",
    "__index__",
    "__int__",
    "__invert__",
    "__le__",
    "__lshift__",
    "__lt__",
    "__mod__",
    "__mul__",
    "__ne__",
    "__neg__",
    "__or__",
    "__pos__",
    "__pow__",
    "__radd__",
    "__rand__",
    "__rdivmod__",
    "__rfloordiv__",
    "__rlshift__",
    "__rmod__",
    "__rmul__",
    "__ror__",
    "__round__",
    "__rpow__",
    "__rrshift__",
    "__rshift__",
    "__rsub__",
    "__rtruediv__",
    "__rxor__",
    "__sub__",
    "__truediv__",
    "__trunc__",
    "__xor__",
]:
    setattr(_UNSET, op, lambda *args: NotImplemented)

UNSET = _UNSET()


def const_key(obj: Any) -> Union[bytes, Tuple[type, int]]:
    try:
        return _dumps(obj)
    except ValueError:
        # For other types, we use the object identifier as an unique identifier
        # to ensure that they are seen as unequal.
        return (type(obj), id(obj))


class Label:
    __slots__ = ()


# : Placeholder label temporarily used when performing some conversions
# : concrete -> bytecode
PLACEHOLDER_LABEL = Label()


class _Variable:
    __slots__ = ("name",)

    def __init__(self, name: str) -> None:
        self.name: str = name

    def __eq__(self, other: Any) -> bool:
        if type(self) is not type(other):
            return False
        return self.name == other.name

    def __str__(self) -> str:
        return self.name

    def __repr__(self) -> str:
        return "<%s %r>" % (self.__class__.__name__, self.name)


class CellVar(_Variable):
    __slots__ = ()


class FreeVar(_Variable):
    __slots__ = ()


def _check_arg_int(arg: Any, name: str) -> TypeGuard[int]:
    if not isinstance(arg, int):
        raise TypeError(
            "operation %s argument must be an int, "
            "got %s" % (name, type(arg).__name__)
        )

    if not (0 <= arg <= 2147483647):
        raise ValueError(
            "operation %s argument must be in " "the range 0..2,147,483,647" % name
        )

    return True


if sys.version_info >= (3, 12):

    def opcode_has_argument(opcode: int) -> bool:
        return opcode in dis.hasarg

else:

    def opcode_has_argument(opcode: int) -> bool:
        return opcode >= dis.HAVE_ARGUMENT

# --- Instruction stack effect impact

# We split the stack effect between the manipulations done on the stack before
# executing the instruction (fetching the elements that are going to be used)
# and what is pushed back on the stack after the execution is complete.

# Stack effects that do not depend on the argument of the instruction
STATIC_STACK_EFFECTS: Dict[str, Tuple[int, int]] = {
    "ROT_TWO": (-2, 2),
    "ROT_THREE": (-3, 3),
    "ROT_FOUR": (-4, 4),
    "DUP_TOP": (-1, 2),
    "DUP_TOP_TWO": (-2, 4),
    "GET_LEN": (-1, 2),
    "GET_ITER": (-1, 1),
    "GET_YIELD_FROM_ITER": (-1, 1),
    "GET_AWAITABLE": (-1, 1),
    "GET_AITER": (-1, 1),
    "GET_ANEXT": (-1, 2),
    "LIST_TO_TUPLE": (-1, 1),
    "LIST_EXTEND": (-2, 1),
    "SET_UPDATE": (-2, 1),
    "DICT_UPDATE": (-2, 1),
    "DICT_MERGE": (-2, 1),
    "COMPARE_OP": (-2, 1),
    "IS_OP": (-2, 1),
    "CONTAINS_OP": (-2, 1),
    "IMPORT_NAME": (-2, 1),
    "ASYNC_GEN_WRAP": (-1, 1),
    "PUSH_EXC_INFO": (-1, 2),
    # Pop TOS and push TOS.__aexit__ and result of TOS.__aenter__()
    "BEFORE_ASYNC_WITH": (-1, 2),
    # Replace TOS based on TOS and TOS1
    "IMPORT_FROM": (-1, 2),
    "COPY_DICT_WITHOUT_KEYS": (-2, 2),
    # Call a function at position 7 (4 3.11+) on the stack and push the return value
    "WITH_EXCEPT_START": (-4, 5) if sys.version_info >= (3, 11) else (-7, 8),
    # Starting with Python 3.11 MATCH_CLASS does not push a boolean anymore
    "MATCH_CLASS": (-3, 1 if sys.version_info >= (3, 11) else 2),
    "MATCH_MAPPING": (-1, 2),
    "MATCH_SEQUENCE": (-1, 2),
    "MATCH_KEYS": (-2, 3 if sys.version_info >= (3, 11) else 4),
    "CHECK_EXC_MATCH": (-2, 2),  # (TOS1, TOS) -> (TOS1, bool)
    "CHECK_EG_MATCH": (-2, 2),  # (TOS, TOS1) -> non-matched, matched or TOS1, None)
    "PREP_RERAISE_STAR": (-2, 1),  # (TOS1, TOS) -> new exception group)
    ** {k: (-1, 1) for k in (o for o in _opcode.opmap if (o.startswith("UNARY_")))},
    **{
        k: (-2, 1)
        for k in (
            o
            for o in _opcode.opmap
            if (o.startswith("BINARY_") or o.startswith("INPLACE_"))
        )
    },
    # Python 3.12 changes not covered by dis.stack_effect
    "BINARY_SLICE": (-3, 1),
    # "STORE_SLICE" handled by dis.stack_effect
    "LOAD_FROM_DICT_OR_GLOBALS": (-1, 1),
    "LOAD_FROM_DICT_OR_DEREF": (-1, 1),
    "LOAD_INTRISIC_1": (-1, 1),
    "LOAD_INTRISIC_2": (-2, 1),
}

DYNAMIC_STACK_EFFECTS: Dict[
    str, Callable[[int, Any, Optional[bool]], Tuple[int, int]]
] = {
    # PRECALL pops all arguments (as per its stack effect) and leaves
    # the callable and either self or NULL
    # CALL pops the 2 above items and push the return
    # (when PRECALL does not exist it pops more as encoded by the effect)
    "CALL": lambda effect, arg, jump: (
        -2 - arg if sys.version_info >= (3, 12) else -2,
        1,
    ),
    # 3.12 changed the behavior of LOAD_ATTR
    "LOAD_ATTR": lambda effect, arg, jump: (-1, 1 + effect),
    "LOAD_SUPER_ATTR": lambda effect, arg, jump: (-3, 3 + effect),
    "SWAP": lambda effect, arg, jump: (-arg, arg),
    "COPY": lambda effect, arg, jump: (-arg, arg + effect),
    "ROT_N": lambda effect, arg, jump: (-arg, arg),
    "SET_ADD": lambda effect, arg, jump: (-arg, arg - 1),
    "LIST_APPEND": lambda effect, arg, jump: (-arg, arg - 1),
    "MAP_ADD": lambda effect, arg, jump: (-arg, arg - 2),
    "FORMAT_VALUE": lambda effect, arg, jump: (effect - 1, 1),
    # FOR_ITER needs TOS to be an iterator, hence a prerequisite of 1 on the stack
    "FOR_ITER": lambda effect, arg, jump: (effect, 0) if jump else (-1, 2),
    **{
        # Instr(UNPACK_* , n) pops 1 and pushes n
        k: lambda effect, arg, jump: (-1, effect + 1)
        for k in (
            "UNPACK_SEQUENCE",
            "UNPACK_EX",
        )
    },
    **{
        k: lambda effect, arg, jump: (effect - 1, 1)
        for k in (
            "MAKE_FUNCTION",
            "CALL_FUNCTION",
            "CALL_FUNCTION_EX",
            "CALL_FUNCTION_KW",
            "CALL_METHOD",
            *(o for o in _opcode.opmap if o.startswith("BUILD_")),
        )
    },
}

# --- Instruction location


def _check_location(
    location: Optional[int], location_name: str, min_value: int
) -> None:
    if location is None:
        return
    if not isinstance(location, int):
        raise TypeError(f"{location_name} must be an int, got {type(location)}")
    if location < min_value:
        raise ValueError(
            f"invalid {location_name}, expected >= {min_value}, got {location}"
        )


@dataclass(frozen=True)
class InstrLocation:
    """Location information for an instruction."""

    # : Lineno at which the instruction corresponds.
    # : Optional so that a location of None in an instruction encode an unset value.
    lineno: Optional[int]

    # : End lineno at which the instruction corresponds (Python 3.11+ only)
    end_lineno: Optional[int]

    # : Column offset at which the instruction corresponds (Python 3.11+ only)
    col_offset: Optional[int]

    # : End column offset at which the instruction corresponds (Python 3.11+ only)
    end_col_offset: Optional[int]

    __slots__ = ["lineno", "end_lineno", "col_offset", "end_col_offset"]

    def __init__(
        self,
        lineno: Optional[int],
        end_lineno: Optional[int],
        col_offset: Optional[int],
        end_col_offset: Optional[int],
    ) -> None:
        # Needed because we want the class to be frozen
        object.__setattr__(self, "lineno", lineno)
        object.__setattr__(self, "end_lineno", end_lineno)
        object.__setattr__(self, "col_offset", col_offset)
        object.__setattr__(self, "end_col_offset", end_col_offset)
        # In Python 3.11 0 is a valid lineno for some instructions (RESUME for example)
        _check_location(lineno, "lineno", 0 if sys.version_info >= (3, 11) else 1)
        _check_location(end_lineno, "end_lineno", 1)
        _check_location(col_offset, "col_offset", 0)
        _check_location(end_col_offset, "end_col_offset", 0)
        if end_lineno:
            if lineno is None:
                raise ValueError("End lineno specified with no lineno.")
            elif lineno > end_lineno:
                raise ValueError(
                    f"End lineno {end_lineno} cannot be smaller than lineno {lineno}."
                )

        if col_offset is not None or end_col_offset is not None:
            if lineno is None or end_lineno is None:
                raise ValueError(
                    "Column offsets were specified but lineno information are "
                    f"incomplete. Lineno: {lineno}, end lineno: {end_lineno}."
                )
            if end_col_offset is not None:
                if col_offset is None:
                    raise ValueError(
                        "End column offset specified with no column offset."
                    )
                # Column offset must be increasing inside a signle line but
                # have no relations between different lines.
                elif lineno == end_lineno and col_offset > end_col_offset:
                    raise ValueError(
                        f"End column offset {end_col_offset} cannot be smaller than "
                        f"column offset: {col_offset}."
                    )
            else:
                raise ValueError(
                    "No end column offset was specified but a column offset was given."
                )

    @classmethod
    def from_positions(cls, position: "dis.Positions") -> "InstrLocation":  # type: ignore
        return InstrLocation(
            position.lineno,
            position.end_lineno,
            position.col_offset,
            position.end_col_offset,
        )


class SetLineno:
    __slots__ = ("_lineno",)

    def __init__(self, lineno: int) -> None:
        # In Python 3.11 0 is a valid lineno for some instructions (RESUME for example)
        _check_location(lineno, "lineno", 0 if sys.version_info >= (3, 11) else 1)
        self._lineno: int = lineno

    @property
    def lineno(self) -> int:
        return self._lineno

    def __eq__(self, other: Any) -> bool:
        if not isinstance(other, SetLineno):
            return False
        return self._lineno == other._lineno

# --- Pseudo instructions used to represent exception handling (3.11+)


class TryBegin:
    __slots__ = ("target", "push_lasti", "stack_depth")

    def __init__(
        self,
        target: Union[Label, "_bytecode.BasicBlock"],
        push_lasti: bool,
        stack_depth: Union[int, _UNSET]=UNSET,
    ) -> None:
        self.target: Union[Label, "_bytecode.BasicBlock"] = target
        self.push_lasti: bool = push_lasti
        self.stack_depth: Union[int, _UNSET] = stack_depth

    def copy(self) -> "TryBegin":
        return TryBegin(self.target, self.push_lasti, self.stack_depth)


class TryEnd:
    __slots__ = "entry"

    def __init__(self, entry: TryBegin) -> None:
        self.entry: TryBegin = entry

    def copy(self) -> "TryEnd":
        return TryEnd(self.entry)


T = TypeVar("T", bound="BaseInstr")
A = TypeVar("A", bound=object)


class BaseInstr(Generic[A]):
    """Abstract instruction."""

    __slots__ = ("_name", "_opcode", "_arg", "_location", "_offset")

    # Work around an issue with the default value of arg
    def __init__(
        self,
        name: str,
        arg: A=UNSET,  # type: ignore
        * ,
        lineno: Union[int, None, _UNSET]=UNSET,
        location: Optional[InstrLocation]=None,
        offset=None,
    ) -> None:
        self._set(name, arg)
        if location:
            self._location = location
        elif lineno is UNSET:
            self._location = None
        else:
            self._location = InstrLocation(lineno, None, None, None)
        self._offset = offset

    # Work around an issue with the default value of arg
    def set(self, name: str, arg: A=UNSET) -> None:  # type: ignore
        """Modify the instruction in-place.

        Replace name and arg attributes. Don't modify lineno.

        """
        self._set(name, arg)

    def require_arg(self) -> bool:
        """Does the instruction require an argument?"""
        return opcode_has_argument(self._opcode)

    @property
    def name(self) -> str:
        return self._name

    @property
    def offset(self) -> int:
        return self._offset

    @name.setter
    def name(self, name: str) -> None:
        self._set(name, self._arg)

    @property
    def opcode(self) -> int:
        return self._opcode

    @opcode.setter
    def opcode(self, op: int) -> None:
        if not isinstance(op, int):
            raise TypeError("operator code must be an int")
        if 0 <= op <= 255:
            name = _opcode.opname[op]
            valid = name != "<%r>" % op
        else:
            valid = False
        if not valid:
            raise ValueError("invalid operator code")

        self._set(name, self._arg)

    @property
    def arg(self) -> A:
        return self._arg

    @arg.setter
    def arg(self, arg: A):
        self._set(self._name, arg)

    @property
    def lineno(self) -> Union[int, _UNSET, None]:
        return self._location.lineno if self._location is not None else UNSET

    @lineno.setter
    def lineno(self, lineno: Union[int, _UNSET, None]) -> None:
        loc = self._location
        if loc and (
            loc.end_lineno is not None
            or loc.col_offset is not None
            or loc.end_col_offset is not None
        ):
            raise RuntimeError(
                "The lineno of an instruction with detailed location information "
                "cannot be set."
            )

        if lineno is UNSET:
            self._location = None
        else:
            self._location = InstrLocation(lineno, None, None, None)

    @property
    def location(self) -> Optional[InstrLocation]:
        return self._location

    @location.setter
    def location(self, location: Optional[InstrLocation]) -> None:
        if location and not isinstance(location, InstrLocation):
            raise TypeError(
                "The instr location must be an instance of InstrLocation or None."
            )
        self._location = location

    def stack_effect(self, jump: Optional[bool]=None) -> int:
        if not self.require_arg():
            arg = None
        # 3.11 where LOAD_GLOBAL arg encode whether or we push a null
        # 3.12 does the same for LOAD_ATTR
        elif self.name in BITFLAG_INSTRUCTIONS and isinstance(self._arg, tuple):
            assert len(self._arg) == 2
            arg = self._arg[0]
        # 3.12 does a similar trick for LOAD_SUPER_ATTR
        elif self.name in BITFLAG2_INSTRUCTIONS and isinstance(self._arg, tuple):
            assert len(self._arg) == 3
            arg = self._arg[0]
        elif not isinstance(self._arg, int) or self._opcode in _opcode.hasconst:
            # Argument is either a non-integer or an integer constant,
            # not oparg.
            arg = 0
        else:
            arg = self._arg

        return dis.stack_effect(self._opcode, arg, jump=jump)

    def pre_and_post_stack_effect(self, jump: Optional[bool]=None) -> Tuple[int, int]:
        # Allow to check that execution will not cause a stack underflow
        _effect = self.stack_effect(jump=jump)

        n = self.name
        if n in STATIC_STACK_EFFECTS:
            return STATIC_STACK_EFFECTS[n]
        elif n in DYNAMIC_STACK_EFFECTS:
            return DYNAMIC_STACK_EFFECTS[n](_effect, self.arg, jump)
        else:
            # For instruction with no special value we simply consider the effect apply
            # before execution
            return (_effect, 0)

    def copy(self: T) -> T:
        return self.__class__(self._name, self._arg, location=self._location, offset=self._offset)

    def has_jump(self) -> bool:
        return self._has_jump(self._opcode)

    def is_cond_jump(self) -> bool:
        """Is a conditional jump?"""
        # Ex: POP_JUMP_IF_TRUE, JUMP_IF_FALSE_OR_POP
        # IN 3.11+ the JUMP and the IF are no necessary adjacent in the name.
        name = self._name
        return "JUMP_" in name and "IF_" in name

    def is_uncond_jump(self) -> bool:
        """Is an unconditional jump?"""
        # JUMP_BACKWARD has been introduced in 3.11+
        # JUMP_ABSOLUTE was removed in 3.11+
        return self.name in {
            "JUMP_FORWARD",
            "JUMP_ABSOLUTE",
            "JUMP_BACKWARD",
            "JUMP_BACKWARD_NO_INTERRUPT",
        }

    def is_abs_jump(self) -> bool:
        """Is an absolute jump."""
        return self._opcode in _opcode.hasjabs

    def is_forward_rel_jump(self) -> bool:
        """Is a forward relative jump."""
        return self._opcode in _opcode.hasjrel and "BACKWARD" not in self._name

    def is_backward_rel_jump(self) -> bool:
        """Is a backward relative jump."""
        return self._opcode in _opcode.hasjrel and "BACKWARD" in self._name

    def is_final(self) -> bool:
        if self._name in {
            "RETURN_VALUE",
            "RETURN_CONST",
            "RAISE_VARARGS",
            "RERAISE",
            "BREAK_LOOP",
            "CONTINUE_LOOP",
        }:
            return True
        if self.is_uncond_jump():
            return True
        return False

    def __repr__(self) -> str:
        if self._arg is not UNSET:
            return "<%s arg=%r location=%s>" % (self._name, self._arg, self._location)
        else:
            return "<%s location=%s>" % (self._name, self._location)

    def __eq__(self, other: Any) -> bool:
        if type(self) is not type(other):
            return False
        return self._cmp_key() == other._cmp_key()

    # --- Private API

    _name: str

    _location: Optional[InstrLocation]

    _opcode: int

    _arg: A

    def _set(self, name: str, arg: A) -> None:
        if not isinstance(name, str):
            raise TypeError("operation name must be a str")
        try:
            opcode = _opcode.opmap[name]
        except KeyError:
            raise ValueError(f"invalid operation name: {name}")  # noqa

        if opcode >= MIN_INSTRUMENTED_OPCODE:
            raise ValueError(
                f"operation {name} is an instrumented or pseudo opcode. "
                "Only base opcodes are supported"
            )

        self._check_arg(name, opcode, arg)

        self._name = name
        self._opcode = opcode
        self._arg = arg

    @staticmethod
    def _has_jump(opcode) -> bool:
        return opcode in _opcode.hasjrel or opcode in _opcode.hasjabs

    @abstractmethod
    def _check_arg(self, name: str, opcode: int, arg: A) -> None:
        pass

    @abstractmethod
    def _cmp_key(self) -> Tuple[Optional[InstrLocation], str, Any]:
        pass


InstrArg = Union[
    int,
    str,
    Label,
    CellVar,
    FreeVar,
    "_bytecode.BasicBlock",
    Compare,
    Tuple[bool, str],
    Tuple[bool, bool, str],
]


class Instr(BaseInstr[InstrArg]):
    __slots__ = ()

    def _cmp_key(self) -> Tuple[Optional[InstrLocation], str, Any]:
        arg: Any = self._arg
        if self._opcode in _opcode.hasconst:
            arg = const_key(arg)
        return (self._location, self._name, arg)

    def _check_arg(self, name: str, opcode: int, arg: InstrArg) -> None:
        if name == "EXTENDED_ARG":
            raise ValueError(
                "only concrete instruction can contain EXTENDED_ARG, "
                "highlevel instruction can represent arbitrary argument without it"
            )

        if opcode_has_argument(opcode):
            if arg is UNSET:
                raise ValueError("operation %s requires an argument" % name)
        else:
            if arg is not UNSET:
                raise ValueError("operation %s has no argument" % name)

        if self._has_jump(opcode):
            if not isinstance(arg, (Label, _bytecode.BasicBlock)):
                raise TypeError(
                    "operation %s argument type must be "
                    "Label or BasicBlock, got %s" % (name, type(arg).__name__)
                )

        elif opcode in _opcode.hasfree:
            if not isinstance(arg, (CellVar, FreeVar)):
                raise TypeError(
                    "operation %s argument must be CellVar "
                    "or FreeVar, got %s" % (name, type(arg).__name__)
                )

        elif opcode in _opcode.haslocal or opcode in _opcode.hasname:
            if name in BITFLAG_INSTRUCTIONS:
                if not (
                    isinstance(arg, tuple)
                    and len(arg) == 2
                    and isinstance(arg[0], bool)
                    and isinstance(arg[1], str)
                ):
                    raise TypeError(
                        "operation %s argument must be a tuple[bool, str], "
                        "got %s (value=%s)" % (name, type(arg).__name__, str(arg))
                    )

            elif name in BITFLAG2_INSTRUCTIONS:
                if not (
                    isinstance(arg, tuple)
                    and len(arg) == 3
                    and isinstance(arg[0], bool)
                    and isinstance(arg[1], bool)
                    and isinstance(arg[2], str)
                ):
                    raise TypeError(
                        "operation %s argument must be a tuple[bool, bool, str], "
                        "got %s (value=%s)" % (name, type(arg).__name__, str(arg))
                    )

            elif not isinstance(arg, str):
                raise TypeError(
                    "operation %s argument must be a str, "
                    "got %s" % (name, type(arg).__name__)
                )

        elif opcode in _opcode.hasconst:
            if isinstance(arg, Label):
                raise ValueError(
                    "label argument cannot be used " "in %s operation" % name
                )
            if isinstance(arg, _bytecode.BasicBlock):
                raise ValueError(
                    "block argument cannot be used " "in %s operation" % name
                )

        elif opcode in _opcode.hascompare:
            if not isinstance(arg, Compare):
                raise TypeError(
                    "operation %s argument type must be "
                    "Compare, got %s" % (name, type(arg).__name__)
                )

        elif opcode in INTRINSIC_1OP:
            if not isinstance(arg, Intrinsic1Op):
                raise TypeError(
                    "operation %s argument type must be "
                    "Intrinsic1Op, got %s" % (name, type(arg).__name__)
                )

        elif opcode in INTRINSIC_2OP:
            if not isinstance(arg, Intrinsic2Op):
                raise TypeError(
                    "operation %s argument type must be "
                    "Intrinsic2Op, got %s" % (name, type(arg).__name__)
                )

        elif opcode_has_argument(opcode):
            _check_arg_int(arg, name)
