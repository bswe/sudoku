package com.bswe;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlReader.Element;
import com.badlogic.gdx.utils.XmlWriter;
import com.badlogic.gdx.utils.viewport.StretchViewport;

import static com.badlogic.gdx.Application.ApplicationType.Desktop;

import org.jasypt.util.password.StrongPasswordEncryptor;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jasypt.util.text.BasicTextEncryptor;
import org.lwjgl.openal.AL;


// object to hold account info
class Account {
	String Name, UserName, Password;
	Integer PersistenceIndex;

	public Account (Integer index, String name, String UN, String PW) {
		PersistenceIndex = index;
		Name = name;
		UserName = UN;
		Password = PW;
		}
 	}


// comparator for sorting accounts by their name
class AccountComparator implements Comparator<Account>{
	@Override
	public int compare (Account a1, Account a2) {
		return a1.Name.compareToIgnoreCase(a2.Name);
		}
	}


// Main application class
public class pWallet extends ApplicationAdapter {
    // TODO: make handling of file i/o and user input more robust and exception safe
	public static final int SCREEN_WIDTH = 400;
	public static final int SCREEN_HEIGHT = 660;
	private static final String PASSWORD_KEY = "1";
	private static final String NUMBER_OF_ACCOUNTS_KEY = "2";
    private static final float INACTIVITY_DURATION = 60f;    // in seconds

	private enum AppStates {PW_REQUIRED, PW_PASSED, INITILIZED, LOGGED_OUT}

	private static final String TAG = pWallet.class.getName();

	private String inputPassword = "";

	private TextField firstTextField;
	private TextField secondTextField;
	private TextField thirdTextField;

	private AppStates appState = AppStates.PW_REQUIRED;

	private Pixmap pixmap;
	private Skin skin;
    private Stage stage;
    private Stage loginStage;

	private Table scrollTable;
	private Label errorText;

	private List<Account> accounts;

	private int numberOfAccounts;

	private StrongPasswordEncryptor passwordEncryptor = new StrongPasswordEncryptor();

	private BasicTextEncryptor textEncryptor;

	private Preferences prefs;

    private float elapsedTimeInSeconds = 0;

    private InputMultiplexer inputMultiplexer;

    private SystemAccess systemAccess;


    public pWallet (SystemAccess sa) {
        super();
        systemAccess = sa;
        }


