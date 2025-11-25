package main;

import java.util.ArrayList;
import piece.Piece;

public class GameHistory {
    GamePanel gp;
    ArrayList<String> moveHistory = new ArrayList<>();

    public GameHistory(GamePanel gp) {
        this.gp = gp;
    }

    public void addMove(String move) {
        moveHistory.add(move);
    }

    public ArrayList<String> getHistory() {
        return moveHistory;
    }

    public String getNotation(Piece piece, int col, int row, int preCol, int preRow, boolean capture, boolean castling) {
        if (castling) {
            return (col > preCol) ? "O-O" : "O-O-O";
        }

        String notation = "";
        
        // Piece type
        if (piece.type != Type.PAWN) {
            switch (piece.type) {
                case ROOK: notation += "R"; break;
                case KNIGHT: notation += "N"; break;
                case BISHOP: notation += "B"; break;
                case QUEEN: notation += "Q"; break;
                case KING: notation += "K"; break;
                default: break;
            }
            
            // Disambiguation
            boolean fileAmbiguous = false;
            boolean rankAmbiguous = false;
            boolean ambiguous = false;
            
            // Check other pieces of same type and color
            for (Piece p : GamePanel.pieces) {
                if (p != piece && p.type == piece.type && p.color == piece.color) {
                    if (p.canMove(col, row)) {
                        ambiguous = true;
                        if (p.col == preCol) fileAmbiguous = true;
                        if (p.row == preRow) rankAmbiguous = true;
                    }
                }
            }
            
            if (ambiguous) {
                if (fileAmbiguous && rankAmbiguous) {
                    notation += getFile(preCol) + getRank(preRow);
                } else if (fileAmbiguous) {
                    notation += getRank(preRow);
                } else {
                    notation += getFile(preCol);
                }
            }
        }

        // Capture
        if (capture) {
            if (piece.type == Type.PAWN) {
                notation += getFile(preCol);
            }
            notation += "x";
        }

        // Destination
        notation += getFile(col) + getRank(row);

        return notation;
    }

    private String getFile(int col) {
        return String.valueOf((char)('a' + col));
    }

    private String getRank(int row) {
        return String.valueOf(8 - row);
    }
}
