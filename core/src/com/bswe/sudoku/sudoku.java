package com.bswe.sudoku;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlReader.Element;
import com.badlogic.gdx.utils.XmlWriter;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.math.Vector2;

import static com.badlogic.gdx.Application.ApplicationType.Desktop;

import org.jasypt.util.password.StrongPasswordEncryptor;

import java.lang.Exception;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.Vector;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

import org.jasypt.util.text.BasicTextEncryptor;
import org.lwjgl.openal.AL;


class Line extends Actor {
    private ShapeRenderer sr;
    private Vector2 start, end;

    Line(float width, float weight, Color color, Vector2 Start, Vector2 End){
        setSize(width, weight);
        setColor(color);
        start = new Vector2(Start);
        end = new Vector2(End);
        //System.out.printf("Line: S=%f,%f   E=%f,%f\n", start.x, start.y, end.x, end.y);
        sr = new ShapeRenderer();
        sr.setAutoShapeType(true);
        }


    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        batch.end();

        Color color = new Color(getColor());
        sr.setProjectionMatrix(batch.getProjectionMatrix());
        sr.setColor(color.r, color.g, color.b, color.a * parentAlpha);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.line(start.x, start.y, end.x, end.y);
        sr.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        Gdx.gl.glLineWidth(1f);
        sr.setColor(Color.WHITE);

        batch.begin();
        }
    }


class Grid extends Actor {
    private ShapeRenderer sr;
    private int numberOfRows, numberOfColumns, cellSize, lineWidth;

    Grid(int LineWidth, Color color, int Rows, int Columns, int CellSize){
        lineWidth = LineWidth;
        setColor(color);
        numberOfRows = Rows;
        numberOfColumns = Columns;
        cellSize = CellSize;
        //System.out.printf("Line: S=%f,%f   E=%f,%f\n", start.x, start.y, end.x, end.y);
        sr = new ShapeRenderer();
        sr.setAutoShapeType(true);
        }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        int height = numberOfRows * cellSize;
        int width = numberOfColumns * cellSize;
        float x = getX();
        float y = getY();
        super.draw(batch, parentAlpha);
        batch.end();

        Color color = new Color(getColor());
        sr.setProjectionMatrix(batch.getProjectionMatrix());
        sr.setColor(color.r, color.g, color.b, color.a * parentAlpha);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        sr.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl20.glLineWidth(lineWidth);
        for (int i=0; i <= numberOfRows; i++)
            if ((i == 3) || (i == 6)) {
                sr.rectLine(x, y + (i * cellSize), x + width, y + (i * cellSize), lineWidth);
                }
            else
                sr.line(x, y + (i * cellSize), x + width, y + (i * cellSize));
        for (int i=0; i <= numberOfColumns; i++)
            if ((i == 3) || (i == 6)) {
                sr.rectLine(x + (i * cellSize), y, x + (i * cellSize), y + height, lineWidth);
            }
            else
                sr.line(x + (i * cellSize), y, x + (i * cellSize), y + height);
        sr.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        Gdx.gl.glLineWidth(1f);
        sr.setColor(Color.WHITE);

        batch.begin();
        }
    }


class cell {
    int rowIndex, columnIndex, containingBlock, size, value = -1;   // set to -1 so first call to setValue(0) runs
    String name;
    Vector row, column, block;
    Vector<Integer> possibleValues = new Vector();
    Label label;
    LabelStyle defaultStyle;
    int originalValue;
    boolean locked = false;
    boolean reserved = false;
    boolean debugMode = false;

    cell(int rowIndex, int columnIndex, int containingBlock, Vector row, Vector column, Vector block, Label l) {
        this.rowIndex = rowIndex;
        this.columnIndex = columnIndex;
        this.containingBlock = containingBlock;
        this.row = row;
        this.column = column;
        this.block = block;
        this.size = 44;     // TODO: make this a constant and add method to change size
        name = Integer.toString(rowIndex) + "," + Integer.toString(columnIndex);
        label = l;
        defaultStyle = l.getStyle();
        l.setWrap(true);
        setValue(0);
        }

    public void setValue(int newValue) {
        if (newValue == 0) {
            // if new value is 0 (effective reset) unlock cell
            locked = false;
            reserved = false;
            label.setStyle(defaultStyle);
            label.setFontScale(1f);
            debugMode = false;
        }
        if (locked)
            return;
        possibleValues.removeAllElements();
        row.removeElement(Math.abs(value));
        column.removeElement(Math.abs(value));
        block.removeElement(Math.abs(value));
        value = newValue;
        if (value != 0) {
            label.setFontScale(1.5f);
            row.add(Math.abs(value));
            column.add(Math.abs(value));
            block.add(Math.abs(value));
            }
        if (value > 0)
            label.setText(Integer.toString(value));
        else {
            originalValue = value;   // 0 or puzzle answer values (for eraser method)
            if (debugMode) {    // show value for debug mode
                if (value < 0)
                    label.setText(Integer.toString(value));
                }
            else
                label.setText("");   // normally show nothing for empty cells and hidden puzzle answer values
            }
        //System.out.printf("row=%s\ncolumn=%s\nblock=%s\n", row.toString(), column.toString(), block.toString());
        }

