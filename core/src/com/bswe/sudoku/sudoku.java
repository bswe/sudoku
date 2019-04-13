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
            sr.line(x, y + (i * cellSize), x + width, y + (i * cellSize));
        for (int i=0; i <= numberOfColumns; i++)
            sr.line(x + (i * cellSize), y, x + (i * cellSize), y + height);
        sr.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        Gdx.gl.glLineWidth(1f);
        sr.setColor(Color.WHITE);

        batch.begin();
        }
    }


class cell {
    int rowIndex, columnIndex, size, value = -1;
    String name;
    Vector row, column, block;
    Label label;
    LabelStyle defaultStyle;
    int originalValue;
    boolean locked;

    cell(int rowIndex, int columnIndex, Vector row, Vector column, Vector block, Label l) {
        this.rowIndex = rowIndex;
        this.columnIndex = columnIndex;
        this.row = row;
        this.column = column;
        this.block = block;
        this.size = 44;     // TODO: make this a constant and add method to change size
        name = Integer.toString(rowIndex) + "," + Integer.toString(columnIndex);
        label = l;
        defaultStyle = l.getStyle();
        setValue(0);
        }

    public void setValue(int value) {
        if (value == 0) locked = false;
        if (locked)
            return;
        if (this.value == value)
            return;
        if (this.value > 0) {
            row.removeElement(this.value);
            column.removeElement(this.value);
            block.removeElement(this.value);
            }
        this.value = value;
        if (this.value > 0) {
            row.add(this.value);
            column.add(this.value);
            block.add(this.value);
            label.setText(Integer.toString(this.value));
            }
        else {
            label.setText("");
            originalValue = value;
            }
        //System.out.printf("row=%s\ncolumn=%s\nblock=%s\n", row.toString(), column.toString(), block.toString());
        }

