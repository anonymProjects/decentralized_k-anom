package anomTrajectories;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

/**
 * Class that represents a progress bar dialog
 * 
 * @author Universitat Rovira i Virgili
 */
public class View {

	private JDialog dialog;
	private JFrame father;
	private JLabel labelElapsed;
	private JLabel labelRemaining;
	private JProgressBar progressBar;
	private JButton bOk;

	/**
	 * Creates an instance of the progres bar dialog
	 * 
	 * @param father: parent view
	 * @author Universitat Rovira i Virgili
	 */
	public View(JFrame father) {
		this.father = father;
		initialize();
	}

	/**
	 * Initializes the progress bar dialog
	 * 
	 * @author Universitat Rovira i Virgili
	 */
	private void initialize() {
		dialog = new JDialog(father, true);
		dialog.setResizable(false);
		dialog.setBounds(100, 100, 260, 200);
		dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		dialog.setLocationRelativeTo(father);
		dialog.getContentPane().setLayout(null);
		
		dialog.addWindowListener(new WindowAdapter() {
			 public void windowClosing(WindowEvent e) {
				 Progress.setEnd(true);
				 dialog.dispose();
			   }
		});
		
		JLabel lblElapsed = new JLabel("Elapsed:");
		lblElapsed.setBounds(55, 20, 52, 14);
		lblElapsed.setHorizontalAlignment(JLabel.RIGHT);
		dialog.getContentPane().add(lblElapsed);
		
		labelElapsed = new JLabel("0");
		labelElapsed.setBounds(124, 20, 122, 14);
		dialog.getContentPane().add(labelElapsed);
		
		JLabel lblRemaining = new JLabel("Remaining:");
		lblRemaining.setBounds(37, 47, 70, 14);
		lblRemaining.setHorizontalAlignment(JLabel.RIGHT);
		dialog.getContentPane().add(lblRemaining);
		
		labelRemaining = new JLabel("0");
		labelRemaining.setBounds(124, 47, 122, 14);
		dialog.getContentPane().add(labelRemaining);
		
		progressBar = new JProgressBar();
		progressBar.setBounds(10, 75, 224, 27);
		progressBar.setStringPainted(true);
		dialog.getContentPane().add(progressBar);
		
		bOk = new JButton("Cancel");
		bOk.setBounds(81, 117, 80, 25);
		bOk.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Progress.setEnd(true);
				dialog.dispose();
			}
		});
		dialog.getContentPane().add(bOk);
		dialog.toFront();
//		dialog.setAlwaysOnTop(true);
	}
	
	/**
	 * Makes visible the progress bar dialog
	 * 
	 * @param visible: true to make visible the dialog
	 * @author Universitat Rovira i Virgili
	 */
	public void setVisible(boolean visible){
		this.dialog.setVisible(visible);
	}
	
	/**
	 * Enables the "ok" button in the progress bar dialog
	 * 
	 * @author Universitat Rovira i Virgili
	 */
	public void enableOkButton(){
		bOk.setText("Ok");
	}
	
	/**
	 * Sets the maximum (final) value for the progress bar
	 * @param value: maximum value for the progress bar
	 * 
	 * @author Universitat Rovira i Virgili
	 */
	public void setTotalProgress(int value){
		progressBar.setMaximum(value);
	}
	
	/**
	 * Updates the progress bar with a defined value
	 * 
	 * @param value: updates the progress bar with the specified value
	 * @author Universitat Rovira i Virgili
	 */
	public void updateProgress(int value){
		progressBar.setValue(value);
	}
	
	/**
	 * Updates the elapsed time
	 * 
	 * @param milis: time elapsed in miliseconds
	 * @author Universitat Rovira i Virgili
	 */
	public void setTextElapsed(long milis){
		String s;
		
		s = convert(milis);
		labelElapsed.setText(s);
	}
	
	/**
	 * Shows the remaining time with hundreths
	 * Used to show the final time spent
	 * 
	 * @param value: time elapsed in miliseconds
	 * @author Universitat Rovira i Virgili
	 */
	public void setTextFinalTime(long milis){
		String s;
		
		s = convertLong(milis);
		labelElapsed.setText(s);
	}
	
	/**
	 * Shows the remaining time without hundredths
	 * Used to show the progress time
	 * 
	 * @param value: time elapsed in miliseconds
	 * @author Universitat Rovira i Virgili
	 */
	public void setTextRemaining(long milis){
		String s;
		
		s = convert(milis);
		labelRemaining.setText(s);
		
	}
	
	public void closeDialog() {
		this.dialog.dispose();
	}
	
	//convert miliseconds to d:hh:mm:ss format
	private String convert(long milis){
		String s;
		int day, hour, minute, second;
		long remDay, remHour, remMinute;
		
		day = (int)(milis / (3600000 * 24));
		remDay = milis % (3600000 * 24);
		hour = (int)(remDay / 3600000);
		remHour = remDay % 3600000;
		minute = (int)(remHour / 60000);
		remMinute = remHour % 60000;
		second = (int)(remMinute / 1000);
		
		s = "";
		if(day > 0){
			s += String.valueOf(day);
			s += "d:";
		}
		if(hour > 0){
			s += String.format("%02d", hour);
			s += "h:";
		}
		if(minute > 0){
			s += String.format("%02d", minute);
			s += "m:";
		}
		s += String.format("%02d", second);
		s += "s";
		
		
		return  s;
	}
	
	//convert miliseconds to d:hh:mm:ss.xx format
	//with hundredths
	private String convertLong(long milis){
		String s;
		int day, hour, minute, second, hundreth;
		long remDay, remHour, remMinute, remSecond;
		
		day = (int)(milis / (3600000 * 24));
		remDay = milis % (3600000 * 24);
		hour = (int)(remDay / 3600000);
		remHour = remDay % 3600000;
		minute = (int)(remHour / 60000);
		remMinute = remHour % 60000;
		second = (int)(remMinute / 1000);
		remSecond = remMinute % 1000;
		hundreth = (int)(remSecond / 10);
		
		s = "";
		if(day > 0){
			s += String.valueOf(day);
			s += "d:";
		}
		if(hour > 0){
			s += String.format("%02d", hour);
			s += "h:";
		}
		if(minute > 0){
			s += String.format("%02d", minute);
			s += "m:";
		}
		s += String.format("%02d", second);
		s += ".";
		s += String.format("%02d", hundreth);
		s += "s";
		
		return  s;
	}
}