    public boolean canSetValue (int value) {
        if (this.value != 0) return false;
        if (row.contains(value)) return false;
        if (column.contains(value)) return false;
        if (block.contains(value)) return false;
        return true;
        }

    public String getName() { return name; }

    public int getValue() {
        return value;
        }

    public int getSize() {
        return size;
        }

    public void lock() {
        locked = true;
        }

    public boolean isLocked() {
        return locked;
        }

    public void reserve() {
        reserved = true;
        }

    public boolean isReserved() {
        return reserved;
        }

    public void setStyle(LabelStyle style) { label.setStyle(style); }

    public void setDebugMode(boolean mode) { debugMode = mode; }

    public void unsetStyle() {
        label.setStyle(defaultStyle);
        }

    public void addPossibleValue(int value) {
        possibleValues.addElement(value);
        if (! debugMode) return;
        String s = new String();
        for (int i=0; i < possibleValues.size(); i++)
            s += Integer.toString(possibleValues.get(i)) + " ";
        label.setText(s);
        }

    public void removePossibleValue(int value) {
        possibleValues.removeElement(value);
        if ((! debugMode) || (possibleValues.size() == 0)) return;
        String s = new String();
        for (int i=0; i < possibleValues.size(); i++)
            s += Integer.toString(possibleValues.get(i)) + " ";
        label.setText(s);
    }

    public void eraseValue() {
        if (locked)
            return;
        value = originalValue;
        label.setText("");
        }
    }

class cellVectors {
    Vector[] rows, columns, blocks;

    cellVectors() {
        rows = new Vector[] {new Vector(), new Vector(), new Vector(),
                             new Vector(), new Vector(), new Vector(),
                             new Vector(), new Vector(), new Vector()};
        columns = new Vector[] {new Vector(), new Vector(), new Vector(),
                                new Vector(), new Vector(), new Vector(),
                                new Vector(), new Vector(), new Vector()};
        blocks = new Vector[] {new Vector(), new Vector(), new Vector(),
                               new Vector(), new Vector(), new Vector(),
                               new Vector(), new Vector(), new Vector()};
        }
    }


class valueVectors {
    Vector[] rows, columns, blocks;

    valueVectors() {
        rows = new Vector[] {new Vector(), new Vector(), new Vector(),
                new Vector(), new Vector(), new Vector(),
                new Vector(), new Vector(), new Vector()};
        columns = new Vector[] {new Vector(), new Vector(), new Vector(),
                new Vector(), new Vector(), new Vector(),
                new Vector(), new Vector(), new Vector()};
        blocks = new Vector[] {new Vector(), new Vector(), new Vector(),
                new Vector(), new Vector(), new Vector(),
                new Vector(), new Vector(), new Vector()};
        }
    }


// Main application class
public class sudoku extends ApplicationAdapter {
    // screen size that seems to work on both desktop and Moto Z Force phone well
	public static final int SCREEN_WIDTH = 400;
	public static final int SCREEN_HEIGHT = 660;

    // the keys used to persist these values
	private static final String PASSWORD_KEY = "1";
	private static final String NUMBER_OF_ACCOUNTS_KEY = "2";

    // the length of time of user inactivity before the logout watchdog fires
    private static final float INACTIVITY_DURATION = 60f;    // in seconds

	private enum AppStates {PW_REQUIRED,    // initial app startup state waiting for password entry
                            PW_PASSED,      // startup state after correct password entry
                            INITIALIZED,    // startup state after accounts screen is initialized
                            LOGGED_OUT}     // state after initialization but while logged out

	private static final String TAG = sudoku.class.getName();   // used for debug logging

	private String inputPassword = "";

    // used for user input from all dialog boxes except login dialog
    private TextField firstTextField;
	private TextField secondTextField;
	private TextField thirdTextField;

	private AppStates appState = AppStates.PW_PASSED;   // init to startup state

	private Pixmap pixmap;
	private Skin skin;
    private Stage stage;        // main accounts displaying stage
    private Stage loginStage;   // for hiding the accounts stage while waiting for password input

	private Table scrollTable;
	private Label errorText;

	private StrongPasswordEncryptor passwordEncryptor = new StrongPasswordEncryptor();

	private BasicTextEncryptor textEncryptor;

	private Preferences prefs;

    private float elapsedTimeInSeconds = 0;     // for logout watchdog

    private InputMultiplexer inputMultiplexer;

    private SystemAccess systemAccess;          // to access platform clipboards & permissions

    private valueVectors values = new valueVectors();

    private cellVectors cells = new cellVectors();

    private cell[][] board = new cell[9][9];

    private List<Integer> numbers = Arrays.asList(new Integer[] {1, 2, 3, 4, 5, 6, 7, 8, 9});

    private cell selectedCell = null;

