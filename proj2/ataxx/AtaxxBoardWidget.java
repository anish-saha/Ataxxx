package ataxx;

import ucb.gui2.Pad;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.util.Observer;
import java.util.Observable;

import java.awt.event.MouseEvent;

import static ataxx.PieceColor.*;

/** Widget for displaying an Ataxx board.
 *  @author Anish Saha
 */
class AtaxxBoardWidget extends Pad implements Observer {

    /** Length of side of one square, in pixels. */
    static final int SQDIM = 50;
    /** Number of squares on a side. */
    static final int SIDE = Board.SIDE;
    /** Radius of circle representing a piece. */
    static final int PIECE_RADIUS = 30;
    /** Size of side with buffers. */
    static final int EXTENDED_SIDE = 11;

    /** Color of red pieces. */
    private static final Color RED_COLOR = Color.RED;
    /** Color of blue pieces. */
    private static final Color BLUE_COLOR = Color.BLUE;
    /** Color of blocks. */
    private static final Color BLOCKS = Color.GRAY;
    /** Color of blank squares. */
    private static final Color BLANK_COLOR = Color.WHITE;

    /** Stroke for lines. */
    private static final BasicStroke LINE_STROKE = new BasicStroke(1.0f);

    /** Model being displayed. */
    private static Board _model;

    /** A new widget displaying MODEL. */
    AtaxxBoardWidget(Board model) {
        _model = model;
        setMouseHandler("click", this::readMove);
        _model.addObserver(this);
        _dim = SQDIM * SIDE;
        setPreferredSize(_dim, _dim);
    }

    @Override
    public synchronized void paintComponent(Graphics2D g) {
        g.setColor(BLANK_COLOR);
        g.fillRect(0, 0, _dim, _dim);
        g.setColor(Color.BLACK);
        drawBoard(g);
    }

    /** Draw a block centered at (CX, CY) on G. */
    void drawBlock(Graphics2D g, int cx, int cy) {
        g.setColor(BLOCKS);
        cy = 6 - cy;
        g.fillRect(SQDIM * cx, SQDIM * cy, SQDIM, SQDIM);
    }

    /** Draw a piece of color on G.
    @param g is the graphics that helps draw things.
    @param color is the color of the piece being drawn.
    @param c is the column pixel.
    @param r is the row pixel. */
    void drawPiece(Graphics2D g, PieceColor color, int r, int c) {
        if (color == PieceColor.RED) {
            g.setColor(RED_COLOR);
        } else if (color == PieceColor.BLUE) {
            g.setColor(BLUE_COLOR);
        } else {
            g.setColor(BLANK_COLOR);
        }
        c = 6 - c;
        g.fillOval(r * SQDIM, c * SQDIM, SQDIM, SQDIM);
    }

    /** Return mouse's column at last click. */
    char mouseCol() {
        return _mouseCol;
    }
    /** Return mouse's row at last click. */
    char mouseRow() {
        return _mouseRow;
    }

    /** Notify observers of mouse's current position from click event WHERE. */
    private void readMove(String unused, MouseEvent where) {
        int x = where.getX(), y = where.getY();
        if (where.getButton() == MouseEvent.BUTTON1) {
            _mouseCol = (char) (x / SQDIM + 'a');
            _mouseRow = (char) ((SQDIM * SIDE - y) / SQDIM + '1');
            if (_mouseCol >= 'a' && _mouseCol <= 'g'
                && _mouseRow >= '1' && _mouseRow <= '7') {
                setChanged();
                notifyObservers("" + _mouseCol + _mouseRow);
            }
        }
    }

    /** Draws the initial state of the board. Also happens after clearing.
    @param g is the graphics that helps draw figures here. */
    private void drawBoard(Graphics2D g) {
        int side = 7 * SQDIM;
        g.drawRect(0, 0, side, side);
        for (int c = 0; c <= side; c += SQDIM) {
            g.drawLine(0, c, side, c);
        }
        for (int r = 0; r <= side; r += SQDIM) {
            g.drawLine(r, 0, r, side);
        }

        for (int i = 0; i < EXTENDED_SIDE * EXTENDED_SIDE; i += 1) {
            if (_model.get(i) == PieceColor.RED) {
                drawPiece(g, PieceColor.RED, i % 11 - 2, i / 11 - 2);
            } else if (_model.get(i) == PieceColor.BLUE) {
                drawPiece(g, PieceColor.BLUE, i % 11 - 2, i / 11 - 2);
            } else if (_model.get(i) == PieceColor.BLOCKED) {
                drawBlock(g, i % 11 - 2, i / 11 - 2);
            }
        }
    }

    @Override
    public synchronized void update(Observable model, Object arg) {
        repaint();
    }

    /** Dimension of current drawing surface in pixels. */
    private int _dim;
    /** Row and column of last mouse click. */
    private char _mouseRow, _mouseCol;
}
