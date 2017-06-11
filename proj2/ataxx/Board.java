package ataxx;

/* Author: P. N. Hilfinger, (C) 2008.
* @author Anish Saha
*/

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Observable;

import static ataxx.PieceColor.*;
import static ataxx.GameException.error;

/** An Ataxx board.   The squares are labeled by column (a char value between
 *  'a' - 2 and 'g' + 2) and row (a char value between '1' - 2 and '7'
 *  + 2) or by linearized index, an integer described below.  Values of
 *  the column outside 'a' and 'g' and of the row outside '1' to '7' denote
 *  two layers of border squares, which are always blocked.
 *  This artificial border (which is never actually printed) is a common
 *  trick that allows one to avoid testing for edge conditions.
 *  For example, to look at all the possible moves from a square, sq,
 *  on the normal board (i.e., not in the border region), one can simply
 *  look at all squares within two rows and columns of sq without worrying
 *  about going off the board. Since squares in the border region are
 *  blocked, the normal logic that prevents moving to a blocked square
 *  will apply.
 *
 *  For some purposes, it is useful to refer to squares using a single
 *  integer, which we call its "linearized index".  This is simply the
 *  number of the square in row-major order (counting from 0).
 *
 *  Moves on this board are denoted by Moves.
 *  @author Anish Saha
 */
class Board extends Observable {

    /** Number of squares on a side of the board. */
    static final int SIDE = 7;
    /** Length of a side + an artificial 2-deep border region. */
    static final int EXTENDED_SIDE = SIDE + 4;
    /** Number of non-extending moves before game ends. */
    static final int JUMP_LIMIT = 25;
    /** Length of buffer size. */
    static final int BUFFER_SIZE = (EXTENDED_SIDE - SIDE) / 2;

    /** A new, cleared board at the start of the game. */
    Board() {
        _board = new PieceColor[EXTENDED_SIDE * EXTENDED_SIDE];
        moveCount = 0;
        jumpCount = 0;
        history = new Stack<Board>();
        clear();
    }

    /** A copy of B. */
    Board(Board b) {
        makeCopyOf(b);
    }

    /** Helper function for copying the board.
    @param b is the board that is to be copied. */
    private void makeCopyOf(Board b) {
        _board = b._board.clone();
        history = (Stack<Board>) b.history.clone();
        moveCount = b.moveCount;
        jumpCount = b.jumpCount;
        _whoseMove = b._whoseMove;
    }

    /** Return the linearized index of square COL ROW. */
    static int index(char col, char row) {
        return (row - '1' + 2) * EXTENDED_SIDE + (col - 'a' + 2);
    }

    /** Return the linearized index of the square that is DC columns and DR
     *  rows away from the square with index SQ. */
    static int neighbor(int sq, int dc, int dr) {
        return sq + dc + dr * EXTENDED_SIDE;
    }

    /** Clear me to my starting state, with pieces in their initial
     *  positions and no blocks. */
    void clear() {
        moveCount = 0;
        jumpCount = 0;
        _whoseMove = RED;
        history = new Stack<Board>();

        for (char i = 'a'; i <= 'g'; i += 1) {
            for (char j = '1'; j < '8'; j += 1) {
                set(i, j, EMPTY);
            }
        }

        set('a', '1', BLUE);
        set('a', '7', RED);
        set('g', '1', RED);
        set('g', '7', BLUE);

        for (int k = 0; k < _board.length; k += 1) {
            if (_board[k] == null) {
                set(k, BUFFER);
            }
        }
        setChanged();
        notifyObservers();
    }

    /** Return true iff the game is over: i.e., if neither side has
     *  any moves, if one side has no pieces, or if there have been
     *  MAX_JUMPS consecutive jumps without intervening extends. */
    boolean gameOver() {
        if (redPieces() == 0 ^ bluePieces() == 0) {
            return true;
        }
        if (!canMove(RED) && !canMove(BLUE)) {
            return true;
        }
        if (numJumps() >= JUMP_LIMIT) {
            return true;
        }
        return false;
    }

