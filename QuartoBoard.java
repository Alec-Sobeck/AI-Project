import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

/**
 * An explaination of how checks for winning/losing/drawing works:
 * (1) A draw occurs if spacesLeft = 0
 * (2) A win (for the player that's moving) occurs if they complete a row/column/diagonal.
 *     With our new structure, this is relatively easily to check. I will explain the process 
 * 	   to check the rows, although it is essentially the same for diagonals and columns. 
 *			> Verify that the entire row has pieces present. 
 *			> Bitwise AND the entire row together, storing that in a variable `andResult'
 *			> Bitwise OR the entire row together, storing that in a separate variable `orResult'
 *			> if orResult is not 11111, then all the pieces share a `0' in common - a win. 
 * 			> If andResult is not 0, then all pieces share a `1' in common - also a win. 
 */
public final class QuartoBoard {
    public static final int numberOfRows = 5;
    public static final int numberOfColumns = 5;
    public static final int numberOfCells = numberOfRows * numberOfColumns;
    public static final int numberOfPieces = 32;
	public static final int numberOfAttempts = 20;
	public static final int MINIMAX_DEPTH = 6;
	/**
	 * The board consists of a series of 25 1-byte integers, representing pieces on the board at the moment. 
	 * If a piece is in play at index i, the value at that index board[i] is in [0, 31], as our pieces are 0 to 31. 
	 * If no piece is present, board[i] = -1. 
	 * Note: cell_number = row * numberOfColumns + column 
	 */
	public byte[] board; 
	/**
	 * I condensed the pieces array down into a single 32 bit integer, which only works because we have exactly 32 pieces. 
	 * If the ith bit in pieces is set to 1 then that indicates that that piece hasn't been played yet 
	 * (and likewise a 0 indicates it's been played). We can extract this with a couple of bitwise operators,
	 * and set a particular bit using XOR, so the structure actually works extremely well. 
	 *
	 * For example, if piece 0 (`00000') hasn't been played, the rightmost bit in pieces is set to 1. 
	 * Therefore, the operation (pieces & 1) == 1. However, if the piece 0 was on the board, then 
	 * (pieces & 1) == 0, as the bit is now toggled to 0. This achieves the same thing as the pieces 
	 * array before, it's just a lot more compact. 
	 */
	public int pieces; 
	/**
	 *  Number of free cells on the board. Starts at 25. Reduced by 1 for every piece played.
     */
	public int spacesLeft;

	public QuartoBoard(String stateFileName) {
        board = new byte[numberOfCells];
        Arrays.fill(board, (byte)-1); 
		pieces = ~0; 
		spacesLeft = numberOfCells;
		assert pieces == -1;

		if (stateFileName != null) 
			setBoardFromFile(stateFileName, numberOfRows, numberOfColumns);
	}
	
	public QuartoBoard(QuartoBoard quartoBoard) {
		this.board = new byte[numberOfCells];
        this.pieces = quartoBoard.pieces;
		this.spacesLeft = quartoBoard.spacesLeft;
		for (int i = 0; i < numberOfCells; i++) 
			this.board[i] = quartoBoard.board[i]; 
	}

	public boolean equals(QuartoBoard other){
		for(int i = 0; i < board.length; i++){
			if(other.board[i] != this.board[i])
				return false;
		}
		return other.pieces == this.pieces && other.spacesLeft == this.spacesLeft; 
	}

	// Number of cells that are empty 
	public int numberOfMovesRemaining(){
		return spacesLeft;
	}

	// Number of unplayed pieces. Conviently, this is (cells_free + 7)
	public int numberOfPiecesRemaining(){
		return 7 + spacesLeft;
	}
	
	/**
	 * Play a piece on the board, and update other information that's relevant
	 */
    public void boardSet(int row, int column, int pieceID){
		assert row >= 0 && row < numberOfRows && column >= 0 && column < numberOfColumns && pieceID >= 0 && 
			pieceID < numberOfPieces && board[row * numberOfColumns + column] == -1 && !isPieceOnBoard(pieceID);

		board[row * numberOfColumns + column] = (byte)pieceID;
		spacesLeft--;
		pieces = (pieces ^ (1<<pieceID)); 
	}

	/**
	 * Play a piece on the board, and update other information that's relevant
	 */
	public void boardSet(int cell, int pieceID){
		assert cell >= 0 && cell < numberOfCells && pieceID >= 0 && pieceID < numberOfPieces && board[cell] == -1 && !isPieceOnBoard(pieceID);
		
		board[cell] = (byte)pieceID;
		spacesLeft--;
		pieces = (pieces ^ (1<<pieceID)); 
	}

	/**
	 * Convert a cell integer between 0-24 --> row,column string
	 */
	public String getCoordinates(int cell) {
		return (cell / numberOfColumns) + "," + (cell % numberOfColumns);
	}