    private Vector rowPermutations = new Vector();

    LabelStyle style1, white, gray, selected, clue;

    TextButton lastNumberClicked = null;

    private enum GameModes {CREATE_PUZZLE,
                            PLAY_PUZZLE,
                            ANALYZE_PUZZLE,
                            EDIT_PUZZLE};

    private GameModes mode = GameModes.EDIT_PUZZLE;

    private boolean debugMode = false;

    private boolean singleStep = false;

    // these variables are global to enable debug mode single stepping
    private int numberOfEmptyCells;
    private boolean foundOne;
    private int row, column;


    public sudoku (SystemAccess sa) {
        super();
        systemAccess = sa;
        }


	@Override
	public void create () {
        Pixmap backGround;
        //Gdx.app.log (TAG, "create: application class = " + Gdx.app.getClass().getName());

        // init preferences object for persistent storage
		//prefs = Gdx.app.getPreferences ("bswe-pwallet");

		// init stages to display
        skin = new Skin (Gdx.files.internal ("clean-crispy-ui.json"));
        stage = new Stage();
        stage.setViewport (new StretchViewport (SCREEN_WIDTH, SCREEN_HEIGHT, new OrthographicCamera()));
        //loginStage = new Stage();
        //loginStage.setViewport (new StretchViewport (SCREEN_WIDTH, SCREEN_HEIGHT, new OrthographicCamera()));

        // use input multiplexer to detect keyboard activity for resetting the inactivity watchdog to
        // keep it from firing while user enters dialog items
        inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor (new MyInputProcessor());  // to detect keyboard activity
        inputMultiplexer.addProcessor (stage);

        // create label styles for different color backgrounds for cells
        Label temp = new Label("", skin);
        backGround = new Pixmap(44, 44, Pixmap.Format.RGB888);
        backGround.setColor(new Color(.8f, .8f, .8f, 1));
        backGround.fill();
        style1 = new LabelStyle(temp.getStyle());
        style1.background = new Image(new Texture(backGround)).getDrawable();

        backGround = new Pixmap(44, 44, Pixmap.Format.RGB888);
        backGround.setColor(new Color(1, 1, 1, 1));
        backGround.fill();
        white = new LabelStyle(temp.getStyle());
        white.background = new Image(new Texture(backGround)).getDrawable();

        backGround = new Pixmap(44, 44, Pixmap.Format.RGB888);
        backGround.setColor(new Color(.8f, .8f, .8f, 1));
        backGround.fill();
        gray = new LabelStyle(temp.getStyle());
        gray.background = new Image(new Texture(backGround)).getDrawable();

        backGround = new Pixmap(44, 44, Pixmap.Format.RGB888);
        backGround.setColor(new Color(.85f, 1, .85f, 1));
        backGround.fill();
        selected = new LabelStyle(temp.getStyle());
        selected.background = new Image(new Texture(backGround)).getDrawable();

        backGround = new Pixmap(44, 44, Pixmap.Format.RGB888);
        backGround.setColor(new Color(.85f, .75f, .5f, 1));
        backGround.fill();
        clue = new LabelStyle(temp.getStyle());
        clue.background = new Image(new Texture(backGround)).getDrawable();
		}


    @Override
	public void render () {
		Gdx.gl.glClearColor (0, 0, 0, 1);
		Gdx.gl.glClear (GL20.GL_COLOR_BUFFER_BIT);

		switch (appState) {
            case PW_REQUIRED:
            case LOGGED_OUT:
                // logged out, so display login stage
                loginStage.getViewport().apply();
                loginStage.act(Gdx.graphics.getDeltaTime());
                loginStage.draw();
                break;
            case PW_PASSED:
                Initialize();
                // intentionally drop thru to INITIALIZED case below
            case INITIALIZED:
                // check fo inactivity to potentially log the user out
                //if (InactivityWatchdogFired())
                //    return;
                // still logged in, so display the app's main stage
                stage.getViewport().apply();
                stage.act(Gdx.graphics.getDeltaTime());
                stage.draw();
            }
		}


	@Override
	public void resize (int width, int height) {
		//Gdx.app.log (TAG, "resize: w=" + width + ", h=" + height);
        stage.getViewport().update (width, height, true);
        //loginStage.getViewport().update (width, height, true);
		}


	@Override
	public void dispose () {
		//Gdx.app.log (TAG, "dispose:");
		try {
			if (stage != null) stage.dispose();
			if (loginStage != null ) loginStage.dispose();
			if (skin != null) skin.dispose();
			if (pixmap != null) pixmap.dispose();
			// try to exit with code 0
			if (Gdx.app.getType() == Desktop)
				AL.destroy();
			}
		catch (Exception e) {
			Gdx.app.log (TAG, "dispose: exception raised - " + e.getLocalizedMessage());
			}
		System.exit(0);
		}


    @Override
    public void pause () {
        //Gdx.app.log (TAG, "pause:");
        //LogoutUser();       // hide the accounts screen by logging user out
        }


