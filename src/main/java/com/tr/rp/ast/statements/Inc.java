package com.tr.rp.ast.statements;

import java.util.Objects;
import java.util.Set;

import com.tr.rp.ast.AbstractStatement;
import com.tr.rp.ast.LanguageElement;
import com.tr.rp.ast.expressions.AssignmentTarget;
import com.tr.rp.ast.statements.FunctionCallForm.ExtractedExpression;
import com.tr.rp.exceptions.RPLException;
import com.tr.rp.iterators.ranked.ExecutionContext;
import com.tr.rp.iterators.ranked.RankTransformIterator;
import com.tr.rp.iterators.ranked.RankedIterator;
import com.tr.rp.varstore.VarStore;

/**
 * Increase integer variable by one
 */
public class Inc extends AbstractStatement {
	
	private final AssignmentTarget target;
	
	public Inc(AssignmentTarget var) {
		this.target = var;
	}

	@Override
	public RankedIterator<VarStore> getIterator(final RankedIterator<VarStore> in, ExecutionContext c) throws RPLException {
		try {
			RankTransformIterator rt = 
				new RankTransformIterator(in, this, target);
			final AssignmentTarget target = (AssignmentTarget)rt.getExpression(0);
			RankedIterator<VarStore> ai = new RankedIterator<VarStore>() {
	
				@Override
				public boolean next() throws RPLException {
					return rt.next();
				}
	
				@Override
				public VarStore getItem() throws RPLException {
					VarStore item = rt.getItem();
					if (item == null) return null;
					try {
						return target.assign(item, target.convertToRHSExpression().getIntValue(item) + 1);
					} catch (RPLException e) {
						e.setStatement(Inc.this);
						throw e;
					}
				}
	
				@Override
				public int getRank() {
					return rt.getRank();
				}
			};
			return ai;
		} catch (RPLException e) {
			e.setStatement(this);
			throw e;
		}
	}
	
	
	public String toString() {
		return target.toString() + "--";
	}
	
	public boolean equals(Object o) {
		return o instanceof Inc && ((Inc)o).target.equals(target);
	}

	@Override
	public int hashCode() {
		return Objects.hash(target);
	}	

	@Override
	public LanguageElement replaceVariable(String a, String b) {
		return new Inc((AssignmentTarget)target.replaceVariable(a, b));
	}

	@Override
	public void getVariables(Set<String> list) {
		target.getVariables(list);
	}

	@Override
	public AbstractStatement rewriteEmbeddedFunctionCalls() {
		ExtractedExpression rewrittenTarget = FunctionCallForm.extractFunctionCalls(target);
		if (rewrittenTarget.isRewritten()) {
			return new FunctionCallForm(
					new Inc((AssignmentTarget)rewrittenTarget.getExpression()), 
						rewrittenTarget.getAssignments());
		} else {
			return this;
		}
	}

	@Override
	public void getAssignedVariables(Set<String> variables) {
		variables.add(target.getAssignedVariable());
	}	
}
