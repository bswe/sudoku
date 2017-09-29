package com.bswe;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlReader.Element;
import com.badlogic.gdx.utils.viewport.StretchViewport;

import org.jasypt.util.password.StrongPasswordEncryptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jasypt.util.text.BasicTextEncryptor;
import org.jasypt.util.text.StrongTextEncryptor;
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
	public static final int SCREEN_WIDTH = 400;
	public static final int SCREEN_HEIGHT = 660;
	private static final String PASSWORD_KEY = "1";
	private static final String NUMBER_OF_ACCOUNTS_KEY = "2";

	private enum AppStates {PW_REQUIRED, PW_PASSED, INITILIZED, LOGGED_OUT, LOGGED_IN}

	private static final String TAG = pWallet.class.getName();

	private String inputPassword = "";

	private TextField firstTextField;
	private TextField secondTextField;
	private TextField thirdTextField;

	private AppStates appState = AppStates.PW_REQUIRED;

	private Pixmap pixmap;
	private Skin skin;
	private Stage stage;

	private Table scrollTable;
	private Label errorText;

	private List<Account> accounts;

	private int numberOfAccounts;

	private StrongPasswordEncryptor passwordEncryptor = new StrongPasswordEncryptor();

	private BasicTextEncryptor textEncryptor;

	private Preferences prefs;


	@Override
	public void create () {
		// init preferences for persistent storage
		prefs = Gdx.app.getPreferences("bswe-pwallet");

		skin = new Skin (Gdx.files.internal ("clean-crispy-ui.json"));
		stage = new Stage();
		stage.setViewport (new StretchViewport (SCREEN_WIDTH, SCREEN_HEIGHT, new OrthographicCamera()));

		// display password entry dialog
		DisplayPasswordDialog("");

		Gdx.input.setInputProcessor (stage);
		}


	@Override
	public void render () {
		Gdx.gl.glClearColor (0, 0, 0, 1);
		Gdx.gl.glClear (GL20.GL_COLOR_BUFFER_BIT);

		if (appState == AppStates.PW_PASSED)
			Initialize();
		stage.getViewport().apply();
		stage.act (Gdx.graphics.getDeltaTime());
		stage.draw();
		}


	@Override
	public void resize (int width, int height) {
		Gdx.app.log (TAG, "resize: w=" + width + ", h=" + height);
		stage.getViewport().update (width, height, true);
		}


	@Override
	public void dispose () {
		Gdx.app.log (TAG, "dispose:");
		stage.dispose();
		skin.dispose();
		if (Gdx.app.getClass().getName().endsWith ("LwjglApplication"))
			AL.destroy();
		System.exit(0);
		}


	// load accounts from persistent memory
	private void LoadAccounts () {
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
		Dialog editDialog = new Dialog (msg+"enter password", skin) {
			protected void result (Object object) {
				//Gdx.app.log (TAG, "DisplayPasswordDialog dialog: chosen = " + object);
				//Gdx.app.log (TAG, "DisplayPasswordDialog dialog: password = " + passwordTextField.getText());
				CheckPassword();
				}
			};
		editDialog.button("OK", "ok");
		editDialog.getContentTable().add(table);
		editDialog.show(stage);
		stage.setKeyboardFocus(firstTextField);
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
		Account a = new Account(++numberOfAccounts, accountName, accountUsername, accountPassword);
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
		for (Account a : accounts)
			if (a.Name.equals (accountName)) {
				UnPersistAllAccounts();
				accounts.remove (a);
                numberOfAccounts--;
				PersistAllAccounts();
				break;
				}
		RedisplayAccountsTable();
		}


	private void EditAccount (String accountName) {
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
						if (object.equals("cancel"))
							return;
						//Gdx.app.log (TAG, "editDialog dialog: name = " + firstTextField.getText());
						if (object.equals("change"))
							ChangeAccount (AccountName,
										   firstTextField.getText(),
										   secondTextField.getText(),
										   thirdTextField.getText());
						else
							DeleteAccount (AccountName);
						}
					};
				editDialog.getContentTable().add(table);
				editDialog.button("Delete", "delete");
				editDialog.button("Change", "change");
				editDialog.button("Cancel", "cancel");
				editDialog.show(stage);
				stage.setKeyboardFocus(firstTextField);
			    }
		}


	private void AddAccountsTableToStage () {
		// remove the error text from stage if it exists
		if (errorText != null) {
			errorText.remove();
			errorText = null;
			}
		// first make sure accounts are ordered alphabetically by account name
		Collections.sort (accounts, new AccountComparator());
		// add scrollable accounts table to stage
		Table table = new Table();
		for (Account a: accounts) {
			final TextButton button = new TextButton (a.Name, skin);
			final String name = a.Name;
			button.addListener (new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					EditAccount (name);
					}
				});
			table.add (button).align(Align.right);
			final Label UnText = new Label(a.UserName, skin);
			table.add (UnText).align(Align.left).pad(10);
			final Label PwText = new Label(a.Password, skin);
			table.add (PwText).align(Align.left);
			table.row();
			}
		ScrollPane scroller = new ScrollPane (table);
		scrollTable = new Table(skin);
		scrollTable.setBounds(0, 40, SCREEN_WIDTH, SCREEN_HEIGHT-40);
		scrollTable.align(Align.left);
		scrollTable.add (scroller);
		scrollTable.setBackground(new TextureRegionDrawable(new TextureRegion(new Texture(pixmap))));
		//sTable.debugAll();
		stage.addActor (scrollTable);
		}


	private void RestoreAccounts() {
        Integer index;
		Gdx.app.log (TAG, "RestoreAccounts: file path = " + firstTextField.getText());
        FileHandle file = Gdx.files.external (firstTextField.getText());
        String xml = file.readString();
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
		Gdx.app.log (TAG, "SaveAccounts: file path = " + firstTextField.getText());

		/* TODO:  test code to be removed
		Gdx.app.log (TAG, "create: is external storage available = " + Gdx.files.isExternalStorageAvailable());
		Gdx.app.log (TAG, "create: external storage path root = " + Gdx.files.getExternalStoragePath());
		FileHandle file = Gdx.files.external("MyTestFile");
		file.writeString("My god, it's full of stars", false);
		FileHandle file = Gdx.files.external("Download/My Preferences");   // for android
		String text = file.readString();
		Gdx.app.log (TAG, "create: text read = " + text);
		*/
	    }


	private void Initialize () {
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
					    }
				    };
				editDialog.getContentTable().add(table);
				editDialog.button("Add", "add");
				editDialog.button("Cancel", "cancel");
				editDialog.show(stage);
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
			public void clicked(InputEvent event, float x, float y) {
				firstTextField = new TextField("", skin);
				secondTextField = new TextField("", skin);
				Table table = new Table(skin);
				table.add("new password ").align(Align.right);
				table.add(firstTextField);
				table.row();
				table.add("confirm password ").align(Align.right);
				table.add(secondTextField);
				Dialog editDialog = new Dialog("Change Password", skin) {
					protected void result(Object object) {
						Gdx.app.log (TAG, "Change Password dialog: chosen = " + object);
						//Gdx.app.log (TAG, "Change Password dialog: new password = " + firstTextField.getText());
						if (object.equals("ok"))
							ChangeAppPassword();
						}
					};
				editDialog.getContentTable().add(table);
				editDialog.button("OK", "ok");
				editDialog.button("Cancel", "cancel");
				editDialog.show(stage);
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
			public void clicked(InputEvent event, float x, float y) {
				Gdx.app.log (TAG, "Logout button clicked");
				scrollTable.remove();
				appState = AppStates.LOGGED_OUT;
				DisplayPasswordDialog("");
				}
			});
		button3.setPosition(170, 0);
		stage.addActor (button3);

		// create the "Restore Accounts" button
		final TextButton button4 = new TextButton ("Restore\nAccounts", skin, "default");
		button4.setWidth (80);
		button4.setHeight (40);
		button4.addListener (new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				Gdx.app.log (TAG, "Restore Accounts button clicked");
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
						}
					};
				editDialog.getContentTable().add(table);
				editDialog.button("OK", "ok");
				editDialog.button("Cancel", "cancel");
				editDialog.show(stage);
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
			public void clicked(InputEvent event, float x, float y) {
				Gdx.app.log (TAG, "Save button clicked");
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
						}
					};
				editDialog.getContentTable().add(table);
				editDialog.button("OK", "ok");
				editDialog.button("Cancel", "cancel");
				editDialog.show(stage);
				stage.setKeyboardFocus(firstTextField);
				}
			});
		button5.setPosition(322, 0);
		stage.addActor (button5);

		appState = AppStates.INITILIZED;
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
			Dialog errorDialog = new Dialog("Error", skin);
			errorDialog.text("Password \"" + firstTextField.getText() + "\" not confirmed by \"" +
							 secondTextField.getText() +"\"");
			errorDialog.button("OK", skin);
			errorDialog.show(stage);
			}
		}


	private void PersistPassword () {
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
					AddAccountsTableToStage();
					appState = AppStates.LOGGED_IN;
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
		errorText.setPosition(100, 200);
		errorText.setFontScale(2, 2);
		stage.addActor(errorText);
		DisplayPasswordDialog("Incorrect password: re-");
		}

	}