	private Boolean InactivityWatchdogFired() {
		// log user out if they haven't interacted with app in INACTIVITY_DURATION number of seconds
		if (Gdx.input.justTouched())
			elapsedTimeInSeconds = 0;
		elapsedTimeInSeconds += Gdx.graphics.getRawDeltaTime();
		if (elapsedTimeInSeconds > INACTIVITY_DURATION) {
			//Gdx.app.log (TAG, "InactivityWatchdogFired: inactivity watchdog fired, logging user out");
			//LogoutUser();
			return true;    // watchdog fired
		    }
		return false;   // watchdog didn't fire
	    }


    private void findPermutations(Vector row, Vector<Integer> numbers) {
        // recursively construct all permutations of an ordered set of n numbers
        if (numbers.size() == 1) {
            // found a complete set, so save it
            row.addElement(numbers.get(0));
            rowPermutations.addElement(row);
            }
        else {
            // iterate thru the rest of all the possible numbers left
            int size = numbers.size();
            for (int i=0; i < size; i++) {
                // remove the number being used from the numbers left
                int n = numbers.remove(i);
                // use the number removed above by adding it to the set
                row.add(n);
                // find all possible permutations
                findPermutations(row, numbers);
                // remove the number just used from the set
                row.remove(row.size()-1);
                // restore the number just used back to the numbers left
                numbers.add(i, n);
                }
            }
        }


    private void createPuzzle() {
        mode = GameModes.CREATE_PUZZLE;
        }


    private void clearPuzzle() {
        mode = GameModes.EDIT_PUZZLE;
        // clear the board by resetting board values to -1
        for (int i = 0; i < 9; i++)
            for (int j = 0; j < 9; j++)
                board[i][j].setValue(0);
        }


    private void showHidenValues() {
        for (int i = 0; i < 9; i++)
            for (int j = 0; j < 9; j++)
                board[i][j].label.setText(Integer.toString(board[i][j].getValue()));

    }


    private void removeFromPossibleValues(cell c, int value) {
        Vector v;
        c.removePossibleValue(value);
        v = cells.rows[c.rowIndex-1];
        for (int i=0; i < 9; i++)
            ((cell)v.get(i)).removePossibleValue(value);
        v = cells.columns[c.columnIndex-1];
        for (int i=0; i < 9; i++)
            ((cell)v.get(i)).removePossibleValue(value);
        v = cells.blocks[c.containingBlock];
        for (int i=0; i < 9; i++)
            ((cell)v.get(i)).removePossibleValue(value);
        }


    private void removeFromPossibleValues(Vector<cell> v, Vector<cell> c, int value) {

        for (int i=0; i < v.size(); i++)
            if (! c.contains(v.get(i))) {
                //System.out.printf("removing %d from %s\n", value, v.get(i).getName());
                v.get(i).removePossibleValue(value);
                }
        }


    private void checkForReservedValues(cell c) {
        if ((c.isReserved()) || (c.possibleValues.size() != 2)) return;
        Vector block = cells.blocks[c.containingBlock];
        for (int i = 0; i < 9; i++) {
            cell secondCell = ((cell) block.get(i));
            if (secondCell == c) continue;           // skip cell that's under investigation
            if (c.possibleValues.equals(secondCell.possibleValues)) {
                c.reserve();
                secondCell.reserve();
                if (c.rowIndex == secondCell.rowIndex) {
                    Vector v = cells.rows[c.rowIndex - 1];
                    for (int j = 0; j < 9; j++) {
                        cell otherCell = ((cell) v.get(j));
                        if ((c == otherCell) || (secondCell == otherCell))
                            continue;  // skip known cells
                        //System.out.printf("c=%s, sc=%s, oc=%s\n", c.getName(), secondCell.getName(), otherCell.getName());
                        //System.out.printf("c.pv=%s\n", c.possibleValues);
                        //System.out.printf("oc.pv=%s\n", otherCell.possibleValues);
                        otherCell.possibleValues.removeElement(c.possibleValues.get(0));
                        otherCell.possibleValues.removeElement(c.possibleValues.get(1));
                        //System.out.printf("oc.pv=%s\n", otherCell.possibleValues);
                    }
                } else if (c.columnIndex == secondCell.columnIndex) {
                    Vector v = cells.columns[c.columnIndex - 1];
                    for (int j = 0; j < 9; j++) {
                        cell otherCell = ((cell) v.get(j));
                        if ((c == otherCell) || (secondCell == otherCell))
                            continue;  // skip known cells
                        //System.out.printf("c=%s, sc=%s, oc=%s\n", c.getName(), secondCell.getName(), otherCell.getName());
                        //System.out.printf("c.pv=%s\n", c.possibleValues);
                        //System.out.printf("oc.pv=%s\n", otherCell.possibleValues);
                        otherCell.possibleValues.removeElement(c.possibleValues.get(0));
                        otherCell.possibleValues.removeElement(c.possibleValues.get(1));
                        //System.out.printf("oc.pv=%s\n", otherCell.possibleValues);
                        }
                    }
                }
            }
        }