	/**
	 * Get the piece in a specified board cell.
	 */
    public int boardGet(int row, int column){
		assert row >= 0 && row < numberOfRows && column >= 0 && column < numberOfColumns;
		
		return board[row * numberOfColumns + column];
    }

	/**
	 * Check if a piece is on the board.
	 */
	public boolean isPieceOnBoard(int pieceID) { 
        assert pieceID >= 0 && pieceID < numberOfPieces;

		return (pieces & (1 << pieceID)) == 0;
	}	
	
	/**
	 * Plays the indicates move. Then returns a value based on the consequences of that move:
	 * INTMAX - game not won/lost/drawn
	 * 0  - game is a draw
	 * 1  - we win
	 * -1 - opponent wins.
	 */
	public int moveAndTestUtility(int modifier, int cell, int pieceID) {
		if(spacesLeft == 0)
			return 0;
	
		boardSet(cell, pieceID);
		
		int row = cell / numberOfColumns;
		int column = cell % numberOfColumns;
		if(checkRow(row) || checkColumn(column) || checkDiagonals()) // This move wins, somehow
			return modifier;
		else if(spacesLeft == 0) 
			return 0;
		else	
			return Integer.MAX_VALUE; 
	}

	/**
	 * Check if playing the specified piece at the specified cell results in a win, or not. Does not 
	 * affect state of the board. 
	 */
	public boolean doesMoveWin(int cell, int pieceID) {
		assert spacesLeft != 0;
		assert cell >= 0 && cell < numberOfCells && pieceID >= 0 && pieceID < numberOfPieces && board[cell] == -1 && !isPieceOnBoard(pieceID);
		
		board[cell] = (byte)pieceID;
		
		int row = cell / numberOfColumns;
		int column = cell % numberOfColumns;

		boolean result = checkRow(row) || checkColumn(column) || checkDiagonals();

		board[cell] = (byte)-1;
	
		return result;

	}

	public boolean canApplyAlphaBeta() {
		return spacesLeft <= MINIMAX_DEPTH;
	}

	public boolean checkRow(int row) {
		
		// Check if row is full
		if (board[row * numberOfColumns + 0] == -1 || 
			board[row * numberOfColumns + 1] == -1 || 
			board[row * numberOfColumns + 2] == -1 || 
			board[row * numberOfColumns + 3] == -1 || 
			board[row * numberOfColumns + 4] == -1)
			return false;

		// Check for a win
		int rowAnd = board[row * numberOfColumns + 0] & 
						board[row * numberOfColumns + 1] & 
						board[row * numberOfColumns + 2] & 
						board[row * numberOfColumns + 3] & 
						board[row * numberOfColumns + 4];
		int rowOr =  board[row * numberOfColumns + 0] |
						board[row * numberOfColumns + 1] | 
						board[row * numberOfColumns + 2] | 
						board[row * numberOfColumns + 3] | 
						board[row * numberOfColumns + 4];
		
		return rowAnd != 0 || rowOr != 0x001F;
	}

	public boolean checkColumn(int column) {

		// Check if column is full
		if (board[0 * numberOfColumns + column] == -1 || 
			board[1 * numberOfColumns + column] == -1 ||
			board[2 * numberOfColumns + column] == -1 || 
			board[3 * numberOfColumns + column] == -1 || 
			board[4 * numberOfColumns + column] == -1) 
			return false;

		// Check for a win
        int rowAnd =    board[0 * numberOfColumns + column] & 
                        board[1 * numberOfColumns + column] & 
                        board[2 * numberOfColumns + column] & 
                        board[3 * numberOfColumns + column] & 
                        board[4 * numberOfColumns + column];
        int rowOr =     board[0 * numberOfColumns + column] | 
                        board[1 * numberOfColumns + column] | 
                        board[2 * numberOfColumns + column] | 
                        board[3 * numberOfColumns + column] | 
                        board[4 * numberOfColumns + column];

        return rowAnd != 0 || rowOr != 0x001F;
	}

	public boolean checkFirstDiagonal() {
		// Check if top-left -> bottom-right diagonal is full.
		if (board[0 * numberOfColumns + 0] == -1 ||  
			board[1 * numberOfColumns + 1] == -1 ||  
			board[2 * numberOfColumns + 2] == -1 ||  
			board[3 * numberOfColumns + 3] == -1 ||  
			board[4 * numberOfColumns + 4] == -1)
			return false;

		// Check if there's a win along it
		int rowAnd =    board[0 * numberOfColumns + 0] & 
						board[1 * numberOfColumns + 1] & 
						board[2 * numberOfColumns + 2] & 
						board[3 * numberOfColumns + 3] & 
						board[4 * numberOfColumns + 4];
		int rowOr =     board[0 * numberOfColumns + 0] | 
						board[1 * numberOfColumns + 1] | 
						board[2 * numberOfColumns + 2] | 
						board[3 * numberOfColumns + 3] | 
						board[4 * numberOfColumns + 4];
		return rowAnd != 0 || rowOr != 0x001F;
	}

