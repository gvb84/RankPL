package com.tr.rp.ast.statements;

import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import com.tr.rp.ast.AbstractExpression;
import com.tr.rp.ast.AbstractStatement;
import com.tr.rp.ast.LanguageElement;
import com.tr.rp.ast.statements.FunctionCallForm.ExtractedExpression;
import com.tr.rp.exceptions.RPLException;
import com.tr.rp.exec.ExecutionContext;
import com.tr.rp.exec.Executor;
import com.tr.rp.exec.RankTransformer;
import com.tr.rp.exec.State;
import com.tr.rp.varstore.types.Type;

public class While extends AbstractStatement implements IfElseErrorHandler {

	/** Optional statement to execute before evaluating while condition */
	private AbstractStatement preStatement;

	/** While condition */
	private AbstractExpression whileCondition;
	
	/** While statement body */
	private AbstractStatement body;
	
	public While(AbstractExpression whileCondition, AbstractStatement body) {
		this.whileCondition = whileCondition;
		this.body = body;
		this.preStatement = null;
	}
	
	private While(AbstractExpression whileCondition, AbstractStatement body, AbstractStatement preStatement) {
		this.whileCondition = whileCondition;
		this.body = body;
		this.preStatement = preStatement;
	}

	private int itCount = 0;
	
	public Executor getIteration(Supplier<AbstractExpression> exp, Executor out, int shift, ExecutionContext c) {
		final int thisCount = itCount;
		itCount++;
		Executor iterate = new Executor() {

			private Executor next;
			private int offset = -1;
			
			@Override
			public void close() throws RPLException {
				if (next != null) {
					next.close();
				} else {
					out.close();
				}
			}

			@Override
			public void push(State s) throws RPLException {
				if (!exp.get().getValue(s.getVarStore(), Type.BOOL)) {
					//System.out.println("push out " + s.shiftUp(shift));
					out.push(s.shiftUp(shift));
				} else {
					if (next == null) {
						offset = s.getRank();
						next = body.getExecutor(getIteration(exp, out, shift + offset, c), c);
					}
					next.push(s.shiftDown(offset));
				}
			}
		};
		return iterate;
	}

	@Override
	public Executor getExecutor(Executor out, ExecutionContext c) {
		RankTransformer<AbstractExpression> transformWhileCond = RankTransformer.create(whileCondition);
		transformWhileCond.setOutput(getIteration(transformWhileCond, out, 0, c), this);
		return transformWhileCond;
	}	
//	private BufferingIterator<VarStore> iterate(BufferingIterator<VarStore> in, ExecutionContext c) throws RPLException {
//		in.reset();
//		in.stopBuffering();
//		return new BufferingIterator<VarStore>(generateIteration(in, c));
//	}
//
//	private boolean isDone(VarStore item) throws RPLException {
//		Objects.requireNonNull(item);
//		return !whileCondition.getValue(item, Type.BOOL);
//	}
//
//	public RankedIterator<VarStore> getIterator(RankedIterator<VarStore> in, ExecutionContext c) throws RPLException {
//		HashSet<RankedItem<VarStore>> seen = new HashSet<RankedItem<VarStore>>();
//
//		return new RankedIterator<VarStore>() {
//
//			private BufferingIterator<VarStore> current = null;
//
//			private RankedItem<VarStore> currentItem = null;
//			
//			private RankedIterator<VarStore> init() throws RPLException {
//				current = new BufferingIterator<VarStore>(in);
//				if (!current.next()) current = null;
//				return this;
//			}
//			
//			@Override
//			public boolean next() throws RPLException {
//				currentItem = getNextUniqueItem();
//				return currentItem != null;
//			}
//
//			@Override
//			public VarStore getItem() throws RPLException {
//				return currentItem != null? currentItem.item: null;
//			}
//
//			@Override
//			public int getRank() {
//				return currentItem != null? currentItem.rank: -1;
//			}
//
//			private RankedItem<VarStore> getNextItem() throws RPLException {
//				while (true) {
//					if (current == null) {
//						return null;
//					}
//			 		VarStore item = current.getItem();
//					if (isDone(item)) {
//						int rank = current.getRank();
//						if (!current.next()) current = null;
//						return new RankedItem<VarStore>(item, rank);
//					} else {
//						current = iterate(current, c);
//						if (!current.next()) current = null;
//					}
//				}
//			}
//			
//			private RankedItem<VarStore> getNextUniqueItem() throws RPLException {
//				RankedItem<VarStore> item = getNextItem();
//				while (item != null && seen.contains(item)) {
//					item = getNextItem();
//				}
//				if (item != null) {
//					seen.add(item);
//				}
//				return item;
//			}
//
//		}.init();
//
//	}
//	
//	private RankedIterator<VarStore> generateIteration(RankedIterator<VarStore> in, ExecutionContext c) throws RPLException {
//		IfElse ifElse = new IfElse(whileCondition, body, new Skip(), this);
//		if (preStatement == null) {
//			return ifElse.getIterator(in, c);
//		} else {
//			return new Composition(preStatement, ifElse).getIterator(in, c);
//		}
//	}
	
	public boolean equals(Object o) {
		return o instanceof While &&
				((While)o).whileCondition.equals(whileCondition) &&
				((While)o).body.equals(body);
	}

	@Override
	public LanguageElement replaceVariable(String a, String b) {
		return new While((AbstractExpression)whileCondition.replaceVariable(a, b),
				(AbstractStatement)this.body.replaceVariable(a, b));
	}
	
	public String toString() {
		String expString = whileCondition.toString();
		if (preStatement != null && preStatement instanceof FunctionCallForm) {
			
		}
		if (!(expString.startsWith("(") && expString.endsWith(")"))) {
			expString = "(" + expString + ")";
		}
		String ss = body.toString();
		if (preStatement != null && preStatement instanceof FunctionCallForm) {
			expString = ((FunctionCallForm)preStatement).transformStatement(expString);
			ss = ((FunctionCallForm)preStatement).transformStatement(ss);
		}
		return "while " + expString + " do " + ss;
	}
	
	@Override
	public void getVariables(Set<String> list) {
		body.getVariables(list);
		whileCondition.getVariables(list);
	}

	@Override
	public AbstractStatement rewriteEmbeddedFunctionCalls() {
		if (preStatement != null) {
			throw new UnsupportedOperationException();
		}
		AbstractStatement sr = body.rewriteEmbeddedFunctionCalls();
		ExtractedExpression rewrittenExp = FunctionCallForm.extractFunctionCalls(whileCondition);
		if (rewrittenExp.isRewritten()) {
			return new While(rewrittenExp.getExpression(), sr, new FunctionCallForm(new Skip(), rewrittenExp.getAssignments()));
		} else {
			return new While(whileCondition, sr);
		}
	}	

	@Override
	public void getAssignedVariables(Set<String> variables) {
		body.getAssignedVariables(variables);
	}

	@Override
	public int hashCode() {
		return Objects.hash(preStatement, whileCondition, body);
	}

	@Override
	public void ifElseConditionError(RPLException e) throws RPLException {
		e.setStatement(this);
		e.setExpression(whileCondition);
		throw e;
	}

	@Override
	public void ifElseThenError(RPLException e) throws RPLException {
		throw e;
	}

	@Override
	public void ifElseElseError(RPLException e) throws RPLException {
		throw e;
	}


}
