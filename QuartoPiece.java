
/**
 * A collection of static functions that could at some point be useful for extracting information 
 * from the new piece representation. 
 * 
 * This class has basically been removed, and replaced with an int primitive. This turns out to be good 
 * for performance, and reduces the size of a piece from about 50 bytes to 4 bytes. The reason this works
 * is that the array of characteristics is boolean (0/1) values that come directly from the integer value
 * itself. That is, 26 = 11010 = {1,1,0,1,0}. It turns out that it is significantly more expensive to copy 
 * and modify the attributes array than to just extract this information with bitwise operations.
 * 
 * 
 */
public class QuartoPiece {
	//first position [0] is tall/short
	//second position [1] is solid/hollow
	//third position [2] is white/black
	//fourth position [3] is wood/metal
	//fifth position [4] is round/square
	
	private QuartoPiece(int pieceID) {
	}
	
	public static boolean isTall(int pieceID) {
		return (pieceID & (1<<4)) != 0;
	}

	public static boolean isSolid(int pieceID) {
		return (pieceID & (1<<3)) != 0;
	}

	public static boolean isWhite(int pieceID) {
		return (pieceID & (1<<2)) != 0;
	}

	public static boolean isWood(int pieceID) {
		return (pieceID & (1<<1)) != 0;
	}

	public static boolean isRound(int pieceID) {
		return (pieceID & (1<<0)) != 0;
	}
		
	public static String getHeight(int pieceID) {
		if(isTall(pieceID)) {
			return "tall";
		} else {
			return "short";
		}
	}

	public static String getStructure(int pieceID) {
		if(isSolid(pieceID)) {
			return "solid";
		} else {
			return "hollow";
		}
	}

	public static String getColor(int pieceID) {
		if(isWhite(pieceID)) {
			return "white";
		} else {
			return "black";
		}
	}

	public static String getMaterial(int pieceID) {
		if(isWood(pieceID)) {
			return "wood";
		} else {
			return "metal";
		}
	}
	
	public static String getShape(int pieceID) {
		if(isRound(pieceID)) {
			return "round";
		} else {
			return "square";
		}
	}

	public static String binaryStringRepresentation(int pieceID) {
		return String.format("%5s", Integer.toBinaryString(pieceID)).replace(' ', '0');
	}
		
	public static String fullIntBinaryStringRepresentation(int pieceID) {
		return String.format("%32s", Integer.toBinaryString(pieceID)).replace(' ', '0');
	}
	
}