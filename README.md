# MySmartRoute

Αυτό το project είναι παράδειγμα εφαρμογής Android.
Προς το παρόν έχει απενεργοποιηθεί το Firebase App Check ώστε να μπορείτε να κάνετε είσοδο χωρίς επιπλέον έλεγχο.
Επιπλέον, η εγγραφή δεν απαιτεί πλέον επιβεβαίωση μέσω SMS.

Για να λειτουργήσει ο χάρτης χρειάζεται να ορίσεις το Google Maps API key στο
`local.properties` της ρίζας του project:

```
MAPS_API_KEY=YOUR_API_KEY
```

Αντικατέστησε το `YOUR_API_KEY` με το πραγματικό κλειδί από το Google Cloud Console.
Επιπλέον, φρόντισε να έχεις ενεργοποιήσει το **Maps SDK for Android** στο Google Cloud
και να μην περιορίζεται το κλειδί σε συγκεκριμένο package μέχρι να το προσθέσεις στο project.

Σε περιβάλλοντα CI μπορείς εναλλακτικά να ορίσεις τη μεταβλητή περιβάλλοντος
`MAPS_API_KEY` ώστε το Gradle script να χρησιμοποιήσει το κλειδί χωρίς να
απαιτείται αρχείο `local.properties`. Αν το κλειδί λείπει, η διαδικασία
build θα εμφανίσει προειδοποίηση ότι το Maps SDK ενδέχεται να μη λειτουργεί
σωστά.

Μπορείς να επιβεβαιώσεις ότι το κλειδί φορτώθηκε σωστά προσθέτοντας στο κώδικα το παρακάτω απόσπασμα:

```kotlin
val apiKey = BuildConfig.MAPS_API_KEY
Log.d("Maps", "API key loaded? ${apiKey.isNotEmpty()}")
```

Έτσι θα δεις ένα μήνυμα στο log που επιβεβαιώνει ότι η εφαρμογή διαβάζει το κλειδί.

Αν αντιμετωπίσεις το σφάλμα **"Unresolved reference: BuildConfig"**, έλεγξε τα παρακάτω:

1. Σιγουρέψου ότι έχεις προσθέσει το import

   ```kotlin
   import com.ioannapergamali.mysmartroute.BuildConfig
   ```

   στην αρχή του αρχείου Kotlin όπου χρησιμοποιείς το `BuildConfig`.
2. Επιβεβαίωσε ότι στο `app/build.gradle.kts` είναι ενεργοποιημένο το `buildConfig`
   μέσω της επιλογής:

   ```kotlin
   buildFeatures {
       buildConfig = true
   }
   ```

3. Τέλος, κάνε "Sync Project with Gradle Files" ώστε να παραχθεί ξανά το αρχείο
   `BuildConfig`. Μετά την επιτυχημένη συγχρονίση, το σφάλμα θα εξαφανιστεί.

### Αν ο χάρτης δεν εμφανίζεται

Αν παρά τις παραπάνω ενέργειες το log γράφει `Maps API key loaded: false` και
βλέπεις στην οθόνη το μήνυμα «Google Maps API key is missing», δοκίμασε τα
εξής:

1. Βεβαιώσου ότι το `local.properties` βρίσκεται στον ίδιο φάκελο με τα
   `settings.gradle.kts` και `gradlew`.
2. Στο αρχείο να υπάρχει *μία* γραμμή της μορφής:

   ```
   MAPS_API_KEY=TO_DIKO_SOU_API_KEY
   ```

   χωρίς εισαγωγικά ή επιπλέον κενά.
3. Εκτέλεσε «Clean Project» και στη συνέχεια «Rebuild Project» για να παραχθεί
   ξανά το `BuildConfig` με το σωστό κλειδί.