    /** Return number of red pieces on the board. */
    int redPieces() {
        return numPieces(RED);
    }

    /** Return number of blue pieces on the board. */
    int bluePieces() {
        return numPieces(BLUE);
    }

    /** Return number of COLOR pieces on the board. */
    int numPieces(PieceColor color) {
        int count = 0;
        for (char i = 'a'; i <= 'g'; i += 1) {
            for (char j = '1'; j <= '7'; j += 1) {
                if (get(i, j) == color) {
                    count += 1;
                }
            }
        }
        return count;
    }

    /** Convert neighbors into opposite color when a move is done; used later.
    @param color if of the piece that was moved; neighbors converted to color.
    @param index is the index of the board to which the piece was moved. */
    private void convertNeighbors(PieceColor color, int index) {
        int convertedCount = 0;
        for (int j = -1; j <= 1; j += 1) {
            for (int i = -1; i <= 1; i += 1) {
                int neighborIndex = neighbor(index, i, j);
                if (get(neighborIndex) == color.opposite()) {
                    set(neighborIndex, color);
                    convertedCount += 1;
                }
            }
        }
    }

    /** The current contents of square CR, where 'a'-2 <= C <= 'g'+2, and
     *  '1'-2 <= R <= '7'+2.  Squares outside the range a1-g7 are all
     *  BLOCKED.  Returns the same value as get(index(C, R)). */
    PieceColor get(char c, char r) {
        return _board[index(c, r)];
    }

    /** Return the current contents of square with linearized index SQ. */
    PieceColor get(int sq) {
        return _board[sq];
    }

    /** Set get(C, R) to V, where 'a' <= C <= 'g', and
     *  '1' <= R <= '7'. */
    private void set(char c, char r, PieceColor v) {
        set(index(c, r), v);
    }

    /** Set square with linearized index SQ to V.  This operation is
     *  undoable. */
    private void set(int sq, PieceColor v) {
        _board[sq] = v;
    }

    /** Set square at C R to V (not undoable). */
    private void unrecordedSet(char c, char r, PieceColor v) {
        _board[index(c, r)] = v;
    }

    /** Set square at linearized index SQ to V (not undoable). */
    private void unrecordedSet(int sq, PieceColor v) {
        _board[sq] = v;
    }

    /** Return true iff MOVE is legal on the current board.
    This method turn insensitive, helpful for canMove.
    @param color is the color of the piece.
    @param move is the move that is being checked. */
    boolean legalMove(Move move, PieceColor color) {
        if (move == null) {
            return false;
        }
        char r0 = move.row0(), r1 = move.row1();
        char c0 = move.col0(), c1 = move.col1();
        if (move.isPass()) {
            return !canMove(color);
        }
        if (c0 < 'a' || c0 > 'g' || c1 < 'a' || c1 > 'g'
            || r0 < '1' || r0 > '7' || r1 < '1' || r1 > '7') {
            return false;
        }
        if (get(c1, r1) != EMPTY) {
            return false;
        }
        if (get(c0, r0) != color) {
            return false;
        }
        int colDist = c1 - c0;
        if (colDist < -2 || colDist > 2) {
            return false;
        }
        int rowDist = r1 - r0;
        if (rowDist < -2 || rowDist > 2) {
            return false;
        }
        return true;
    }

    /** Turn sensitive legalMove.
    @param move is the move that is made.
    @return if move is legal. */
    boolean legalMove(Move move) {
        return legalMove(move, whoseMove());
    }


    /** Checks legal move on the row/col of the move, to and from.
    @param c0 is the initial column.
    @param r0 is the initial row.
    @param c1 is the column the piece moves to.
    @param r1 is the row the piece moves to.
    @return the function returns if the move is legal. */
    boolean legalMove(char c0, char r0, char c1, char r1) {
        if (c0 == c1 && r0 == r1) {
            return false;
        }
        return legalMove(Move.move(c0, r0, c1, r1));
    }

