package main;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JRadioButtonMenuItem;

/**
 * In this class the JFrame is created and set.
 * <p>
 * Then the JPanel is set and the game can be played
 * @author bovacu
 */

public class MineSweeper {
	
	/**
	 * This will be used to change the width of the screen when more mines are added to the game or vice versa
	 */
	public static int defaultWidth;
	
	/**
	 * This will be used to change the height of the screen when more mines are added to the game or vice versa
	 */
	public static int defaultHeight;
	
	/**
	 * This is the width of the screen without setting any width manually
	 */
	public static int initialWidth;
	
	/**
	 * This is the height of the screen without setting any height manually
	 */
	public static int initialHeight;
	
	/**
	 * The method that makes the program run
	 * @param args not needed
	 */
	public static void main(String args[]) {
		JFrame f = new JFrame("MineSweeper");
		f.setVisible(true);
		f.setResizable(false);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		initialWidth = f.getWidth();
		initialHeight = f.getHeight();
		defaultWidth = 0;
		defaultHeight = 0;
		addMenuBar(f);
		f.setContentPane(new MineBoard(f, 10, 10));
		f.invalidate();
        f.validate();
	}
	
	/**
	 * Generates the menu bar options for the game
	 * @param f the main JFrame
	 */
	private static void addMenuBar(JFrame f) {
		JMenuBar menuBar = new JMenuBar();
		JMenu boardSizeMenu = new JMenu("Board Size Options");
		customizeElements(boardSizeMenu, new String[] {"10x10", "15x15", "20x20", "25x25", "30x30"});
		
		
		JMenu bombsMenu = new JMenu("Bombs Options");
		customizeElements(bombsMenu, new String[] {"10", "15", "20", "25", "30", "40", "50", "60"});
		
		menuBar.add(boardSizeMenu);
		menuBar.add(bombsMenu);
		
		f.setJMenuBar(menuBar);
	}
	
	/**
	 * This adds to the JMenu all the needed radio buttons an its functionalities
	 * @param menu the JMenu that needs options to be added
	 * @param names the list of names of the radio buttons
	 */
	private static void customizeElements(JMenu menu, String names[]) {
		for(int i = 0; i < names.length; i++) {
			JRadioButtonMenuItem button = new JRadioButtonMenuItem (names[i]);
			if(i == 0)
				button.setSelected(true);
			
			button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					for(int i = 0; i < menu.getItemCount(); i++) {
						if(menu.getItem(i) != button)
							menu.getItem(i).setSelected(false);
					}
				}
			});
			
			menu.add(button);
		}
	}
}
