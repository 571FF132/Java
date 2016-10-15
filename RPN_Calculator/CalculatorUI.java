import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

public class CalculatorUI extends JFrame implements ActionListener {
	
	JTextArea display1, display2;
	JButton one, zero, add, sub, mult, div, enter, clear, delete;
	char pls = 0x002b;
	String plus = String.valueOf(pls);
	char mns = 0x002d;
	String minus = String.valueOf(mns);
	char tms = 0x00d7;
	String times = String.valueOf(tms);
	char dvd = 0x00f7;
	String divide = String.valueOf(dvd);
	char lAro = 0x2B05;
	String leftArrow = String.valueOf(lAro);
	
	Operand operand1;
	Operand operand2;
	
	Object source;
	TheStack stack; 
	
	JPanel resultPanel, buttonRow1, buttonRow2, controlPanel;
	
	/*
	 * Constructor of the UI
	 */
	public CalculatorUI(){
		
	operand1 = new Operand();	
	operand2 = new Operand();
	stack = new TheStack();
	stack.stackPush(operand2);
	stack.stackPush(operand1);
	
	Container c = getContentPane();
	
	c.setLayout(new GridLayout(4, 1));
	
	resultPanel = new JPanel();
	resultPanel.setLayout(new FlowLayout());
	display1 = new JTextArea(operand1.toString(),1,25);
	display2 = new JTextArea(operand2.toString(),1,25);
	display1.setEditable(false);
	display1.setAlignmentX(SwingConstants.RIGHT);
	display2.setEditable(false);
	display2.setAlignmentX(SwingConstants.RIGHT);
	resultPanel.add(display1);
	resultPanel.add(display2);
	c.add(resultPanel);
	
	buttonRow1 = new JPanel();
	buttonRow1.setLayout(new FlowLayout());
	one = new JButton(" 1 ");
	one.addActionListener(this);
	one.setEnabled(true);
	buttonRow1.add(one);
	zero = new JButton(" 0 ");
	zero.addActionListener(this);
	zero.setEnabled(true);
	buttonRow1.add(zero);
	c.add(buttonRow1);
	
	buttonRow2= new JPanel();
	buttonRow2.setLayout(new FlowLayout());
	add = new JButton(" " +plus+" ");
	add.addActionListener(this);
	add.setEnabled(true);
	buttonRow2.add(add);
	sub = new JButton(" "+minus+" ");
	sub.addActionListener(this);
	sub.setEnabled(true);
	buttonRow2.add(sub);
	mult = new JButton(" "+times+" ");
	mult.addActionListener(this);
	mult.setEnabled(true);
	buttonRow2.add(mult);
	div = new JButton(" "+divide+" ");
	div.addActionListener(this);
	div.setEnabled(true);
	buttonRow2.add(div);
	c.add(buttonRow2);
	
	controlPanel = new JPanel();
	controlPanel.setLayout(new FlowLayout());
	delete = new JButton(" "+ leftArrow + " ");
	delete.addActionListener(this);
	delete.setEnabled(true);
	controlPanel.add(delete);
	clear = new JButton(" C ");
	clear.addActionListener(this);
	clear.setEnabled(true);
	controlPanel.add(clear);
	enter = new JButton("ENTER");
	enter.addActionListener(this);
	enter.setEnabled(true);
	controlPanel.add(enter);
	c.add(controlPanel);
	}

	public void updateDisplay(){
		operand1 = stack.stackPop();
		operand2 = stack.stackPop();
		display1.setText(operand1.toString());
		display2.setText(operand2.toString());
		stack.stackPush(operand2);
		stack.stackPush(operand1);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		source = e.getSource();
		int b =-1;
		if (source==zero) {
			b=0;
			stack.stackOperate(b);
		}else if (source==one){
			b=1;
			stack.stackOperate(b);
		}else if (source==add){
			b=2;
			stack.add();
		}else if (source==sub){
			b=3;
			stack.sub();
		}else if (source==mult){
			b=4;
			stack.mult();
		}else if (source==div){
			b=5;
			stack.div();
		}else if (source==delete){
			b=6;
			stack.stackOperate(6);
		}else if (source==clear){
			b=7;
			stack.stackClear();
		}else if (source==enter){
			b=8;
			stack.stackOperate(8);
		}else{
			b=-1;
		}
        updateDisplay();
	}
	
}
