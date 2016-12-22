package ataxx;

import static ataxx.PieceColor.*;
import java.util.ArrayList;

/** A Player that computes its own moves.
 *  Inspired by Paul Hilfinger :)
 *  @author Anish Saha
 */
class AI extends Player {

    /** Maximum minimax search depth before going to static evaluation. */
    private static final int MAX_DEPTH = 4;
    /** A position magnitude indicating a win (for red if positive, blue
     *  if negative). */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 1;
    /** A magnitude greater than a normal value. */
    private static final int INFTY = Integer.MAX_VALUE;

    /** A new AI for GAME that will play MYCOLOR. */
    AI(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    @Override
    Move myMove() {
        if (!board().canMove(myColor())) {
            System.out.println(myColor() + " passes.");
            return Move.pass();
        }
        Move move = findMove();
        System.out.println(myColor() + " moves " + move + ".");
        return move;
    }

    /** Return a move for me from the current position, assuming there
     *  is a move. */
    private Move findMove() {
        Board b = new Board(board());
        if (myColor() == RED) {
            findMove(b, MAX_DEPTH, true, 1, -INFTY, INFTY);
        } else {
            findMove(b, MAX_DEPTH, true, -1, -INFTY, INFTY);
        }
        return _lastFoundMove;
    }

    /** Used to communicate best moves found by findMove, when asked for. */
    private Move _lastFoundMove;

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _lastFoundMove iff SAVEMOVE. The move
     *  should have maximal value or have value >= BETA if SENSE==1,
     *  and minimal value or value <= ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels before using a static estimate. */
    private int findMove(Board board, int depth, boolean saveMove, int sense,
                         int alpha, int beta) {
        if (depth == 0) {
            return staticScore(board);
        }
        ArrayList<Move> moves = board.legalMoves(board.whoseMove());
        for (int i = 0; i < moves.size(); i += 1) {
            Move m = moves.get(i);
            board.makeMove(m);
            int score = findMove(board, depth - 1,
                false, sense * -1, alpha, beta);
            if (sense == 1 && score > alpha) {
                alpha = score;
                if (saveMove) {
                    _lastFoundMove = m;
                }
            }
            if (sense == -1 && score < beta) {
                beta = score;
                if (saveMove) {
                    _lastFoundMove = m;
                }
            }
            board.undo();
            if (alpha >= beta) {
                break;
            }
        }
        if (sense == 1) {
            return alpha;
        }
        return beta;
    }

    /** Return a heuristic value for BOARD. */
    private int staticScore(Board board) {
        return board.redPieces() - board.bluePieces();
    }
}
