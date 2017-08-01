package com.tr.rp.ast.expressions;

import java.util.Objects;
import java.util.Set;

import com.tr.rp.ast.AbstractExpression;
import com.tr.rp.ast.LanguageElement;
import com.tr.rp.exceptions.RPLException;
import com.tr.rp.varstore.VarStore;
import com.tr.rp.varstore.types.Type;

/**
 * set = remove(set, item) - remove item from set
 */
public class SetRemove extends AbstractExpression {

	private final AbstractExpression set;
	private final AbstractExpression value;
	
	public SetRemove(AbstractExpression set, AbstractExpression value) {
		this.set = set;
		this.value = value;
	}

	@Override
	public void getVariables(Set<String> list) {
		set.getVariables(list);
		value.getVariables(list);
	}

	@Override
	public LanguageElement replaceVariable(String a, String b) {
		return new SetRemove((AbstractExpression)set.replaceVariable(a, b), 
				(AbstractExpression)value.replaceVariable(a, b));
	}

	@Override
	public boolean needsRankExpressionTransformation() {
		return set.needsRankExpressionTransformation() || value.needsRankExpressionTransformation();
	}

	@Override
	public AbstractExpression doRankExpressionTransformation(VarStore v, int rank) throws RPLException {
		return new SetRemove(set.doRankExpressionTransformation(v, rank),
				value.doRankExpressionTransformation(v, rank));
	}

	@Override
	public AbstractFunctionCall getEmbeddedFunctionCall() {
		AbstractFunctionCall fc = set.getEmbeddedFunctionCall();
		if (fc == null) {
			return value.getEmbeddedFunctionCall();
		} else {
			return fc;
		}
	}

	@Override
	public AbstractExpression replaceEmbeddedFunctionCall(AbstractFunctionCall fc, String var) {
		return new SetRemove((AbstractExpression)set.replaceEmbeddedFunctionCall(fc, var),
				(AbstractExpression)value.replaceEmbeddedFunctionCall(fc, var));
	}

	@Override
	public Object getValue(VarStore e) throws RPLException {
		return set.getValue(e, Type.SET).remove(value.getValue(e));
	}

	@Override
	public boolean hasDefiniteValue() {
		return set.hasDefiniteValue() && value.hasDefiniteValue();
	}

	@Override
	public Object getDefiniteValue() throws RPLException {
		return set.getDefiniteValue(Type.SET).remove(value.getDefiniteValue());
	}

	public String toString() {
		return "remove(" + set + ", "+ value +")";
	}
	
	public boolean equals(Object o) {
		return (o instanceof SetRemove) && ((SetRemove)o).set.equals(set) 
				&& ((SetRemove)o).value.equals(value);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(getClass().hashCode(), set.hashCode(), value.hashCode());
	}

}