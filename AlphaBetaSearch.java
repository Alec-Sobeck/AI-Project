
/**
 * Performs an Alpha-Beta Search, starting at the specified node. 
 * This is based on the code from Lab#2, but modified so the overall search returns the best move from ROOT
 * 
 * It is somewhat wasteful with memory, expanding all children of a node the first time it's visited.
 * However, this doesn't seem to be a particularly big problem because I only apply this on the last ~6-12
 * moves of a game, to make my program execute quicker. We can easily store a tree of this size in memory.
 */
public class AlphaBetaSearch {

	/**
	 * Executes an alpha-beta search. 
	 * @return the node we should move to
	 */
	public static Node alphaBetaSearch(Node node) {
		node.expand();
        if(node.solved) {
			System.out.println("Solved. Minimax implicitly = 1");
        	return node.solvedNode;
        } 
        
        assert node instanceof MaxChooseNode || node instanceof MaxMoveNode;

        Node best = null;
        int value = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE; 
        int temp;
        for(Node child : node.children.values()) { 
            if(!child.isMax()) {
                temp = Math.max(value, minValue(child, alpha, beta));
            } else {
                temp = Math.max(value, maxValue(child,alpha,beta));
            }

            assert temp != Integer.MAX_VALUE && temp != Integer.MIN_VALUE;

            // We can't prune much at the root level, but we need to know what choice is best so far. 
            if(temp > value) {
                value = temp;
                best = child;
            }

			assert value < beta;
			
            alpha = Math.max(alpha, value);
            if(alpha == 1) {
				// We win. Normally, we can't prune at the root level, but in this case
				// we already know such a path is optimal. Why explore further?
                break;
			}
			
			child.clearChildren();
			System.gc();
        }
        System.out.println("Alpha-Beta value: " + value);
        return best;	
	}		

	public static int maxValue(Node node, int alpha, int beta) {
		if (node instanceof TerminatingNode) {
			return ((TerminatingNode)node).value;
		} else {
			int value = Integer.MIN_VALUE;
			for(Node child : node) { 
				if(!child.isMax()) {
					value = Math.max(value, minValue(child, alpha, beta));
				} else {
					value = Math.max(value, maxValue(child,alpha,beta));
				}
				assert value != Integer.MAX_VALUE && value != Integer.MIN_VALUE;
				if (value >= beta) {
					return value;
				}
				alpha = Math.max(alpha,value);
			}
			assert value != Integer.MAX_VALUE && value != Integer.MIN_VALUE;
            return value;	
		}
	}

	public static int minValue(Node node, int alpha, int beta) {
		if (node instanceof TerminatingNode) {
			return ((TerminatingNode)node).value;
		} else {			
			int value = Integer.MAX_VALUE;
			for(Node child : node){
				if(child.isMax()) {
					value = Math.min(value, maxValue(child,alpha,beta));
				} else {
					value = Math.min(value, minValue(child, alpha, beta));
				}
				assert value != Integer.MAX_VALUE && value != Integer.MIN_VALUE;
				if (value <= alpha) {
					return value;
				}
				beta = Math.min(beta,value);
			}
			assert value != Integer.MAX_VALUE && value != Integer.MIN_VALUE;
			return value;	
		}
	}

}