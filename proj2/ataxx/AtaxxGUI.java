package ataxx;

import ucb.gui2.TopLevel;
import ucb.gui2.LayoutSpec;

import java.util.Observable;
import java.util.Observer;

import java.io.Writer;
import java.io.PrintWriter;

/** The GUI for the Ataxx game.
 *  @author Anish Saha
 */
class AtaxxGUI extends TopLevel implements Observer, Reporter {

    /* The implementation strategy applied here is to make it as
     * unnecessary as possible for the rest of the program to know that it
     * is interacting with a GUI as opposed to a terminal.
     *
     * To this end, we first have made Board observable, so that the
     * GUI gets notified of changes to a Game's board and can interrogate
     * it as needed, while the Game and Board themselves need not be aware
     * that it is being watched.
     *
     * Second, instead of creating a new API by which the GUI communicates
     * with a Game, we instead simply arrange to make the GUI's input look
     * like that from a terminal, so that we can reuse all the machinery
     * in the rest of the program to interpret and execute commands.  The
     * AtaxxGUI simply composes commands (such as "start" or "clear") and
     * writes them to a Writer that (using the Java library's PipedReader
     * and PipedWriter classes) provides input to the Game using exactly the
     * same API as would be used to read from a terminal. Thus, a simple
     * Manual player can handle all commands and moves from the GUI.
     *
     * See also Main.java for how this might get set up.
     */

    /** Minimum size of board in pixels. */
    private static final int MIN_SIZE = 300;

    /** A new display observing MODEL, with TITLE as its window title.
     *  It uses OUTCOMMANDS to send commands to a game instance, using the
     *  same commands as the text format for Ataxx. */
    AtaxxGUI(String title, Board model, Writer outCommands) {
        super(title, true);
        addMenuButton("Game->Start", this::setStart);
        addMenuButton("Game->New", this::newGame);
        addMenuButton("Game->Quit", this::quit);
        addMenuRadioButton("Setup->Move Pieces", "Pieces",
            true, this::placer1);
        addMenuRadioButton("Setup->Set Blocks", "Pieces",
            false, this::placer2);
        addMenuRadioButton("Red->Red Manual", "Red", true, this::setRedManual);
        addMenuRadioButton("Red->Red AI", "Red", false, this::setRedAI);
        addMenuRadioButton("Blue->Blue Manual", "Blue",
            false, this::setBlueManual);
        addMenuRadioButton("Blue->Blue AI", "Blue", true, this::setBlueAI);
        addMenuButton("PASS->Pass", this::makePass);
        addMenuButton("Options->Seed", this::setSeed);
        addMenuButton("Options->Help", this::getHelp);
        addMenuButton("Options->Stats", this::getStats);
        _model = model;
        state = 0;
        place = true;
        _widget = new AtaxxBoardWidget(model);
        _out = new PrintWriter(outCommands, true);
        add(_widget,
            new LayoutSpec("height", "1",
                           "width", "REMAINDER",
                           "ileft", 5, "itop", 5, "iright", 5,
                           "ibottom", 5));
        setMinimumSize(MIN_SIZE, MIN_SIZE);
        _widget.addObserver(this);
        _model.addObserver(this);
    }

    /** Execute the "Quit" button function. */
    private synchronized void quit(String unused) {
        _out.printf("quit%n");
    }

    /** Execute Seed... command. */
    private synchronized void setSeed(String unused) {
        String resp =
            getTextInput("Random Seed", "Get Seed", "question", "");
        if (resp == null) {
            return;
        }
        try {
            long s = Long.parseLong(resp);
            _out.printf("seed %d%n", s);
        } catch (NumberFormatException excp) {
            return;
        }
    }

    /** Execute start command.
    @param unused is an unused parameter. */
    private synchronized void setStart(String unused) {
        if (state == 0) {
            _out.printf("start%n");
            state = 1;
        } else {
            showMessage("Cannot start now.", "Error", "msg");
        }
    }

    /** Execute the "New Game" button function. */
    private synchronized void newGame(String unused) {
        _model.clear();
        _out.printf("clear%n");
        state = 0;
        setChanged();
        notifyObservers("New");
    }

    /** Indicates turns and number of pieces on each side.
    @param unused is an unused parameter. */
    private synchronized void getStats(String unused) {
        String s = _model.whoseMove().toString();
        int r = _model.redPieces();
        int b = _model.bluePieces();
        showMessage(s + "'s turn\n" + "red pieces: " + r
            + "\nblue pieces: " + b, "STATS", "msg");
    }

