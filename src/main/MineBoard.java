package main;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * This is the main class, where all the calculus is made.
 * @author bovacu
 *
 */

public class MineBoard extends JPanel implements Runnable{
	private static final long serialVersionUID = -5450874958181074569L;
	
	private final int MINE_SIZE = 64;
	private final int CLOCK_SIZE = 3;
	private final int CLOCK_HEIGHT = 138;
	private final int CLOCK_WIDTH = 78;
	
	private final int RESTART_X;
	private final int RESTART_Y;
	private final int RESTART_WIDTH = 69;
	private final int RESTART_HEIGHT = 69;
	
	private final String DEAD = "/graphics/dead.png";
	private final String FLAG = "/graphics/flag.png";
	private final String UNREVEALED = "/graphics/unrevealed.png";
	private final String EXPLODING_BOMB = "/graphics/explodingBomb.png"; 
	private final String WIN = "/graphics/dead.png";
	private final String QUESTION = "/graphics/question.png";
	private final String RESET = "/graphics/smile_up.png";
	
	private int board[][];
	private Image images[];
	private Image clock[];
	private Image bombs[];
	private Image restart;
	private Image gameResult;
	private boolean loose;
	private boolean win;
	private JFrame f;
	private final int Y_MATRIX_OFFSET = 150;
	private int bombsLeft;
	private int timeLeft;
	private List<Integer[]> markedBombs;
	
	private int boardSize;
	private int numberOfBombs;
	
	private Thread thread;
	