	@Override
	public void create () {
        Gdx.app.log (TAG, "create: application class = " + Gdx.app.getClass().getName());

        // init preferences for persistent storage
		prefs = Gdx.app.getPreferences("bswe-pwallet");

		skin = new Skin (Gdx.files.internal ("clean-crispy-ui.json"));
        //skin.getFont("font").getData().setScale(1f);
        stage = new Stage();
        stage.setViewport (new StretchViewport (SCREEN_WIDTH, SCREEN_HEIGHT, new OrthographicCamera()));
        loginStage = new Stage();
        loginStage.setViewport (new StretchViewport (SCREEN_WIDTH, SCREEN_HEIGHT, new OrthographicCamera()));

        // use input multiplexer to detect keyboard activity for reseting the inactivity watchdog
        inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(new MyInputProcessor());
        inputMultiplexer.addProcessor(stage);

		// display password entry dialog
		DisplayPasswordDialog("");
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
            case INITILIZED:
                // check fo inactivity to potentially log the user out
                if (InactivityWatchdogFired())
                    return;
                // still logged in, so display the app's main stage
                stage.getViewport().apply();
                stage.act(Gdx.graphics.getDeltaTime());
                stage.draw();
            }
		}


	@Override
	public void resize (int width, int height) {
		Gdx.app.log (TAG, "resize: w=" + width + ", h=" + height);
        stage.getViewport().update (width, height, true);
        loginStage.getViewport().update (width, height, true);
		}


	@Override
	public void dispose () {
		Gdx.app.log (TAG, "dispose:");
		try {
			stage.dispose();
			loginStage.dispose();
			skin.dispose();
			pixmap.dispose();
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
        Gdx.app.log (TAG, "pause:");
        LogoutUser();
        }


	private Boolean InactivityWatchdogFired() {
		// log user out if they haven't touched the screen in INACTIVITY_DURATION number of seconds
		if (Gdx.input.justTouched())
			elapsedTimeInSeconds = 0;
		elapsedTimeInSeconds += Gdx.graphics.getRawDeltaTime();
		if (elapsedTimeInSeconds > INACTIVITY_DURATION) {
			Gdx.app.log(TAG, "InactivityWatchdogFired: inactivity watchdog fired, logging user out");
			LogoutUser();
			return true;
		}
		// watchdog didn't fire
		return false;
	}


	// load accounts from persistent memory
	private void LoadAccounts () {
        // TODO: add error checking and exception handling
		for (int i=1; i <= numberOfAccounts; i++) {
            String key;
            Integer index = i * 3;
            key = Integer.toString(index++);
			String name = prefs.getString(key, "");
			name = textEncryptor.decrypt(name);
            key = Integer.toString(index++);
            String userName = prefs.getString(key, "");
			userName = textEncryptor.decrypt(userName);
            key = Integer.toString(index++);
            String password = prefs.getString(key, "");
			password = textEncryptor.decrypt(password);
			Account a = new Account(i, name, userName, password);
			accounts.add(a);
			}
		}


	private void DisplayPasswordDialog (String msg) {
		firstTextField = new TextField("", skin);
		Table table = new Table();
		table.add (firstTextField);
		Dialog editDialog = new Dialog ("enter password", skin) {
			protected void result (Object object) {
				//Gdx.app.log (TAG, "DisplayPasswordDialog dialog: chosen = " + object);
				//Gdx.app.log (TAG, "DisplayPasswordDialog dialog: password = " + passwordTextField.getText());
				CheckPassword();
                Gdx.input.setOnscreenKeyboardVisible(false);
				}
			};
        if (!msg.equals("")) {
            table.row();
            Label label = new Label(msg, skin);
            label.setAlignment(Align.center);
            label.setColor(Color.RED);
            table.add(label);
            }
		editDialog.button("OK", "ok");
		editDialog.getContentTable().add(table);
        editDialog.scaleBy(.5f);
        editDialog.show(loginStage).setX(70f);
        loginStage.setKeyboardFocus(firstTextField);
        Gdx.input.setInputProcessor (loginStage);
		}


	private void PersistAccount (Account a) {
		String encryptedText;
		String key;
        Integer index = a.PersistenceIndex * 3;
        key = Integer.toString(index++);
		encryptedText = textEncryptor.encrypt (a.Name);
		prefs.putString(key, encryptedText);
        key = Integer.toString(index++);
		encryptedText = textEncryptor.encrypt (a.UserName);
		prefs.putString(key, encryptedText);
        key = Integer.toString(index++);
		encryptedText = textEncryptor.encrypt (a.Password);
		prefs.putString(key, encryptedText);
		prefs.putString(NUMBER_OF_ACCOUNTS_KEY, textEncryptor.encrypt (Integer.toString(numberOfAccounts)));
		prefs.flush();
		}


	private void UnPersistAllAccounts () {
        // TODO: add error checking and exception handling
        for (Account a : accounts) {
            String key;
            Integer index = a.PersistenceIndex * 3;
            key = Integer.toString(index++);
			prefs.remove(key);
            key = Integer.toString(index++);
            prefs.remove(key);
            key = Integer.toString(index++);
            prefs.remove(key);
			}
		prefs.putString(NUMBER_OF_ACCOUNTS_KEY, textEncryptor.encrypt (Integer.toString(0)));
		prefs.flush();
		}


	private void PersistAllAccounts () {
        // TODO: add error checking and exception handling
		Integer i = 0;
		for (Account a : accounts) {
			a.PersistenceIndex = ++i;
			PersistAccount(a);
			}
		prefs.putString(NUMBER_OF_ACCOUNTS_KEY, textEncryptor.encrypt (Integer.toString(i)));
		prefs.flush();
	}


	private void RedisplayAccountsTable() {
		scrollTable.remove();
		AddAccountsTableToStage();
		}


	private void AddNewAccount () {
        // TODO: add error checking and exception handling
		String accountName = firstTextField.getText();
		String accountUsername = secondTextField.getText();
		String accountPassword = thirdTextField.getText();
		if (accountName.equals("") || accountUsername.equals("") || accountPassword.equals(""))
			return;
		// check for account name being unique
		for (Account a: accounts)
			if (a.Name.equals(accountName)) {
				Dialog errorDialog = new Dialog("Error", skin);
				errorDialog.text("Account with the name (" + accountName + ") already exists");
				errorDialog.button("OK", skin);
				errorDialog.show(stage);
				return;
				}
		Gdx.app.log (TAG, "AddNewAccount: (an=" + accountName + ", un=" + accountUsername + ", pw=" + accountPassword + ")");
		Account a = new Account (++numberOfAccounts, accountName, accountUsername, accountPassword);
		PersistAccount(a);
		accounts.add(a);
		RedisplayAccountsTable();
		}


	private void ChangeAccount (String accountName, String newName, String newUsername, String newPassword) {
		for (Account a : accounts)
			if (a.Name.equals (accountName)) {
				a.Name = newName;
				a.UserName = newUsername;
				a.Password = newPassword;
				PersistAccount (a);
				}
		RedisplayAccountsTable();
		}


	private void DeleteAccount (String accountName) {
        // TODO: add error checking and exception handling
        final String name = accountName;
        Dialog confirmationDialog = new Dialog ("Delete Account Confirmation", skin) {
            protected void result (Object object) {
                Gdx.app.log (TAG, "DeleteAccount confirmation dialog: chosen = " + object);
                if (object.equals("ok"))
                    for (Account a : accounts)
                        if (a.Name.equals (name)) {
                            UnPersistAllAccounts();
                            accounts.remove (a);
                            numberOfAccounts--;
                            PersistAllAccounts();
                            break;
                        }
                RedisplayAccountsTable();
                }
            };
        Table table = new Table();
        Label label = new Label("Confirm the deletion\n\rof account " + name, skin);
        label.setAlignment(Align.center);
        table.add(label);
        confirmationDialog.getContentTable().add(table);
        confirmationDialog.button ("OK", "ok");
        confirmationDialog.button ("Cancel", "cancel");
        confirmationDialog.scaleBy (.5f);
        confirmationDialog.show (stage).setX(10f);
		}


	private void EditAccount (String accountName) {
        // TODO: add error checking and exception handling
		final String AccountName = accountName;
		Gdx.app.log (TAG, "EditAccount: account name = " + accountName);
		for (Account a : accounts)
			if (a.Name.equals (accountName)) {
				firstTextField = new TextField(a.Name, skin);
				secondTextField = new TextField(a.UserName, skin);
				thirdTextField = new TextField(a.Password, skin);
				Table table = new Table (skin);
				table.add ("Name ").align(Align.right);
				table.add (firstTextField);
				table.row();
				table.add ("Username ").align(Align.right);
				table.add (secondTextField);
				table.row();
				table.add ("Password ").align(Align.right);
				table.add (thirdTextField);
				Dialog editDialog = new Dialog ("Edit Account Information", skin) {
					protected void result (Object object) {
						Gdx.app.log (TAG, "EditAccount dialog: chosen = " + object);
						//Gdx.app.log (TAG, "editDialog dialog: name = " + firstTextField.getText());
						if (object.equals("change"))
							ChangeAccount (AccountName,
										   firstTextField.getText(),
										   secondTextField.getText(),
										   thirdTextField.getText());
						else if (object.equals("delete"))
							DeleteAccount (AccountName);
                        Gdx.input.setOnscreenKeyboardVisible(false);
						}
					};
				editDialog.getContentTable().add(table);
				editDialog.button("Delete", "delete");
				editDialog.button("Change", "change");
				editDialog.button("Cancel", "cancel");
                editDialog.scaleBy(.5f);
				editDialog.show(stage).setX(10f);
				stage.setKeyboardFocus(firstTextField);
			    }
		}


    private void CopyToSystemClipboard (String s) {
        // TODO: add error checking and exception handling
        final String S = s;
        Dialog confirmationDialog = new Dialog ("Copy Password Confirmation", skin) {
            protected void result (Object object) {
                Gdx.app.log (TAG, "CopyToSystemClipboard confirmation dialog: chosen = " + object);
                systemAccess.WriteClipboard (S);
                }
            };
        Table table = new Table();
        Label label = new Label("Confirm the copying\n\rof password\n\r" + S + "\n\rto system clipboard", skin);
        label.setAlignment(Align.center);
        table.add(label);
        confirmationDialog.getContentTable().add(table);
        confirmationDialog.button("OK", "ok");
        confirmationDialog.button("Cancel", "cancel");
        confirmationDialog.scaleBy(.5f);
        confirmationDialog.show(stage).setX(10f);
        }


	private void AddAccountsTableToStage () {
        // TODO: add error checking and exception handling
		// first make sure accounts are ordered alphabetically by account name
		Collections.sort (accounts, new AccountComparator());
		// add scrollable accounts table to stage
		Table table = new Table();
		for (Account a: accounts) {
			final TextButton button = new TextButton (a.Name, skin);
            button.getLabel().setFontScale(1.25f, 1.25f);
			table.add (button).align(Align.right);
			final Label UnText = new Label(a.UserName, skin);
            UnText.setFontScale(1.25f);
			table.add (UnText).align(Align.left).pad(10);
			final Label PwText = new Label(a.Password, skin);
            PwText.setFontScale(1.25f);
            table.add (PwText).align(Align.left).pad(10);
			table.row();
            final String name = a.Name;
            button.addListener (new ClickListener() {
                @Override
                public void clicked (InputEvent event, float x, float y) {
                    EditAccount (name);
                }
            });
            final String password = a.Password;
            PwText.addListener (new ClickListener(){
                @Override
                public void clicked (InputEvent event, float x, float y) {
                    Gdx.app.log(TAG, "password " + password + " clicked for " + name);
                    CopyToSystemClipboard (password);
                    }
                });
			}
		ScrollPane scroller = new ScrollPane (table);
		scrollTable = new Table(skin);
		scrollTable.setBounds(0, 40, SCREEN_WIDTH, SCREEN_HEIGHT-40);
		scrollTable.align(Align.left);
        // force scroller to fill the scroll table so user can touch any area and get it to scroll
		scrollTable.add (scroller).expand().fill();
		scrollTable.setBackground(new TextureRegionDrawable(new TextureRegion(new Texture(pixmap))));
		//sTable.debugAll();
		stage.addActor (scrollTable);
		}


    private void DisplayFileError (String accessType, String cause) {
        cause = cause.replace(')', ' ');
        String delimiters = "\\(";
        String[] subStrings = cause.split(delimiters);
        Table table = new Table(skin);
        for (String s : subStrings) {
            Label l = new Label (s + "\n\r", skin);
            l.setWrap(true);
            table.add(l).width(225f).align(Align.center);
            table.row();
        }
        Dialog editDialog = new Dialog ("File " + accessType + " Error", skin);
        editDialog.getContentTable().add(table).align(Align.center);
        editDialog.button("OK", "ok");
        editDialog.scaleBy(.5f);
        editDialog.show(stage).setX(10f);
        }


	private void RestoreAccounts() {
        String xml;
		Gdx.app.log (TAG, "RestoreAccounts: file path = " + firstTextField.getText());
        FileHandle file = Gdx.files.external (firstTextField.getText());
        try {
            xml = file.readString();
            }
        catch (GdxRuntimeException e) {
            Gdx.app.log (TAG, "RestoreAccounts: File Read Error cause - " + e.getCause().getLocalizedMessage());
            DisplayFileError ("Read", e.getCause().getLocalizedMessage());
            return;
            }
        // TODO: add error checking and exception handling
        XmlReader reader = new XmlReader();
        Element root = reader.parse (xml);
        Array<Element> items = root.getChildrenByName ("entry");
        UnPersistAllAccounts();
        for (Element item : items) {
            Gdx.app.log (TAG, "RestoreAccounts: entry " + item.getAttribute("key") + " = " + item.getText());
            prefs.putString(item.getAttribute("key"), item.getText());
            }
        prefs.flush();
        stage.clear();
        appState = AppStates.PW_REQUIRED;
        DisplayPasswordDialog ("");
	    }


	private void SaveAccounts() {
        // TODO: add error checking and exception handling
		Gdx.app.log (TAG, "SaveAccounts: file path = " + firstTextField.getText());
        FileHandle file = Gdx.files.external (firstTextField.getText());
        Writer writer = file.writer(false);
        XmlWriter xml = new XmlWriter(writer);
        try {
            // TODO: complete this method by creating the xml to write
            /*
            xml.element("meow")
                .attribute("moo", "cow")
                .element("child")
                    .attribute("moo", "cow")
                    .element("child")
                    .attribute("moo", "cow")
                    .text("XML is like violence. If it doesn't solve your problem, you're not using enough of it.")
                    .pop()
                .pop()
            .pop();
            */
            writer.close();
            }
        catch (IOException e) {
            Gdx.app.log (TAG, "SaveAccounts: File Write Error cause - " + e.getCause().getLocalizedMessage());
            DisplayFileError ("Write", e.getCause().getLocalizedMessage());
            return;
            }
	    }


    private void LogoutUser() {
        appState = AppStates.LOGGED_OUT;
        elapsedTimeInSeconds = 0;
        DisplayPasswordDialog("");
        }


	private void Initialize () {
        // TODO: add error checking and exception handling
        accounts = new ArrayList<Account>();
		// check preferences for any accounts
		if (prefs.contains(NUMBER_OF_ACCOUNTS_KEY)) {
			// load any persisted accounts
			numberOfAccounts = Integer.parseInt(textEncryptor.decrypt(prefs.getString(NUMBER_OF_ACCOUNTS_KEY)));
            Gdx.app.log (TAG, "Initialize: persisted numberOfAccounts = " + numberOfAccounts);
            LoadAccounts();
			}
		else {
			// no accounts persisted yet so initialize the key
			numberOfAccounts = 0;
            Gdx.app.log (TAG, "Initialize: initial numberOfAccounts = " + numberOfAccounts);
			prefs.putString(NUMBER_OF_ACCOUNTS_KEY, textEncryptor.encrypt (Integer.toString(numberOfAccounts)));
            prefs.flush();
		    }

		// password matched, so show the accounts and buttons
		pixmap = new Pixmap (1, 1, Pixmap.Format.RGB565);
		pixmap.setColor(Color.SALMON);
		pixmap.fill();

        AddAccountsTableToStage();

		// create the "Add Account" button
		final TextButton button1 = new TextButton ("Add\nAccount", skin, "default");
		button1.setWidth (75);
		button1.setHeight (40);
		button1.addListener (new ClickListener() {
            @Override
			public void clicked(InputEvent event, float x, float y) {
				firstTextField = new TextField("", skin);
				secondTextField = new TextField("", skin);
				thirdTextField = new TextField("", skin);
				Table table = new Table(skin);
				table.add("Name ").align(Align.right);
				table.add(firstTextField);
				table.row();
				table.add("Username ").align(Align.right);
				table.add(secondTextField);
				table.row();
				table.add("Password ").align(Align.right);
				table.add(thirdTextField);
				Dialog editDialog = new Dialog("Add Account", skin) {
					protected void result(Object object) {
						Gdx.app.log (TAG, "AddAccount dialog: chosen = " + object);
						//Gdx.app.log (TAG, "AddAccount dialog: name = " + firstTextField.getText());
						if (object.equals("add"))
							AddNewAccount();
                        Gdx.input.setOnscreenKeyboardVisible(false);
                    }
				    };
				editDialog.getContentTable().add(table);
				editDialog.button("Add", "add");
				editDialog.button("Cancel", "cancel");
                editDialog.scaleBy(.5f);
                editDialog.show(stage).setX(10f);
				stage.setKeyboardFocus(firstTextField);
				}
			});
		button1.setPosition(0, 0);
		stage.addActor (button1);

		// create the "Change Password" button
		final TextButton button2 = new TextButton ("Change\nPassword", skin, "default");
		button2.setWidth (85);
		button2.setHeight (40);
		button2.addListener (new ClickListener() {
            @Override
			public void clicked(InputEvent event, float x, float y) {
				firstTextField = new TextField("", skin);
				secondTextField = new TextField("", skin);
				Table table = new Table(skin);
				table.add("new pwd ").align(Align.right);
				table.add(firstTextField);
				table.row();
				table.add("confirm pwd ").align(Align.right);
				table.add(secondTextField);
				Dialog editDialog = new Dialog("Change Password", skin) {
					protected void result(Object object) {
						Gdx.app.log (TAG, "Change Password dialog: chosen = " + object);
						//Gdx.app.log (TAG, "Change Password dialog: new password = " + firstTextField.getText());
						if (object.equals("ok"))
							ChangeAppPassword();
                        Gdx.input.setOnscreenKeyboardVisible(false);
						}
					};
				editDialog.getContentTable().add(table);
				editDialog.button("OK", "ok");
				editDialog.button("Cancel", "cancel");
                editDialog.scaleBy(.4f);
                editDialog.show(stage).setX(15f);
				stage.setKeyboardFocus(firstTextField);
				}
			});
		button2.setPosition(80, 0);
		stage.addActor (button2);

		// create the "Logout" button
		final TextButton button3 = new TextButton ("Logout", skin, "default");
		button3.setWidth (65);
		button3.setHeight (40);
		button3.addListener (new ClickListener() {
            @Override
			public void clicked(InputEvent event, float x, float y) {
				Gdx.app.log (TAG, "Logout button clicked");
                LogoutUser();
				}
			});
		button3.setPosition(170, 0);
		stage.addActor (button3);

		// create the "Restore Accounts" button
		final TextButton button4 = new TextButton ("Restore\nAccounts", skin, "default");
		button4.setWidth (80);
		button4.setHeight (40);
		button4.addListener (new ClickListener() {
            @Override
			public void clicked(InputEvent event, float x, float y) {
				Gdx.app.log (TAG, "Restore Accounts button clicked");
                // try to get external access if it hasn't already been granted
                systemAccess.RequestExternalAccess();
                firstTextField = new TextField("", skin);
				Table table = new Table(skin);
				table.add("File Path ").align(Align.right);
				table.add(firstTextField);
                table.row();
                Label warning = new Label("WARNING: password will be set\n\rto what is in the restore file", skin);
                warning.setColor(Color.RED);
                table.add(warning).colspan(2);
				Dialog editDialog = new Dialog("Restore Accounts", skin) {
					protected void result(Object object) {
						Gdx.app.log (TAG, "Restore Accounts dialog: chosen = " + object);
						//Gdx.app.log (TAG, "Restore Accounts dialog: file path = " + firstTextField.getText());
						if (object.equals("ok"))
							RestoreAccounts();
                        Gdx.input.setOnscreenKeyboardVisible(false);
						}
					};
				editDialog.getContentTable().add(table);
				editDialog.button("OK", "ok");
				editDialog.button("Cancel", "cancel");
                editDialog.scaleBy(.5f);
                editDialog.show(stage).setX(20f);
				stage.setKeyboardFocus(firstTextField);
				}
			});
		button4.setPosition(240, 0);
		stage.addActor (button4);

		// create the "Save Accounts" button
		final TextButton button5 = new TextButton ("Save\nAccounts", skin, "default");
		button5.setWidth (80);
		button5.setHeight (40);
		button5.addListener (new ClickListener() {
            @Override
			public void clicked(InputEvent event, float x, float y) {
				Gdx.app.log (TAG, "Save button clicked");
                // try to get external access if it hasn't already been granted
                systemAccess.RequestExternalAccess();
				firstTextField = new TextField("", skin);
				Table table = new Table(skin);
				table.add("File Path ").align(Align.right);
				table.add(firstTextField);
				Dialog editDialog = new Dialog("Save Accounts", skin) {
					protected void result(Object object) {
						Gdx.app.log (TAG, "Save Accounts dialog: chosen = " + object);
						//Gdx.app.log (TAG, "Save Accounts dialog: file path = " + firstTextField.getText());
						if (object.equals("ok"))
							SaveAccounts();
                        Gdx.input.setOnscreenKeyboardVisible(false);
						}
					};
				editDialog.getContentTable().add(table);
				editDialog.button("OK", "ok");
				editDialog.button("Cancel", "cancel");
                editDialog.scaleBy(.5f);
                editDialog.show(stage).setX(20f);
				stage.setKeyboardFocus(firstTextField);
				}
			});
		button5.setPosition(322, 0);
		stage.addActor (button5);

		appState = AppStates.INITILIZED;
        Gdx.input.setInputProcessor (inputMultiplexer);
		}


    private void DisplayInformationDialog (String title, String text) {
        Dialog errorDialog = new Dialog(title, skin);
        errorDialog.text(text);
        errorDialog.button("OK", skin);
        errorDialog.show(stage);
    }


	private void ChangeAppPassword() {
        if (firstTextField.getText().equals(secondTextField.getText())) {
			inputPassword = firstTextField.getText();
			PersistPassword();
			for (Account a : accounts) {
				PersistAccount(a);
				}
			}
		else {
            DisplayInformationDialog ("Error", "Password \"" + firstTextField.getText() +
                                      "\" not confirmed by \"" + secondTextField.getText() +"\"");
			}
		}


	private void PersistPassword () {
        // TODO: add error checking and exception handling
		String encryptedPassword = passwordEncryptor.encryptPassword(inputPassword);
		Gdx.app.log (TAG, "PersistPassword: new encrypted password=(" + inputPassword +
				" - " + encryptedPassword + ")");
		prefs.putString(PASSWORD_KEY, encryptedPassword);
		prefs.flush();

		textEncryptor = new BasicTextEncryptor();
		// TODO: make this textEncryptor password more robust
		textEncryptor.setPassword(inputPassword);
		}


	private void CheckPassword () {
        // TODO: add error checking and exception handling
        // clear the error text and login dialog from stage
        loginStage.clear();
        inputPassword = firstTextField.getText();
		String savedPassword = prefs.getString(PASSWORD_KEY, "Not stored");
		Gdx.app.log (TAG, "CheckPassword: saved password=(" + savedPassword + ")");
		try {
			if (savedPassword.equals("Not stored")) {
				// initialize the app to this new password
				PersistPassword();
				appState = AppStates.PW_PASSED;
				return;
				}
			else if (passwordEncryptor.checkPassword(inputPassword, savedPassword)) {
				Gdx.app.log (TAG, "CheckPassword: password passed");
				if (appState == AppStates.LOGGED_OUT) {
					appState = AppStates.INITILIZED;
                    Gdx.input.setInputProcessor (inputMultiplexer);
					}
				else {
					textEncryptor = new BasicTextEncryptor();
					textEncryptor.setPassword(inputPassword);
					appState = AppStates.PW_PASSED;
					}
				return;
				}
			else {
				Gdx.app.log (TAG, "CheckPassword: password failed)");
				}
			}
		catch (Exception e) {
			Gdx.app.log (TAG, "CheckPassword: password failed - exception caught)");
			}
		// if we get here the password failed so indicate this and prompt again
		errorText = new Label("Incorrect Password", skin);
		errorText.setColor(Color.RED);
		errorText.setPosition(60, 200);
		errorText.setFontScale(2, 2);
		loginStage.addActor(errorText);
		DisplayPasswordDialog("Incorrect password\n\rplease try again");
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
