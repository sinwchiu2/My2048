package My2048;

/**
 *
 * @author waiwaichiu
 */
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Random;

public class GameBoard {

    enum Direction {

        UP, DOWN, LEFT, RIGHT
    }
    //4x4 initialization
    public static final int ROWS = 4;
    public static final int COLS = 4;
    //start with 2 tiles
    private final int startingTiles = 2;
    private Tile[][] board;
    private boolean lose, isWin;
    private BufferedImage gameBoard, finalBoard;
    private int x, y;
    private int score = 0;
    private int bestScore = 0;
    private Font scoreFont;

    private static int SPACING = 10;
    public static int BOARD_WIDTH = (COLS + 1) * SPACING + COLS * Tile.WIDTH;
    public static int BOARD_HEIGHT = (ROWS + 1) * SPACING + ROWS * Tile.HEIGHT;

    private long elapsedMS, fastestMS, startTime;
    private boolean hasStarted;
    private String formattedTime = "00:00:000";

    // Saving
    private String saveDataPath;
    private String fileName = "SaveData";

    //private AudioHandler audio;
    public GameBoard(int x, int y) {
        try {
            saveDataPath = GameBoard.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            saveDataPath = System.getProperty("user.home") + "\\Desktop";
        } catch (Exception e) {
            e.printStackTrace();
        }

        scoreFont = Game.main.deriveFont(28f);
        this.x = x;
        this.y = y;
        board = new Tile[ROWS][COLS];
        gameBoard = new BufferedImage(BOARD_WIDTH, BOARD_HEIGHT, BufferedImage.TYPE_INT_RGB);
        finalBoard = new BufferedImage(BOARD_WIDTH, BOARD_HEIGHT, BufferedImage.TYPE_INT_RGB);
        startTime = System.nanoTime();

        loadBestScore();
        createBoardImage();
        start();
        /*
         audio = AudioHandler.getInstance();
         audio.loadSound("MyLove.mp3", "BGM");
         audio.loadSound("click.wav", "click");
         audio.adjustVolume("BGM", -10);
         audio.playSound("BGM", Clip.LOOP_CONTINUOUSLY);
         */
    }

