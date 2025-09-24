# BookBuddies
An Android app to manage a book collection. It acts as a virtual catalogue where users can view, edit and manage their books' data.

## Features
- Viewing and sorting through the book collection, along with collapsable groups, a fast scroll bar and a selection mode for quick actions over multiple book entries.
- General settings such as appearance, bug reporting (logs are written on a local file and can be sent to a Telegram bot) and credits.
- Importing a CSV file of book data (following the data structure from the app [BookCatalogue](https://github.com/eleybourn/Book-Catalogue) by eleybourn.
- Exporting the current app's book data into a CSV file that can be re-imported into the app.
- Automatic search for book covers using search APIs from Google Books, OpenLibrary and Mangadex.
- Viewing screen for each book's data, along with quick action buttons for some operations like marking as read, started/finished reading today, ...
- Editing screen to update a book's data fields
- Adding a new book: each field can be written and manually and/or auto-filled with book info queried from search APIs (using the book's ISBN).
- Scan reader to automatically fetch a book's ISBN from its barcode.
- Calendar to show reading progress on books, and mark events such as a book delivery or release.
- Wishlist for wanted books.

## Architecture and Tools
The app is developped on AndroidStudio, and all its data is stored locally on the phone.
Simple data like appearance preferences are stored locally in a "settings" folder.
Book data is organized using a Android's Room system, and accessed with queries via a DAO (Data Access Object) interface.
Features that use API searches, like reading an ISBN or searching for book covers, need an Internet access. Otherwise the app can be fully used offline.  

While the app is used, any important Log that could be needed for eventual debugging is printed inside a log.txt file on the user's phone.  
The users can "report a bug" (available in the app's Settings page). When a bug is reported, the sender's username, bug description and log.txt file are sent to a Telegram Bot.

## Development
### Log tags
There are different logcat tags to help with debugging:
- BookCover : Automatic API search for book covers.
- BookExport : Exporting app's books into a CSV file.
- BookImport : Reading a CSV file and importing its content as books on the app.
- BookVM : All ViewModel actions taken on a (list of) book(s).
- Compose : When new screens are successfully composed.
- Debug : Only to use when currently debugging a specific feature. There shouldn't be any Debug tag on stable versions.
- Error : Details of an error that was made evident to the user with a Toast.
- NavActions : Navigation from a route to the other, backstack controls when going back in navigation history.
All logs use a severity level: either for d (debug) for simple logs or e (errors) for caught unexpected errors. 
  
### JavaDoc
All functions are commented with typical JavaDoc. To write them more easily, you can follow these steps:
- Go to Settings > Editor > Live Templates
- Click the + to create a new template
- Write what you want for the Abbreviation (e.g. funcDoc) and add a Description if you want
- Copy the following in the Template text:
```
/**
 *
 * 
 * @param 
 * @param 
 * @param 
 * @return
 */
```

- Click Define and tick Kotlin
- Now when you type the Abbreviation in your code and hit Tab, the JavaDoc template will paste.
  
You can also follow these same steps for a class JavaDoc (e.g. classDoc):
```
/**
 * 
 *
 * @property 
 * @property 
 * @property 
 */
```