    private Vector<cell> findCellsWithSamePossibleValue(Vector<cell> v, cell c, int value) {
        Vector<cell> cells = new Vector<cell>();
        for (int k = 0; k < 9; k++) {
            cell otherCell = v.get(k);
            if (otherCell == c) continue;  // skip the original cell that's under investigation
            if (otherCell.possibleValues.contains(value))
                cells.add(otherCell);
            if (cells.size() > 2)
                break;
            }
        return cells;
        }


    private void checkForTwoVectorCondition(Vector<cell> v, int value, boolean vectorIsBlock) {
        boolean conditionFound;

        if (vectorIsBlock) {
            // check for all cells belonging to the same row
            conditionFound = true;
            for (int i=0; i < v.size()-1; i++)
                if (v.get(i).row != v.get(i+1).row) {
                    conditionFound = false;
                    break;
                    }
            if (conditionFound)  {
                // remove the value from all the other cells in the row
                removeFromPossibleValues(cells.rows[v.get(0).rowIndex-1], v, value);
                return;
                }
            // check for all cells belonging to the same column
            conditionFound = true;
            for (int i=0; i < v.size()-1; i++)
                if (v.get(i).column != v.get(i+1).column) {
                    conditionFound = false;
                    break;
                    }
            if (conditionFound)  {
                // remove the value from all the other cells in the column
                removeFromPossibleValues(cells.columns[v.get(0).columnIndex-1], v, value);
                return;
                }
            }
        else {
            // check for all cells belonging to the same block
            conditionFound = true;
            for (int i=0; i < v.size()-1; i++)
                if (v.get(i).containingBlock != v.get(i+1).containingBlock) {
                    conditionFound = false;
                    break;
                    }
            if (conditionFound)  {
                // remove the value from all the other cells in the block
                removeFromPossibleValues(cells.blocks[v.get(0).containingBlock], v, value);
                }
            }
        }


    private void lookForNextValue() {
        cell c;
        Vector<cell> otherCells;
        int loopCount = 0;

        // look for next cell that still needs to be set and quit if one is found
        foundOne = false;
        while (loopCount++ <= 81) {    // check all cells only once per call
            if (++column == 9) {
                if (++row == 9)
                    row = 0;
                column = 0;
                }
            if (board[row][column].getValue() == 0) {  // cell is empty, so look for possible 'known' value
                c = board[row][column];
                if (c.possibleValues.size() == 1) {     // check to see if value is 'known'
                    int v = (Integer) c.possibleValues.get(0);
                    c.setValue(-1 * v);
                    removeFromPossibleValues(c, v);
                    //System.out.printf("size=1: set cell %d,%d to %d\n", i, j, c.getValue());
                    foundOne = true;
                    return;
                }
                for (int v = 0; v < c.possibleValues.size(); v++) {   // iterating thru all possible values for cell
                    int value = c.possibleValues.get(v);
                    // search cell's block for any other cells that work for this number
                    otherCells = findCellsWithSamePossibleValue(cells.blocks[c.containingBlock], c, value);
                    if (otherCells.size() == 0) {
                        // this is the only cell in vector that can be set to this value
                        c.setValue(-1 * value);
                        removeFromPossibleValues(c, value);
                        //System.out.printf("block known: set cell %d,%d to %d\n", i, j, c.getValue());
                        foundOne = true;
                        return;
                        }
                    if (otherCells.size() < 3) {
                        // check for '2-vector' condition
                        otherCells.add(c);
                        checkForTwoVectorCondition(otherCells, value, true);
                        }
                    // search cell's row for any other cells that work for this number
                    otherCells = findCellsWithSamePossibleValue(cells.rows[c.rowIndex - 1], c, value);
                    if (otherCells.size() == 0) {
                        // this is the only cell in vector that can be set to this value
                        c.setValue(-1 * value);
                        removeFromPossibleValues(c, value);
                        //System.out.printf("block known: set cell %d,%d to %d\n", i, j, c.getValue());
                        foundOne = true;
                        return;
                        }
                    if (otherCells.size() < 3) {
                        // check for '2-vector' condition
                        otherCells.add(c);
                        checkForTwoVectorCondition(otherCells, value, false);
                        }
                    // search cell's column for any other cells that work for this number
                    otherCells = findCellsWithSamePossibleValue(cells.columns[c.columnIndex - 1], c, value);
                    if (otherCells.size() == 0) {
                        // this is the only cell in vector that can be set to this value
                        c.setValue(-1 * value);
                        removeFromPossibleValues(c, value);
                        //System.out.printf("block known: set cell %d,%d to %d\n", i, j, c.getValue());
                        foundOne = true;
                        return;
                        }
                    if (otherCells.size() < 3) {
                        // check for '2-vector' condition
                        otherCells.add(c);
                        checkForTwoVectorCondition(otherCells, value, false);
                        }
                    }
                //checkForReservedValues(c);
                }
            }
        }