    /** Return true iff player WHO can move, ignoring whether it is
     *  that player's move and whether the game is over. */
    boolean canMove(PieceColor who) {
        for (char i = 'a'; i <= 'g'; i += 1) {
            for (char j = '1'; j <= '7'; j += 1) {
                if (who == get(i, j)) {
                    for (int k = -2; k <= 2; k += 1) {
                        for (int l = -2; l <= 2; l += 1) {
                            Move move = Move.move(i, j,
                                (char) (i + k), (char) (j + l));
                            if (legalMove(move, who)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /** This is a function that returns all possible legal moves for a color.
    @param who is the color that we are finding all possible legal moves for.
    @return the function returns an arraylist of all possible legal moves. */
    ArrayList<Move> legalMoves(PieceColor who) {
        ArrayList<Move> moves = new ArrayList<Move>();
        for (char i = 'a'; i <= 'g'; i += 1) {
            for (char j = '1'; j <= '7'; j += 1) {
                if (who == get(i, j)) {
                    for (int k = -2; k <= 2; k += 1) {
                        for (int l = -2; l <= 2; l += 1) {
                            Move move = Move.move(i, j,
                                (char) (i + k), (char) (j + l));
                            if (legalMove(move, who)) {
                                moves.add(move);
                            }
                        }
                    }
                }
            }
        }
        if (moves.size() == 0) {
            moves.add(Move.pass());
        }
        return moves;
    }

    /** Return the color of the player who has the next move.  The
     *  value is arbitrary if gameOver(). */
    PieceColor whoseMove() {
        return _whoseMove;
    }

    /** Return total number of moves and passes since the last
     *  clear or the creation of the board. */
    int numMoves() {
        return moveCount;
    }

    /** Return number of non-pass moves made in the current game since the
     *  last extend move added a piece to the board (or since the
     *  start of the game). Used to detect end-of-game. */
    int numJumps() {
        return jumpCount;
    }

    /** Perform the move C0R0-C1R1, or pass if C0 is '-'.  For moves
     *  other than pass, assumes that legalMove(C0, R0, C1, R1). */
    void makeMove(char c0, char r0, char c1, char r1) {
        if (c0 == '-') {
            makeMove(Move.pass());
        } else {
            makeMove(Move.move(c0, r0, c1, r1));
        }
    }

    /** Make the MOVE on this Board, assuming it is legal. */
    void makeMove(Move move) {
        assert legalMove(move);
        history.push(new Board(this));
        moveCount += 1;
        if (move.isPass()) {
            pass();
            return;
        } else if (move.isExtend()) {
            jumpCount = 0;
        } else if (move.isJump()) {
            jumpCount += 1;
            set(move.col0(), move.row0(), EMPTY);
        }
        set(move.col1(), move.row1(), whoseMove());
        convertNeighbors(whoseMove(), index(move.col1(), move.row1()));
        PieceColor opponent = _whoseMove.opposite();
        _whoseMove = opponent;
        setChanged();
        notifyObservers();
    }

    /** Update to indicate that the current player passes, assuming it
     *  is legal to do so.  The only effect is to change whoseMove(). */
    void pass() {
        assert !canMove(_whoseMove);
        PieceColor opponent = _whoseMove.opposite();
        _whoseMove = opponent;
        setChanged();
        notifyObservers();
    }

    /** Undo the last move. */
    void undo() {
        makeCopyOf(history.pop());
        setChanged();
        notifyObservers();
    }

    /** This function returns the reflected column. Used for placing blocks.
    @param c is the character of the column that is passed in (a-g). */
    char oppCol(char c) {
        char[] columns = new char[] {'a', 'b', 'c', 'd', 'e', 'f', 'g'};
        for (int i = 0; i < columns.length; i += 1) {
            if (columns[i] == c) {
                return columns[columns.length - i - 1];
            }
        }
        throw new GameException("Invalid block placement.");
    }

    /** This function returns the reflected row. Used for placing blocks.
    @param c is the character of the row that is passed in (1-7). */
    char oppRow(char c) {
        char[] columns = new char[] {'1', '2', '3', '4', '5', '6', '7'};
        for (int i = 0; i < columns.length; i += 1) {
            if (columns[i] == c) {
                return columns[columns.length - i - 1];
            }
        }
        throw new GameException("Invalid block placement.");
    }

    /** Return true iff it is legal to place a block at C R. */
    boolean legalBlock(char c, char r) {
        if (c < 'a' || c > 'g' || r < '1' || r > '7') {
            return false;
        }
        if (get(c, r) != EMPTY || get(oppCol(c), oppRow(r)) != EMPTY
            || get(oppCol(c), r) != EMPTY || get(c, oppRow(r)) != EMPTY) {
            return false;
        }
        return true;
    }

    /** Return true iff it is legal to place a block at CR. */
    boolean legalBlock(String cr) {
        return legalBlock(cr.charAt(0), cr.charAt(1));
    }

    /** Set a block on the square C R and its reflections across the middle
     *  row and/or column, if that square is unoccupied and not
     *  in one of the corners. Has no effect if any of the squares is
     *  already occupied by a block.  It is an error to place a block on a
     *  piece. */
    void setBlock(char c, char r) {
        if (!legalBlock(c, r)) {
            throw error("illegal block placement");
        }
        set(c, r, BLOCKED);
        set(oppCol(c), oppRow(r), BLOCKED);
        set(oppCol(c), r, BLOCKED);
        set(c, oppRow(r), BLOCKED);
        setChanged();
        notifyObservers();
    }

    /** Place a block at CR. */
    void setBlock(String cr) {
        setBlock(cr.charAt(0), cr.charAt(1));
    }

    @Override
    public String toString() {
        return toString(false);
    }

    /* .equals used only for testing purposes. */
    @Override
    public boolean equals(Object obj) {
        Board other = (Board) obj;
        return Arrays.equals(_board, other._board);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(_board);
    }

    /** Return a text depiction of the board (not a dump).  If LEGEND,
     *  supply row and column numbers around the edges. */
    String toString(boolean legend) {
        String out = "";
        for (char i = '7'; i >= '1'; i -= 1) {
            for (char j = 'a'; j <= 'g'; j += 1) {
                PieceColor color = get(j, i);
                if (color == RED) {
                    out = out.concat("r ");
                } else if (color == BLUE) {
                    out = out.concat("b ");
                } else if (color == BLOCKED) {
                    out = out.concat("X ");
                } else {
                    out = out.concat("- ");
                }
            }
            out = out.concat("\n");
        }
        return out;
    }

    /** For reasons of efficiency in copying the board,
     *  we use a 1D array to represent it, using the usual access
     *  algorithm: row r, column c => index(r, c).
     *
     *  Next, instead of using a 7x7 board, we use an 11x11 board in
     *  which the outer two rows and columns are blocks, and
     *  row 2, column 2 actually represents row 0, column 0
     *  of the real board.  As a result of this trick, there is no
     *  need to special-case being near the edge: we don't move
     *  off the edge because it looks blocked.
     *
     *  Using characters as indices, it follows that if 'a' <= c <= 'g'
     *  and '1' <= r <= '7', then row c, column r of the board corresponds
     *  to board[(c -'a' + 2) + 11 (r - '1' + 2) ], or by a little
     *  re-grouping of terms, board[c + 11 * r + SQUARE_CORRECTION]. */
    private PieceColor[] _board;
    /** Player that is on move. */
    private PieceColor _whoseMove;
    /** Counts the number of moves. */
    private int moveCount;
    /** Counts number of jumps since last extend. Prevents infinite game. */
    private int jumpCount;
    /** Stack that keeps track of boards after moves. Used for undo. */
    private Stack<Board> history;
}
