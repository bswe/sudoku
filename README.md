# pWallet
libGDX app for Android and Windows - implementation of a password wallet

This libGDX Java application is a password wallet that uses the jasypt encryption library to encrypt the user's application password in a digested form and also all of the information about the accounts that are stored in the wallet.  All this information, password and accounts, are persisted in highly encrypted form so that it can not be stolen. Only the user knows their application password; it is not stored unencrypted.  After the password is correctly entered at the start of the aaplication it is used to seed the encryption/decryption of the account information.  NOTE: if the user forgets his application password there is no way to recover the encrypted account information; SO DON"T FORGET IT!!!
