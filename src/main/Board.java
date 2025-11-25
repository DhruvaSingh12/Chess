package main;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

public class Board {
	
	final int MAX_COL = 8;
	final int MAX_ROW = 8;
	public static final int SQUARE_SIZE = 85;
	public static final int HALF_SQUARE_SIZE = SQUARE_SIZE/2;
	
	public void draw(Graphics2D g2) {
		
		int c = 0;
		
		for(int row = 0; row < MAX_ROW; row++)
		{
			for(int col = 0; col < MAX_COL; col++) {
				
				if(c==0) {
					g2.setColor(new Color(255, 239, 168));
					c=1;
				}
				else {
					g2.setColor(new Color(25, 140, 25));
					c=0;
				}
				g2.fillRect(col*SQUARE_SIZE, row*SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
			}
			
			if(c==0) {
				c=1;
			}
			else c=0;
		}

		// Draw coordinates
		g2.setFont(new Font("Sans", Font.BOLD, 14));
		
		for(int row = 0; row < MAX_ROW; row++) {
			for(int col = 0; col < MAX_COL; col++) {
				
				if (col == 0) {
					// Draw Rank numbers (1-8) on the left edge
					if ((row + col) % 2 == 0) {
						 g2.setColor(new Color(25, 140, 25)); // Dark color on Light square
					} else {
						 g2.setColor(new Color(255, 239, 168)); // Light color on Dark square
					}
					g2.drawString("" + (8 - row), col * SQUARE_SIZE + 5, row * SQUARE_SIZE + 15);
				}
				
				if (row == 7) {
					// Draw File letters (a-h) on the bottom edge
					if ((row + col) % 2 == 0) {
						 g2.setColor(new Color(25, 140, 25));
					} else {
						 g2.setColor(new Color(255, 239, 168));
					}
					g2.drawString("" + (char)('a' + col), col * SQUARE_SIZE + SQUARE_SIZE - 12, (row + 1) * SQUARE_SIZE - 5);
				}
			}
		}
	}

}
