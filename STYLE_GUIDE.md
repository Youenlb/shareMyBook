# Guide de Style de l'Application ShareMyBook

## 🎨 Palette de Couleurs Moderne

L'application utilise désormais une palette de couleurs moderne inspirée des bibliothèques numériques contemporaines.

### Mode Clair
- **Primaire** : Vert forêt moderne (#2E7D32) - Évoque la nature et les livres
- **Secondaire** : Violet profond (#5E35B1) - Pour les accents et actions importantes
- **Tertiaire** : Orange vif (#FF6F00) - Pour les highlights et notifications
- **Fond** : Gris très clair (#FAFAFA) - Confortable pour les yeux
- **Surface** : Blanc pur (#FFFFFF) - Cartes et conteneurs

### Mode Sombre
- **Primaire** : Vert clair (#81C784) - Adapté au mode sombre
- **Secondaire** : Violet clair (#B39DDB) - Contrastes agréables
- **Tertiaire** : Orange lumineux (#FFAB40) - Visibilité optimale
- **Fond** : Noir profond (#121212) - Réduit la fatigue oculaire
- **Surface** : Gris très foncé (#1E1E1E) - Cartes visibles

### Couleurs Sémantiques
- **Disponible** : Vert (#4CAF50) - Le livre peut être emprunté
- **Prêté** : Orange (#FF9800) - Le livre est prêté à quelqu'un
- **Emprunté** : Bleu (#2196F3) - Vous avez emprunté ce livre
- **Non disponible** : Gris (#9E9E9E) - Statut indéterminé

## 📐 Formes et Espacement

### Formes Arrondies
- **Extra Small** : 4dp - Petits badges
- **Small** : 8dp - Badges de statut
- **Medium** : 12dp - Images de couverture
- **Large** : 16dp - Cartes principales
- **Extra Large** : 28dp - Dialogs et modals

### Espacement
- **Horizontal** : 16dp entre les cartes et les bords
- **Vertical** : 8dp entre les éléments de liste
- **Interne** : 16-24dp de padding dans les cartes
- **Entre éléments** : 8-12dp entre les textes et composants

## 🔤 Typographie

### Hiérarchie Visuelle
- **Display Large** : 57sp - Titres d'accueil
- **Headline Small** : 24sp - Titres de sections
- **Title Medium** : 16sp - Titres de livres
- **Body Large** : 16sp - Texte principal
- **Body Medium** : 14sp - Informations secondaires
- **Label Medium** : 12sp - Labels et badges

## 🎭 Composants Modernes

### Cartes de Livre
- Forme arrondie avec ombre subtile
- Image de couverture avec coins arrondis (120dp)
- Badge de statut avec fond coloré semi-transparent
- Espacement cohérent entre les éléments
- Boutons d'action alignés à droite

### Cartes d'Information
- Fond coloré selon le contexte (succès, erreur, info)
- Padding généreux (24dp)
- Typographie claire et hiérarchisée
- Dividers pour séparer les sections

### QR Codes
- Conteneur en carte avec forme arrondie
- Taille optimale (280dp)
- Texte d'instruction clair
- États de chargement avec CircularProgressIndicator

### Boutons
- Utilisation du primary color
- Padding vertical pour meilleure touche
- Texte en labelLarge
- États disabled gérés

## 🌟 Améliorations Apportées

### Interface Utilisateur
1. **Navigation moderne** : Barre de navigation inférieure avec icônes claires
2. **Cartes élégantes** : Toutes les informations dans des cartes bien structurées
3. **Feedback visuel** : États de chargement, succès et erreur clairement différenciés
4. **Espacement cohérent** : Utilisation systématique des espacements standards

### Expérience Utilisateur
1. **Lecture facilitée** : Hiérarchie typographique claire
2. **Statuts évidents** : Badges colorés pour identifier rapidement l'état des livres
3. **Actions intuitives** : Boutons contextuels selon l'état du livre
4. **Transitions fluides** : Animations d'apparition des éléments de liste

### Accessibilité
1. **Contrastes élevés** : Couleurs respectant les normes WCAG
2. **Zones tactiles** : Tailles de boutons suffisantes (48dp minimum)
3. **Support du mode sombre** : Palette complète pour réduire la fatigue oculaire
4. **Textes lisibles** : Tailles de police appropriées

## 🚀 Utilisation

Le thème est automatiquement appliqué via `MitosBookingTheme` :
```kotlin
MitosBookingTheme {
    // Votre contenu ici
}
```

Pour désactiver les couleurs dynamiques (Android 12+) et forcer la palette personnalisée :
```kotlin
MitosBookingTheme(dynamicColor = false) {
    // Votre contenu ici
}
```

## 📱 Compatibilité

- **Android 5.0+** (API 21+)
- **Mode sombre** : Basculement automatique selon les préférences système
- **Material Design 3** : Utilisation des derniers composants Material
- **Compose** : Interface entièrement en Jetpack Compose

