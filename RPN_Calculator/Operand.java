
public class Operand {
	
	private String text;
	private int value;
	
	public Operand(){
		this.text = "0000";
		this.value = Integer.parseUnsignedInt(text, 2);
	}
	
	public int getValue(){
		return value;
	}
	
	public void setOperand(String t){
		this.text = t;
		this.value = Integer.parseInt(text, 2);
	}
	
	public void setOperand(int t){
		this.value = t;
		this.text = Integer.toBinaryString(value);
		while (this.text.length()<4){
			this.text = "0".concat(this.text);
		}
	}
	
	public String toString(){
		return text;
	}

}
