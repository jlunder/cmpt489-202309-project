import functools, re
import sympy as sp


class Env:
    x: int | sp.Symbol
    y: int | sp.Symbol
    z: int | sp.Symbol

    def __init__(self, x: int | sp.Symbol, y: int | sp.Symbol, z: int | sp.Symbol):
        self.x = x if isinstance(x, sp.Symbol) else int(x)
        self.y = y if isinstance(y, sp.Symbol) else int(y)
        self.z = z if isinstance(z, sp.Symbol) else int(z)

    def __str__(self):
        return "x = %d, y = %d, z = %d" % (self.x, self.y, self.z)


(sym_x, sym_y, sym_z) = sp.symbols("x y z")
sym_env = Env(sym_x, sym_y, sym_z)


@functools.total_ordering
class AstNode:
    _memo_str = None
    _memo_sym = None
    _memo_canon = None
    name: str
    p: []

    def __init__(self, *p):
        self.name = self.__class__.__name__
        self.p = p

    def to_sym(self):
        if self._memo_sym == None:
            self._memo_sym = self.to_sym_op(*[p.to_sym() for p in self.p])
        return self._memo_sym

    def to_sym_op(self, *p):
        return self.eval_op(*p)

    def eval(self, env: Env):
        return self.eval_op(*[p.eval(env) for p in self.p])

    def eval_op(self, *p):
        raise "AstNode.eval_op must be overridden"

    def canon_op(self, *p):
        return self

    def folded(self):
        return self

    def canonical(self):
        if self._memo_canon == None:
            self._memo_canon = self.canon_op(*self.p)
        return self._memo_canon

    def __str__(self):
        if self._memo_str == None:
            self._memo_str = "%s(%s)" % (self.name, ", ".join([str(p) for p in self.p]))
        return self._memo_str

    def __eq__(self, other):
        if self is other:
            return True
        if (other is None) or not (type(self) is type(other)):
            return False
        return str(self) == str(other)

    def __lt__(self, other):
        if (self is other) or (other is None):
            return False
        if not isinstance(other, AstNode):
            return True
        return str(self) == str(other)
        # Note that self < other iff str(self) < str(other)!
        # if self.name is not other.name:
        #     assert self.name != other.name
        #     return self.name < other.name
        # for i in range(min(len(self.p), len(other.p))):
        #     if self.p[i] != other.p[i]:
        #         return self.p[i] < other.p[i]
        # # self and other probably equal, I guess -- mostly we try to avoid this
        # return len(self.p) < len(other.p)

    # We CAN use AstNode.__eq__ and __lt__, but it's very precarious: the name
    # comes first in self.p and because of the order of checks and comparisons
    # we won't try to compare the lambdas which are in self.p[1]. If it breaks
    # it's reasonable to implement custom __eq__ and __lt__ here.


class Const(AstNode):
    v: int

    def __init__(self, v):
        AstNode.__init__(self)
        self.v = v

    def to_sym(self):
        return self.v

    def eval(self, _: Env):
        return self.v

    def __str__(self):
        return str(self.v)


class Var(AstNode):
    v: int

    def __init__(self, name, a):
        AstNode.__init__(self)
        self.name = name
        self.a = a

    def to_sym(self):
        global sym_env
        return self.a(sym_env)

    def eval(self, env: Env):
        return self.a(env)

    def __str__(self):
        return self.name


class Add(AstNode):
    def canon_op(self, e0, e1):
        if str(e1) < str(e0):
            return Add(e1, e0)
        return self

    def eval_op(self, e0, e1):
        return e0 + e1


class Multiply(AstNode):
    def canon_op(self, e0, e1):
        if str(e1) < str(e0):
            return Multiply(e1, e0)
        return self

    def eval_op(self, e0, e1):
        return e0 * e1


class Ite(AstNode):
    def eval_op(self, b, e0, e1):
        return e0 if b else e1

    def to_sym_op(self, b, e0, e1):
        return sp.Piecewise((e0, b), (e1, True))


class And(AstNode):
    def canon_op(self, b0, b1):
        if str(b1) < str(b0):
            return And(b1, b0)
        return self

    def eval_op(self, b0, b1):
        return b0 and b1

    def to_sym_op(self, b0, b1):
        return sp.And(b0, b1)


class Or(AstNode):
    def canon_op(self, b0, b1):
        if str(b1) < str(b0):
            return And(b1, b0)
        return self

    def eval_op(self, b0, b1):
        return b0 or b1

    def to_sym_op(self, b0, b1):
        return sp.Or(b0, b1)


class Not(AstNode):
    def eval_op(self, b):
        return not b

    def to_sym_op(self, b):
        return sp.Not(b)


class Lt(AstNode):
    def eval_op(self, e0, e1):
        return e0 < e1

    def to_sym_op(self, e0, e1):
        return sp.Lt(e0, e1)


class Eq(AstNode):
    def canon_op(self, e0, e1):
        if str(e1) < str(e0):
            return Eq(e1, e0)
        return self

    def eval_op(self, e0, e1):
        return e0 == e1

    def to_sym_op(self, e0, e1):
        return sp.Eq(e0, e1)


c1 = Const(1)
c2 = Const(2)
c3 = Const(3)

x = Var("x", lambda e: e.x)
y = Var("y", lambda e: e.y)
z = Var("z", lambda e: e.z)


eval_globals = {
    "c1": c1,
    "c2": c2,
    "c3": c3,
    "x": x,
    "y": y,
    "z": z,
    "Add": Add,
    "Multiply": Multiply,
    "Ite": Ite,
    "And": And,
    "Or": Or,
    "Not": Not,
    "Lt": Lt,
    "Eq": Eq,
}


def parse_examples(filename):
    global eval_globals
    with open(filename) as f:
        m = re.match("#\\s*(.*)\\s*$", f.readline())
        if not m:
            return None
        expr_str = m.group(1)
        return eval(
            expr_str.replace("1", "c1").replace("2", "c2").replace("3", "c3"),
            eval_globals,
        )
