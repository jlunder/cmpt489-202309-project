package synth.algorithms.representation;

import synth.algorithms.ast.*;
import synth.core.*;

public interface BoolRepresentation {
    public BoolNode reifyAsBoolAst();
    public ParseNode reifyAsBoolParse();
    public boolean evalBool(Environment env);
}
