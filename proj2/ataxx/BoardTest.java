package ataxx;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.HashSet;

/** Tests of the Board class.
 *  @author Anish Saha
 */
public class BoardTest {

    private static final String[]
        GAME1 = { "a7-b7", "a1-a2",
                  "a7-a6", "a2-a3",
                  "a6-a5", "a3-a4" },

        GAME2 = { "a7-a5", "a1-a2", "a5-a3", "a1-b2",
                  "g1-e1", "b2-c1", "e1-e2", "c1-d1"},

        GAME3 = { "a7-a5", "a1-a3", "a5-a7", "a3-a1", "a7-a5",
                  "a1-a3", "a5-a7", "a3-a1", "a7-a5", "a1-a3",
                  "a5-a7", "a3-a1", "a7-a5", "a1-a3", "a5-a7",
                  "a3-a1", "a7-a5", "a1-a3", "a5-a7", "a3-a1",
                  "a7-a5", "a1-a3", "a5-a7", "a3-a1", "a7-a5", "a1-a3"},

        GAME4 = { "a7-a5", "a1-a3", "a5-a7", "a3-a1", "a7-a5",
                  "a1-a3", "a5-a7", "a3-a1", "a7-a5", "a1-a3",
                  "a5-a7", "a3-a1", "a7-a5", "a1-a3", "a5-a7",
                  "a3-a1", "a7-a5", "a1-a3", "a5-a7", "a3-a1"};

    private static void makeMoves(Board b, String[] moves) {
        for (String s : moves) {
            b.makeMove(s.charAt(0), s.charAt(1),
                       s.charAt(3), s.charAt(4));
        }
    }

    @Test
    public void testBoard() {
        Board b0 = new Board();
        assertEquals("game over at initial state.", false, b0.gameOver());
        makeMoves(b0, GAME1);
        assertEquals("incorrect game over", false, b0.gameOver());
        b0.clear();
        makeMoves(b0, GAME2);
        assertEquals("red cannot move, it has no pieces on the board.",
            false, b0.canMove(PieceColor.RED));
        assertEquals("game should be over, blue won.", true, b0.gameOver());
        b0.clear();
        makeMoves(b0, GAME3);
        assertEquals("clear/numJumps working incorrectly.", 26, b0.numJumps());
        assertEquals("clear or numPieces is working incorrectly for red.",
            2, b0.numPieces(PieceColor.RED));
        assertEquals("clear or numPieces is working incorrectly for blue.",
            2, b0.numPieces(PieceColor.BLUE));
        assertEquals("Blue can move, ignore game over and whose move it is.",
            true, b0.canMove(PieceColor.BLUE));
        assertEquals("Jump limit - game should be over.", true, b0.gameOver());
    }

    @Test
    public void testMoves() {
        Board b0 = new Board();
        makeMoves(b0, GAME4);
        Move[] myMoves = new Move[] { Move.move('a', '7', 'a', '6'),
            Move.move('a', '7', 'b', '7'), Move.move('a', '7', 'b', '6'),
            Move.move('a', '7', 'a', '5'), Move.move('a', '7', 'b', '5'),
            Move.move('a', '7', 'c', '7'), Move.move('a', '7', 'c', '6'),
            Move.move('a', '7', 'c', '5'), Move.move('g', '1', 'g', '2'),
            Move.move('g', '1', 'g', '3'), Move.move('g', '1', 'f', '1'),
            Move.move('g', '1', 'f', '2'), Move.move('g', '1', 'f', '3'),
            Move.move('g', '1', 'e', '1'), Move.move('g', '1', 'e', '2'),
            Move.move('g', '1', 'e', '3')};
        ArrayList<Move> list = new ArrayList<Move>();
        for (int i = 0; i < myMoves.length; i += 1) {
            list.add(myMoves[i]);
        }

        HashSet<Move> expected = new HashSet<Move>();
        ArrayList<Move> legalMoves = b0.legalMoves(PieceColor.RED);
        expected.addAll(list);
        assertEquals("legalMoves is incorrect.",
            true, expected.containsAll(legalMoves));
        expected.clear();
        expected.addAll(legalMoves);
        assertEquals("legalMoves is incorrect.",
            true, expected.containsAll(list));
        b0.setBlock('a', '5');
        assertEquals(true, b0.legalBlock('a', '4'));
        assertEquals(false, b0.legalBlock('a', '3'));
        assertEquals(true, b0.legalMove(Move.move('a', '7', 'a', '6')));
        assertEquals(false, b0.legalMove(Move.move('a', '7', 'd', '6')));
        assertEquals(false, b0.legalMove(Move.move('a', '6', 'a', '5')));
        assertEquals(false, b0.legalMove(Move.move('a', '7', 'd', '6')));
        assertEquals(false, b0.legalMove(Move.move('a', '7', 'a', '5')));

        b0.clear();
        ArrayList<Move> onlyPass = new ArrayList<Move>();
        onlyPass.add(Move.pass());
        makeMoves(b0, GAME2);
        assertEquals("legalMoves should only have pass.", onlyPass,
            b0.legalMoves(PieceColor.RED));
    }

    @Test
    public void testFunctions() {
        Board b0 = new Board();
        Board b1 = new Board();
        makeMoves(b1, GAME2);
        Board b2 = new Board(b1);
        assertEquals("Boards copy not working.", b2, b1);
        b1.clear();
        assertEquals("Clear failed.", b0, b1);
        for (int i = 0; i < 8; i += 1) {
            b2.undo();
        }
        assertEquals("Multiple undo failed.", b0, b2);
    }

    @Test
    public void testUndo() {
        Board b0 = new Board();
        Board b1 = new Board(b0);
        makeMoves(b0, GAME1);
        Board b2 = new Board(b0);
        for (int i = 0; i < GAME1.length; i += 1) {
            b0.undo();
        }
        assertEquals("failed to return to start", b1, b0);
        makeMoves(b0, GAME1);
        assertEquals("second pass failed to reach same position", b2, b0);
    }

}

