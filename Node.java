import java.util.*;

abstract class Node implements Iterable<Node> {
	public static int iterations = 20;
	public Map<Integer, Node> children;
	public Node parent;
	public QuartoBoard board;
	public int totalGames;
	public double utilitySum;
	public boolean solved;
	public Node solvedNode;
	public byte parentActionCell;
	public byte parentActionPieceID;

	public Node() {
		utilitySum = 0;
		totalGames = 0;
		solved = false;
		solvedNode = null;
		children = new HashMap<Integer, Node>();	
	}

	public Node(QuartoBoard board) {
		utilitySum = 0;
		totalGames = 0;
		children = new HashMap<Integer, Node>();	
		solved = false;
		solvedNode = null;
		this.board = board;
	}

	/**
	 * Expand a random node that hasn't been discovered yet.
	 */
	public abstract Node expandRandom();
	/**
	 * Retrieve a child if it exists; otherwise create it and return the newly created instance
	 */
	public abstract Node getOrCreateChild(int cell, int pieceID);
	/**
	 * Check if a node is fully expanded, or if it has remaining possible children
	 */
	public abstract boolean isExpanded();
	/**
	 * Expand ALL node children.
	 */
	public abstract boolean expand();
	/**
	 * True if node subclass is a Max node of some type, else false for a Min node
	 */
	public abstract boolean isMax();

	public abstract int maxChildren();

	public void addChild(Node child) {
		assert !children.containsKey(child.hashCode());
		children.put(child.hashCode(), child);
		child.parent = this;
	}

	public void clearChildren(){
		children.clear();
	}

	/**
	 * Add a child if it doesn't exist. This is needed for node expansion to 
	 * keep statistics accurate.
	 */
	public void addChildIfNotExist(Node child) {
		if(children.containsKey(child.hashCode()))
			return;

		children.put(child.hashCode(), child);
		child.parent = this;
	}
		
	public static int Hash(int cell, int pieceID){
		return ((int)cell << 6) | (int)pieceID;
	}

	@Override
	public int hashCode(){
		return Hash(parentActionCell, parentActionPieceID);
	}	

	/**
	 * Print the tree in a semi-readable format. 
	 * May take a really long time for big trees
	 */
	public void printPretty(String indent, boolean last, int depth) { 
		if(depth == 0)
			return;
		System.out.print(indent);
		if (last) {
			System.out.print("\\-");
			indent += "\t\t";
		} else {
			System.out.print("|-");
			indent += "|\t";
		}
		System.out.println("(" + toString() + ")");

		int count = 0;

		if(children != null){
			for(Node n : children.values()){
				n.printPretty(indent, count == children.size() - 1, depth - 1);
				count++;
			}
		} else {
			solvedNode.printPretty(indent, true, depth - 1);
			count++;
		}

	}
}

class MinMoveNode extends Node {

	public MinMoveNode(QuartoBoard board, int pieceID) {
		super(board);
		this.parentActionPieceID = (byte)pieceID;
	}

	@Override
	public String toString() {
		return "----"+  "Parent Selected: " + QuartoPiece.binaryStringRepresentation(parentActionPieceID) + 
			" (" + utilitySum + "/" + totalGames + " = " + ((double)utilitySum / totalGames * 100) +  "%) upto " + maxChildren() + "children";
	}	

	@Override
	public boolean expand() {
		if(!isExpanded()) {
			Node sureThing = null;
            for(int i = 0; i < QuartoBoard.numberOfCells; i++){
                if(board.board[i] == -1) {
                    QuartoBoard copy = new QuartoBoard(board);
                    int result = copy.moveAndTestUtility(-1, i, parentActionPieceID);
					if(result != Integer.MAX_VALUE) {
						if(result == -1){
							sureThing = new TerminatingNode(i, parentActionPieceID, result);
							break;
						}		
						addChildIfNotExist(new TerminatingNode(i, parentActionPieceID, result));
					} else {
						addChildIfNotExist(new MinChooseNode(copy, i, parentActionPieceID));
					}					
                }
			}
			if(sureThing != null){
				solved = true;
				children = null;
				solvedNode = sureThing;
				utilitySum = ((TerminatingNode)sureThing).value * totalGames;
			}
		}
		return false;
	}