    public boolean canSetValue (int value) {
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

    public void setStyle(LabelStyle style) { label.setStyle(style); }

    public void unsetStyle() {
        label.setStyle(defaultStyle);
        }

    public void eraseValue() {
        value = originalValue;
        label.setText("");
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

    // used for user input for login dialog since logout can happen at any time even when other
    // dialog boxes are being displayed, so this dialog cannot share the other fields below
    private TextField passwordTextField;
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

    private cell[][] board = new cell[9][9];

    private Vector[] blocks = new Vector[] {new Vector(), new Vector(), new Vector(),
                                            new Vector(), new Vector(), new Vector(),
                                            new Vector(), new Vector(), new Vector()};

    private List<Integer> numbers = Arrays.asList(new Integer[] {1, 2, 3, 4, 5, 6, 7, 8, 9});

    private cell selectedCell = null;

    private Vector rowPermutations = new Vector();

    LabelStyle style1, style2, style3, style4;

    TextButton lastNumberClicked = null;

    private enum GameModes {CREATE_PUZZLE,
                            PLAY_PUZZLE,
                            ANALYZE_PUZZLE,
                            EDIT_PUZZLE};

    private GameModes mode = GameModes.EDIT_PUZZLE;


    public sudoku (SystemAccess sa) {
        super();
        systemAccess = sa;
        }


	@Override
	public void create () {
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
        Pixmap Background1 = new Pixmap(44, 44, Pixmap.Format.RGB888);
        Background1.setColor(new Color(.8f, .8f, .8f, 1));
        Background1.fill();
        style1 = new LabelStyle(temp.getStyle());
        style1.background = new Image(new Texture(Background1)).getDrawable();

        Pixmap Background2 = new Pixmap(44, 44, Pixmap.Format.RGB888);
        Background2.setColor(new Color(1, 1, 1, 1));
        Background2.fill();
        style2 = new LabelStyle(temp.getStyle());
        style2.background = new Image(new Texture(Background2)).getDrawable();

        Pixmap Background3 = new Pixmap(44, 44, Pixmap.Format.RGB888);
        Background3.setColor(new Color(.9f, .9f, .9f, 1));
        Background3.fill();
        style3 = new LabelStyle(temp.getStyle());
        style3.background = new Image(new Texture(Background3)).getDrawable();

        Pixmap Background4 = new Pixmap(44, 44, Pixmap.Format.RGB888);
        Background4.setColor(new Color(.85f, 1, .85f, 1));
        Background4.fill();
        style4 = new LabelStyle(temp.getStyle());
        style4.background = new Image(new Texture(Background4)).getDrawable();
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


	private void fillRow(int r, List<Integer> n) {
        for (int i = 0; i < 9; i++)
            board[r][i].setValue(n.get(i));
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


    private void analyzePuzzle() {
        mode = GameModes.ANALYZE_PUZZLE;
        /*
        Vector v = new Vector(numbers);
        rowPermutations.removeAllElements();
        findPermutations(new Vector(), v);
        System.out.printf("# of row permutations = %d\n", rowPermutations.size());
        */
        }


    private void clearPuzzle() {
        mode = GameModes.EDIT_PUZZLE;
        // clear the board by resetting board values to -1
        for (int i = 0; i < 9; i++)
            for (int j = 0; j < 9; j++)
                board[i][j].setValue(-1);
        }


    private void createPuzzle() {
        mode = GameModes.CREATE_PUZZLE;
        // lock all cells with values as they are the clues and can't be set by user
        for (int i = 0; i < 9; i++)
            for (int j = 0; j < 9; j++)
                if (board[i][j].getValue() > 0) {
                    board[i][j].lock();
                    board[i][j].setStyle(style1);
                    }
        // TODO: add code to try to find puzzle solution
        }


    private void playPuzzle() {
        mode = GameModes.PLAY_PUZZLE;
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
        c.setStyle(style4);

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


    private void CopyToSystemClipboard (String s) {
        final String S = s;
        Dialog confirmationDialog = new Dialog ("Copy Password Confirmation", skin) {
            protected void result (Object object) {
                //Gdx.app.log (TAG, "CopyToSystemClipboard confirmation dialog: chosen = " + object);
                systemAccess.WriteClipboard (S);
                }
            };
        Table table = new Table();
        Label label = new Label ("Confirm the copying\n\rof password\n\r" + S + "\n\rto system clipboard", skin);
        label.setAlignment (Align.center);
        table.add (label);
        confirmationDialog.getContentTable().add (table);
        confirmationDialog.button ("OK", "ok");
        confirmationDialog.button ("Cancel", "cancel");
        confirmationDialog.scaleBy (.5f);
        confirmationDialog.show(stage).setX (10f);
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
        Vector block;
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
                block = values.blocks[b-1];
                //System.out.printf("r=%d, c=%d, br=%d, bc=%d, ff=%d, b=%d\n", i , j, br, bc, ff, b);
                Label l = new Label("", skin);
                if (sideBlocks.contains(b))
                    l.setStyle(style2);
                else
                    l.setStyle(style3);
                c = new cell(i+1, j+1, values.rows[i], values.columns[j], block, l);
                board[i][j] = c;
                blocks[b-1].add(c);
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

        Grid grid = new Grid(2, Color.BLACK, 9, 9, 44);
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
            table.add(button);
            }
        button = new TextButton ("BS", skin, "default");
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { backSpace(); }});
        table.add(button);

        table.setBounds(0, 100, SCREEN_WIDTH, 20);
        table.setPosition(0, 230);
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

		/*
		// create the "Change Password" button
		final TextButton button2 = new TextButton ("Change\nPassword", skin, "default");
		button2.setWidth (85);
		button2.setHeight (40);
		button2.addListener (new ClickListener() {
            @Override
			public void clicked (InputEvent event, float x, float y) {
				firstTextField = new TextField ("", skin);
				secondTextField = new TextField ("", skin);
				Table table = new Table (skin);
				table.add ("new pwd ").align (Align.right);
				table.add (firstTextField);
				table.row();
				table.add ("confirm pwd ").align (Align.right);
				table.add (secondTextField);
				Dialog editDialog = new Dialog ("Change Password", skin) {
					protected void result (Object object) {
						//Gdx.app.log (TAG, "Change Password dialog: chosen = " + object);
						//Gdx.app.log (TAG, "Change Password dialog: new password = " + firstTextField.getText());
						if (object.equals ("ok"))
							//ChangeAppPassword();
                        Gdx.input.setOnscreenKeyboardVisible (false);
						}
					};
				editDialog.getContentTable().add (table);
				editDialog.button ("OK", "ok");
				editDialog.button ("Cancel", "cancel");
                editDialog.scaleBy (.4f);
                editDialog.show (stage).setX (15f);
				stage.setKeyboardFocus (firstTextField);
				}
			});
		button2.setPosition (80, 0);
		stage.addActor (button2);

		// create the "Logout" button
		final TextButton button3 = new TextButton ("Logout", skin, "default");
		button3.setWidth (65);
		button3.setHeight (40);
		button3.addListener (new ClickListener() {
            @Override
			public void clicked (InputEvent event, float x, float y) {
				//Gdx.app.log (TAG, "Logout button clicked");
                //LogoutUser();
				}
			});
		button3.setPosition (170, 0);
		stage.addActor (button3);

		// create the "Restore Accounts" button
		final TextButton button4 = new TextButton ("Restore\nAccounts", skin, "default");
		button4.setWidth (80);
		button4.setHeight (40);
		button4.addListener (new ClickListener() {
            @Override
			public void clicked (InputEvent event, float x, float y) {
				//Gdx.app.log (TAG, "Restore Accounts button clicked");
                // try to get external access if it hasn't already been granted
                systemAccess.RequestExternalAccess();
                firstTextField = new TextField ("", skin);
				Table table = new Table (skin);
				table.add ("File Path ").align (Align.right);
				table.add (firstTextField);
                table.row();
                Label warning = new Label("WARNING: password will be set\n\rto what is in the restore file", skin);
                warning.setColor (Color.RED);
                table.add (warning).colspan (2);
				Dialog editDialog = new Dialog ("Restore Accounts", skin) {
					protected void result(Object object) {
						//Gdx.app.log (TAG, "Restore Accounts dialog: chosen = " + object);
						//Gdx.app.log (TAG, "Restore Accounts dialog: file path = " + firstTextField.getText());
						if (object.equals ("ok"))
							//RestoreAccounts();
                        Gdx.input.setOnscreenKeyboardVisible (false);
						}
					};
				editDialog.getContentTable().add (table);
				editDialog.button ("OK", "ok");
				editDialog.button ("Cancel", "cancel");
                editDialog.scaleBy (.5f);
                editDialog.show (stage).setX (20f);
				stage.setKeyboardFocus (firstTextField);
				}
			});
		button4.setPosition (240, 0);
		stage.addActor (button4);

		// create the "Archive Accounts" button
		final TextButton button5 = new TextButton ("Archive\nAccounts", skin, "default");
		button5.setWidth (80);
		button5.setHeight (40);
		button5.addListener (new ClickListener() {
            @Override
			public void clicked (InputEvent event, float x, float y) {
				//Gdx.app.log (TAG, "Archive button clicked");
                // try to get external access if it hasn't already been granted
                systemAccess.RequestExternalAccess();
				firstTextField = new TextField ("", skin);
				Table table = new Table (skin);
				table.add ("File Path ").align (Align.right);
				table.add (firstTextField);
				Dialog editDialog = new Dialog ("Archive Accounts", skin) {
					protected void result (Object object) {
						//Gdx.app.log (TAG, "Archive Accounts dialog: chosen = " + object);
						//Gdx.app.log (TAG, "Archive Accounts dialog: file path = " + firstTextField.getText());
						if (object.equals ("ok"))
							//ArchiveAccounts();
                        Gdx.input.setOnscreenKeyboardVisible (false);
						}
					};
				editDialog.getContentTable().add (table);
				editDialog.button ("OK", "ok");
				editDialog.button ("Cancel", "cancel");
                editDialog.scaleBy (.5f);
                editDialog.show (stage).setX (20f);
				stage.setKeyboardFocus (firstTextField);
				}
			});
		button5.setPosition (322, 0);
		stage.addActor (button5);
        */

		appState = AppStates.INITIALIZED;
        Gdx.input.setInputProcessor (inputMultiplexer);
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
