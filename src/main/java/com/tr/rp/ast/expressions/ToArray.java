package com.tr.rp.ast.expressions;

import java.util.Set;

import com.tr.rp.ast.AbstractExpression;
import com.tr.rp.ast.LanguageElement;
import com.tr.rp.exceptions.RPLException;
import com.tr.rp.exceptions.RPLTypeError;
import com.tr.rp.exceptions.RPLUndefinedException;
import com.tr.rp.varstore.VarStore;
import com.tr.rp.varstore.types.PersistentArray;
import com.tr.rp.varstore.types.PersistentList;
import com.tr.rp.varstore.types.PersistentMap;
import com.tr.rp.varstore.types.PersistentSet;
import com.tr.rp.varstore.types.PersistentStack;
import com.tr.rp.varstore.types.Type;

/**
 * Convert set or list to array
 */
public class ToArray extends AbstractExpression {

	private final AbstractExpression e;
	
	public ToArray(AbstractExpression e) {
		this.e = e;
	}

	@Override
	public void getVariables(Set<String> list) {
		e.getVariables(list);
	}

	@Override
	public LanguageElement replaceVariable(String a, String b) {
		return new ToArray((AbstractExpression)e.replaceVariable(a, b));
	}

	@Override
	public boolean needsRankExpressionTransformation() {
		return e.needsRankExpressionTransformation();
	}

	@Override
	public AbstractExpression doRankExpressionTransformation(VarStore v, int rank) throws RPLException {
		return new ToArray(e.doRankExpressionTransformation(v, rank));
	}

	@Override
	public AbstractFunctionCall getEmbeddedFunctionCall() {
		return e.getEmbeddedFunctionCall();
	}

	@Override
	public AbstractExpression replaceEmbeddedFunctionCall(AbstractFunctionCall fc, String var) {
		return new ToArray((AbstractExpression)e.replaceEmbeddedFunctionCall(fc, var));
	}

	@Override
	public Object getValue(VarStore v) throws RPLException {
		Object o = e.getValue(v);
		if (o != null) {
			if (o instanceof PersistentSet) {
				return new PersistentArray(e.getValue(v, Type.SET).asCollection());
			} else if (o instanceof PersistentList) {
				return new PersistentArray(e.getValue(v, Type.LIST).asCollection());
			} else {
				throw new RPLTypeError("set or list", o, e);
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
			} else if (o instanceof PersistentArray) {
				return ((PersistentArray)o).size();
			} else if (o instanceof PersistentStack) {
				return ((PersistentStack<?>)o).size();
			} else if (o instanceof PersistentSet) {
				return ((PersistentSet<?>)o).size();
			} else if (o instanceof PersistentMap) {
				return ((PersistentMap<?, ?>)o).size();
			} else if (o instanceof PersistentList) {
				return ((PersistentList<?>)o).size();
			} else {
				throw new RPLTypeError("string, array, set, map or stack", o, e);
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
		return "size(" + es + ")";
	}

	public boolean equals(Object o) {
		return (o instanceof ToArray) && ((ToArray)o).e.equals(e);
	}

	@Override
	public int hashCode() {
		return e.hashCode();
	}

}