public class EricConnectFourAgent extends Agent {
    public static final int SIMULATION_DEPTH = 10; // The amount of levels of moves simulated per real move
    private static final int[] colPriority = {3, 2, 4, 1, 5, 0, 6}; // Order to simulate column placement
    public static final int RED_WIN = 1000, YEL_WIN = -1000; // The values that indicate winning for each player

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
        dropToken(myGame, moveCol, iAmRed);
    }

    /**
     * Implementation of the minimax algorithm with alpha beta pruning
     * @param g A Connect4Game board
     * @param alpha The highest score that red (max) can achieve
     * @param beta The lowest score that yellow (min) can achieve
     * @param depth The steps to simulate
     * @param isMax Whether the starting player is the maximizing (red) or minimizing (yellow)
     * @return Returns an array of the best play and the highest (red) or lowest (yellow) score
     * possible given this board
     */
    private static int[] minimax(Connect4Game g, int alpha, int beta, int depth, boolean isMax) {
        int checkScore = minimaxHeuristic(g);

        // Quit if the game is already completed
        if (depth == 0 || g.boardFull() || checkScore == RED_WIN || checkScore == YEL_WIN)
            return new int[] {-1, checkScore + (!isMax ? depth : -depth)};

        int bestPlay = -1, bestScore = isMax ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (int col : colPriority) {
            if (g.getColumn(col).getIsFull()) continue;

            // Place the test token and continue the simulation tree
            int slot = dropToken(g, col, isMax);
            int score = minimax(g, alpha, beta, depth-1, !isMax)[1];

            // If two best moves are symmetric, use randomness to increase possible outcomes against
            // nonrandom opponents
            if (score > bestScore && isMax || score < bestScore && !isMax ||
                    score == bestScore && col == g.getColumnCount()-1-bestPlay && Math.random() > 0.5) {
                bestPlay = col;
                bestScore = score;
            }
            if (isMax)
                alpha = Math.max(score, alpha);
            else
                beta = Math.min(score, beta);

            g.getColumn(col).getSlot(slot).clear();

            // Break if this path is not plausible
            if (beta <= alpha)
                break;
        }
        return new int[] {bestPlay, bestScore};
    }

    /**
     * Calculates an estimate on board state using the number of configurations that are likely to
     * lead to wins.
     * @param g A Connect4Game board
     * @return An estimate in the range of [Integer.MIN_VALUE, Integer.MAX_VALUE]. More negative values
     *  indicate that the yellow player is favored while more positive values indicate that the red
     *  player is favored
     */
    private static int minimaxHeuristic(Connect4Game g) {
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
                if (redCnt == 4) return RED_WIN;
                if (yelCnt == 4) return YEL_WIN;
                score += 3 * getScore(redCnt, yelCnt); // A small priority in row stimulates center column pivoting

                // Check all vertical groups
                redCnt = 0; yelCnt = 0;
                for (int row = initRow; row < Math.min(g.getRowCount(), initRow + 4); row++) {
                    Connect4Slot s = g.getColumn(initCol).getSlot(row);
                    if (s.getIsRed())
                        redCnt++;
                    if (!s.getIsRed() && s.getIsFilled())
                        yelCnt++;
                }
                if (redCnt == 4) return RED_WIN;
                if (yelCnt == 4) return YEL_WIN;
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
                if (redCnt == 4) return RED_WIN;
                if (yelCnt == 4) return YEL_WIN;
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
                if (redCnt == 4) return RED_WIN;
                if (yelCnt == 4) return YEL_WIN;
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
    private static int getScore(int redCnt, int yelCnt) {
        if (redCnt == 0 && yelCnt >= 2)
            return -yelCnt;
        if (yelCnt == 0 && redCnt >= 2)
            return redCnt;
        return 0;
    }

    /**
     * Places a token in the next available spot in a row, then returns the row index
     * @param g A Connect4Game board
     * @param col The column to place a token in
     * @param isRed Whether this player is red
     * @return The row index that the token was placed in
     */
    public static int dropToken(Connect4Game g, int col, boolean isRed) {
        int dropRow = g.getRowCount()-1;
        for (int row = 0; row < g.getRowCount(); row++) {
            if (g.getColumn(col).getSlot(row).getIsFilled()) {
                dropRow = row-1;
                break;
            }
        }
        if (isRed)
            g.getColumn(col).getSlot(dropRow).addRed();
        else
            g.getColumn(col).getSlot(dropRow).addYellow();
        return dropRow;
    }

    @Override
    public String getName() {
        return "Eric";
    }
}
