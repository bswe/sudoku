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
import com.badlogic.gdx.utils.viewport.StretchViewport;

import org.jasypt.util.password.StrongPasswordEncryptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

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
	private static final String PASSWORD_KEY = "p";
	private static final String NUMBER_OF_ACCOUNTS_KEY = "noa";

	private enum AppStates {PW_REQUIRED, PW_PASSED, INITILIZED}
	private enum NewAccountStates {WAITING, NAME_ENTERED, USERNAME_ENTERED, PASSWORD_ENTERED}

	private static final String TAG = pWallet.class.getName();

	private String inputPassword = "";
	private String accountName = "";
	private String userName = "";
	private String accountPassword = "";

	TextField nameTextField;
	TextField usernameTextField;
	TextField passwordTextField;

	private AppStates appState = AppStates.PW_REQUIRED;
	private NewAccountStates newAccountState = NewAccountStates.WAITING;

	private Pixmap pixmap;
	private Skin skin;
	private Stage stage;

	private Table scrollTable;

	private List<Account> accounts = new ArrayList<Account>();

	private int numberOfAccounts;

	StrongPasswordEncryptor passwordEncryptor = new StrongPasswordEncryptor();

	private BasicTextEncryptor textEncryptor;

	/*
	List<Account> accounts = new ArrayList<Account>(Arrays.asList (
					new Account ("Netflixa", "mindspring", "Simba348"),
					new Account ("Netflixb", "mindspring", "Simba348"),
					new Account ("Netflixc", "mindspring", "Simba348"),
					new Account ("Netflixg", "mindspring", "Simba348"),
					new Account ("Netflixh", "mindspring", "Simba348"),
					new Account ("Netflixi", "mindspring", "Simba348"),
					new Account ("Netflixj", "mindspringyomama", "Simba348"),
					new Account ("Netflixk", "mindspring", "Simba348"),
					new Account ("Chase", "gmail", "Simba348"),
					new Account ("Netflixl", "mindspring", "Simba348"),
					new Account ("Netflixm", "mindspring", "Simba348"),
					new Account ("Netflixn", "mindspring", "Simba348"),
					new Account ("Netflixo", "mindspring", "Simba348"),
					new Account ("Netflixp", "mindspring", "Simba348"),
					new Account ("Netflixq", "mindspring", "Simba348"),
					new Account ("Netflixd", "mindspring", "Simba348"),
					new Account ("Netflixe", "mindspring", "Simba348"),
					new Account ("Netflixf", "mindspring", "Simba348"),
					new Account ("Netflixr", "mindspring", "Simba348"),
					new Account ("Netflixs", "mindspring", "Simba348"),
					new Account ("Netflixt", "mindspring", "Simba348"),
					new Account ("Netflixu", "mindspring", "Simba348"),
					new Account ("Netflixv", "mindspring", "Simba348"),
					new Account ("Mindspring", "wcbwcb", "Yomama348")));
	 */

	private Preferences prefs;


	@Override
	public void create () {
		// display password entry dialog
		DisplayPasswordDialog("");

		// init preferences for persistent storage
		prefs = Gdx.app.getPreferences("My Preferences");

		skin = new Skin (Gdx.files.internal ("clean-crispy-ui.json"));
		stage = new Stage();
		stage.setViewport (new StretchViewport (SCREEN_WIDTH, SCREEN_HEIGHT, new OrthographicCamera()));

		/* TODO:  test code to be removed
		boolean isExtAvailable = Gdx.files.isExternalStorageAvailable();
		Gdx.app.log(TAG, "create: is external storage available = " + isExtAvailable);
		String extRoot = Gdx.files.getExternalStoragePath();
		Gdx.app.log(TAG, "create: external storage path root = " + extRoot);
		//FileHandle file = Gdx.files.external("MyTestFile");
		//file.writeString("My god, it's full of stars", false);
		FileHandle file = Gdx.files.external("Download/My Preferences");
		String text = file.readString();
		Gdx.app.log(TAG, "create: text read = " + text);
		*/
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
		if (newAccountState != NewAccountStates.WAITING)
			// go check on new account input
			AddNewAccount ();
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
			String key = Integer.toString(i) + "-";
			String name = prefs.getString(key+"0", "");
			//Gdx.app.log (TAG, "LoadAccounts: ename=(" + ename + ")");
			name = textEncryptor.decrypt(name);
			String userName = prefs.getString(key+"1", "");
			userName = textEncryptor.decrypt(userName);
			String password = prefs.getString(key+"2", "");
			password = textEncryptor.decrypt(password);
			Account a = new Account(i, name, userName, password);
			accounts.add(a);
			}
		}


	private void DisplayPasswordDialog (String msg) {
		Gdx.input.getTextInput (new Input.TextInputListener() {
			@Override
			public void input (String text) {
				inputPassword = text;
				Gdx.app.log (TAG, "Password getTextInput: password=(" + inputPassword + ")");
				CheckPassword();
				}

			@Override
			public void canceled () {
				inputPassword = "canceled by user";
			}
			}, msg+"enter password", "", "");
		}


	private void PersistAccount (Account a) {
		String encryptedText;
		String key = Integer.toString(a.PersistenceIndex) + "-";
		//Gdx.app.log (TAG, "PersistAccount: key=(" + key + ")");
		encryptedText = textEncryptor.encrypt (a.Name);
		prefs.putString(key+"0", encryptedText);
		encryptedText = textEncryptor.encrypt (a.UserName);
		prefs.putString(key+"1", encryptedText);
		encryptedText = textEncryptor.encrypt (a.Password);
		prefs.putString(key+"2", encryptedText);
		prefs.putInteger(NUMBER_OF_ACCOUNTS_KEY, numberOfAccounts);
		prefs.flush();
	}


	private void UnPersistAllAccounts () {
		for (Account a : accounts) {
			String key = Integer.toString(a.PersistenceIndex) + "-";
			prefs.remove(key + "0");
			prefs.remove(key + "1");
			prefs.remove(key + "2");
		}
		prefs.putInteger(NUMBER_OF_ACCOUNTS_KEY, 0);
		prefs.flush();
	}


	private void PersistAllAccounts () {
		Integer i = 0;
		for (Account a : accounts) {
			a.PersistenceIndex = ++i;
			PersistAccount(a);
			}
		prefs.putInteger(NUMBER_OF_ACCOUNTS_KEY, i);
		prefs.flush();
	}


	private void RedisplayAccountsTable() {
		scrollTable.remove();
		AddAccountsTable();
		}


	private void AddNewAccount () {
		// save state for use here and reset global to wait for next state
		NewAccountStates state = newAccountState;
		newAccountState = NewAccountStates.WAITING;
		switch (state) {
			case NAME_ENTERED:
				if (accountName.equals(""))
					return;
				Gdx.app.log (TAG, "addNewAccount: name=(" + accountName + ")");
				// check for account name being unique
				for (Account a: accounts) {
					if (a.Name.equals(accountName)) {
						Dialog errorDialog = new Dialog("Error", skin);
						errorDialog.text("Account with the name (" + accountName + ") already exists");
						errorDialog.button("OK", skin);
						errorDialog.show(stage);
						return;
						}
					}
				// prompt user for account username
				Gdx.input.getTextInput (new Input.TextInputListener() {
					@Override
					public void input (String text) {
						Gdx.app.log (TAG, "TextInputListener: username=" + text);
						userName = text;
						newAccountState = NewAccountStates.USERNAME_ENTERED;
						}

					@Override
					public void canceled() {
						userName = "";
						newAccountState = NewAccountStates.USERNAME_ENTERED;
						}
					}, "enter username", "", "");
				return;
			case USERNAME_ENTERED:
				if (userName.equals(""))
					return;
				Gdx.app.log (TAG, "addNewAccount: username=(" + userName + ")");
				// prompt user for account password
				Gdx.input.getTextInput (new Input.TextInputListener() {
					@Override
					public void input (String text) {
						Gdx.app.log (TAG, "TextInputListener: account password=" + text);
						accountPassword = text;
						newAccountState = NewAccountStates.PASSWORD_ENTERED;
						}

					@Override
					public void canceled () {
						accountPassword = "";
						newAccountState = NewAccountStates.PASSWORD_ENTERED;
						}
					}, "enter account password", "", "");
				return;
			case PASSWORD_ENTERED:
				if (accountPassword.equals(""))
					return;
			}
		Gdx.app.log (TAG, "AddNewAccount: (an=" + accountName + ", un=" + userName + ", pw=" + accountPassword + ")");
		Account a = new Account(++numberOfAccounts, accountName, userName, accountPassword);
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
				nameTextField = new TextField(a.Name, skin);
				usernameTextField = new TextField(a.UserName, skin);
				passwordTextField = new TextField(a.Password, skin);
				Table table = new Table (skin);
				table.add ("Name ").align(Align.right);
				table.add (nameTextField);
				table.row();
				table.add ("Username ").align(Align.right);
				table.add (usernameTextField);
				table.row();
				table.add ("Password ").align(Align.right);
				table.add (passwordTextField);
				Dialog editDialog = new Dialog ("Edit Account Information", skin) {
					protected void result (Object object) {
						Gdx.app.log (TAG, "EditAccount dialog: chosen = " + object);
						if (object.equals("cancel"))
							return;
						//Gdx.app.log (TAG, "editDialog dialog: name = " + nameTextField.getText());
						if (object.equals("change"))
							ChangeAccount (AccountName,
										   nameTextField.getText(),
										   usernameTextField.getText(),
										   passwordTextField.getText());
						else
							DeleteAccount (AccountName);
						}
					};
				editDialog.getContentTable().add(table);
				editDialog.button("Delete", "delete");
				editDialog.button("Change", "change");
				editDialog.button("Cancel", "cancel");
				editDialog.show(stage);
				}
		}


	private void AddAccountsTable () {
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


	private void Initialize () {
		// check preferences for any accounts
		if (prefs.contains(NUMBER_OF_ACCOUNTS_KEY)) {
			// load any persisted accounts
			numberOfAccounts = prefs.getInteger(NUMBER_OF_ACCOUNTS_KEY);
			LoadAccounts();
			}
		else {
			// no accounts persisted yet so initilize the key
			numberOfAccounts = 0;
			prefs.putInteger(NUMBER_OF_ACCOUNTS_KEY, numberOfAccounts);
			}

		// password matched, so show the accounts and buttons
		pixmap = new Pixmap (1, 1, Pixmap.Format.RGB565);
		pixmap.setColor(Color.SALMON);
		pixmap.fill();

		AddAccountsTable();

		// create the "Add Account" button
		final TextButton button1 = new TextButton ("Add\nAccount", skin, "default");
		button1.setWidth (70);
		button1.setHeight (40);
		button1.addListener (new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				Gdx.input.getTextInput (new Input.TextInputListener() {
					@Override
					public void input (String text) {
						Gdx.app.log (TAG, "Add Account TextInputListener: account name=" + text);
						accountName = text;
						newAccountState = NewAccountStates.NAME_ENTERED;
						}

					@Override
					public void canceled() {
						accountName = "";
						newAccountState = NewAccountStates.NAME_ENTERED;
						}
					}, "enter account name", "", "");
				}
			});
		button1.setPosition(0, 0);
		stage.addActor (button1);

		// create the "New Password" button
		final TextButton button2 = new TextButton ("New\nPassword", skin, "default");
		button2.setWidth (80);
		button2.setHeight (40);
		button2.addListener (new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				Gdx.input.getTextInput (new Input.TextInputListener() {
					@Override
					public void input(String text) {
						Gdx.app.log(TAG, "New Password TextInputListener: password=" + text);
						if (text.equals(""))
							return;
						inputPassword = text;
						ConfirmNewPassword ();
						};

					@Override
					public void canceled() {}
					}, "enter new password", "", "");
				}
			});
		button2.setPosition(70, 0);
		stage.addActor (button2);

		Gdx.input.setInputProcessor (stage);
		appState = AppStates.INITILIZED;
		}


	private void ConfirmNewPassword () {
		Gdx.input.getTextInput (new Input.TextInputListener() {
			@Override
			public void input (String text) {
				Gdx.app.log (TAG, "ConfirmNewPassword TextInputListener: password=" + text);
				if (!text.equals(inputPassword)) {
					Dialog errorDialog = new Dialog("Error", skin);
					errorDialog.text("Password \"" + inputPassword + "\" not equal to \"" + text +"\"");
					errorDialog.button("OK", skin);
					errorDialog.show(stage);
					return;
					}
				ChangeAppPassword();
				}

			@Override
			public void canceled () {}
			}, "confirm new password", "", "");
		}


	private void ChangeAppPassword() {
		PersistPassword();
		for (Account a: accounts) {
			PersistAccount(a);
			}
		}


	private void PersistPassword () {
		String encryptedPassword = passwordEncryptor.encryptPassword(inputPassword);
		Gdx.app.log(TAG, "PersistPassword: new encrypted password=(" + inputPassword +
				" - " + encryptedPassword + ")");
		prefs.putString(PASSWORD_KEY, encryptedPassword);
		prefs.flush();

		textEncryptor = new BasicTextEncryptor();
		// TODO: make this textEncryptor password more robust
		textEncryptor.setPassword(inputPassword);
	}


	private void CheckPassword () {
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
				Gdx.app.log(TAG, "CheckPassword: password passed");
				textEncryptor = new BasicTextEncryptor();
				textEncryptor.setPassword(inputPassword);
				appState = AppStates.PW_PASSED;
				return;
				}
			else {
				Gdx.app.log(TAG, "CheckPassword: password failed)");
				}
			}
		catch (Exception e) {
			Gdx.app.log(TAG, "CheckPassword: password failed - exception caught)");
			}
		// if we get here the password failed so indicate this and prompt again
		final Label text = new Label("Incorrect Password", skin);
		text.setColor(Color.RED);
		text.setPosition(100, 200);
		text.setFontScale(2, 2);
		stage.addActor(text);
		DisplayPasswordDialog("Incorrect password: re-");
		}

	}
