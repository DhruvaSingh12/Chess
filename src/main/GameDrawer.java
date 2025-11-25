package main;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;

import piece.Piece;

public class GameDrawer {
    GamePanel gp;

    public GameDrawer(GamePanel gp) {
        this.gp = gp;
    }

    public void draw(Graphics2D g2) {
        // Draw board
        gp.board.draw(g2);

        // Draw pieces
        for (Piece p : GamePanel.simPieces) {
            p.draw(g2);
        }
        
        drawCapturedPieces(g2);

        if (gp.stalemate) {
            g2.setFont(new Font("Arial", Font.PLAIN, 90));
            g2.setColor(Color.gray);
            g2.drawString("Stalemate!", 150, 340);
        }

        if (gp.activeP != null) {
            if (gp.canMove) {

                if (gp.isIllegal(gp.activeP) || gp.opponentCanCaptureKing()) {
                    g2.setColor(Color.yellow);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                    g2.fillRect(gp.activeP.col * Board.SQUARE_SIZE, gp.activeP.row * Board.SQUARE_SIZE, Board.SQUARE_SIZE,
                            Board.SQUARE_SIZE);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                }

                else {
                    g2.setColor(Color.gray);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                    g2.fillRect(gp.activeP.col * Board.SQUARE_SIZE, gp.activeP.row * Board.SQUARE_SIZE, Board.SQUARE_SIZE,
                            Board.SQUARE_SIZE);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                }
            }

            // Draw active piece at the end so it's not hidden by the board
            gp.activeP.draw(g2);
        }

        // status messages
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(new Font("Book Antiqua", Font.PLAIN, 25));
        g2.setColor(Color.white);

        if (gp.promotion) {
            g2.drawString("Promote to:", 700, 150);
            for (Piece piece : gp.promoPieces) {
                g2.drawImage(piece.image, piece.getX(piece.col), piece.getY(piece.row), Board.SQUARE_SIZE,
                        Board.SQUARE_SIZE, null);
            }
        }

        else {
            if (gp.currentColor == GamePanel.WHITE) {
                g2.drawString("White's turn.", 700, 450);
                if (gp.checkingP != null && gp.checkingP.color == GamePanel.BLACK && !gp.gameover) {
                    g2.setColor(Color.red);
                    g2.drawString("Check!", 700, 530);
                } else if (gp.gameover) {
                    String s = "";
                    if (gp.currentColor == GamePanel.WHITE) {
                        s = "Black Wins by Checkmate";
                    } else {
                        s = "White Wins by Checkmate";
                    }
                    g2.setFont(new Font("Arial", Font.PLAIN, 60));
                    g2.setColor(Color.green);
                    g2.drawString(s, 50, 340);
                }
            } else {
                g2.drawString("Black's turn.", 700, 200);
                if (gp.checkingP != null && gp.checkingP.color == GamePanel.WHITE && !gp.gameover) {
                    g2.setColor(Color.red);
                    g2.drawString("Check!", 700, 140);
                } else if (gp.gameover) {
                    String s = "";
                    if (gp.currentColor == GamePanel.WHITE) {
                        s = "Black Wins by Checkmate";
                    } else {
                        s = "White Wins by Checkmate";
                    }
                    g2.setFont(new Font("Arial", Font.PLAIN, 60));
                    g2.setColor(Color.green);
                    g2.drawString(s, 50, 340);
                }
            }
        }
    }

    private void drawCapturedPieces(Graphics2D g2) {
        int[] whitePieceCounts = new int[5]; // Captured White pieces (by Black)
        int[] blackPieceCounts = new int[5]; // Captured Black pieces (by White)
        Piece[] whiteImages = new Piece[5];
        Piece[] blackImages = new Piece[5];

        for (Piece p : gp.capturedPieces) {
            int index = -1;
            switch (p.type) {
                case PAWN: index = 0; break;
                case KNIGHT: index = 1; break;
                case BISHOP: index = 2; break;
                case ROOK: index = 3; break;
                case QUEEN: index = 4; break;
                default: break;
            }
            
            if (index != -1) {
                if (p.color == GamePanel.WHITE) {
                    whitePieceCounts[index]++;
                    if (whiteImages[index] == null) whiteImages[index] = p;
                } else {
                    blackPieceCounts[index]++;
                    if (blackImages[index] == null) blackImages[index] = p;
                }
            }
        }

        // Draw captured White pieces (on Black's side - Top Right)
        int x = 700;
        int y = 30;
        int size = 35;
        int spacing = 45;
        
        g2.setFont(new Font("Arial", Font.PLAIN, 15));
        g2.setColor(Color.white);

        for (int i = 0; i < 5; i++) {
            if (whitePieceCounts[i] > 0) {
                g2.drawImage(whiteImages[i].image, x, y, size, size, null);
                g2.drawString("x" + whitePieceCounts[i], x + size/2 + 5, y + size + 15);
                x += spacing;
            }
        }

        // Draw captured Black pieces (on White's side - Bottom Right)
        x = 700;
        y = 600;
        
        for (int i = 0; i < 5; i++) {
            if (blackPieceCounts[i] > 0) {
                g2.drawImage(blackImages[i].image, x, y, size, size, null);
                g2.drawString("x" + blackPieceCounts[i], x + size/2 + 5, y + size + 15);
                x += spacing;
            }
        }
        
        // Draw Move History
        drawMoveHistory(g2);
    }
    
    private void drawMoveHistory(Graphics2D g2) {
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        int startX = 950; // New column start
        int startY = 30;  // Start from top
        int lineHeight = 20;
        int currentX = startX;
        int currentY = startY;
        int maxWidth = 280;
        
        ArrayList<String> moveHistory = gp.gameHistory.getHistory();
        
        for (int i = 0; i < moveHistory.size(); i += 2) {
            String moveNum = (i / 2 + 1) + ". ";
            String whiteMove = moveHistory.get(i);
            String blackMove = (i + 1 < moveHistory.size()) ? moveHistory.get(i + 1) : "";
            
            String text = moveNum + whiteMove + " " + blackMove + " ";
            
            // Check if text fits in current line
            int textWidth = g2.getFontMetrics().stringWidth(text);
            if (currentX + textWidth > startX + maxWidth) {
                currentX = startX;
                currentY += lineHeight;
            }
            
            // Draw Move Number
            g2.setColor(Color.GRAY);
            g2.drawString(moveNum, currentX, currentY);
            currentX += g2.getFontMetrics().stringWidth(moveNum);
            
            // Draw White Move
            g2.setColor(Color.WHITE); // Or yellowish
            g2.drawString(whiteMove, currentX, currentY);
            currentX += g2.getFontMetrics().stringWidth(whiteMove + " ");
            
            // Draw Black Move
            if (!blackMove.isEmpty()) {
                g2.setColor(Color.WHITE);
                g2.drawString(blackMove, currentX, currentY);
                currentX += g2.getFontMetrics().stringWidth(blackMove + " ");
            }
        }
    }
}
