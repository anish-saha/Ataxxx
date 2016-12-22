package ataxx;

/* Author: P. N. Hilfinger */

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.function.Consumer;
import java.math.BigInteger;

import static ataxx.PieceColor.*;
import static ataxx.Game.State.*;
import static ataxx.Command.Type.*;
import static ataxx.GameException.error;

/** Controls the play of the game.
 *  @author Anish Saha
 */

class Game {

    /** States of play. */
    static enum State {
        SETUP, PLAYING, FINISHED;
    }

    /** A new Game, using BOARD to play on, reading initially from
     *  BASESOURCE and using REPORTER for error and informational messages. */
    Game(Board board, CommandSource baseSource, Reporter reporter) {
        _inputs.addSource(baseSource);
        _board = board;
        _cleanBoard = new Board(_board);
        _reporter = reporter;
    }

    /** Run a session of Ataxx gaming.  Use an AtaxxGUI iff USEGUI. */
    void process(boolean useGUI) {
        _state = SETUP;
        redPlayer = new Manual(this, PieceColor.RED);
        bluePlayer = new AI(this, PieceColor.BLUE);

        GameLoop:
        while (true) {
            doClear(null);

            SetupLoop:
            while (_state == SETUP) {
                doCommand();
            }

            while (_state != SETUP && !_board.gameOver()) {
                Move move;
                if (_board.whoseMove() == PieceColor.RED) {
                    move = redPlayer.myMove();
                } else {
                    move = bluePlayer.myMove();
                }
                if (_state == PLAYING) {
                    if (_board.legalMove(move)) {
                        _board.makeMove(move);
                    } else {
                        System.out.println("Illegal move.");
                    }
                }
            }

            if (_state != SETUP) {
                reportWinner();
            }

            if (_state == PLAYING) {
                _state = FINISHED;
            }

            while (_state == FINISHED) {
                doCommand();
            }
        }

    }

    /** Return a view of my game board that should not be modified by
     *  the caller. */
    Board board() {
        return _board;
    }

    /** Perform the next command from our input source. */
    void doCommand() {
        try {
            Command cmnd =
                Command.parseCommand(_inputs.getLine("ataxx: "));
            _commands.get(cmnd.commandType()).accept(cmnd.operands());
        } catch (GameException excp) {
            _reporter.errMsg(excp.getMessage());
        }
    }

    /** Read and execute commands until encountering a move or until
     *  the game leaves playing state due to one of the commands. Return
     *  the terminating move command, or null if the game first drops out
     *  of playing mode. If appropriate to the current input source, use
     *  PROMPT to prompt for input. */
    Command getMoveCmnd(String prompt) {
        while (_state == PLAYING) {
            try {
                Command cmnd = Command.parseCommand(_inputs.getLine(prompt));
                if (cmnd.commandType() == PIECEMOVE) {
                    return cmnd;
                } else if (cmnd.commandType() == PASS) {
                    if (_board.canMove(_board.whoseMove())) {
                        _commands.get
                            (cmnd.commandType()).accept(cmnd.operands());
                    } else {
                        return cmnd;
                    }
                } else {
                    _commands.get(cmnd.commandType()).accept(cmnd.operands());
                }
            } catch (GameException excp) {
                _reporter.errMsg(excp.getMessage());
            }
        }
        return null;
    }

    /** Return random integer between 0 (inclusive) and MAX>0 (exclusive). */
    int nextRandom(int max) {
        return _randoms.nextInt(max);
    }

    /** Report a move, using a message formed from FORMAT and ARGS as
     *  for String.format. */
    void reportMove(String format, Object... args) {
        _reporter.moveMsg(format, args);
    }

    /** Report an error, using a message formed from FORMAT and ARGS as
     *  for String.format. */
    void reportError(String format, Object... args) {
        _reporter.errMsg(format, args);
    }

    /* Command Processors */

    /** Perform the command 'auto OPERANDS[0]'. */
    void doAuto(String[] operands) {
        checkState("auto", SETUP);
        if (operands[0].toLowerCase().equals("blue")) {
            bluePlayer = new AI(this, PieceColor.BLUE);
        }
        if (operands[0].toLowerCase().equals("red")) {
            redPlayer = new AI(this, PieceColor.RED);
        }
    }

    /** Perform a 'help' command. */
    void doHelp(String[] unused) {
        InputStream helpIn =
            Game.class.getClassLoader().getResourceAsStream("ataxx/help.txt");
        if (helpIn == null) {
            System.err.println("No help available.");
        } else {
            try {
                BufferedReader r
                    = new BufferedReader(new InputStreamReader(helpIn));
                while (true) {
                    String line = r.readLine();
                    if (line == null) {
                        break;
                    }
                    System.out.println(line);
                }
                r.close();
            } catch (IOException e) {
                /* Ignore IOException */
            }
        }
    }

    /** Perform the command 'load OPERANDS[0]'. */
    void doLoad(String[] operands) {
        checkState("load", SETUP, PLAYING, FINISHED);
        try {
            FileReader reader = new FileReader(operands[0]);
            BufferedReader b = new BufferedReader(reader);
            String s = b.readLine();
            while (s != null) {
                s.trim();
                Command cmnd = Command.parseCommand(s);
                if (cmnd.commandType() == ERROR) {
                    s = b.readLine();
                    continue;
                } else {
                    _commands.get(cmnd.commandType()).accept(cmnd.operands());
                    s = b.readLine();
                    continue;
                }
            }
        } catch (IOException e) {
            throw error("Cannot open file %s", operands[0]);
        }
    }

