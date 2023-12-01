package synth.algorithms.voltron;

import synth.algorithms.ast.*;

public class DecisionTreeBranch extends IteNode{

    public DecisionTreeBranch(BoolNode cond, ExprNode ifCond, ExprNode elseCond) {
        super(cond, ifCond, elseCond);
    }
    
}