	/**
	 * Takes 3 arguments
	 * @param f main JFrame to be used in the whole game 
	 * @param boardSize width or height of the board
	 * @param numberOfBombs bombs to be set in the matrix
	 */
	public MineBoard(JFrame f, int boardSize, int numberOfBombs) {
		super.requestFocus();
		super.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {}
			
			@Override
			public void mousePressed(MouseEvent e) {}
			
			@Override
			public void mouseExited(MouseEvent e) {}
			
			@Override
			public void mouseEntered(MouseEvent e) {}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				if(isInMatrix(e.getX(), e.getY(), 0, Y_MATRIX_OFFSET) && !loose && !win && e.getButton() == MouseEvent.BUTTON1)
					revealMine(e);
				else if(isInMatrix(e.getX(), e.getY(), 0, Y_MATRIX_OFFSET) && !loose && !win && e.getButton() == MouseEvent.BUTTON3)
					putFlag(e);
				else if(isInMatrix(e.getX(), e.getY(), 0, Y_MATRIX_OFFSET) && !loose && !win && e.getButton() == MouseEvent.BUTTON2)
					putQuestionMark(e);
				else if(mouseOverRestart(e.getX(), e.getY(), RESTART_X, RESTART_Y))
					reset();
			}
		});
		
		this.thread = new Thread(this);
		this.thread.start();
		
		this.f = f;
		final int INITIAL_WIDTH = MineSweeper.initialWidth;
		final int INITIAL_HEIGHT = MineSweeper.initialHeight;
		if(f.getWidth() != MineSweeper.defaultWidth && f.getHeight() != MineSweeper.defaultHeight) {
			f.setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
			final int defaultHeight = f.getHeight(); 
			this.f.setSize(this.MINE_SIZE * boardSize + RESTART_WIDTH / 2, this.CLOCK_HEIGHT + this.MINE_SIZE * boardSize + defaultHeight + (Y_MATRIX_OFFSET - CLOCK_HEIGHT));
		}
		
		this.board = new int[boardSize][boardSize];
		this.clock = new Image[CLOCK_SIZE];
		this.bombs = new Image[CLOCK_SIZE];
		this.restart = getImage(RESET);
		
		this.bombsLeft = numberOfBombs;
		this.timeLeft = 999;
		this.markedBombs = new ArrayList<Integer[]>();
		this.win = false;
		this.loose = false;
		
		this.RESTART_X = f.getWidth() / 2 - RESTART_WIDTH / 2;
		this.RESTART_Y = 0;
		
		this.boardSize = boardSize;
		this.numberOfBombs = numberOfBombs;
		
		initializeClockAndBombsCounter();
		updateBombsScoreBoard();
		createBoard(boardSize, numberOfBombs);
		setMineValues(boardSize); 
		setImagesOnBoard(boardSize);
	}
	
	/**
	 * This method is called when a click is detected and reveals the clicked square.
	 * It also changes the image from unrevealed to the revealed image with the correct number.
	 *
	 * @param  e  MouseEvent object to get the XY position of the mouse
	 */
	private void revealMine(MouseEvent e) {
		int coords[] = mouseCoordinatesToMatrixCoordinates(e.getX(), e.getY(), 0, Y_MATRIX_OFFSET);
		if(bufferedImagesEqual((BufferedImage)images[coords[1]*boardSize + coords[0] % boardSize], (BufferedImage)getImage(UNREVEALED))) {
			String path = "";
			if(board[coords[1]][coords[0]] != 9) 
				path = "/graphics/" + board[coords[1]][coords[0]] + ".png";
			else {
				path = EXPLODING_BOMB;
				loose = true;
				gameResult = getImage(DEAD);
				showAllBombs();
			}
				
			images[coords[1]*boardSize + coords[0] % boardSize] = getImage(path);
			repaint();
			
			if(board[coords[1]][coords[0]] == 0) {
				revealBlankNeigbors(coords[1], coords[0], boardSize, new ArrayList<Integer[]>());
			}
			repaint();
		}
	}
	
	/**
	 * This method is called when a click is detected over an unrevealed mine and puts a flag on it.
	 * <p>
	 * It also changes the image from unrevealed to the the flag image.
	 *
	 * @param  e  MouseEvent object to get the XY position of the mouse
	 */
	private void putFlag(MouseEvent e) {
		int coords[] = mouseCoordinatesToMatrixCoordinates(e.getX(), e.getY(), 0, Y_MATRIX_OFFSET);
		if(bufferedImagesEqual((BufferedImage)images[coords[1]*boardSize + coords[0] % boardSize], (BufferedImage)getImage(UNREVEALED)) && bombsLeft > 0) {
			images[coords[1]*boardSize + coords[0] % boardSize] = getImage(FLAG);
			bombsLeft--; 
			markedBombs.add(new Integer[] {coords[1], coords[0]});
		}else if(bufferedImagesEqual((BufferedImage)images[coords[1]*boardSize + coords[0] % boardSize], (BufferedImage)getImage(FLAG)) && bombsLeft < numberOfBombs) {
			images[coords[1]*boardSize + coords[0] % boardSize] = getImage(UNREVEALED);
			bombsLeft++;
			for(Integer i [] : markedBombs) {
				if(i[0] == coords[1] && i[1] == coords[0]) {
					markedBombs.remove(i);
					break;
				}
			}		
		}
		updateBombsScoreBoard();
		
		if(bombsLeft == 0) {
			if(checkIfWin()) {
				win = true;
				gameResult = getImage(WIN);
			}else {
				loose = true;
				gameResult = getImage(DEAD);
			}
		}
	}
	
	/**
	 * This method is called when a click is detected over an unrevealed mine and puts a question mark on it.
	 * <p>
	 * It also changes the image from unrevealed to the the question mark image.
	 *
	 * @param  e  MouseEvent object to get the XY position of the mouse
	 */
	private void putQuestionMark(MouseEvent e) {
		int coords[] = mouseCoordinatesToMatrixCoordinates(e.getX(), e.getY(), 0, Y_MATRIX_OFFSET);
		if(bufferedImagesEqual((BufferedImage)images[coords[1]*boardSize + coords[0] % boardSize], (BufferedImage)getImage(UNREVEALED)) ||
				bufferedImagesEqual((BufferedImage)images[coords[1]*boardSize + coords[0] % boardSize], (BufferedImage)getImage(FLAG))) {
			if(bufferedImagesEqual((BufferedImage)images[coords[1]*boardSize + coords[0] % boardSize], (BufferedImage)getImage(FLAG)))
				bombsLeft++;
			
			updateBombsScoreBoard();
			images[coords[1]*boardSize + coords[0] % boardSize] = getImage(QUESTION);
			repaint();
		}else if(bufferedImagesEqual((BufferedImage)images[coords[1]*boardSize + coords[0] % boardSize], (BufferedImage)getImage(QUESTION))) {
			images[coords[1]*boardSize + coords[0] % boardSize] = getImage(UNREVEALED);
			repaint();
		}
	}
	
	/**
	 * This method is called when a click is detected over the restart button and generates a new game.
	 */
	private void reset() {
		int size = 0;
		for(int i = 0; i < f.getJMenuBar().getMenu(0).getItemCount(); i++) {
			if(f.getJMenuBar().getMenu(0).getItem(i).isSelected()) {
				
				size = Integer.parseInt(f.getJMenuBar().getMenu(0).getItem(i).getText().substring(0, 2));
				break;
			}
		}
		
		int bombs = 0;
		for(int i = 0; i < f.getJMenuBar().getMenu(1).getItemCount(); i++) {
			if(f.getJMenuBar().getMenu(1).getItem(i).isSelected()) {
				bombs = Integer.parseInt(f.getJMenuBar().getMenu(1).getItem(i).getText());
				break;
			}
		}
		
		f.setContentPane(new MineBoard(f, size, bombs));
		f.invalidate();
        f.validate();
	}
	
	/**
	 * This method checks if mouse is over the restart button
	 *
	 * @param xMouse X position of the mouse
	 * @param yMouse Y position of the mouse
	 * @param xImage X position of the button on screen
	 * @param yImage Y position of the button on screen
	 * @return true if mouse is over, false otherwise
	 */
	private boolean mouseOverRestart(int xMouse, int yMouse, int xImage, int yImage) {
		if((xMouse >= xImage && xMouse <= xImage + RESTART_WIDTH) && (yMouse >= yImage && yMouse <= yImage + RESTART_HEIGHT))
			return true;
		return false;
	}
	
	/**
	 * When all flags are set, checks if the are correctly set. It checks if all flags are over 9 values
	 * @return true if all are 9 values, false otherwise
	 */
	private boolean checkIfWin() {
		for(Integer i[] : markedBombs)
			if(this.board[i[0]][i[1]] != 9)
				return false;
		return true; 
	}
	
	/**
	 * This method shows where are all the bombs when you loose
	 */
	private void showAllBombs() {
		for(int i = 0; i < this.board.length; i++)
			for(int j = 0; j < this.board[0].length; j++)
				if(this.board[i][j] == 9)
					this.images[i*boardSize + j % boardSize] = getImage(EXPLODING_BOMB);
	}
	
	/**
	 * Updates the time score board depending on the timeLeft variable
	 */
	private void updateTimeScoreBoard() {
		int hundred = this.timeLeft / 100;
		int ten = (this.timeLeft / 10) % 10;
		int unit = this.timeLeft % 10;
		
		this.clock[0] = getImage("/graphics/" + hundred + "_clock.png");
		this.clock[1] = getImage("/graphics/" + ten + "_clock.png");
		this.clock[2] = getImage("/graphics/" + unit + "_clock.png");
		
		repaint();
	}
	
	/**
	 * Updates the time score board depending on the bombsLeft variable
	 */
	private void updateBombsScoreBoard() {
		int hundred = this.bombsLeft / 100;
		int ten = (this.bombsLeft / 10) % 10;
		int unit = this.bombsLeft % 10;
		
		this.bombs[0] = getImage("/graphics/" + hundred + "_clock.png");
		this.bombs[1] = getImage("/graphics/" + ten + "_clock.png");
		this.bombs[2] = getImage("/graphics/" + unit + "_clock.png");
		
		repaint();
	}
	
	/**
	 * Checks if two images are equals comparing pixel per pixel
	 * @param img1 first image
	 * @param img2 second image
	 * @return true if they are the same, false otherwise
	 */
	private boolean bufferedImagesEqual(BufferedImage img1, BufferedImage img2) {
	    if (img1.getWidth() == img2.getWidth() && img1.getHeight() == img2.getHeight()) {
	        for (int x = 0; x < img1.getWidth(); x++) {
	            for (int y = 0; y < img1.getHeight(); y++) {
	                if (img1.getRGB(x, y) != img2.getRGB(x, y))
	                    return false;
	            }
	        }
	    } else {
	        return false; 
	    }
	    return true;
	}
	
	/**
	 * Sets initial images for both score boards
	 */
	private void initializeClockAndBombsCounter() {
		for(int i = 0; i < this.CLOCK_SIZE; i++)
			this.clock[i] = getImage("/graphics/9_clock.png");
		this.bombs[0] = getImage("/graphics/0_clock.png");
		this.bombs[1] = getImage("/graphics/1_clock.png");
		this.bombs[2] = getImage("/graphics/0_clock.png");
	}
	
	@Override
	public void paint(Graphics g) {
		super.paintComponent(g);
		for(int i = 0; i < this.images.length; i++) {
			g.drawImage(this.images[i], (i % this.boardSize) * MINE_SIZE, (i / this.boardSize) * MINE_SIZE + Y_MATRIX_OFFSET, MINE_SIZE, MINE_SIZE, null);
		}
		
		int offset = 0;
		for(int i = 0; i < this.CLOCK_SIZE; i++) {
			g.drawImage(this.bombs[i], 0 + offset, 0, CLOCK_WIDTH, CLOCK_HEIGHT, null);
			offset += CLOCK_WIDTH;
		}
		
		int separation = RESTART_X + RESTART_WIDTH / 2 + (RESTART_X - (3 * CLOCK_WIDTH));
		offset = 0;
		for(int i = 0; i < this.CLOCK_SIZE; i++) {
			g.drawImage(this.clock[i], 0 + offset + separation, 0, CLOCK_WIDTH, CLOCK_HEIGHT, null);
			offset += CLOCK_WIDTH;
		}
		
		g.drawImage(restart, RESTART_X, RESTART_Y, RESTART_WIDTH, RESTART_HEIGHT, null); 
		
		final int textOffset = -75;
		final int faceOffset = 5;
		if(this.loose) {
			g.setFont(new Font("Dialog", Font.BOLD, 35)); 
			g.setColor(Color.BLACK);
			g.drawString("YOU LOOSE", (f.getWidth() / 2) + textOffset, f.getHeight() / 2);
			
			g.drawImage(gameResult, RESTART_X, RESTART_Y + RESTART_HEIGHT + faceOffset, RESTART_WIDTH, RESTART_HEIGHT, null);
		}else if(win) {
			g.setFont(new Font("Dialog", Font.BOLD, 35)); 
			g.setColor(Color.BLACK);
			g.drawString("YOU WIN!!!", (f.getWidth() / 2) + textOffset, f.getHeight() / 2);
			
			g.drawImage(gameResult, RESTART_X, RESTART_Y + RESTART_HEIGHT + faceOffset, RESTART_WIDTH, RESTART_HEIGHT, null);
		}
	}
	
	/**
	 * This method changes the image from unrevealed to revealed for every neighbor of a mine that has blank number using recursivity
	 * @param x position in X axis of the mine that is being analyzed
	 * @param y position in Y axis of the mine that is being analyzed
	 * @param boardSize width or height of the board
	 * @param visited List of two dimensional array [x, y], to save the neighbors that has been visited
	 */
	public void revealBlankNeigbors(int x, int y, int boardSize, List<Integer[]> visited) {
		final int MAX_NEIGHBOURS = 3;
		if(bufferedImagesEqual((BufferedImage)images[x*this.boardSize + y % this.boardSize], (BufferedImage)getImage(FLAG))) {
			this.bombsLeft++;
			updateBombsScoreBoard();
		}
		if(this.board[x][y] == 0)
			images[x*this.boardSize + y % this.boardSize] = getImage("/graphics/0.png");
		for(int i = -1; i < MAX_NEIGHBOURS - 1; i++) {
			for (int j = -1; j < MAX_NEIGHBOURS - 1; j++) {
				if((y + i) >= 0 && (x + j) >= 0 && (x + j) <= boardSize - 1 && (y + i) <= boardSize - 1)
					if(this.board[x + j][y + i] == 0 && !containsMine(visited, new Integer[] {(x + j), (y + i)})) {
						visited.add(new Integer[]{(x + j), (y + i)});
						images[x*this.boardSize + y % this.boardSize] = getImage("/graphics/0.png");
						revealBlankNeigbors((x + j), (y + i), boardSize, visited);
					}
			}
		}
	}
	
	/**
	 * This method is used inside revealBlankNeighbors and checks if an array [x, y] is inside the visitd list
	 * @param visited List of already mines visited
	 * @param toVisit array [x, y] that needs to be checked before adding it to the visited List
	 * @return returns true if toVisit is in the List, false otherwise
	 */
	public boolean containsMine(List<Integer[]> visited, Integer[] toVisit) {
		for(Integer i [] : visited) {
			if(i[0] == toVisit[0] && i[1] == toVisit[1])
				return true;
		}
		return false;
	}
	
	/**
	 * Sets every mine image to the unrevealed image
	 * @param boardSize the width or height of the board
	 */
	private void setImagesOnBoard(int boardSize) {
		images = new Image[boardSize * boardSize];
		for(int i = 0; i < boardSize * boardSize; i++) {
			images[i] = getImage(UNREVEALED);
		}
	}
	
	/**
	 * Gives a value to every cell of the matrix, depending on the number of neighbor bombs that they have
	 * @param boardSize the width or height of the board
	 */
	private void setMineValues(int boardSize) {
		for(int i = 0; i < boardSize; i++) { 
			for (int j = 0; j < boardSize; j++) {
				if(this.board[i][j] != 9) {
					this.board[i][j] = getNeighboursBombs(i, j, boardSize);
				}
			}
		}
	}
	
	/**
	 * Calculates the number of bombs that a mine has as neighbors
	 * @param x position in X axis of the mine that needs to be analyzed
	 * @param y position in Y axis of the mine that needs to be analyzed
	 * @param boardSize width or height of the board
	 * @return the number of bomb neighbors
	 */
	private int getNeighboursBombs(int x, int y, int boardSize) {
		final int MAX_NEIGHBOURS = 3;
		int bombs = 0;
		for(int i = -1; i < MAX_NEIGHBOURS - 1; i++) { 
			for (int j = -1; j < MAX_NEIGHBOURS - 1; j++) {
				if((y + i) >= 0 && (x + j) >= 0 && (x + j) <= boardSize - 1 && (y + i) <= boardSize - 1)
					if(this.board[x + j][y + i] == 9)
						bombs++;
			}
		}
		return bombs;
	}
	
	/**
	 * Initializes the matrix with all 0 and sets randomly as many bombs as numberOfBombs
	 * @param boardSize width or height of the board
	 * @param numberOfBombs the quantity of bombs to be set on the matrix
	 */
	private void createBoard(int boardSize, int numberOfBombs) {
		int bombsSetted = 0;
		while(bombsSetted != numberOfBombs) {
			int i = new Random().nextInt(boardSize);
			int j = new Random().nextInt(boardSize);
			
			if(this.board[i][j] == 0) {
				this.board[i][j] = 9;
				bombsSetted++;
			}
		}
	}
	
	/**
	 * Determines if the mouse is over the board matrix or not
	 * @param x position in X axis of the mouse
	 * @param y position in Y axis of the mouse
	 * @param xOffset how much pixels the matrix is moved to the right in X axis
	 * @param yOffset how much pixels the matrix is moved down in Y axis
	 * @return true if the mouse is inside, false otherwise
	 */
	private boolean isInMatrix(int x, int y, int xOffset, int yOffset) {
		return ((x >= xOffset && x + xOffset <= this.board.length * MINE_SIZE) && (y >= yOffset && y <= this.board[0].length * MINE_SIZE + yOffset));
	}
	
	/**
	 * Transforms the screen mouse position to a matrix position
	 * @param x position in X axis of the mouse
	 * @param y position in Y axis of the mouse
	 * @param xOffset how much pixels the matrix is moved to the right in X axis
	 * @param yOffset how much pixels the matrix is moved down in Y axis
	 * @return an array [x, y] of the coordinates of the mouse in matrix coordinates, not screen coordinates
	 */
	private int[] mouseCoordinatesToMatrixCoordinates(int x, int y, int xOffset, int yOffset) {
		double dx = Math.floor((x - xOffset) / MINE_SIZE);
		double dy = Math.floor((y - yOffset) / MINE_SIZE);
		
		return new int[] {(int)dx, (int)dy};
	}
	
	/**
	 * Gets an image from the resources file
	 * @param imageUrl url where the image is saved in the project
	 * @return the image in the url if found
	 */
	private Image getImage(String imageUrl) {
		try {
		    return ImageIO.read(getClass().getResource(imageUrl)); 
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public void run() {
		while(!win && !loose) {
			long second = 1000;
			if(timeLeft <= 0)
				loose = true;
			try {
				Thread.sleep(second);
				timeLeft--;
				updateTimeScoreBoard();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
