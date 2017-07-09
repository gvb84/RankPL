package com.tr.rp.ast.expressions;

import java.util.Arrays;
import java.util.Set;

import com.tr.rp.ast.AbstractExpression;
import com.tr.rp.ast.LanguageElement;
import com.tr.rp.exceptions.RPLException;
import com.tr.rp.exceptions.RPLTypeError;
import com.tr.rp.exceptions.RPLUndefinedException;
import com.tr.rp.varstore.VarStore;
import com.tr.rp.varstore.types.PersistentList;
import com.tr.rp.varstore.types.PersistentSet;
import com.tr.rp.varstore.types.PersistentStack;
import com.tr.rp.varstore.types.Type;

/**
 * The len expression. Returns the length of the given array or string.
 */
public class Len extends AbstractExpression {

	private final AbstractExpression e;
	
	public Len(AbstractExpression e) {
		this.e = e;
	}

	@Override
	public void getVariables(Set<String> list) {
		e.getVariables(list);
	}

	@Override
	public LanguageElement replaceVariable(String a, String b) {
		return new Len((AbstractExpression)e.replaceVariable(a, b));
	}

	@Override
	public boolean hasRankExpression() {
		return e.hasRankExpression();
	}

	@Override
	public AbstractExpression transformRankExpressions(VarStore v, int rank) throws RPLException {
		return new Len(e.transformRankExpressions(v, rank));
	}

	@Override
	public AbstractFunctionCall getEmbeddedFunctionCall() {
		return e.getEmbeddedFunctionCall();
	}

	@Override
	public AbstractExpression replaceEmbeddedFunctionCall(AbstractFunctionCall fc, String var) {
		return new Len((AbstractExpression)e.replaceEmbeddedFunctionCall(fc, var));
	}

	@Override
	public Object getValue(VarStore v) throws RPLException {
		Object o = e.getValue(v);
		if (o != null) {
			if (o instanceof String) {
				return ((String)o).length();
			} else if (o instanceof PersistentList) {
				return ((PersistentList)o).size();
			} else if (o instanceof PersistentStack) {
				return ((PersistentStack<?>)o).size();
			} else if (o instanceof PersistentSet) {
				return ((PersistentSet<?>)o).size();
			} else {
				throw new RPLTypeError("string, array, set or stack", o, e);
			}
		} else {
			throw new RPLUndefinedException(e);
		}
	}

	@Override
	public boolean hasDefiniteValue() {
		return e.hasDefiniteValue();
	}

	@Override
	public Object getDefiniteValue() throws RPLException {
		Object o = e.getDefiniteValue();
		if (o != null) {
			if (o instanceof String) {
				return ((String)o).length();
			} else if (o instanceof PersistentList) {
				return ((PersistentList)o).size();
			} else if (o instanceof PersistentStack) {
				return ((PersistentStack<?>)o).size();
			} else if (o instanceof PersistentSet) {
				return ((PersistentSet<?>)o).size();
			} else {
				throw new RPLTypeError("string, array, set or stack", o, e);
			}
		} else {
			throw new RPLUndefinedException(e);
		}
	}

	public String toString() {
		String es = e.toString();
		if (es.startsWith("(") && es.endsWith(")")) {
			es = es.substring(1, es.length()-1);
		}
		return "len(" + es + ")";
	}

	public boolean equals(Object o) {
		return (o instanceof Len) && ((Len)o).e.equals(e);
	}

	@Override
	public int hashCode() {
		return e.hashCode();
	}

}