    private void analyzePuzzle() {
        mode = GameModes.ANALYZE_PUZZLE;
        /*
        // code to generate all permutations of a vector
        Vector v = new Vector(numbers);
        rowPermutations.removeAllElements();
        findPermutations(new Vector(), v);
        System.out.printf("# of row permutations = %d\n", rowPermutations.size());
        */

        // lock all cells that have values as they are the clues and can't be set by user and
        // initialize the possibleValues vectors for the other cells
        for (int i = 0; i < 9; i++)
            for (int j = 0; j < 9; j++) {
                board[i][j].setDebugMode(debugMode);
                if (board[i][j].getValue() > 0) {
                    board[i][j].lock();
                    board[i][j].setStyle(clue);
                    numberOfEmptyCells--;
                    }
                else
                    for (int n = 1; n <= 9; n++)
                        if (board[i][j].canSetValue(n))
                            board[i][j].addPossibleValue(n);
                //System.out.printf("cell[%d][%d].pv=%s\n", i, j, board[i][j].possibleValues);
                }
        /*
        // code to display all vectors
        for (int i = 0; i < 9; i++)
            System.out.printf("row[%d]=%s\n", i, values.rows[i].toString());
        for (int i = 0; i < 9; i++)
            System.out.printf("column[%d]=%s\n", i, values.columns[i].toString());
        for (int i = 0; i < 9; i++)
            System.out.printf("block[%d]=%s\n", i, values.blocks[i].toString());
        */

        numberOfEmptyCells = 81;
        foundOne = true;
        row = 0;
        column = 0;
        long startTime = System.nanoTime();
        if (debugMode) return;
        while ((numberOfEmptyCells > 0) && (foundOne))
            lookForNextValue();
        float elapsedTime = System.nanoTime() - startTime;
        System.out.printf("analysis took %f nano seconds (%f seconds)\n", elapsedTime, elapsedTime / 1000000000f);
        if (! debugMode)
            showHidenValues();
        }


    private void playPuzzle() {
        mode = GameModes.PLAY_PUZZLE;
    }


    private void fillRow(int r, List<Integer> n) {
        for (int i = 0; i < 9; i++)
            board[r][i].setValue(n.get(i));
    }


    private void newPuzzle() {
        cell c;

        clearPuzzle();

        // create a valid board solution
        Collections.shuffle(numbers);
        fillRow(0, numbers);
        Collections.rotate(numbers, -3);
        fillRow(1, numbers);
        Collections.rotate(numbers, -3);
        fillRow(2, numbers);
        Collections.rotate(numbers, -1);
        fillRow(3, numbers);
        Collections.rotate(numbers, -3);
        fillRow(4, numbers);
        Collections.rotate(numbers, -3);
        fillRow(5, numbers);
        Collections.rotate(numbers, -1);
        fillRow(6, numbers);
        Collections.rotate(numbers, -3);
        fillRow(7, numbers);
        Collections.rotate(numbers, -3);
        fillRow(8, numbers);

        // remove from view some of the values to make puzzle
        board[1][3].setValue(board[1][3].getValue()*-1);
        }


    private void cellClicked (cell c) {
        //DisplayInformationDialog(c.getName(), "cell clicked: v=" + Integer.toString(c.getValue()));
        if ((mode != GameModes.PLAY_PUZZLE) && (lastNumberClicked != null))
            c.setValue(Integer.parseInt(lastNumberClicked.getText().toString()));
        if (selectedCell == c)
            return;
        // TODO: change focus highlighting on board
        if (selectedCell != null)
            selectedCell.unsetStyle();
        selectedCell = c;
        c.setStyle(selected);

        }


    private void backSpace() {
        if (selectedCell != null)
            selectedCell.eraseValue();
        }


    private void numberClicked (TextButton button, int n, boolean doubleClicked) {
        //DisplayInformationDialog("numberClicked", "number clicked: v=" + Integer.toString(n));
        if (selectedCell != null)
            selectedCell.setValue(n);
        if (doubleClicked) {
            // toggle number button off if it was on
            if (lastNumberClicked != null)
                lastNumberClicked.setColor(1, 1, 1, 1);
            if (lastNumberClicked == button)
                lastNumberClicked = null;
            else {
                lastNumberClicked = button;
                button.setColor(.8f, .8f, .4f, 1);
                }
            }
        //System.out.printf("%s\ncolor=%s", button, button.getColor());
        }


    private void setDebugMode(TextButton b, boolean doubleClicked) {
        if (doubleClicked) {
            debugMode = !debugMode;
            if (debugMode)
                b.setColor(.8f, .8f, .4f, 1);
            else
                b.setColor(1, 1, 1, 1);
            }
        else
            lookForNextValue();
        }


