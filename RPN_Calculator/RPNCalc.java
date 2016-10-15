import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


public class RPNCalc {

	public static void main(String[] args) {
		
		CalculatorUI RPNCalc = new CalculatorUI();
		RPNCalc.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
		});
        RPNCalc.setSize(320, 300);
        RPNCalc.setTitle("STIFFLER'S RPN BINARY CALCULATOR");
        RPNCalc.setVisible(true);
	}
}
