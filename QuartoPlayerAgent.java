import java.util.*;


public class QuartoPlayerAgent extends QuartoAgent {
    // Higher = more exploration. Optimal = 1.0 / SQRT(2.0) 
    private final double optimalParameter = 1.0 / Math.sqrt(2.0);
    private double expParameter = optimalParameter;
    private Node root = null;
    private int simulations = 0;

    public QuartoPlayerAgent(GameClient gameClient, String stateFileName) {
        super(gameClient, stateFileName);
    }

    public static void main(String[] args) {
        //start the game client
        GameClient gameClient = new GameClient();

        String ip = null;
        String stateFileName = null;
        if(args.length > 0) {
            ip = args[0];
        } else {
            System.out.println("No IP Specified");
            ip = null;
            System.exit(0);
        }
        if (args.length > 1) {
            stateFileName = args[1];
        }
        
        gameClient.connectToServer(ip, 4321);
        QuartoPlayerAgent quartoAgent = new QuartoPlayerAgent(gameClient, stateFileName);
        quartoAgent.play();

        gameClient.closeConnection();     
    }

    @Override
    protected String pieceSelectionAlgorithm() {
        this.startTimer();
        System.out.println("--------------------------------------");
        System.out.println("Entering piece selection");
        System.out.println("--------------------------------------");
        simulations = 0;     
        if(quartoBoard.spacesLeft >= 24) {
            // The first 2 moves are the things that cause the most problems with garbage collection,
            // and don't seem to ever matter.
            // so I just choose a random move, to be pragmatic about the limitations of the JVM in realtime applications
            root = null;
            System.out.println("Taking a random move to prevent GC problems. ");
            int piece = quartoBoard.chooseRandomPieceNotPlayed();
            String answer = QuartoPiece.binaryStringRepresentation(piece);
            System.out.println("answer=" + answer);
            return answer;
        } else if(quartoBoard.canApplyAlphaBeta()) {
            // Alpha Beta Prune for last ~7 moves
            root = null;
            System.out.println("Applying Minimax!");
            Node result = AlphaBetaSearch.alphaBetaSearch(new MaxChooseNode(new QuartoBoard(quartoBoard), -1, -1));
            String answer = QuartoPiece.binaryStringRepresentation(((MinMoveNode)result).parentActionPieceID);
            System.out.println("answer=" + answer);
            return answer;
        } else { 
            // Else: Simulations
            int result = getBestPiece();
            String answer = QuartoPiece.binaryStringRepresentation(result);
            System.out.println("answer=" + answer);
            return answer;        
        }
    }

    @Override
    protected String moveSelectionAlgorithm(int pieceID) {
        this.startTimer();        
        System.out.println("--------------------------------------");
        System.out.println("Entering move selection.");
        System.out.println("--------------------------------------");
        this.startTimer();
        simulations = 0;

        if(quartoBoard.spacesLeft >= 24) {
            // The first 2 moves are the things that cause the most problems with garbage collection,
            // and don't seem to ever matter.
            // so I just choose a random move, to be pragmatic about the limitations of the JVM in realtime applications
            root = null;
            System.out.println("Taking a random move to prevent GC problems. ");
            int cell = quartoBoard.chooseRandomPositionNotPlayed();
            String answer = quartoBoard.getCoordinates(cell);
            System.out.println("move=" + answer);
            return answer;
        } else if(quartoBoard.canApplyAlphaBeta()) {
            // Alpha Beta Prune for last ~7 moves
            root = null;
            System.out.println("Applying Minimax!");
            Node result = AlphaBetaSearch.alphaBetaSearch(new MaxMoveNode(new QuartoBoard(quartoBoard), pieceID));
            String answer;
            if(result instanceof MaxChooseNode){
                answer = quartoBoard.getCoordinates(((MaxChooseNode)result).parentActionCell);
            } else {
                answer = quartoBoard.getCoordinates(((TerminatingNode)result).parentActionCell);
            }
            System.out.println("move=" + answer);
            return answer;
        } else {
            // Else: Simulations
            int result = getBestMove(pieceID);
            String answer = quartoBoard.getCoordinates(result);
            System.out.println("move=" + answer);
            return answer;
        }
    }

