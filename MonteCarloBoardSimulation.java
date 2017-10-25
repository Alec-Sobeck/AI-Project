
/**
 * Plays out a game to the end.
 * This makes use of mutual recursion because it is naturally suited to the tree structure. 
 */
public class MonteCarloBoardSimulation {

    // Use only 1 copy of the board over and over again to reduce strain on the garbage collector
    private static QuartoBoard copy = new QuartoBoard((String)null);
	private static int[] piecesBuffer = new int[32]; 
    
    /**
     * Take a Node and play out (a copy of) its board to the very end. Then return the resulting utility
     * 1 - win 
     * 0 - draw 
     * -1 - loss
     */
    public static int playGameToFinish(Node node) {
        // This is implemented with a bunch of mutually recursive calls.
        // We need to branch into the correct one at the start, then we're good to go
        if(node instanceof TerminatingNode) {
        	return ((TerminatingNode)node).value;
        } 

        // Copy the board. 
        copy.pieces = node.board.pieces;
		copy.spacesLeft = node.board.spacesLeft;
		for (int i = 0; i < QuartoBoard.numberOfCells; i++) 
            copy.board[i] = node.board.board[i]; 
        assert copy.equals(node.board);

        // Branch into the correct part of the simulation      
        if(node instanceof MaxChooseNode){
            return playGameMaxChoose();
        } else if (node instanceof MaxMoveNode){ 
            int lastPiece = ((MaxMoveNode)node).parentActionPieceID; 
            return playGameMaxMove(lastPiece);
        } else if (node instanceof MinChooseNode){
            return playGameMinChoose();
        } else if (node instanceof MinMoveNode){
            int lastPiece = ((MinMoveNode)node).parentActionPieceID; 
            return playGameMinMove(lastPiece);
        }
        throw new RuntimeException("Invalid play_Game");
    }
    
    private static int playGameMaxMove(int pieceID) {
        int result = copy.moveAndTestUtility(1, chooseWinningCell(pieceID), pieceID);
        if(result == Integer.MAX_VALUE) 
            return playGameMaxChoose();
        else 
            return result;
    }

    private static int playGameMaxChoose() {
        return playGameMinMove(chooseNonWinningPiece());
    }

    private static int playGameMinMove(int pieceID) {
        int result = copy.moveAndTestUtility(-1, chooseWinningCell(pieceID), pieceID);
        if(result == Integer.MAX_VALUE) 
            return playGameMinChoose();
        else 
            return result;
    }

    private static int playGameMinChoose() {
        return playGameMaxMove(chooseNonWinningPiece());
    }
    
    /**
     * Select a (random) piece that doesn't let the opponent win on the next move; if none available 
     * then just return a random piece.
     */
	public static int chooseNonWinningPiece(){
        int count = 0;
        for(int i = 0; i < QuartoBoard.numberOfPieces; i++){
            if(!copy.isPieceOnBoard(i) && !doesWinExist(i)) {
                piecesBuffer[count] = i;
                count++;
            }
        }

        if(count == 0){
            return copy.chooseRandomPieceNotPlayed();
        } else {
            return piecesBuffer[(int)(Math.random() * count)];
        }
	}

    /**
     * Checks if the pieceID can be played anywhere on the board, resulting in
     * a win from that particular move.
     */
    public static boolean doesWinExist(int pieceID){
        for(int i = 0; i < QuartoBoard.numberOfCells; i++) {
            if(copy.board[i] == -1 && copy.doesMoveWin(i, pieceID)){ 
                return true;
            }
        }
        return false;
    }

    /**
     * Choose winning cell if it exists; else play a random move.
     */
    public static int chooseWinningCell(int pieceID){
        for(int i = 0; i < QuartoBoard.numberOfCells; i++) {
            if(copy.board[i] == -1 && copy.doesMoveWin(i, pieceID)){ 
                return i;
            }
        }
        return copy.chooseRandomPositionNotPlayed();
    }
}