    private void DisplayError (String accessType, String cause) {
        cause = cause.replace (')', ' ');
        String delimiters = "\\(";
        String[] subStrings = cause.split (delimiters);
        Table table = new Table (skin);
        for (String s : subStrings) {
            Label l = new Label (s + "\n\r", skin);
            l.setWrap (true);
            table.add (l).width (225f).align (Align.center);
            table.row();
            }
        Dialog editDialog = new Dialog (accessType + " Error", skin);
        editDialog.getContentTable().add (table).align (Align.center);
        editDialog.button ("OK", "ok");
        editDialog.scaleBy (.5f);
        editDialog.show (stage).setX (10f);
        }


    private void AddBoardToStage () {
        cell c;
        Vector valuesBlock;
        Color color;
        Set<Integer> sideBlocks = new HashSet(Arrays.asList(2, 4, 6, 8));
        int br, bc, ff, b;

        Table table = new Table(skin);
        for (int i=0; i < 9; i++) {
            for (int j=0; j < 9; j++) {
                // block numbering
                //  #############
                //  # 1 # 2 # 3 #
                //  #############
                //  # 4 # 5 # 6 #
                //  #############
                //  # 7 # 8 # 9 #
                //  #############
                br = (i / 3) + 1;
                bc = (j / 3) + 1;
                ff = (2 - (j / 3)) * (i / 3);
                b = (br * bc) + ff;
                valuesBlock = values.blocks[b-1];
                //System.out.printf("r=%d, c=%d, br=%d, bc=%d, ff=%d, b=%d\n", i , j, br, bc, ff, b);
                Label l = new Label("", skin);
                if (sideBlocks.contains(b))
                    l.setStyle(white);
                else
                    l.setStyle(gray);
                c = new cell(i+1, j+1, b-1, values.rows[i], values.columns[j], valuesBlock, l);
                board[i][j] = c;
                cells.blocks[b-1].add(c);
                cells.columns[j].add(c);
                cells.rows[i].add(c);
                //l.setText(c.getName());
                l.setAlignment(Align.center);
                table.add(l).height(c.getSize()).width(c.getSize());
                final cell fc = c;
                l.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) { cellClicked(fc); }});
                }
            table.row();
            }
        table.setBounds(0, 260, SCREEN_WIDTH, SCREEN_HEIGHT-260);
        stage.addActor (table);

        Grid grid = new Grid(1, Color.BLACK, 9, 9, 44);
        grid.setPosition(2, 262);
        stage.addActor (grid);
        }


    private TextButton createNumberButton(int n) {
        final TextButton button = new TextButton (Integer.toString(n), skin, "default");
        final int N = n;
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (getTapCount() == 2)
                    numberClicked(button, N, true);
                else
                    numberClicked(button, N, false);
                }
            });
        return button;
        }


    private void Initialize () {
        TextButton button;
        AddBoardToStage();

        /*
        // code to display the blocks lists to see that they are correctly created
        for (int i=0; i < 9; i++) {
            System.out.printf("block %d: ", i);
            Iterator l = blocks[i].iterator();
            while (l.hasNext()) {
                c = ((cell)l.next());
                System.out.printf("%s, ", c.getName());
                }
            System.out.printf("\n");
            }
        */

        // create number buttons
        Table table = new Table(skin);
        for (int i=1; i <= 9; i++) {
            button = createNumberButton(i);
            table.add(button).size(40, 40);
            }
        button = new TextButton ("BS", skin, "default");
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { backSpace(); }});
        table.add(button).size(40, 40);

        table.setBounds(0, 100, SCREEN_WIDTH, 20);
        table.setPosition(1, 230);
        stage.addActor(table);

        // create the "create puzzle" button
        button = new TextButton ("Create\nPuzzle", skin, "default");
        button.setWidth (75);
        button.setHeight (40);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { createPuzzle(); }});
        button.setPosition(4, 0);
        stage.addActor (button);

        // create the "Analyze Mode" button
        button = new TextButton ("Analyze\nPuzzle", skin, "default");
        button.setWidth (75);
        button.setHeight (40);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { analyzePuzzle(); }});
        button.setPosition(84, 0);
        stage.addActor (button);

        // create the "Clear Puzzle" button
        button = new TextButton ("Clear\nPuzzle", skin, "default");
        button.setWidth (75);
        button.setHeight (40);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { clearPuzzle(); }});
        button.setPosition(164, 0);
        stage.addActor (button);

        // create the "Generate Puzzle" button
        button = new TextButton ("New\nPuzzle", skin, "default");
        button.setWidth (75);
        button.setHeight (40);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { newPuzzle(); }});
        button.setPosition(244, 0);
        stage.addActor (button);

        // create the "Play Puzzle" button
        button = new TextButton ("Play\nPuzzle", skin, "default");
        button.setWidth (75);
        button.setHeight (40);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { playPuzzle(); }});
        button.setPosition(324, 0);
        stage.addActor (button);


		// create the "Save Game" button
		button = new TextButton ("Save\nGame", skin, "default");
		button.setWidth (75);
		button.setHeight (40);
		button.addListener (new ClickListener() {
            @Override
			public void clicked (InputEvent event, float x, float y) {
                // try to get external access if it hasn't already been granted
                systemAccess.RequestExternalAccess();
				firstTextField = new TextField ("", skin);
				Table table = new Table (skin);
				table.add ("File Pathname ").align (Align.right);
				table.add (firstTextField);
				Dialog editDialog = new Dialog ("Save Game", skin) {
					protected void result (Object object) {
						if (object.equals ("ok"))
							saveGame();
                        Gdx.input.setOnscreenKeyboardVisible (false);
						}
					};
				editDialog.getContentTable().add (table);
				editDialog.button ("OK", "ok");
				editDialog.button ("Cancel", "cancel");
                editDialog.scaleBy (.2f);
                editDialog.show (stage).setX (25f);
				stage.setKeyboardFocus (firstTextField);
				}
			});
		button.setPosition (4,40);
		stage.addActor (button);

        // create the "Load Game" button
        button = new TextButton ("Load\nGame", skin, "default");
        button.setWidth (75);
        button.setHeight (40);
        button.addListener (new ClickListener() {
            @Override
            public void clicked (InputEvent event, float x, float y) {
                // try to get external access if it hasn't already been granted
                systemAccess.RequestExternalAccess();
                firstTextField = new TextField ("", skin);
                Table table = new Table (skin);
                table.add ("File Pathname ").align (Align.right);
                table.add (firstTextField);
                Dialog editDialog = new Dialog ("Load Game", skin) {
                    protected void result(Object object) {
                        if (object.equals ("ok"))
                            loadGame();
                        Gdx.input.setOnscreenKeyboardVisible (false);
                    }
                };
                editDialog.getContentTable().add (table);
                editDialog.button ("OK", "ok");
                editDialog.button ("Cancel", "cancel");
                editDialog.scaleBy (.2f);
                editDialog.show (stage).setX (25f);
                stage.setKeyboardFocus (firstTextField);
            }
        });
        button.setPosition (84, 40);
        stage.addActor (button);

        // create the "Debug Mode" button
        final TextButton b = new TextButton ("Debug\nMode", skin, "default");
        b.setWidth (75);
        b.setHeight (40);
        b.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (getTapCount() == 2)
                    setDebugMode(b, true);
                else
                    setDebugMode(b, false);
                }
            });
        b.setPosition(164, 40);
        stage.addActor (b);


        appState = AppStates.INITIALIZED;
        Gdx.input.setInputProcessor (inputMultiplexer);
		}


    private void saveGame() {
        // TODO: add code to prompt to over-write existing file
        //Gdx.app.log (TAG, "saveGame: file path = " + firstTextField.getText());
        try {
            FileHandle file = Gdx.files.external(firstTextField.getText());
            for (int i = 0; i < 9; i++)
                for (int j = 0; j < 9; j++)
                    file.writeString(Integer.toString(board[i][j].getValue()) + ", ", true);
            }
        catch (Exception e) {
            Gdx.app.log (TAG, "saveGame: Error cause - " + e.getCause().getLocalizedMessage());
            DisplayError ("saveGame", e.getCause().getLocalizedMessage());
            }
        }


    private void loadGame() {
        String s = "";
        //Gdx.app.log (TAG, "loadGame: file path = " + firstTextField.getText());
        try {
            FileHandle file = Gdx.files.external(firstTextField.getText());
            s = file.readString();
            }
        catch (Exception e) {
            Gdx.app.log (TAG, "loadGame: Error cause - " + e.getCause().getLocalizedMessage());
            DisplayError ("loadGame", e.getCause().getLocalizedMessage());
            return;
            }
        //System.out.printf("loadGame: s=%s\n", s);
        String delimiter = "[,]";
        String[] cellValues = s.split(delimiter);
        clearPuzzle();
        for (int i = 0; i < 9; i++)
            for (int j = 0; j < 9; j++)
                //board[i][j].setValue(Integer.parseInt(cellValues[i*9+j]));
                board[i][j].setValue(Integer.valueOf(cellValues[i*9+j].trim()));
        }


    private void DisplayInformationDialog (String title, String text) {
        Dialog errorDialog = new Dialog (title, skin);
        errorDialog.text (text);
        errorDialog.button ("OK", skin);
        errorDialog.show (stage);
        }


	public class MyInputProcessor implements InputProcessor {
        // this input "listener" allows the app to detect keyboard activity so it can reset the
        // inactivity watchdog timer if the user is detecting typing

		public boolean keyDown (int keycode) {elapsedTimeInSeconds = 0; return false;}

		public boolean keyUp (int keycode) {elapsedTimeInSeconds = 0; return false;}

		public boolean keyTyped (char character) {elapsedTimeInSeconds = 0;	return false;}

		public boolean touchDown (int x, int y, int pointer, int button) {return false;}

		public boolean touchUp (int x, int y, int pointer, int button) {return false;}

		public boolean touchDragged (int x, int y, int pointer) {return false;}

		public boolean mouseMoved (int x, int y) {return false;}

		public boolean scrolled (int amount) {return false;}
	    }

	}