	@Override 
	public Node getOrCreateChild(int cell, int pieceID){
		// To minimize the potential for dumb, whenever we see a move that's a sure win for the player,
		// we just take it. Always. No randomness involved.
		if(solved) {
			return solvedNode;
		}

		Integer hash = Hash(cell, pieceID);
		if(children.containsKey(hash)){
			return children.get(hash);
		} else {
			// Create the child.
			// Note that we want to cut off the game and insert a terminal node 
			// if the move results in a game over.
			// Furthermore, we want to mark this node as 'solved' if there's a certain victory move for the player
			// now available. After all, under minimax, they'd never take anything else.
			Node child;
			QuartoBoard copy = new QuartoBoard(board);
			int result = copy.moveAndTestUtility(-1, cell, parentActionPieceID);
			if(result != Integer.MAX_VALUE) {
				if(result == -1){
					solved = true;
					children = null;
					solvedNode = new TerminatingNode(cell, parentActionPieceID, result);
					utilitySum = ((TerminatingNode)solvedNode).value * totalGames;
					return solvedNode;
				} else {
					child = new TerminatingNode(cell, parentActionPieceID, result);	
				}	
			} else {
				child = (new MinChooseNode(copy, cell, parentActionPieceID));
			}
			addChild(child);
			return child;		
		}
	}

	@Override 
	public Node expandRandom(){
		// No need for further expansion if solved.
		if(solved) {
			return solvedNode;
		}

		// Try some random guesses, hoping we get lucky.
		for(int i = 0; i < iterations; i++){
			int cell = (int)(Math.random() * QuartoBoard.numberOfCells);
			if(board.board[cell] == -1){
				Integer hash = Hash(cell, parentActionPieceID);
				if(!children.containsKey(hash)){
					// Success
					Node result = getOrCreateChild(cell, parentActionPieceID);
					return result;
				}
			} 
		}

		// RNG was not kind to us, just return the first thing we see. 
		for(int i = 0; i < QuartoBoard.numberOfCells; i++) {
            if(board.board[i] == -1) {
				Integer hash = Hash(i, parentActionPieceID);
				if(!children.containsKey(hash)){
					//Success
					Node result = getOrCreateChild(i, parentActionPieceID);
					return result;
				}	
			}
		}
		
		throw new RuntimeException();
	}

	@Override
	public boolean isExpanded() {
		return solved || maxChildren() == children.size(); 
	}

	@Override
	public int maxChildren() {
		return board.numberOfMovesRemaining();
	}

	@Override
	public boolean isMax(){
		return false;
	}

