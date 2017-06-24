package com.tr.rp.ast.statements;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.tr.rp.ast.AbstractExpression;
import com.tr.rp.ast.AbstractStatement;
import com.tr.rp.ast.LanguageElement;
import com.tr.rp.ast.statements.FunctionCallForm.ExtractedExpression;
import com.tr.rp.exceptions.RPLAssertionException;
import com.tr.rp.exceptions.RPLException;
import com.tr.rp.exceptions.RPLMiscException;
import com.tr.rp.exceptions.RPLTypeError;
import com.tr.rp.iterators.ranked.BufferingIterator;
import com.tr.rp.iterators.ranked.DuplicateRemovingIterator;
import com.tr.rp.iterators.ranked.ExecutionContext;
import com.tr.rp.iterators.ranked.RankedIterator;
import com.tr.rp.varstore.PersistentList;
import com.tr.rp.varstore.VarStore;

/**
 * The assert statement is intended for testing purposes. It takes as input
 * a value
 */
public class Assert extends AbstractStatement {

	private AbstractExpression expression;
	private AbstractExpression[] expected;
	
	public Assert(AbstractExpression expression, AbstractExpression[] expected) {
		this.expression = expression;
		this.expected = expected;
	}
	
	@Override
	public RankedIterator<VarStore> getIterator(RankedIterator<VarStore> parent, ExecutionContext c) throws RPLException {
		// Build map expected value -> expected rank
		final Map<Object, Integer> expectedValues = new LinkedHashMap<Object, Integer>();
		for (AbstractExpression e: expected) {
			if (!e.hasDefiniteValue()) {
				throw new RPLMiscException("Expected expression must be a definite expression", this);
			}
			PersistentList pl = e.getDefiniteListValue();
			if (pl.size() != 2) {
				throw new RPLMiscException("Expected list [rank, value]", e);
			}
			Object ro = pl.get(0);
			if (!(ro instanceof Integer)) {
				throw new RPLTypeError("integer", ro, e);
			}
			int rank = (Integer)ro;
			Object value = pl.get(1);
			expectedValues.put(value, rank);
		}
		
		// Buffer for re-use
		BufferingIterator<VarStore> bi = new BufferingIterator<VarStore>(parent);
		
		// Create iterator that generates the value to check
		RankedIterator<Object> vi = new RankedIterator<Object>() {

			@Override
			public boolean next() throws RPLException {
				return bi.next();
			}

			@Override
			public Object getItem() throws RPLException {
				return expression.getValue(bi.getItem());
			}

			@Override
			public int getRank() {
				return bi.getRank();
			}
		};

		// Remove duplicates
		DuplicateRemovingIterator<Object> dri = new DuplicateRemovingIterator<Object>(vi);
		
		// Check values
		RankedIterator<Object> ci = new RankedIterator<Object>() {

			@Override
			public boolean next() throws RPLException {
				boolean next = dri.next();
				if (next) {
					Object checkValue = dri.getItem();
					if (expectedValues.containsKey(checkValue)) {
						if (!expectedValues.get(checkValue).equals(getRank())) {
							throw new RPLAssertionException("Outcome " + checkValue + " has wrong rank (is " + getRank() + " but should be " + expectedValues.get(checkValue) + ")", Assert.this);
						}
						expectedValues.remove(checkValue);
					} else {
						throw new RPLAssertionException("Unexpected value " + checkValue + " (ranked " + getRank() + ")", Assert.this);
					}
				} else {
					if (!expectedValues.isEmpty()) {
						String missingValuesString = 
								expectedValues.entrySet().stream().map(e -> "[" + e.getKey() + ", rank " + e.getValue() + "]").collect(Collectors.joining(", "));
						throw new RPLAssertionException("Missing values: " + missingValuesString, Assert.this);
					}
				}
				return next;
			}

			@Override
			public Object getItem() throws RPLException {
				return dri.getItem();
			}

			@Override
			public int getRank() {
				return dri.getRank();
			}
			
		};
		
		// Consume all and buffer
		while (ci.next()) {}
		
		// Reset buffer and return original iterator
		bi.reset();
		bi.stopBuffering();
		return bi;
	}

	@Override
	public void getVariables(Set<String> list) {
		expression.getVariables(list);
		Arrays.stream(expected).forEach(e -> e.getVariables(list));
	}

	@Override
	public LanguageElement replaceVariable(String a, String b) {
		AbstractExpression newValue = (AbstractExpression)expression.replaceVariable(a, b);
		AbstractExpression[] newExpected = new AbstractExpression[expected.length];
		for (int i = 0; i < expected.length; i++) {
			newExpected[i] = (AbstractExpression)expected[i].replaceVariable(a, b);
		}
		return new Assert(newValue, newExpected);
	}

	@Override
	public AbstractStatement rewriteEmbeddedFunctionCalls() {
		// We're not rewriting the expected value here because it should not 
		// contain function calls (will throw exception in iterator)
		ExtractedExpression rewrittenExpression = FunctionCallForm.extractFunctionCalls(expression);
		if (rewrittenExpression.isRewritten()) {
			return new FunctionCallForm(
					new Assert(rewrittenExpression.getExpression(), expected), rewrittenExpression.getAssignments());
		} else {
			return this;
		}
	}

	@Override
	public void getAssignedVariables(Set<String> variables) {
		// nop
	}
	
	public String toString() {
		return "assert("+expression+","+
				Arrays.stream(expected).map(e -> e.toString()).collect(Collectors.joining(", ")) + ")";
	}

	@Override
	public boolean equals(Object o) {
		return (o instanceof Assert) &&
				((Assert)o).expression.equals(expression) &&
				Arrays.deepEquals(((Assert)o).expected, expected);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(expression) + Arrays.hashCode(expected);
	}	

}