    /** Perform the command 'manual OPERANDS[0]'. */
    void doManual(String[] operands) {
        checkState("manual", SETUP);
        if (operands[0].toLowerCase().equals("blue")) {
            bluePlayer = new Manual(this, PieceColor.BLUE);
        }
        if (operands[0].toLowerCase().equals("red")) {
            redPlayer = new Manual(this, PieceColor.RED);
        }
    }

    /** Exit the program. */
    void doQuit(String[] unused) {
        System.exit(0);
    }

    /** Perform the command 'start'. */
    void doStart(String[] unused) {
        checkState("start", SETUP);
        _state = PLAYING;
    }

    /** Perform the move OPERANDS[0]. */
    void doMove(String[] operands) {
        checkState("", SETUP, PLAYING);
        if (_state == SETUP || _state == PLAYING) {
            try {
                _board.makeMove(operands[0].charAt(0), operands[1].charAt(0),
                    operands[2].charAt(0), operands[3].charAt(0));
            } catch (AssertionError e) {
                throw error("Illegal move.");
            }
        }
    }

    /** Cause current player to pass. */
    void doPass(String[] unused) {
        if (_board.legalMove(Move.pass())) {
            _board.makeMove(Move.pass());
        } else {
            System.out.println("Illegal pass.");
        }
    }

    /** Perform the command 'clear'. */
    void doClear(String[] unused) {
        _board = new Board(_cleanBoard);
        _state = SETUP;
    }

    /** Perform the command 'dump'. */
    void doDump(String[] unused) {
        System.out.println("===");
        for (char i = '7'; i >= '1'; i -= 1) {
            System.out.print("  ");
            for (char j = 'a'; j <= 'g'; j += 1) {
                PieceColor color = _board.get(j, i);
                if (color == RED) {
                    System.out.print("r ");
                } else if (color == BLUE) {
                    System.out.print("b ");
                } else if (color == BLOCKED) {
                    System.out.print("X ");
                } else {
                    System.out.print("- ");
                }
            }
            System.out.println("");
        }
        System.out.println("===");
    }

    /** Execute 'seed OPERANDS[0]' command, where the operand is a string
     *  of decimal digits. Silently substitutes another value if
     *  too large. */
    void doSeed(String[] operands) {
        checkState("seed", SETUP);
        long seed;
        try {
            seed = Long.parseLong(operands[0]);
        } catch (NumberFormatException e) {
            seed = Long.parseLong((new BigInteger
            (operands[0])).mod(BigInteger.valueOf(Long.MAX_VALUE)).toString());
        }
        _randoms.setSeed(seed);
    }

    /** Execute the command 'block OPERANDS[0]'. */
    void doBlock(String[] operands) {
        checkState("block", SETUP);
        if (_board.legalBlock(operands[0])) {
            _board.setBlock(operands[0]);
        } else {
            System.out.println("Illegal block placement.");
        }
    }

    /** Execute the artificial 'error' command. */
    void doError(String[] unused) {
        throw error("Command not understood");
    }

    /** Report the outcome of the current game. */
    void reportWinner() {
        String msg = "";
        if (_board.gameOver()) {
            if (_board.bluePieces() > _board.redPieces()) {
                msg = "Blue wins.";
            } else if (_board.redPieces() > _board.bluePieces()) {
                msg = "Red wins.";
            } else {
                msg = "Draw.";
            }
        }
        _reporter.outcomeMsg(msg);
    }

    /** Check that game is currently in one of the states STATES, assuming
     *  CMND is the command to be executed. */
    private void checkState(Command cmnd, State... states) {
        for (State s : states) {
            if (s == _state) {
                return;
            }
        }
        throw error("'%s' command is not allowed now.", cmnd.commandType());
    }

    /** Check that game is currently in one of the states STATES, using
     *  CMND in error messages as the name of the command to be executed. */
    private void checkState(String cmnd, State... states) {
        for (State s : states) {
            if (s == _state) {
                return;
            }
        }
        throw error("'%s' command is not allowed now.", cmnd);
    }

    /** Mapping of command types to methods that process them. */
    private final HashMap<Command.Type, Consumer<String[]>> _commands =
        new HashMap<>();

    {
        _commands.put(AUTO, this::doAuto);
        _commands.put(BLOCK, this::doBlock);
        _commands.put(CLEAR, this::doClear);
        _commands.put(DUMP, this::doDump);
        _commands.put(HELP, this::doHelp);
        _commands.put(MANUAL, this::doManual);
        _commands.put(PASS, this::doPass);
        _commands.put(PIECEMOVE, this::doMove);
        _commands.put(SEED, this::doSeed);
        _commands.put(START, this::doStart);
        _commands.put(LOAD, this::doLoad);
        _commands.put(QUIT, this::doQuit);
        _commands.put(ERROR, this::doError);
        _commands.put(EOF, this::doQuit);
    }

    /** Input source. */
    private final CommandSources _inputs = new CommandSources();
    /** My board. */
    private Board _board;
    /** A clean board, at start. */
    private Board _cleanBoard;
    /** Current game state. */
    private State _state;
    /** Used to send messages to the user. */
    private Reporter _reporter;
    /** Source of pseudo-random numbers (used by AIs). */
    private Random _randoms = new Random();
    /** Helps keep track of current player. This indicates blue player. */
    private Player bluePlayer;
    /** Helps keep track of current player. This indicates red player. */
    private Player redPlayer;
}
