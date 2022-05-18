public class EricConnectFourAgent extends Agent {
    private static final int[] colPriority = {3, 2, 4, 1, 5, 0, 6};
    public static final int SIMULATION_DEPTH = 10;

    /**
     * Constructs a new agent.
     *
     * @param game   the game for the agent to play.
     * @param iAmRed whether the agent is the red player.
     */
    public EricConnectFourAgent(Connect4Game game, boolean iAmRed) {
        super(game, iAmRed);
    }

    @Override
    public void move() {
        int moveCol = minimax(myGame, Integer.MIN_VALUE, Integer.MAX_VALUE, SIMULATION_DEPTH, iAmRed)[0];
        if (moveCol == -1)
            for (int col : colPriority)
                if (!myGame.getColumn(col).getIsFull())
                    moveCol = col;
        if (iAmRed)
            myGame.getColumn(moveCol).getSlot(getDropRow(myGame, moveCol)).addRed();
        else
            myGame.getColumn(moveCol).getSlot(getDropRow(myGame, moveCol)).addYellow();
    }

    /**
     * Implementation of the minimax algorithm with alpha beta pruning
     * @param g A Connect4Game board
     * @param alpha The highest score that red (max) can achieve
     * @param beta The lowest score that yellow (min) can achieve
     * @param depth The steps to simulate
     * @param isMax Whether the starting player is the maximizing (red) or minimizing (yellow)
     * @return Returns the highest (red) or lowest (yellow) score possible given this board
     */
    private int[] minimax(Connect4Game g, int alpha, int beta, int depth, boolean isMax) {
        // Quit if the game is already winnable
        int checkScore = minimaxHeuristic(g);
        if (checkScore == Integer.MAX_VALUE || checkScore == Integer.MIN_VALUE || depth == 0 || g.boardFull())
            return new int[] {-1, checkScore};

        if (isMax) {
            int maxScore = Integer.MIN_VALUE, bestPlay = -1;
            for (int col : colPriority) {
                // Prevent bot from placing in full col
                if (g.getColumn(col).getIsFull()) continue;

                // Place the test token
                int dropRow = getDropRow(g, col);
                g.getColumn(col).getSlot(dropRow).addRed();

                // Update the highest possible score of this node and the entire tree so far
                int score = minimax(g, alpha, beta, depth-1, false)[1];
                alpha = Math.max(score, alpha);
                if (score > maxScore) {
                    bestPlay = col;
                    maxScore = score;
                }

                g.getColumn(col).getSlot(dropRow).clear();

                // Break if this path is not plausible
                if (beta <= alpha) break;
            }
            return new int[] {bestPlay, maxScore};
        } else {
            int minScore = Integer.MAX_VALUE, bestPlay = -1;
            for (int col : colPriority) {
                if (g.getColumn(col).getIsFull()) continue;

                int dropRow = getDropRow(g, col);
                g.getColumn(col).getSlot(dropRow).addYellow();

                int score = minimax(g, alpha, beta, depth-1, true)[1];
                beta = Math.min(score, beta);
                if (score < minScore) {
                    bestPlay = col;
                    minScore = score;
                }

                g.getColumn(col).getSlot(dropRow).clear();

                if (beta <= alpha) break;
            }
            return new int[] {bestPlay, minScore};
        }
    }

    /**
     * Calculates an estimate on board state using the number of configurations that are likely to
     * lead to wins.
     * @param g A Connect4Game board
     * @return An estimate in the range of [Integer.MIN_VALUE, Integer.MAX_VALUE]. More negative values
     *  indicate that the yellow player is favored while more positive values indicate that the red
     *  player is favored
     */
    private int minimaxHeuristic(Connect4Game g) {
        int score = 0;
        for (int initRow = 0; initRow < g.getRowCount(); initRow++) {
            for (int initCol = 0; initCol < g.getColumnCount(); initCol++) {
                // Check all horizontal groups
                int redCnt = 0, yelCnt = 0;
                for (int col = initCol; col < Math.min(g.getColumnCount(), initCol + 4); col++) {
                    Connect4Slot s = g.getColumn(col).getSlot(initRow);
                    if (s.getIsRed())
                        redCnt++;
                    if (!s.getIsRed() && s.getIsFilled())
                        yelCnt++;
                }
                if (redCnt == 4) return Integer.MAX_VALUE;
                if (yelCnt == 4) return Integer.MIN_VALUE;
                score += getScore(redCnt, yelCnt);

                // Check all vertical groups
                redCnt = 0; yelCnt = 0;
                for (int row = initRow; row < Math.min(g.getRowCount(), initRow + 4); row++) {
                    Connect4Slot s = g.getColumn(initCol).getSlot(row);
                    if (s.getIsRed())
                        redCnt++;
                    if (!s.getIsRed() && s.getIsFilled())
                        yelCnt++;
                }
                if (redCnt == 4) return Integer.MAX_VALUE;
                if (yelCnt == 4) return Integer.MIN_VALUE;
                score += getScore(redCnt, yelCnt);

                // Check the forward diagonal groups
                redCnt = 0; yelCnt = 0;
                for (int dis = 0; dis < 4; dis++) {
                    int newCol = initCol + dis, newRow = initRow + dis;
                    if (newCol >= g.getColumnCount() || newRow >= g.getRowCount())
                        break;
                    Connect4Slot s = g.getColumn(newCol).getSlot(newRow);
                    if (s.getIsRed())
                        redCnt++;
                    if (!s.getIsRed() && s.getIsFilled())
                        yelCnt++;
                }
                if (redCnt == 4) return Integer.MAX_VALUE;
                if (yelCnt == 4) return Integer.MIN_VALUE;
                score += getScore(redCnt, yelCnt);

                // Check the backward diagonal groups
                redCnt = 0; yelCnt = 0;
                for (int dis = 0; dis < 4; dis++) {
                    int newCol = initCol - dis, newRow = initRow + dis;
                    if (newCol < 0 || newRow >= g.getRowCount())
                        break;
                    Connect4Slot s = g.getColumn(newCol).getSlot(newRow);
                    if (s.getIsRed())
                        redCnt++;
                    if (!s.getIsRed() && s.getIsFilled())
                        yelCnt++;
                }
                if (redCnt == 4) return Integer.MAX_VALUE;
                if (yelCnt == 4) return Integer.MIN_VALUE;
                score += getScore(redCnt, yelCnt);
            }
        }
        return score;
    }

    /**
     * Return the score to give to a group of four tokens
     * @param redCnt The amount of red tokens
     * @param yelCnt The amount of yellow tokens
     * @return The score for this group. Groups with mixed color tokens return a score of 0
     */
    private int getScore(int redCnt, int yelCnt) {
        if (redCnt == 0 && yelCnt >= 2)
            return -yelCnt;
        if (yelCnt == 0 && redCnt >= 2)
            return redCnt;
        return 0;
    }

    /**
     * Return the lowest empty row in a column
     * @param g A Connect4Game board
     * @param col The column the token is being dropped in
     * @return The row the token will be placed in
     */
    public static int getDropRow(Connect4Game g, int col) {
        for (int row = 0; row < g.getRowCount(); row++) {
            if (g.getColumn(col).getSlot(row).getIsFilled())
                return row-1;
        }
        return g.getRowCount()-1;
    }

    @Override
    public String getName() {
        return "Eric";
    }
}