    @Override
    protected void applyMoveToBoard(int row, int column, int pieceID, boolean isMaxMove) {
		this.quartoBoard.boardSet(row, column, pieceID);

        // Update the game tree root 

        if(root == null) 
            return;

        int cell = row * QuartoBoard.numberOfColumns + column;
        if(isMaxMove) {
            // Last action was MAX MOVE
            // Thus, level 2 of the tree is a MAX_CHOOSE
            assert root instanceof MaxMoveNode;
            assert (root.solvedNode != null && root.solvedNode instanceof TerminatingNode) || root.children.values().iterator().next() instanceof MaxChooseNode || root.children.values().iterator().next() instanceof TerminatingNode;
            int hash = MaxChooseNode.Hash(cell, pieceID);
            if(!root.solved && root.children.containsKey(hash))
            	root = root.children.get(hash);
            else
            	root = null;
        } else {
        	// Root of tree is MAX choose. the thing below it should be MIN_MOVE
        	// we can take both those plays easily enough. 
        	// After that, we need some server input 
        	// to figure out what to do with the MIN_CHOOSE root        	
            assert root instanceof MaxChooseNode;
            assert (root.solvedNode != null && root.solvedNode instanceof TerminatingNode) ||root.children.values().iterator().next() instanceof MinMoveNode || root.children.values().iterator().next() instanceof TerminatingNode;
            int hash = Node.Hash(0, pieceID);
            if(!root.solved && root.children.containsKey(hash)) {
                root = root.children.get(hash);
                hash = MinChooseNode.Hash(cell, pieceID);
                if(!root.solved && root.children.containsKey(hash)) {
                	root = root.children.get(hash);
                } else {
                	root = null;
                }
            } else {
            	root = null;
            }            
        }
    }

    // -------------------------------------------------------------
    // Tree search code:
    //

    /**
     * Computes the bound specified in the monte carlo algorithm
     */
    private double computeUCB(Node node, int parentSimulations, int multiplier){
        if(node.totalGames == 0) 
            return Double.POSITIVE_INFINITY;
        return node.utilitySum * multiplier / node.totalGames + 2 * expParameter * Math.sqrt(2.0 * Math.log(parentSimulations) / node.totalGames);
    }

    /**
     * Take the argmax over a collection of nodes
     * @param parentSimulations - # simulations run by the parent node (whose children are passed to function)
     * @param multiplier - +1/-1, depending on whether MIN or MAX is playing
     */
    private Node argmax(Collection<Node> stats, int parentSimulations, int multiplier){
        Iterator<Node> it = stats.iterator();
        Node best = it.next();
        double bestStat = computeUCB(best, parentSimulations, multiplier);
        while(it.hasNext()) {
            Node node = it.next();
            double temp = computeUCB(node, parentSimulations, multiplier);
            if(temp > bestStat) {
                best = node;
                bestStat = temp;
            }
        }    
        return best;
    }

    /**
     * Run a Monte Carlo Simulation to get the best piece.
     */
    public int getBestPiece() {
        // Make sure we have a valid root.
        if(root == null || root instanceof TerminatingNode)
        	root = new MaxChooseNode(new QuartoBoard(quartoBoard), -1, -1);
        root.parent = null;
        root.expand();
        assert root instanceof MaxChooseNode;
        assert root.isExpanded();
        assert this.quartoBoard.equals(root.board);
        assert expParameter > 0;
        System.gc();

        if(root.solved) {
            // There's a winning move available at the root. Just take it.
        	System.out.println("Root is solved.");
        	return root.solvedNode.parentActionPieceID;
        } else {
            expParameter = optimalParameter;
            // Run simulations while there's time.
            while(hasTimeLeft()) {
                // Explore more generously at the root.
                expParameter *= 5;
                Node best = argmax(root.children.values(), simulations, 1);
                expParameter /= 5;
                runSimulation(best);  
            }
 
            // Get the answer (and print diagnostics)
            expParameter = 0;
            Node best = argmax(root.children.values(), simulations, 1);
            for(Node child : root.children.values()){
        		System.out.println(child);
            }
            expParameter = optimalParameter;
            System.out.println("best=" + best);
            System.out.println("Ran " + simulations + " simulations");
            return best.parentActionPieceID;
        }     
    }

    public int getBestMove(int pieceID) {
        // Make sure root is in a valid state
        if(root != null) {
    		int hash = Node.Hash(0, pieceID); 
    		if(root.children.containsKey(hash)) {
    			root = root.children.get(hash);
    		} else {
    			root = null;
    		}
    	}
       	if(root == null || root instanceof TerminatingNode) 
            root = new MaxMoveNode(new QuartoBoard(quartoBoard), pieceID);            
        root.parent = null;
        root.expand();
        assert root instanceof MaxMoveNode;
        assert root.isExpanded();
        assert this.quartoBoard.equals(root.board);
        assert expParameter > 0;
        System.gc();
        
        if(root.solved) {
            // There's a winning move available at the root. Just take it.
        	System.out.println("Root is solved.");
            return root.solvedNode.parentActionCell;
        } else {
            expParameter = optimalParameter;
            // While time left, run simulations
            while(hasTimeLeft()) {
                // Explore more generously at the root.
                expParameter *= 5;
                Node best = argmax(root.children.values(), simulations, 1); 
                expParameter /= 5;
                runSimulation(best);          
            }

            // Print diagnostics + get the answer
            expParameter = 0;
            Node best = argmax(root.children.values(), simulations, 1);
            for(Node child : root.children.values()){
        		System.out.println(child);
            }
            System.out.println("best=" + best);
            System.out.println("Ran " + simulations + " simulations");
            expParameter = optimalParameter;
            
            return best.parentActionCell;
        }
    }

