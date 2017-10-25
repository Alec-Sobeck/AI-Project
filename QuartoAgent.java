
/**
 * Largely unmodified version of provided QuartoAgent base class.
 */
public abstract class QuartoAgent {

	protected static final int NUMBER_OF_ROWS = 5;
	protected static final int NUMBER_OF_COLUMNS = 5;
	protected static final int NUMBER_OF_PIECES = 32;

	protected static final String SELECT_PIECE_HEADER = "Q1:";
	protected static final String SELECT_MOVE_HEADER = "Q2:";

	protected static final String INFO_MESSAGE_HEADER = "INFO:";

	protected static final String ERROR_PIECE_HEADER = "ERR_PIECE:";
	protected static final String ERROR_MOVE_HEADER = "ERR_MOVE:";

	protected static final String ACKNOWLEDGMENT_PIECE_HEADER = "ACK_PIECE:";
	protected static final String ACKNOWLEDGMENT_MOVE_HEADER = "ACK_MOVE:";

	protected static final String PIECE_MESSAGE_HEADER = "PIECE:";
	protected static final String MOVE_MESSAGE_HEADER = "MOVE:";
	protected static final String INFORM_PLAYER_NUMBER_HEADER = "PLAYER:";

	protected static final String GAME_OVER_HEADER = "GAME_OVER:";
	public static final String TURN_TIME_LIMIT_HEADER = "TURN_TIME_LIMIT:";

	//time limit is in milliseconds
	protected int timeLimitForResponse = 10000;
	protected static final int COMMUNICATION_DELAY = 2000;
	protected long startTime = System.currentTimeMillis();

	protected static int playerNumber;
	protected GameClient gameClient;
	protected QuartoBoard quartoBoard;

	protected abstract String pieceSelectionAlgorithm();
	protected abstract String moveSelectionAlgorithm(int pieceID);
	protected abstract void applyMoveToBoard(int row, int column, int pieceID, boolean isMaxMove);

	public QuartoAgent(GameClient gameClient, String stateFileName) {
		this.gameClient = gameClient;
		this.quartoBoard = new QuartoBoard(stateFileName);
	}

	//Main game loop
	protected void play() {
		boolean gameOn = true;
		setPlayerNumber();
		setTurnTimeLimit();

		//player 2 gets first move
		if (playerNumber == 2) {
			choosePieceTurn();
		}

		while(gameOn) {
			//print board
			this.quartoBoard.printBoardState();
			//turn order swaps
			chooseMoveTurn();

			this.quartoBoard.printBoardState();

			choosePieceTurn();
		}

	}

	//first response a client receives from server should be the player's number
	protected void setPlayerNumber() {
		String message = this.gameClient.readFromServer(1000000);
		String[] splittedMessage = message.split("\\s+");
		System.out.println(splittedMessage[0] + " " + splittedMessage[1]);
		if (isExpectedMessage(splittedMessage, INFORM_PLAYER_NUMBER_HEADER)) {
			playerNumber = Integer.parseInt(splittedMessage[1], 10);
		} else {
			System.out.println("Did not Receive Player Number");
			System.exit(0);
		}

	}

	//client should receive time limit from server
	protected void setTurnTimeLimit() {
		String message = this.gameClient.readFromServer(1000000);
		String[] splittedMessage = message.split("\\s+");
		System.out.println(splittedMessage[0] + " " + splittedMessage[1]);
		if (isExpectedMessage(splittedMessage, TURN_TIME_LIMIT_HEADER)) {
			timeLimitForResponse = Integer.parseInt(splittedMessage[1], 10);
		} else {
			System.out.println("Did not Receive TURN_TIME_LIMIT_HEADER");
			System.exit(0);
		}

	}

	//control flow for player choosing a piece
	protected void choosePieceTurn() {

		String MessageFromServer;
		//get message
		MessageFromServer = this.gameClient.readFromServer(1000000);
		String[] splittedMessage = MessageFromServer.split("\\s+");

		//close program if message is not the expected message
		isExpectedMessage(splittedMessage, SELECT_PIECE_HEADER, true);

		//determine piece
		long start = System.currentTimeMillis();
		String pieceMessage = pieceSelectionAlgorithm();
		long end = System.currentTimeMillis();
		System.out.println("Piece Selection took " + (end - start) + "ms");

		this.gameClient.writeToServer(pieceMessage);

		MessageFromServer = this.gameClient.readFromServer(1000000);
		String[] splittedResponse = MessageFromServer.split("\\s+");
		if (!isExpectedMessage(splittedResponse, ACKNOWLEDGMENT_PIECE_HEADER) && !isExpectedMessage(splittedResponse, ERROR_PIECE_HEADER)) {
			turnError(MessageFromServer);
		}

		int pieceID = Integer.parseInt(splittedResponse[1], 2);

		MessageFromServer = this.gameClient.readFromServer(1000000);
		String[] splittedMoveResponse = MessageFromServer.split("\\s+");

		isExpectedMessage(splittedMoveResponse, MOVE_MESSAGE_HEADER, true);

		String[] moveString = splittedMoveResponse[1].split(",");
		int[] move = new int[2];
		move[0] = Integer.parseInt(moveString[0]);
		move[1] = Integer.parseInt(moveString[1]);

		applyMoveToBoard(move[0], move[1], pieceID, false);

	}

