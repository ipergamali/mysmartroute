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