    /**
     * Traverse the tree using mutual recursion, until hitting an unexplored node.
     * Then, expand it, run a simulation on it, and backtrace the results.
     */
    private void runSimulation(Node node){
    	simulations++;
        
    	if(node.solved) {
            node.utilitySum += ((TerminatingNode)node.solvedNode).value;
            node.totalGames++;
            return;
        }    	
    	
        if(node instanceof MaxMoveNode) {
            playGameMaxMove(node, ((MaxMoveNode)node).parentActionPieceID);
        } else if(node instanceof MaxChooseNode) {
            playGameMaxChoose(node);
        } else if(node instanceof MinMoveNode) {
            playGameMinMove(node, ((MinMoveNode)node).parentActionPieceID);
        } else if(node instanceof MinChooseNode) {
            playGameMinChoose(node);
        } else if(node instanceof TerminatingNode){
            node.utilitySum += ((TerminatingNode)node).value;
            node.totalGames++;
            return;
        } else {
            throw new RuntimeException("Invalid simulation state");
        }
    }

    private int playGameMaxMove(Node node, int pieceID) {        
        if(node.isExpanded()){
            Node child = argmax(node.children.values(), node.totalGames, 1);
            if(child instanceof TerminatingNode){                
                int utility = ((TerminatingNode)child).value;
                node.utilitySum += utility;
                node.totalGames++;
                child.utilitySum += utility;
                child.totalGames++;                
                return utility;
            }
            int utility = playGameMaxChoose(child);
            node.utilitySum += utility;
            node.totalGames++;
            return utility;
        } else {
            return simulateRandom(node);
        }
    }

    private int playGameMaxChoose(Node node) {
        if(node.isExpanded()){
            Node child = argmax(node.children.values(), node.totalGames, 1);
            if(child instanceof TerminatingNode){                
                int utility = ((TerminatingNode)child).value;
                node.utilitySum += utility;
                node.totalGames++;
                child.utilitySum += utility;
                child.totalGames++;
                return utility;
            }

            int utility = playGameMinMove(child, child.parentActionPieceID);
            node.utilitySum += utility;
            node.totalGames++;
            if(child.solved){
                TerminatingNode childChild = (TerminatingNode)(child.solvedNode);
                MinMoveNode childCast = (MinMoveNode)child;
                TerminatingNode newTerm = new TerminatingNode(0, childCast.parentActionPieceID, childChild.value);
                node.children.put(childCast.hashCode(), newTerm);
                assert node.children.size() == node.maxChildren();
            }
            return utility;
        } else {
        	return simulateRandom(node);            
        }
    }

    private int playGameMinMove(Node node, int pieceID) {
        if(node.isExpanded()){
            Node child = argmax(node.children.values(), node.totalGames, -1);
            if(child instanceof TerminatingNode) {
                int utility = ((TerminatingNode)child).value;
                node.utilitySum += utility;
                node.totalGames++;
                child.utilitySum += utility;
                child.totalGames++;
                return utility;
            }
    
            int utility = playGameMinChoose(child);
            node.utilitySum += utility;
            node.totalGames++;
            return utility;
        } else {
        	return simulateRandom(node);            
        }        
    }

    private int playGameMinChoose(Node node) {
        if(node.isExpanded()) {
            // Get best child. Recurse.
            Node child = argmax(node.children.values(), node.totalGames, -1);
            if(child instanceof TerminatingNode){                
                int utility = ((TerminatingNode)child).value;
                node.utilitySum += utility;
                node.totalGames++;
                child.utilitySum += utility;
                child.totalGames++;
                return utility;
            }
            
            int utility = playGameMaxMove(child, ((MaxMoveNode)child).parentActionPieceID);
            node.utilitySum += utility;
            node.totalGames++;
            if(child.solved){
                TerminatingNode childChild = (TerminatingNode)(child.solvedNode);
                MaxMoveNode childCast = (MaxMoveNode)child;
                TerminatingNode newTerm = new TerminatingNode(0, childCast.parentActionPieceID, childChild.value);
                node.children.put(childCast.hashCode(), newTerm);
                assert node.children.size() == node.maxChildren();
            }
            return utility;
        } else {
        	return simulateRandom(node);
        }
    }

    private int simulateRandom(Node node){
        // Expand a random child. Run a simulation on it. 
        Node child = node.expandRandom();
        int utility = MonteCarloBoardSimulation.playGameToFinish(child);
        node.utilitySum += utility;
        node.totalGames++;
        child.utilitySum += utility;
        child.totalGames++;
        return utility;
    }

}
