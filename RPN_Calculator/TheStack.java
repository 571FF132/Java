import java.util.LinkedList;

public class TheStack {
	protected int operandOneInt;
	LinkedList<Operand> stack;
	
	public TheStack(){
		 stack = new LinkedList<Operand>();	
	}

	public void stackClear(){
		while(stack.size()>0){
			stack.pop();
		}
		Operand nu1 = new Operand();
		Operand nu2 = new Operand();
		stack.push(nu2);
		stack.push(nu1);
	}
	
	public void add(){
		operandOneInt =stack.pop().getValue() + stack.pop().getValue();
		Operand nu = new Operand();
		nu.setOperand(operandOneInt);
		stack.push(nu);
	}
	
	public void sub(){
		operandOneInt =stack.pop().getValue() - stack.pop().getValue();
		Operand nu = new Operand();
		nu.setOperand(operandOneInt);
		stack.push(nu);	
	}
	
	public void mult(){
		operandOneInt =stack.pop().getValue() * stack.pop().getValue();
		Operand nu = new Operand();
		nu.setOperand(operandOneInt);
		stack.push(nu);
	}
	public void div(){
		operandOneInt =stack.pop().getValue() / stack.pop().getValue();
		Operand nu = new Operand();
		nu.setOperand(operandOneInt);
		stack.push(nu);	
	}
	
	public void stackOperate(int b){
		if (b==0){
			if(stack.peek().toString().length()>=4){
				stack.peek().setOperand(stack.peek().toString().substring(1, 4));
			}
			stack.peek().setOperand(stack.peek().toString().concat("0"));
		}
		if (b==1){
			if(stack.peek().toString().length()>=4){
				stack.peek().setOperand(stack.peek().toString().substring(1, 4));
			}
			stack.peek().setOperand(stack.peek().toString().concat("1"));
		}
		if (b==6){
			if(stack.peek().toString().length()>1){
				stack.peek().setOperand(stack.peek().toString().substring(0, stack.peek().toString().length()-1));
			}else{
				stack.peek().setOperand("0000");
			}
		}
		if (b==8){
			Operand nu = new Operand();
			String nustr = stack.peek().toString();
			nu.setOperand(nustr);
			stack.push(nu);
		}
	}
	
	public Operand stackPop(){
		if(stack.isEmpty()){
			return new Operand();
		}else
		return stack.pop();
	}
	
	public void stackPush(Operand o){
		stack.push(o);
	}
}
