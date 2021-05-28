package anomTrajectories;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Class with static methods to create and manage a progress bar dialog
 * 
 * @author Universitat Rovira i Virgili
 */
public class Progress {
	private static View window;
	private static JFrame father;
	private static int currentValue;
	private static int totalValue;
	private static long iniTime;
	private static boolean end;
	private static Timer timer;
	private static int scale;
	private static int partial;
	private static boolean closeWhenFinish;

	/**
	 * Method that creates a progress bar dialog
	 * @param p: parent view
	 * @param total: the maximum number of the progress bar
	 * 			the progress finishess when the progress value reaches the total value
	 * 
	 * @author Universitat Rovira i Virgili
	 */
	public static void createProgress(JFrame p, long total, boolean closeOnFinish) {
		father = p;
		iniTime = 0;
		end = false;
		currentValue = 0;
		partial = 1;
		closeWhenFinish = closeOnFinish;
		calculateScale(total);
		createWindow();
	}
	
	/**
	 * Method that updates the progress bar in one step
	 * 
	 * @return true if it has been clicked the cancel button, false otherwise
	 * @author Universitat Rovira i Virgili
	 */
	public static boolean update(){
		if(partial >= scale){
			partial = 1;
			currentValue++;
			update(currentValue);
		}
		else{
			partial++;
		}
		
		return end;
	}
	
	/**
	 * Method that updates the progress bar with a defined number
	 * 
	 * @param value: the number which the progress bar is updated
	 * @return true if it has been clicked the cancel button, false otherwise
	 * @author Universitat Rovira i Virgili
	 */
	public static boolean update(int value){
		long currentTime, elapsedTime, estimatedTime, remainingTime;
		
		currentTime = System.currentTimeMillis();
		if(iniTime == 0){
			iniTime = currentTime;
			elapsedTime = currentTime - iniTime;
		}
		else{
			elapsedTime = currentTime - iniTime;
			estimatedTime = (long)((double)((double)elapsedTime / (double)value) * totalValue);
			remainingTime = estimatedTime - elapsedTime;
			window.setTextRemaining(remainingTime);
		}
		window.updateProgress(value);
		currentValue = value;
		if(currentValue >= totalValue){
			finish();
		}
		
		return end;
	}
	
	/**
	 * Method that finishes the progress bar
	 * used when the maximum value has been reached
	 * 
	 * @author Universitat Rovira i Virgili
	 */
	public static void finish(){
		window.setTextFinalTime(System.currentTimeMillis()-iniTime);
		setEnd(false);
		window.enableOkButton();
		window.updateProgress(totalValue);
		if(closeWhenFinish) {
			window.closeDialog();
		}
	}
	
	//creates the dialog window
	private static void createWindow(){
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					timer = new Timer(500,null);
					timer.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							if(iniTime > 0){
								window.setTextElapsed(System.currentTimeMillis()-iniTime);
							}
						}
					});
					window = new View(father);
					window.setTotalProgress(totalValue);
					timer.start();
					window.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		try {
		Thread.sleep(500);
	} catch (InterruptedException e) {
		e.printStackTrace();
	}
	}
	
	//Calculates the scale when the maximum number of the progress bar
	//is greater than an the maximum value of a integer
	private static void calculateScale(long total){
		boolean ok;

		scale = 1;
		ok = false;
		while(!ok){
			if(total > Integer.MAX_VALUE){
				total /= 10;
				scale *= 10;
			}
			else{
				ok = true;
			}
		}
		totalValue = (int)total;
	}
	
	/**
	 * Method that finishes the progress bar
	 * used when the cancel button has been clicked
	 * 
	 * @param Boolean indicating if the operation is finished
	 * 
	 * @author Universitat Rovira i Virgili
	 */
	public static void setEnd(boolean e){
		timer.stop();
		end = e;
	}
}