    private void createSaveData() {
        try {
            File savefile = new File(saveDataPath, fileName);

            FileWriter outFile = new FileWriter(savefile);
            BufferedWriter writer = new BufferedWriter(outFile);
            writer.write("" + 0);
            writer.newLine();
            writer.write("" + Integer.MAX_VALUE);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadBestScore() {
        try {
            File inFile = new File(saveDataPath, fileName);
            if (!inFile.isFile()) {
                createSaveData();
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inFile)));
            bestScore = Integer.parseInt(reader.readLine());
            fastestMS = Long.parseLong(reader.readLine());
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setBestScore() {
        FileWriter output = null;

        try {
            File f = new File(saveDataPath, fileName);
            output = new FileWriter(f);
            BufferedWriter writer = new BufferedWriter(output);

            writer.write("" + bestScore);
            writer.newLine();

            if (elapsedMS <= fastestMS && isWin) {
                writer.write("" + elapsedMS);
            } else {
                writer.write("" + fastestMS);
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createBoardImage() {
        Graphics2D g = (Graphics2D) gameBoard.getGraphics();
        //spacing color
        g.setColor(new Color(0x8B7D6B));
        g.fillRect(0, 0, BOARD_WIDTH, BOARD_HEIGHT);
        //grid color
        g.setColor(new Color(0xCDC0B0));

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int x = SPACING + SPACING * col + Tile.WIDTH * col;
                int y = SPACING + SPACING * row + Tile.HEIGHT * row;
                g.fillRoundRect(x, y, Tile.WIDTH, Tile.HEIGHT, Tile.ARC_WIDTH, Tile.ARC_HEIGHT);
            }
        }
    }

    private void start() {
        for (int i = 0; i < startingTiles; i++) {
            spawnRandom();
        }
    }

    private void spawnRandom() {
        Random random = new Random();
        boolean notValid = true;

        while (notValid) {
            int location = random.nextInt(ROWS * COLS);
            int row = location / ROWS;
            int col = location % COLS;
            Tile current = board[row][col];
            if (current == null) {
                int value = random.nextInt(10) < 9 ? 2 : 4;
                Tile tile = new Tile(value, getTileX(col), getTileY(row));
                board[row][col] = tile;
                notValid = false;
            }
        }
    }

    public int getTileX(int col) {
        return SPACING + col * Tile.WIDTH + col * SPACING;
    }

    public int getTileY(int row) {
        return SPACING + row * Tile.HEIGHT + row * SPACING;
    }

    public void render(Graphics2D g) {
        Graphics2D g2d = (Graphics2D) finalBoard.getGraphics();
        g2d.drawImage(gameBoard, 0, 0, null);

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Tile current = board[row][col];
                if (current == null) {
                    continue;
                }
                current.render(g2d);
            }
        }

        g.drawImage(finalBoard, x, y, null);
        g2d.dispose();

        g.setColor(Color.lightGray);
        g.setFont(scoreFont);
        g.drawString("" + score, 30, 40);
        g.setColor(Color.red);
        g.drawString("Best: " + bestScore, Game.WIDTH - DrawUtils.getMessageWidth("BestScore: " + bestScore, scoreFont, g) - 20, 40);
        g.setColor(Color.black);
        g.drawString("Time: " + formattedTime, 30, 90);
    }

    public void update() {
        if (!isWin && !lose) {
            if (hasStarted) {
                elapsedMS = (System.nanoTime() - startTime) / 1000000;
                formattedTime = formatTime(elapsedMS);
            } else {
                startTime = System.nanoTime();
            }
        }

        checkKeys();

        if (score >= bestScore) {
            bestScore = score;
        }

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Tile current = board[row][col];
                if (current == null) {
                    continue;
                }
                current.update();
                resetPosition(current, row, col);
                if (current.getValue() == 2048) {
                    isWin = true;
                }
            }
        }
    }

    private String formatTime(long time) {
        String formattedTime;

        String hourFormat = "";
        int hours = (int) (time / 3600000);
        if (hours >= 1) {
            time -= hours * 3600000;
            if (hours < 10) {
                hourFormat = "0" + hours;
            } else {
                hourFormat = "" + hours;
            }
            hourFormat += ":";
        }

        String minuteFormat;
        int minutes = (int) (time / 60000);
        if (minutes >= 1) {
            time -= minutes * 60000;
            if (minutes < 10) {
                minuteFormat = "0" + minutes;
            } else {
                minuteFormat = "" + minutes;
            }
        } else {
            minuteFormat = "00";
        }

        String secondFormat;
        int seconds = (int) (time / 1000);
        if (seconds >= 1) {
            time -= seconds * 1000;
            if (seconds < 10) {
                secondFormat = "0" + seconds;
            } else {
                secondFormat = "" + seconds;
            }
        } else {
            secondFormat = "00";
        }

        String milliFormat;
        if (time > 99) {
            milliFormat = "" + time;
        } else if (time > 9) {
            milliFormat = "0" + time;
        } else {
            milliFormat = "00" + time;
        }

        formattedTime = hourFormat + minuteFormat + ":" + secondFormat + ":" + milliFormat;
        return formattedTime;
    }

    private void resetPosition(Tile current, int row, int col) {
        if (current == null) {
            return;
        }

        int x = getTileX(col);
        int y = getTileY(row);

        int distX = current.getX() - x;
        int distY = current.getY() - y;

        if (Math.abs(distX) < Tile.SLIDE_SPEED) {
            current.setX(current.getX() - distX);
        }

        if (Math.abs(distY) < Tile.SLIDE_SPEED) {
            current.setY(current.getY() - distY);
        }

        if (distX < 0) {
            current.setX(current.getX() + Tile.SLIDE_SPEED);
        }
        if (distY < 0) {
            current.setY(current.getY() + Tile.SLIDE_SPEED);
        }
        if (distX > 0) {
            current.setX(current.getX() - Tile.SLIDE_SPEED);
        }
        if (distY > 0) {
            current.setY(current.getY() - Tile.SLIDE_SPEED);
        }
    }

    private boolean move(int row, int col, int horizontalDirection, int verticalDirection, Direction dir) {
        boolean canMove = false;

        Tile current = board[row][col];
        if (current == null) {
            return false;
        }
        boolean move = true;
        int newCol = col;
        int newRow = row;
        while (move) {
            newCol += horizontalDirection;
            newRow += verticalDirection;
            if (checkOutOfBounds(dir, newRow, newCol)) {
                break;
            }
            if (board[newRow][newCol] == null) {
                board[newRow][newCol] = current;
                board[newRow - verticalDirection][newCol - horizontalDirection] = null;
                board[newRow][newCol].setSlideTo(new Point(newRow, newCol));
                canMove = true;
            } else if (board[newRow][newCol].getValue() == current.getValue() && board[newRow][newCol].canCombine()) {
                board[newRow][newCol].setCanCombine(false);
                board[newRow][newCol].setValue(board[newRow][newCol].getValue() * 2);
                canMove = true;
                board[newRow - verticalDirection][newCol - horizontalDirection] = null;
                board[newRow][newCol].setSlideTo(new Point(newRow, newCol));
                board[newRow][newCol].setCombineAnimation(true);
                score += board[newRow][newCol].getValue();
            } else {
                move = false;
            }
        }
        return canMove;
    }

    //checking for out of bounds, if the bounding direction is full, return false

    private boolean checkOutOfBounds(Direction dir, int row, int col) {
        if (dir == Direction.LEFT) {
            return col < 0;
        } else if (dir == Direction.RIGHT) {
            return col > COLS - 1;
        } else if (dir == Direction.UP) {
            return row < 0;
        } else if (dir == Direction.DOWN) {
            return row > ROWS - 1;
        }
        return false;
    }

    private void moveTiles(Direction dir) {
        boolean canMove = false;
        int horizontalDirection = 0;
        int verticalDirection = 0;

        if (dir == Direction.LEFT) {
            horizontalDirection = -1;
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
                    if (!canMove) {
                        canMove = move(row, col, horizontalDirection, verticalDirection, dir);
                    } else {
                        move(row, col, horizontalDirection, verticalDirection, dir);
                    }
                }
            }
        } else if (dir == Direction.RIGHT) {
            horizontalDirection = 1;
            for (int row = 0; row < ROWS; row++) {
                for (int col = COLS - 1; col >= 0; col--) {
                    if (!canMove) {
                        canMove = move(row, col, horizontalDirection, verticalDirection, dir);
                    } else {
                        move(row, col, horizontalDirection, verticalDirection, dir);
                    }
                }
            }
        } else if (dir == Direction.UP) {
            verticalDirection = -1;
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
                    if (!canMove) {
                        canMove = move(row, col, horizontalDirection, verticalDirection, dir);
                    } else {
                        move(row, col, horizontalDirection, verticalDirection, dir);
                    }
                }
            }
        } else if (dir == Direction.DOWN) {
            verticalDirection = 1;
            for (int row = ROWS - 1; row >= 0; row--) {
                for (int col = 0; col < COLS; col++) {
                    if (!canMove) {
                        canMove = move(row, col, horizontalDirection, verticalDirection, dir);
                    } else {
                        move(row, col, horizontalDirection, verticalDirection, dir);
                    }
                }
            }
        } else {
            System.out.println(dir + " is not a valid direction.");
        }

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Tile current = board[row][col];
                if (current == null) {
                    continue;
                }
                current.setCanCombine(true);
            }
        }

        if (canMove) {
            //audio.playSound("click", 0);
            spawnRandom();
            checkDead();
        }
    }

    private void checkDead() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (board[row][col] == null) {
                    return;
                }
                if (checkSurroundingTiles(row, col, board[row][col])) {
                    return;
                }
            }
        }

        lose = true;
        if (score >= bestScore) {
            bestScore = score;
        }
        setBestScore();
    }

    private boolean checkSurroundingTiles(int row, int col, Tile current) {
        if (row > 0) {
            Tile check = board[row - 1][col];
            if (check == null) {
                return true;
            }
            if (current.getValue() == check.getValue()) {
                return true;
            }
        }
        if (row < ROWS - 1) {
            Tile check = board[row + 1][col];
            if (check == null) {
                return true;
            }
            if (current.getValue() == check.getValue()) {
                return true;
            }
        }
        if (col > 0) {
            Tile check = board[row][col - 1];
            if (check == null) {
                return true;
            }
            if (current.getValue() == check.getValue()) {
                return true;
            }
        }
        if (col < COLS - 1) {
            Tile check = board[row][col + 1];
            if (check == null) {
                return true;
            }
            if (current.getValue() == check.getValue()) {
                return true;
            }
        }
        return false;
    }
    //chceking for start of the game if any arrow key is pressed
    private void checkKeys() {
        if (Keyboard.typed(KeyEvent.VK_LEFT)) {
            moveTiles(Direction.LEFT);
            if (!hasStarted) {
                hasStarted = true;
            }
        }
        if (Keyboard.typed(KeyEvent.VK_RIGHT)) {
            moveTiles(Direction.RIGHT);
            if (!hasStarted) {
                hasStarted = true;
            }
        }
        if (Keyboard.typed(KeyEvent.VK_UP)) {
            moveTiles(Direction.UP);
            if (!hasStarted) {
                hasStarted = true;
            }
        }
        if (Keyboard.typed(KeyEvent.VK_DOWN)) {
            moveTiles(Direction.DOWN);
            if (!hasStarted) {
                hasStarted = true;
            }
        }
    }
}
