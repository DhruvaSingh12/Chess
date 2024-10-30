package main;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import piece.Bishop;
import piece.King;
import piece.Knight;
import piece.Pawn;
import piece.Piece;
import piece.Queen;
import piece.Rook;

public class GamePanel extends JPanel implements Runnable {
    public static final int WIDTH = 1100;
    public static final int HEIGHT = 800;
    final int FPS = 30;
    Thread gameThread;
    Board board = new Board();
    Mouse mouse = new Mouse();

    // Pieces
    public static ArrayList<Piece> pieces = new ArrayList<>();
    public static ArrayList<Piece> simPieces = new ArrayList<>();
    ArrayList<Piece> promoPieces = new ArrayList<>();
    Piece activeP, checkingP;
    public static Piece castlingP;

    // Color
    public static final int WHITE = 0;
    public static final int BLACK = 1;
    int currentColor = WHITE;

    // Booleans
    boolean canMove;
    boolean validSquare;
    boolean promotion;
    boolean gameover;
    boolean stalemate;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.black);
        addMouseMotionListener(mouse);
        addMouseListener(mouse);

        setPieces();
        copyPieces(pieces, simPieces);
    }

    public void launchGame() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void setPieces() {
        // white pieces
        pieces.add(new Pawn(WHITE, 0, 6));
        pieces.add(new Pawn(WHITE, 1, 6));
        pieces.add(new Pawn(WHITE, 2, 6));
        pieces.add(new Pawn(WHITE, 3, 6));
        pieces.add(new Pawn(WHITE, 4, 6));
        pieces.add(new Pawn(WHITE, 5, 6));
        pieces.add(new Pawn(WHITE, 6, 6));
        pieces.add(new Pawn(WHITE, 7, 6));

        pieces.add(new Rook(WHITE, 0, 7));
        pieces.add(new Rook(WHITE, 7, 7));
        pieces.add(new Knight(WHITE, 1, 7));
        pieces.add(new Knight(WHITE, 6, 7));
        pieces.add(new Bishop(WHITE, 2, 7));
        pieces.add(new Bishop(WHITE, 5, 7));
        pieces.add(new Queen(WHITE, 3, 7));
        pieces.add(new King(WHITE, 4, 7));

        // black pieces
        pieces.add(new Pawn(BLACK, 0, 1));
        pieces.add(new Pawn(BLACK, 1, 1));
        pieces.add(new Pawn(BLACK, 2, 1));
        pieces.add(new Pawn(BLACK, 3, 1));
        pieces.add(new Pawn(BLACK, 4, 1));
        pieces.add(new Pawn(BLACK, 5, 1));
        pieces.add(new Pawn(BLACK, 6, 1));
        pieces.add(new Pawn(BLACK, 7, 1));

        pieces.add(new Rook(BLACK, 0, 0));
        pieces.add(new Rook(BLACK, 7, 0));
        pieces.add(new Knight(BLACK, 1, 0));
        pieces.add(new Knight(BLACK, 6, 0));
        pieces.add(new Bishop(BLACK, 2, 0));
        pieces.add(new Bishop(BLACK, 5, 0));
        pieces.add(new Queen(BLACK, 3, 0));
        pieces.add(new King(BLACK, 4, 0));
    }

    private void copyPieces(ArrayList<Piece> source, ArrayList<Piece> target) {
        target.clear();
        target.addAll(source);
    }

    @Override
    public void run() {
        // Game loop
        double drawInterval = 1000000000 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while (gameThread != null) {
            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                update();
                // Ensure repaint runs on the Event Dispatch Thread
                SwingUtilities.invokeLater(() -> repaint());
                delta--;
            }
        }
    }

    private void update() {
        if (promotion) {
            promoting();
        } 
        
        else if(gameover==false && stalemate==false){
            // Mouse pressed
            if (mouse.pressed) {
                if (activeP == null) {
                    // Check if you can pick up a piece
                    for (Piece piece : simPieces) {
                        // Pick the piece if it is your colored piece
                        if (piece.color == currentColor &&
                                piece.col == mouse.x / Board.SQUARE_SIZE &&
                                piece.row == mouse.y / Board.SQUARE_SIZE) {
                            activeP = piece;
                        }
                    }
                } else {
                    simulate();
                }
            }

            // Mouse released
            if (!mouse.pressed) {
                if (activeP != null) {
                    if (validSquare) {
                        // Move confirmed, update piece list if a piece has been captured
                        copyPieces(simPieces, pieces);
                        activeP.updatePosition();

                        if (castlingP != null) {
                            castlingP.updatePosition();
                        }
                        
                        if(isKingInCheck() && isCheckmate()) {
                        	gameover=true;
                        }
                        
                        else if(isStalemate() && isKingInCheck()==false) {
                        	stalemate=true;
                        }
                        
                        else {
                        	if (canPromote()) {
                                promotion = true;
                            } 
                        	else {
                                changePlayer();
                            }
                        }


                    } 
                    
                    else {
                        copyPieces(pieces, simPieces);
                        activeP.resetPosition();
                    }

                    // Only reset activeP if not promoting
                    if (!promotion) {
                        activeP = null;
                    }
                }
            }
        }
    }

    private void simulate() {
        canMove = false;
        validSquare = false;

        // Reset piece list after every loop
        copyPieces(pieces, simPieces);

        // Reset castling piece's position
        if (castlingP != null) {
            castlingP.col = castlingP.preCol;
            castlingP.x = castlingP.getX(castlingP.col);
            castlingP = null;
        }

        // Update position of piece being held
        activeP.x = mouse.x - Board.HALF_SQUARE_SIZE;
        activeP.y = mouse.y - Board.HALF_SQUARE_SIZE;
        activeP.col = activeP.getCol(activeP.x);
        activeP.row = activeP.getRow(activeP.y);

        // Temporary list to store pieces that are hit and should be removed
        ArrayList<Piece> piecesToRemove = new ArrayList<>();

        // Check if piece is hovering over reachable square
        if (activeP.canMove(activeP.col, activeP.row)) {
            canMove = true;

            if (activeP.hittingP != null) {
                // Instead of removing directly, add to the temporary list
                piecesToRemove.add(activeP.hittingP);
            }

            checkCastling();
            
            if(isIllegal(activeP)==false && opponentCanCaptureKing()==false) {
            	validSquare = true;
            }
        }

        // Now safely remove all pieces after the iteration is complete
        simPieces.removeAll(piecesToRemove);
    }
    
    private boolean isIllegal(Piece king) {
    	
    	if(king.type==Type.KING) {
    		for(Piece piece:simPieces) {
    			if(piece!=king && piece.color!=king.color && piece.canMove(king.col, king.row)) {
    				return true;
    			}
    		}
    	}
    	return false;
    }

    private boolean opponentCanCaptureKing() {
    	
    	Piece king=getKing(false);
    	
    	for(Piece piece:simPieces) {
    		if(piece.color!=king.color && piece.canMove(king.col, king.row)) {
    			return true;
    		}
    	}
    	
    	return false;
    }
    
    private boolean isKingInCheck() {
    	
    	Piece king=getKing(true);
    	
    	if(activeP.canMove(king.col, king.row)) {
    		checkingP=activeP;
    		return true;
    	}
    	else {
    		checkingP=null;
    	}
    	return false;
    }
    
    private Piece getKing(boolean opponent) {
    	Piece king=null;
    	
    	for(Piece piece:simPieces) {
    		if(opponent) {
    			if(piece.type==Type.KING && piece.color!=currentColor) {
    				king=piece;
    			}
    		}
    		else {
    			if(piece.type==Type.KING && piece.color==currentColor) {
    				king=piece;
    			}
    		}
    	}
    	return king;
    }
    
    private boolean isCheckmate() {
        Piece king = getKing(true); // Assuming true represents the active color's king

        // First, check if the king can move to any valid position
        if (kingCanMove(king)) {
            return false; // If the king can move, it's not checkmate
        }

        // Check if any piece can capture the attacking piece
        if (canCaptureAttackingPiece(king)) {
            return false; // If the attacker can be captured, it's not checkmate
        }

        // Check if the attack on the king can be blocked
        if (checkingP != null) {
            int colDiff = Math.abs(checkingP.col - king.col);
            int rowDiff = Math.abs(checkingP.row - king.row);

            // Check for vertical, horizontal, and diagonal attacks
            if (colDiff == 0) {
                // Vertical attack (same column, different rows)
                if (canBlockAttack(king, checkingP.col, Math.min(checkingP.row, king.row), Math.max(checkingP.row, king.row), true)) {
                    return false; // If the attack can be blocked vertically, it's not checkmate
                }
            } else if (rowDiff == 0) {
                // Horizontal attack (same row, different columns)
                if (canBlockAttack(king, Math.min(checkingP.col, king.col), Math.max(checkingP.col, king.col), checkingP.row, false)) {
                    return false; // If the attack can be blocked horizontally, it's not checkmate
                }
            } else if (colDiff == rowDiff) {
                // Diagonal attack (equal column and row difference)
                if (canBlockDiagonalAttack(king, checkingP, colDiff)) {
                    return false; // If the attack can be blocked diagonally, it's not checkmate
                }
            }
        }

        // If no valid moves for the king, no piece can block or capture, it's checkmate
        return true;
    }

    
    private boolean canCaptureAttackingPiece(Piece king) {
        if (checkingP == null) {
            return false; // No attacking piece, hence no need to capture
        }

        for (Piece piece : simPieces) {
            // Ensure the piece is of the same color as the king and isn't the king itself
            if (piece != king && piece.color == king.color) {
                // Check if the piece can move to the attacking piece's position and potentially capture it
                if (piece.canMove(checkingP.col, checkingP.row)) {
                    
                    // Simulate the capture: temporarily remove the attacking piece
                    Piece capturedPiece = checkingP;
                    simPieces.remove(capturedPiece);

                    // Simulate the move: update the board state
                    int originalCol = piece.col;
                    int originalRow = piece.row;
                    piece.canMove(checkingP.col, checkingP.row); // Simulate the move

                    // Check if the king is still in check after the move
                    boolean stillInCheck = isIllegal(king);

                    // Restore the original state: move piece back and add the captured piece
                    piece.canMove(originalCol, originalRow); // Restore the piece's original position
                    simPieces.add(capturedPiece); // Add back the removed piece

                    // If the king is no longer in check after the capture, return true
                    if (!stillInCheck) {
                        return true;
                    }
                }
            }
        }

        return false; // No valid capture move to resolve check
    }




    
    private boolean canBlockAttack(Piece king, int fixed, int start, int end, boolean isVertical) {
        if (start < end) {
            for (int i = start + 1; i < end; i++) {
                for (Piece piece : simPieces) {
                    if (piece != king && piece.color != currentColor) {
                        if (isVertical && piece.canMove(fixed, i)) {
                            return true;
                        } else if (!isVertical && piece.canMove(i, fixed)) {
                            return true;
                        }
                    }
                }
            }
        } else {
            for (int i = start - 1; i > end; i--) {
                for (Piece piece : simPieces) {
                    if (piece != king && piece.color != currentColor) {
                        if (isVertical && piece.canMove(fixed, i)) {
                            return true;
                        } else if (!isVertical && piece.canMove(i, fixed)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean canBlockDiagonalAttack(Piece king, Piece checkingP, int colDiff) {
        int colDirection = (checkingP.col < king.col) ? 1 : -1;
        int rowDirection = (checkingP.row < king.row) ? 1 : -1;

        for (int i = 1; i < colDiff; i++) {
            int col = checkingP.col + i * colDirection;
            int row = checkingP.row + i * rowDirection;
            
            for (Piece piece : simPieces) {
                if (piece != king && piece.color != currentColor && piece.canMove(col, row)) {
                    return true;
                }
            }
        }
        return false;
    }


    
    private boolean kingCanMove(Piece king) {
    	
    	//simulate if there is any square where king can move
    	if(isValidMove(king, -1, -1)) return true;
    	if(isValidMove(king, 0, -1)) return true;
    	if(isValidMove(king, 1, -1)) return true;
    	if(isValidMove(king, -1, 0)) return true;
    	if(isValidMove(king, 1, 0)) return true;
    	if(isValidMove(king, -1, 1)) return true;
    	if(isValidMove(king, 0, 1)) return true;
    	if(isValidMove(king, 1, 1)) return true;
    	
    	return false;
    }
    
    private boolean isValidMove(Piece king, int colPlus, int rowPlus) {

        boolean isValidMove = false;

        // Update king's position temporarily
        king.col += colPlus;
        king.row += rowPlus;

        // Check if the king can move to the new position
        if (king.canMove(king.col, king.row)) {
            // Only attempt to remove hittingP if it is not null
            if (king.hittingP != null) {
                simPieces.remove(king.hittingP.getIndex());
            }
        }
     // Check if the move is not illegal
        if (!isIllegal(king)) {
            isValidMove = true;
        }

        // Reset king's position and restore removed pieces
        king.resetPosition();
        copyPieces(pieces, simPieces);

        return isValidMove;
    }
    
    
    private boolean isStalemate() {
    	
    	int count=0;
    	//count no.of pieces
    	for(Piece piece: simPieces) {
    		if(piece.color!=currentColor) {
    			count++;
    		}
    	}
    	
    	if(count==1) {
    		if(kingCanMove(getKing(true))==false) {
    			return true;
    		}
    	}
    	return false;
    }
    
    
    private void checkCastling() {
        if (castlingP != null) {
            if (castlingP.col == 0) {
                castlingP.col += 3;
            } else if (castlingP.col == 7) {
                castlingP.col -= 2;
            }
            castlingP.x = castlingP.getX(castlingP.col);
        }
    }

    private void changePlayer() {
        if (currentColor == WHITE) {
            currentColor = BLACK;

            // Reset black's two-stepped status
            for (Piece piece : pieces) {
                if (piece.color == BLACK) {
                    piece.twoStepped = false;
                }
            }

        } else {
            currentColor = WHITE;

            // Reset white's two-stepped status
            for (Piece piece : pieces) {
                if (piece.color == WHITE) {
                    piece.twoStepped = false;
                }
            }
        }
        activeP = null;
    }

    private boolean canPromote() {
        if (activeP.type == Type.PAWN) {
            if (currentColor == WHITE && activeP.row == 0 || currentColor == BLACK && activeP.row == 7) {
                promoPieces.clear();
                promoPieces.add(new Rook(currentColor, 9, 2));
                promoPieces.add(new Knight(currentColor, 9, 3));
                promoPieces.add(new Bishop(currentColor, 9, 4));
                promoPieces.add(new Queen(currentColor, 9, 5));
                return true;
            }
        }
        return false;
    }

    public void promoting() {
        if (mouse.pressed) {
            for (Piece piece : promoPieces) {
                if (piece.col == mouse.x / Board.SQUARE_SIZE && piece.row == mouse.y / Board.SQUARE_SIZE) {
                    switch (piece.type) {
                        case ROOK:
                            simPieces.add(new Rook(currentColor, activeP.col, activeP.row));
                            break;
                        case KNIGHT:
                            simPieces.add(new Knight(currentColor, activeP.col, activeP.row));
                            break;
                        case BISHOP:
                            simPieces.add(new Bishop(currentColor, activeP.col, activeP.row));
                            break;
                        case QUEEN:
                            simPieces.add(new Queen(currentColor, activeP.col, activeP.row));
                            break;
                        default:
                            break;
                    }

                    simPieces.remove(activeP.getIndex());  // Remove the pawn being promoted
                    copyPieces(simPieces, pieces);
                    activeP = null;
                    promotion = false;
                    changePlayer();
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Draw board
        board.draw(g2);

        // Draw pieces
        for (Piece p : simPieces) {
            p.draw(g2);
        }
        
        if(stalemate) {
        	g2.setFont(new Font("Arial", Font.PLAIN, 90));
        	g2.setColor(Color.gray);
        	g2.drawString("Stalemate!", 200, 420);
        }

        if (activeP != null) {
            if (canMove) {
            	
            	if(isIllegal(activeP) || opponentCanCaptureKing()) {
            		g2.setColor(Color.yellow);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                    g2.fillRect(activeP.col * Board.SQUARE_SIZE, activeP.row * Board.SQUARE_SIZE, Board.SQUARE_SIZE, Board.SQUARE_SIZE);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            	}
            	
            	else {
            		g2.setColor(Color.gray);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                    g2.fillRect(activeP.col * Board.SQUARE_SIZE, activeP.row * Board.SQUARE_SIZE, Board.SQUARE_SIZE, Board.SQUARE_SIZE);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            	}
            }

            // Draw active piece at the end so it's not hidden by the board
            activeP.draw(g2);
        }
        
        //status messages
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(new Font("Book Antiqua", Font.PLAIN, 40));
        g2.setColor(Color.white);
        
        if(promotion) {
        	g2.drawString("Promote to:", 840, 150);
        	for(Piece piece:promoPieces) {
        		g2.drawImage(piece.image, piece.getX(piece.col), piece.getY(piece.row), Board.SQUARE_SIZE, Board.SQUARE_SIZE, null);
        	}
        }
        
        else {
        	if(currentColor==WHITE) {
            	g2.drawString("White's turn.", 840, 550);
            	if(checkingP!=null && checkingP.color==BLACK && !gameover) {
            		g2.setColor(Color.red);
            		g2.drawString("The King", 840, 650);
            		g2.drawString("is in check!", 840, 700);
            	}
            	else if(gameover) {
                	String s="";
                	if(currentColor==WHITE) {
                		s="White Wins by Checkmate";
                	}
                	else {
                		s="Black Wins by Checkmate";
                	}
                	g2.setFont(new Font("Arial", Font.PLAIN, 90));
                	g2.setColor(Color.green);
                	g2.drawString(s, 200, 420);
                }
            }
            else {
            	g2.drawString("Black's turn.", 840, 250);
            	if(checkingP!=null && checkingP.color==WHITE && !gameover) {
            		g2.setColor(Color.red);
            		g2.drawString("The King", 840, 100);
            		g2.drawString("is in check!", 840, 150);
            	}
            	else if(gameover) {
                	String s="";
                	if(currentColor==WHITE) {
                		s="White Wins by Checkmate";
                	}
                	else {
                		s="Black Wins by Checkmate";
                	}
                	g2.setFont(new Font("Arial", Font.PLAIN, 90));
                	g2.setColor(Color.green);
                	g2.drawString(s, 200, 420);
                }
            		
            	}
            }
        }
        
    }