    /** Creates a window that shows a help menu.
    @param unused is an unused parameter. */
    private synchronized void getHelp(String unused) {
        showMessage("Click on a piece of your color, then on an adjacent\n"
                + "square to move and add a piece of your color to board."
                + "You can\n"
                + "also jump by going any other position within two"
                + "squares of\n"
                + "your location. For either option, diagonal moves"
                + "are permitted\n"
                + "If (and only if) you have no moves, make a pass"
                + "You turn pieces\n"
                + "of the opposite color that are adjacent to"
                + "your move to your\n"
                + "color. The side with more pieces when the"
                + "board is filled\n"
                + "(or if one side has no pieces or if an infinite game"
                + "has been\n"
                + "forced). You can setup blocks where neither side"
                + "can move to\n"
                + "before starting the game, as well as setup a manual/AI"
                + "game. You\n"
                + "can also set up pieces to make a handicap or create"
                + "a different\n"
                + "starting configuration.\n"
                + "Press NEW to start new game, or QUIT to quit.",
                "HELP MENU", "msg");
    }

    /** Executes the pass command.
    @param unused is an unused parameter. */
    private synchronized void makePass(String unused) {
        if (state == 0 || state == 1) {
            if (_model.legalMove(Move.pass(), _model.whoseMove())) {
                _out.printf("pass%n");
            } else {
                showMessage("You can still move.", "ILLEGAL PASS", "msg");
            }
        }
    }

    /** Executes the block... command.
    @param sq is where the block is set.. */
    private synchronized void setBlocks(String sq) {
        if (state == 0) {
            _model.setBlock(sq);
            _out.printf("block %s%n", sq);
        } else {
            showMessage("Cannot set blocks now.", "Setup", "msg");
        }
    }

    /** Executes the manual red command.
    @param unused is an unused parameter. */
    private synchronized void setRedManual(String unused) {
        if (state == 0) {
            _out.printf("manual red%n");
        } else {
            showMessage("Cannot set player now.", "Error", "msg");
        }
    }

    /** Executes the auto red command.
    @param unused is an unused parameter. */
    private synchronized void setRedAI(String unused) {
        if (state == 0) {
            _out.printf("auto red%n");
        } else {
            showMessage("Cannot set player now.", "Error", "msg");
        }
    }

    /** Executes the manual blue command.
    @param unused is an unused parameter. */
    private synchronized void setBlueManual(String unused) {
        if (state == 0) {
            _out.printf("manual blue%n");
        } else {
            showMessage("Cannot set player now.", "Error", "msg");
        }
    }

    /** Executes the auto blue command.
    @param unused is an unused parameter. */
    private synchronized void setBlueAI(String unused) {
        if (state == 0) {
            _out.printf("auto blue%n");
        } else {
            showMessage("Cannot set player now.", "Error", "msg");
        }
    }

    @Override
    public void errMsg(String format, Object... args) {

    }

    @Override
    public void outcomeMsg(String format, Object... args) {

    }

    @Override
    public void moveMsg(String format, Object... args) {

    }

    @Override
    public void update(Observable obs, Object arg) {
        if (obs == _widget && state == 0) {
            if (place) {
                movePiece((String) arg);
            }
            if (!place) {
                setBlocks((String) arg);
            }
            setChanged();
            notifyObservers("click");
        }
        if (state == 1) {
            movePiece((String) arg);
            if (_model.gameOver()) {
                showMessage("Game Over.", "END", "msg");
                if (_model.redPieces() > _model.bluePieces()) {
                    showMessage("Red wins.", "WINNER", "msg");
                } else if (_model.bluePieces() > _model.redPieces()) {
                    showMessage("Blue wins.", "WINNER", "msg");
                } else {
                    showMessage("Draw.", "DRAW", "msg");
                }
                state = 2;
            }
            setChanged();
            notifyObservers("click");
        }
    }

    /** Sets place to true, allowing us to move pieces in setup.
    @param unused is an unused parameter. */
    private void placer1(String unused) {
        place = true;
    }

    /** Sets place to false, allowing us to place blocks in setup.
    @param unused is an unused parameter. */
    private void placer2(String unused) {
        place = false;
    }

    /** Respond to a click on SQ while in "play" mode. */
    private void movePiece(String sq) {
        if (from == null) {
            from = sq;
        } else if (to == null) {
            to = sq;
            Move move = Move.move(from.charAt(0), from.charAt(1),
                to.charAt(0), to.charAt(1));
            if (_model.legalMove(move)) {
                _model.makeMove(move);
                _out.printf(move.col0() + move.row0()
                    + "-" + move.col1() + move.row1());
                from = null;
                to = null;
                setChanged();
                notifyObservers();
            } else {
                from = null;
                to = null;
            }
        }
    }

    /** Contains the drawing logic for the Ataxx model. */
    private AtaxxBoardWidget _widget;
    /** The model of the game. */
    private Board _model;
    /** Output sink for sending commands to a game. */
    private PrintWriter _out;
    /** String that stores from position. */
    private String from;
    /** String that stores to position. */
    private String to;
    /** Keeps track of state. 0 = SETUP, 1 = PLAYING, 2 = FINISHED. */
    private int state;
    /** Boolean for checking whether setting up blocks or pieces. */
    private boolean place;
}
