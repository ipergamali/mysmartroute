# MySmartRoute

Αυτό το project είναι παράδειγμα εφαρμογής Android. Για να βρείτε το **debug token** του Firebase App Check:

1. Προσθέστε την εξάρτηση `firebase-appcheck-debug` μόνο στο `debugImplementation` του `app/build.gradle.kts`.
2. Τρέξτε την εφαρμογή σε debug από το Android Studio.
3. Στο Logcat θα εμφανιστεί μήνυμα όπως:
   ```
   AppCheck debug token: <TOKEN>
   ```
   Αντιγράψτε το και προσθέστε το στο Firebase Console (App Check → Debug tokens). Δεν χρειάζεται επιπλέον εγγραφή.

