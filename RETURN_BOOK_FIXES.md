# Correctifs pour le retour de livres

## Problèmes identifiés

### 1. Scanner de code-barres ML Kit
**Erreur** : `com.google.mlkit.common.MlKitException: This detector is already closed!`

**Cause** : Le scanner était fermé immédiatement après la première détection, mais continuait à recevoir des frames.

**Solution** : 
- Ajout d'un flag `@Volatile isScanning` pour arrêter les analyses après détection
- Méthode `close()` séparée appelée lors de la destruction de la vue
- Gestion propre du cycle de vie avec `onRelease` dans `AndroidView`

### 2. Retour de livre non synchronisé
**Problème** : Le livre retourné par l'emprunteur ne revenait pas dans la bibliothèque du prêteur.

**Cause** : Chaque téléphone a sa propre base de données Room locale. La suppression côté emprunteur n'affectait pas le prêteur.

**Solution** :
1. **Ajout d'un endpoint API** `POST /shareMyBook/return/{bookId}` pour notifier le backend du retour
2. **Ajout d'un endpoint API** `GET /shareMyBook/bookStatus/{bookId}` pour vérifier le statut d'un livre
3. **Modification de `ReturnTransactionViewModel`** : Appel de l'API lors de la confirmation du retour
4. **Modification de `ConfirmReturnViewModel`** : Polling de l'API (toutes les 2 secondes) pour détecter le retour

## Fichiers modifiés

### 1. ScannerActivity.kt
- Ajout du flag `isScanning` dans `BarcodeAnalyzer`
- Méthode `close()` pour fermer proprement le scanner
- Utilisation de `remember` pour l'analyseur
- Callback `onRelease` dans `AndroidView`

### 2. TransactionService.kt
```kotlin
@POST("shareMyBook/return/{bookId}")
suspend fun returnBook(@Path("bookId") bookId: String, @Body request: AcceptRequest): TransactionData

@GET("shareMyBook/bookStatus/{bookId}")
suspend fun getBookStatus(@Path("bookId") bookId: String): TransactionData
```

### 3. BookRepository.kt
```kotlin
suspend fun returnBook(bookId: String, borrower: User): TransactionData
suspend fun getBookStatus(bookId: String): TransactionData
```

### 4. ReturnTransactionViewModel.kt
- Appel de `bookRepository.returnBook()` avant suppression locale
- Gestion des erreurs avec continuation même en cas d'échec API

### 5. ConfirmReturnViewModel.kt
- Polling de l'API avec `bookRepository.getBookStatus()`
- Mise à jour automatique de la base locale quand le retour est détecté
- Timeout de 2 minutes (60 tentatives × 2 secondes)

## Architecture du système de retour

### Flux de retour d'un livre

1. **Prêteur** : Clique sur "Retourner" dans l'onglet "Mes prêts"
   - Ouvre `ConfirmReturnActivity`
   - Génère un QR code contenant `{ "bookUid": "...", "lenderUid": "..." }`
   - Lance le polling de l'API toutes les 2 secondes

2. **Emprunteur** : Clique sur "Retourner" dans l'onglet "Mes emprunts"
   - Ouvre `ScannerActivity`
   - Scanne le QR code du prêteur
   - Ouvre `ReturnTransactionActivity`
   - Confirme le retour
   - **Appelle l'API backend** `POST /shareMyBook/return/{bookId}`
   - Supprime le livre de sa base locale

3. **Backend** : Reçoit la notification de retour
   - Met à jour le statut du livre (borrowerId = null)
   - Stocke l'information dans Firebase

4. **Prêteur** : Son polling détecte le changement
   - `GET /shareMyBook/bookStatus/{bookId}` retourne borrowerId = null
   - Met à jour sa base locale (borrowerId = null, lenderId = null)
   - Affiche "Livre retourné avec succès !"
   - Le livre redevient disponible dans "Ma bibliothèque"

## Configuration Backend requise

⚠️ **IMPORTANT** : Le backend Firebase Cloud Functions doit implémenter les endpoints suivants :

### 1. POST /shareMyBook/return/{bookId}
```javascript
// Reçoit: { borrower: { uid, fullName } }
// Met à jour le livre dans Firestore: borrowerId = null
// Retourne: TransactionData avec le livre mis à jour
```

### 2. GET /shareMyBook/bookStatus/{bookId}
```javascript
// Interroge Firestore pour obtenir le statut actuel du livre
// Retourne: TransactionData avec les informations du livre
```

## Tests à effectuer

1. ✅ **Scanner sans crash** : Vérifier que le scanner ne plante plus après détection
2. ✅ **Retour simple** : 
   - Prêteur génère QR code
   - Emprunteur scanne et confirme
   - Prêteur voit le livre revenir (max 2 secondes)
3. ✅ **Timeout** : Vérifier que le message d'erreur s'affiche après 2 minutes si pas de retour
4. ✅ **Erreur API** : Tester avec le backend arrêté pour vérifier la gestion d'erreur

## Améliorations futures

1. **Notifications Push** : Utiliser Firebase Cloud Messaging pour notifier immédiatement le prêteur
2. **Cache API** : Éviter de surcharger l'API avec trop de requêtes
3. **Retry logic** : Réessayer automatiquement en cas d'échec réseau
4. **Offline mode** : Mettre en file d'attente les retours si pas de connexion

