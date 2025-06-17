# MySmartRoute

Αυτό το project είναι παράδειγμα εφαρμογής Android. Για να βρείτε το **debug token** του Firebase App Check:

1. Βεβαιωθείτε ότι η εξάρτηση `firebase-appcheck-debug` υπάρχει μόνο στο `debugImplementation` στο `app/build.gradle.kts`.
2. Τρέξτε την εφαρμογή σε debug mode μέσα από το Android Studio.
3. Στο Logcat θα εμφανιστεί μήνυμα με το debug token (`AppCheck debug token: ...`). Αντιγράψτε το και προσθέστε το στο Firebase Console (App Check → Debug tokens).

