package ataxx;

import static ataxx.PieceColor.*;

/** A Player that receives its moves from its Game's getMoveCmnd method.
 *  @author Anish Saha
 */
class Manual extends Player {

    /** A Player that will play MYCOLOR on GAME, taking its moves from
     *  GAME. */
    Manual(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    @Override
    Move myMove() {
        Command cmnd = game().getMoveCmnd(myColor().toString() + ": ");
        if (cmnd == null) {
            return null;
        }
        String[] args = cmnd.operands();
        return Move.move(args[0].charAt(0), args[1].charAt(0),
            args[2].charAt(0), args[3].charAt(0));
    }
}

