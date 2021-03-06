package com.tr.rp.ast.expressions;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.tr.rp.ast.AbstractExpression;
import com.tr.rp.ast.LanguageElement;
import com.tr.rp.exceptions.RPLException;
import com.tr.rp.varstore.VarStore;
import com.tr.rp.varstore.arrays.ArrayFactory;

/**
 * Array construction expression. Evaluates to an array with a given
 * set of values.
 */
public class ArrayConstructExpression extends AbstractExpression {

	private final AbstractExpression[] values;
	
	public ArrayConstructExpression(AbstractExpression ... values) {
		this.values = values;
	}
	
	public ArrayConstructExpression(int ... values) {
		this.values = new AbstractExpression[values.length];
		for (int i = 0; i < values.length; i++) {
			this.values[i] = new Literal<Integer>(values[i]);
		}
	}

	@Override
	public void getVariables(Set<String> list) {
		Arrays.stream(values).forEach(e -> e.getVariables(list));
	}

	@Override
	public boolean hasRankExpression() {
		return Arrays.stream(values).anyMatch(e -> e.hasRankExpression());
	}

	@Override
	public AbstractExpression transformRankExpressions(VarStore v, int rank) throws RPLException {
		AbstractExpression[] newValues = new AbstractExpression[values.length];
		for (int i = 0; i < values.length; i++) {
			newValues[i] = (AbstractExpression)values[i].transformRankExpressions(v, rank);
		}
		ArrayConstructExpression a = new ArrayConstructExpression(newValues);
		a.setLineNumber(getLineNumber());
		return a;
	}

	@Override
	public AbstractFunctionCall getEmbeddedFunctionCall() {
		for (int i = 0; i < values.length; i++) {
			AbstractFunctionCall fc = values[i].getEmbeddedFunctionCall();
			if (fc != null) {
				return fc;
			}
		}
		return null;
	}

	@Override
	public AbstractExpression replaceEmbeddedFunctionCall(AbstractFunctionCall fc, String var) {
		AbstractExpression[] newValues = new AbstractExpression[values.length];
		for (int i = 0; i < values.length; i++) {
			newValues[i] = (AbstractExpression)values[i].replaceEmbeddedFunctionCall(fc, var);
		}
		ArrayConstructExpression a = new ArrayConstructExpression(newValues);
		a.setLineNumber(getLineNumber());
		return a;
	}

	@Override
	public Object getValue(VarStore e) throws RPLException {
		Object[] evaluatedValues = new Object[values.length];
		for (int i = 0; i < values.length; i++) {
			evaluatedValues[i] = values[i].getValue(e);
		}
		return ArrayFactory.newArray(evaluatedValues);
	}

	@Override
	public boolean hasDefiniteValue() {
		for (AbstractExpression e: values) {
			if (!e.hasDefiniteValue()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Object getDefiniteValue() throws RPLException {
		Object[] evaluatedValues = new Object[values.length];
		for (int i = 0; i < values.length; i++) {
			evaluatedValues[i] = values[i].getDefiniteValue();
		}
		return ArrayFactory.newArray(evaluatedValues);
	}
	
	public String toString() {
		return "[" + Arrays.stream(values)
			.map(v -> v.toString())
			.collect(Collectors.joining(", ")) + "]";
	}
	
	public boolean equals(Object o) {
		return (o instanceof ArrayConstructExpression) &&
				Arrays.deepEquals(((ArrayConstructExpression)o).values, values);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(values);
	}

}