	protected void chooseMoveTurn() {
		//get message
		String MessageFromServer;
		MessageFromServer = this.gameClient.readFromServer(1000000);
		String[] splittedMessage = MessageFromServer.split("\\s+");

		//close program if message is not the expected message
		isExpectedMessage(splittedMessage, SELECT_MOVE_HEADER, true);
		int pieceID = Integer.parseInt(splittedMessage[1], 2);

		//determine piece
		long start = System.currentTimeMillis();
		String moveMessage = moveSelectionAlgorithm(pieceID);
		long end = System.currentTimeMillis();
		System.out.println("Move Selection took " + (end - start) + "ms");


		this.gameClient.writeToServer(moveMessage);

		MessageFromServer = this.gameClient.readFromServer(1000000);
		String[] splittedMoveResponse = MessageFromServer.split("\\s+");
		if (!isExpectedMessage(splittedMoveResponse, ACKNOWLEDGMENT_MOVE_HEADER) && !isExpectedMessage(splittedMoveResponse, ERROR_MOVE_HEADER)) {
			turnError(MessageFromServer);
		}

		String[] moveString = splittedMoveResponse[1].split(",");
		int[] move = new int[2];
		move[0] = Integer.parseInt(moveString[0]);
		move[1] = Integer.parseInt(moveString[1]);

		applyMoveToBoard(move[0], move[1], pieceID, true);
	}


	/*
	 ********************* UTILITY FUNCTIONS ************************
	*/
	protected static void turnError(String message) {
		System.out.println("Turn order out of sync, last message received was: " + message);
		System.exit(0);
	}

	protected static boolean isExpectedMessage(String[] splittedMessage, String header) {
		if (splittedMessage.length > 0 && splittedMessage[0].equals(header)) {
			return true;
		} else if (splittedMessage.length > 0 && splittedMessage[0].equals(GAME_OVER_HEADER)) {
			gameOver(splittedMessage);
		}

		assert false;
		return false;
	}

	protected static boolean isExpectedMessage(String[] splittedMessage, String header, boolean fatal) {
		if (splittedMessage.length > 0 && splittedMessage[0].equals(header)) {
			return true;
		} else if (splittedMessage.length > 0 && splittedMessage[0].equals(GAME_OVER_HEADER)) {
			gameOver(splittedMessage);
		} else if (fatal) {
			turnError(splittedMessage[0] + " " + splittedMessage[1]);
		}
		System.exit(-1);
		return false;
	}


	protected static void gameOver(String[] splittedMessage) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < splittedMessage.length; i++) {
			builder.append(splittedMessage[i] + " ");
		}
		builder.toString();
		System.out.println(builder.toString());
		System.exit(0);
	}


	protected boolean hasTimeLeft(){
		//System.out.println("Elapsed Time: " + getMillisecondsFromTimer() + " " + (this.timeLimitForResponse - COMMUNICATION_DELAY);
		return getMillisecondsFromTimer() < (this.timeLimitForResponse - COMMUNICATION_DELAY);
	}

	//Records the current time in milliseconds from when this function is called
	protected void startTimer() {
		startTime = System.currentTimeMillis();
	}

	//gets the time difference between now and when startTimer() was last called
	protected long getMillisecondsFromTimer() {
		//System.out.println("Nanos:" + ((System.nanoTime() - startTime) / 1000));
		return (System.currentTimeMillis() - startTime);
	}

	/*
	 * Does a quick check (1 millisecond) for messages from the server.
	 * This function will return the oldest missed message if there is one (and null if there is not).
	 * If this function does return a message from the server, you may want to call this function again to see if there are additional messages
	 */

	protected String checkForMissedServerMessages() {
		return this.gameClient.readFromServer(1);
	}


}