	@Override
    public Iterator<Node> iterator() {
        Iterator<Node> it = new Iterator<Node>() {
            private int currentIndex = 0;

            @Override
            public boolean hasNext() {
                return currentIndex < QuartoBoard.numberOfCells;
            }

            @Override
            public Node next() {
				for(int i = currentIndex; i < QuartoBoard.numberOfCells; i++){
					if(board.board[i] == -1) {
						QuartoBoard copy = new QuartoBoard(board);
						int result = copy.moveAndTestUtility(-1, i, parentActionPieceID);
						currentIndex = i + 1;
						while(currentIndex < QuartoBoard.numberOfCells && board.board[currentIndex] != -1){
							currentIndex++;
						}
						if(result != Integer.MAX_VALUE) {
							return new TerminatingNode(i, parentActionPieceID, result);
						} else {
							return new MinChooseNode(copy, i, parentActionPieceID);
						}					
					}
				}
				throw new RuntimeException("Error invalid state.");
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
        return it;
	}
	
}

class MaxMoveNode extends Node {
	
	public MaxMoveNode(QuartoBoard board, int pieceID) {
		super(board);
		this.parentActionPieceID = (byte)pieceID;
	}

	@Override
	public String toString() {
		return "++++"+  "Parent Selected: " + QuartoPiece.binaryStringRepresentation(parentActionPieceID)  + " (" + utilitySum + "/" + totalGames + " = " + ((double)utilitySum / totalGames * 100) +  "%)  upto " + maxChildren() + "children";
	}
	
	@Override
	public boolean expand() {
		if(!isExpanded()) {
			Node sureThing = null;

			for(int i = 0; i < QuartoBoard.numberOfCells; i++){
                if(board.board[i] == -1) {
                    QuartoBoard copy = new QuartoBoard(board);
                    int result = copy.moveAndTestUtility(1, i, parentActionPieceID);
					if(result != Integer.MAX_VALUE) {
						if(result == 1){
							sureThing = new TerminatingNode(i, parentActionPieceID, result);					
							break;
						}		
						addChildIfNotExist(new TerminatingNode(i, parentActionPieceID, result));
						
					} else {
						addChildIfNotExist(new MaxChooseNode(copy, i, parentActionPieceID));
					}					
                }
			}
			if(sureThing != null){
				children = null;
				solvedNode = sureThing;
				solved = true;
				utilitySum = ((TerminatingNode)sureThing).value * totalGames;
			}
		}
		return false;
	}

	@Override
	public boolean isExpanded() {
		return solved || maxChildren() == children.size(); 
	}

	@Override
	public int maxChildren() {
		return board.numberOfMovesRemaining();
	}

	@Override 
	public Node getOrCreateChild(int cell, int pieceID){
		// If we hit a 100% sure win or loss, then we mark this node as 'solved'
		// and just return whatever that move was.
		// This doesn't prevent the simulation from making stupid moves, 
		// but if more than a couple hundred statistics exist for the given node, 
		// it's unlikely we'll make a mistake.
		if(solved) {
			return solvedNode;
		}

		Integer hash = Hash(cell, pieceID);
		if(children.containsKey(hash)){
			return children.get(hash);
		} else {
			Node child;
			QuartoBoard copy = new QuartoBoard(board);
			int result = copy.moveAndTestUtility(1, cell, parentActionPieceID);
			if(result != Integer.MAX_VALUE) {
				if(result == 1){
					solved = true;
					children = null;
					solvedNode = new TerminatingNode(cell, parentActionPieceID, result);
					utilitySum = ((TerminatingNode)solvedNode).value * totalGames;					
					return solvedNode;
				} else {
					child = new TerminatingNode(cell, parentActionPieceID, result);					
				}
			} else {
				child = (new MaxChooseNode(copy, cell, parentActionPieceID));
			}
			addChildIfNotExist(child);
			return child;		
		}
	}

	@Override 
	public Node expandRandom(){

		if(solved) {
			return solvedNode;
		}

		// Try some random moves
		for(int i = 0; i < iterations; i++){
			int cell = (int)(Math.random() * QuartoBoard.numberOfCells);
			if(board.board[cell] == -1){
				Integer hash = Hash(cell, parentActionPieceID);
				if(!children.containsKey(hash)){
					// Success
					Node result = getOrCreateChild(cell, parentActionPieceID);
					return result;
				}
			} 
		}

		// Just make a move. Whatever's left.
		for(int i = 0; i < QuartoBoard.numberOfCells; i++) {
            if(board.board[i] == -1) {
				Integer hash = Hash(i, parentActionPieceID);
				if(!children.containsKey(hash)){
					//Success
					Node result = getOrCreateChild(i, parentActionPieceID);
					return result;
				}	
			}
		}

		throw new RuntimeException();
	}

	@Override
	public boolean isMax(){
		return true;
	}

	@Override
    public Iterator<Node> iterator() {
        Iterator<Node> it = new Iterator<Node>() {
            private int currentIndex = 0;

            @Override
            public boolean hasNext() {
                return currentIndex < QuartoBoard.numberOfCells;
            }

            @Override
            public Node next() {
				for(int i = currentIndex; i < QuartoBoard.numberOfCells; i++){
					if(board.board[i] == -1) {
						QuartoBoard copy = new QuartoBoard(board);
						int result = copy.moveAndTestUtility(1, i, parentActionPieceID);
						currentIndex = i + 1;
						while(currentIndex < QuartoBoard.numberOfCells && board.board[currentIndex] != -1){
							currentIndex++;
						}

						if(result != Integer.MAX_VALUE) {
							return new TerminatingNode(i, parentActionPieceID, result);
						} else {
							return new MaxChooseNode(copy, i, parentActionPieceID);
						}					
					}
				}
				throw new RuntimeException("Error invalid state.");
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
        return it;
	}
	
}

class MinChooseNode extends Node {

	public MinChooseNode(QuartoBoard board, int cell, int pieceID) {
		super(board);
		this.parentActionCell = (byte)cell;
		this.parentActionPieceID = (byte)pieceID;
	}

	@Override
	public String toString() {
		return "----" + "Parent Placed: " + QuartoPiece.binaryStringRepresentation(parentActionPieceID) + " onto " + parentActionCell  + " (" + utilitySum + "/" + totalGames + " = " + ((double)utilitySum / totalGames * 100) +  "%)"; 
	}


	@Override
	public boolean expand() {
		if(!isExpanded()){
            for(int i = 0; i < QuartoBoard.numberOfPieces; i++){
                if(!board.isPieceOnBoard(i)) {
                    addChildIfNotExist(new MaxMoveNode(new QuartoBoard(board), i));
                }
            }
		}
		return false;
	}

	@Override
	public boolean isExpanded() {
		return solved || maxChildren() == children.size(); 
	}

	@Override
	public int maxChildren() {
		return board.numberOfPiecesRemaining();
	}

	@Override 
	public Node expandRandom(){
		// Try for a random move
		for(int i = 0; i < iterations; i++){
			int pieceId = (int)(Math.random() * QuartoBoard.numberOfPieces);
			if(!board.isPieceOnBoard(pieceId)){
				Integer hash = Hash(0, pieceId);
				if(!children.containsKey(hash)){
					// Success
					Node result = getOrCreateChild(-1, pieceId);
					return result;
				}
			} 
		}

		// Just return some move
		for(int i = 0; i < QuartoBoard.numberOfPieces; i++) {
            if(!board.isPieceOnBoard(i)) {
				Integer hash = Hash(0, i);
				if(!children.containsKey(hash)){
					//Success
					Node result = getOrCreateChild(-1, i);
					return result;
				}	
			}
		}

		throw new RuntimeException();
	}

	@Override 
	public Node getOrCreateChild(int cell, int pieceID){
		Integer hash = Hash(0, pieceID);
		if(children.containsKey(hash)){
			return children.get(hash);
		} else {
			Node child;
			child = new MaxMoveNode(board, pieceID);	
			addChild(child);
			return child;		
		}
	}
	
	@Override
	public boolean isMax(){
		return false;
	}

	@Override
    public Iterator<Node> iterator() {
        Iterator<Node> it = new Iterator<Node>() {
            private int currentIndex = 0;

            @Override
            public boolean hasNext() {
                return currentIndex < QuartoBoard.numberOfPieces;
            }

            @Override
            public Node next() {
				for(int i = currentIndex; i < QuartoBoard.numberOfPieces; i++){
					if(!board.isPieceOnBoard(i)) {
						currentIndex = i + 1;
						while(currentIndex < QuartoBoard.numberOfPieces && board.isPieceOnBoard(currentIndex))
							currentIndex++;

						return new MaxMoveNode(new QuartoBoard(board), i);
					}
				}
				currentIndex = 1000;
				return null;//	throw new RuntimeException("Error invalid state.");
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
        return it;
	}
	
}

class MaxChooseNode extends Node {

	public MaxChooseNode(QuartoBoard board, int cell, int pieceID) {
		super(board);
		this.parentActionCell = (byte)cell;
		this.parentActionPieceID = (byte)pieceID;
	}

	@Override
	public String toString() {
		return "++++" + "Parent Placed: " + QuartoPiece.binaryStringRepresentation(parentActionPieceID) + " onto " + parentActionCell + " (" + utilitySum + "/" + totalGames + " = " + ((double)utilitySum / totalGames * 100) +  "%) upto " + maxChildren() + "children";
	}

	@Override
	public boolean expand() {
		if(!isExpanded()){
            for(int i = 0; i < QuartoBoard.numberOfPieces; i++){
                if(!board.isPieceOnBoard(i)) {
                    addChild(new MinMoveNode(board, i));
                }
            }
		}
		return false;
	}

	@Override
	public boolean isExpanded() {
		return solved || maxChildren() == children.size(); 
	}

	@Override
	public int maxChildren() {
		return board.numberOfPiecesRemaining();
	}


	@Override 
	public Node expandRandom(){
		// Try random pieces for a bit
		for(int i = 0; i < iterations; i++){
			int pieceId = (int)(Math.random() * QuartoBoard.numberOfPieces);
			if(!board.isPieceOnBoard(pieceId)){
				Integer hash = Hash(0, pieceId);
				if(!children.containsKey(hash)){
					// Success
					Node result = getOrCreateChild(-1, pieceId);
					return result;
				}
			} 
		}

		// Just return something. It probably doesn't matter much if we've failed this many times.
		for(int i = 0; i < QuartoBoard.numberOfPieces; i++) {
            if(!board.isPieceOnBoard(i)) {
				Integer hash = Hash(0, i);
				if(!children.containsKey(hash)){
					//Success
					Node result = getOrCreateChild(-1, i);
					return result;
				}	
			}
		}

		throw new RuntimeException();
	}

	@Override 
	public Node getOrCreateChild(int cell, int pieceID){
		Integer hash = Hash(0, pieceID);
		if(children.containsKey(hash)){
			return children.get(hash);
		} else {
			Node child = new MinMoveNode(new QuartoBoard(board), pieceID);	
			addChild(child);
			return child;		
		}
	}

	@Override
	public boolean isMax(){
		return true;
	}

	@Override
    public Iterator<Node> iterator() {
        Iterator<Node> it = new Iterator<Node>() {
            private int currentIndex = 0;

            @Override
            public boolean hasNext() {
                return currentIndex < QuartoBoard.numberOfPieces;
            }

            @Override
            public Node next() {
				for(int i = currentIndex; i < QuartoBoard.numberOfPieces; i++){
					if(!board.isPieceOnBoard(i)) {
						currentIndex = i + 1;
						while(currentIndex < QuartoBoard.numberOfPieces && board.isPieceOnBoard(currentIndex))
							currentIndex++;
						return new MinMoveNode(new QuartoBoard(board), i);
					}
				}
					throw new RuntimeException("Error invalid state.");
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
        return it;
	}
}

class TerminatingNode extends Node {
	public int value;

	public TerminatingNode(int cell, int pieceID, int value) {
		super();
		this.parentActionCell = (byte)cell;
		this.parentActionPieceID = (byte)pieceID;
		this.value = value;	
		this.utilitySum = value;
		this.totalGames = 1;
	}
	
	@Override
	public void addChild(Node child) {
		throw new RuntimeException("terminal node cannot have children.");
	}

	@Override
	public String toString() {
		return "TRM:Parent: PieceID=" + QuartoPiece.binaryStringRepresentation(parentActionPieceID) + "; Cell=" + parentActionCell + " ==> "+value +  " (" + utilitySum + "/" + totalGames + ")";
	}

	@Override
	public boolean expand(){
		throw new RuntimeException("Cannot expand a terminal node");
	}
	
	@Override
	public int maxChildren() {
		return 0;
	}

	@Override
	public Node getOrCreateChild(int cell, int pieceID){
		throw new RuntimeException("Cannot have a child");
	}

	@Override
	public boolean isMax(){
		return true;
	}
		
	@Override
	public boolean isExpanded() {
		return true; 
	}

	@Override 
	public Node expandRandom(){
		throw new RuntimeException("Cannot expand a terminal node.");
	}

	@Override
    public Iterator<Node> iterator() {
        Iterator<Node> it = new Iterator<Node>() {

            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public Node next() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
        return it;
	}
}