	public boolean checkSecondDiagonal(){
		// Check if the top-right -> bottom-left diagonals are full.
        if (board[0 * numberOfColumns + 4] == -1 ||  
			board[1 * numberOfColumns + 3] == -1 || 
			board[2 * numberOfColumns + 2] == -1 || 
			board[3 * numberOfColumns + 1] == -1 ||  
			board[4 * numberOfColumns + 0] == -1 )
			return false;

		// Check if there's a win along the top-right -> bottom-left diagonal
		int rowAnd =    board[0 * numberOfColumns + 4] & 
						board[1 * numberOfColumns + 3] & 
						board[2 * numberOfColumns + 2] & 
						board[3 * numberOfColumns + 1] & 
						board[4 * numberOfColumns + 0];
		int rowOr =     board[0 * numberOfColumns + 4] | 
						board[1 * numberOfColumns + 3] | 
						board[2 * numberOfColumns + 2] | 
						board[3 * numberOfColumns + 1] | 
						board[4 * numberOfColumns + 0];

		return rowAnd != 0 || rowOr != 0x001F;	
	}

	public boolean checkDiagonals() {
		return checkFirstDiagonal() || checkSecondDiagonal();
	}

	public int chooseRandomPieceNotPlayed() {
		for(int i = 0; i < numberOfAttempts; i++){
			int pieceId = (int)(Math.random() * numberOfPieces);
			if(!isPieceOnBoard(pieceId)) 
				return pieceId;
		}

		// Just choose the next free piece. 
		for(int i = 0; i < numberOfPieces; i++) 
            if((pieces & (1<<i)) != 0) 
                return i;

		return -1;
	}

	public int chooseRandomPositionNotPlayed() {
		for(int i = 0; i < numberOfAttempts; i++){
			int move = (int)(Math.random() * numberOfCells);
			if(board[move] == -1) 
				return move;
		}

		// Just choose the next free cell. 
		for(int i = 0; i < numberOfCells; i++) 
			if(board[i] == -1) 
				return i;

		return -1;
	}

	// ------------------------------------------------------------------------------
	// Extra utility functions:
	//
	public void printBoardState() {
		System.out.println("-----------------------------------");
		for(int row = 0; row < numberOfRows; row++) {
			for(int column = 0; column < numberOfColumns; column++) {
				if(boardGet(row, column) >= 0) {
					System.out.print(QuartoPiece.binaryStringRepresentation(boardGet(row, column)) + "  ");
				} else {
					System.out.print("null   ");
				}
			}
			System.out.print("\n");
		}
		System.out.println("-----------------------------------");
	}

	public void printPiecesLeft() {
		for(int i = 0 ; i < numberOfPieces; i++) {
			if(!isPieceOnBoard(i)) {
				System.out.println("FREE: " + QuartoPiece.binaryStringRepresentation(i));
			}
		}
	}

	public void setBoardFromFile(String stateFileName, int numberOfRows, int numberOfColumns) {
		try (BufferedReader br = new BufferedReader(new FileReader(stateFileName))) {
			String line;
			int row = 0;
			while ((line = br.readLine()) != null) {
				if(line.trim().equals("")) // Empty line - skip it
					continue;
				String[] splitted = line.split("\\s+");
				if (splitted.length != numberOfRows) {
					throw new Error("malformed .quarto file");
				}

				for (int col = 0; col < splitted.length; col++) {
					if (!splitted[col].equals("null")) {
						int pieceID = Integer.parseInt(splitted[col], 2);
						boardSet(row, col, pieceID);
					}
				}
				row++;
			}

			// Check for wins:
			// Rows
			for(int i = 0; i < numberOfRows; i++) {
				if (this.checkRow(i)) {
					System.out.println("Error: Win via row: " + (i));
					throw new Error(".quarto file is not formatted correctly");
				}
			}
			//Columns
			for(int i = 0; i < numberOfColumns; i++) {
				if (this.checkColumn(i)) {
					System.out.println("Error: Win via column: " + (i));
					throw new Error(".quarto file is not formatted correctly");
				}
			}
			//Diagonals
			if (this.checkDiagonals()) {
				System.out.println("Error: Win via diagonal");
				throw new Error(".quarto file is not formatted correctly");
			}
		}catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error parsing quarto File");
			System.exit(0);
		}
	}
	
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		for(int row = 0; row < numberOfRows; row++) {
			for(int column = 0; column < numberOfColumns; column++) {
				if(boardGet(row, column) >= 0) {
					buf.append(QuartoPiece.binaryStringRepresentation(boardGet(row, column))).append(" ");
				} else {
					buf.append("null   ");
				}
			}
			buf.append("\n");
		}
		return buf.toString();
	}
}