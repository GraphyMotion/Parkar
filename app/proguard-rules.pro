-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Room : entites manipulees par reflexion lors des migrations
-keep class com.carparking.app.data.model.** { *; }
-keep class com.carparking.app.data.database.** { *; }

# WorkManager : les Workers sont instancies par reflexion
-keep class com.carparking.app.notification.ReminderWorker { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker { *; }

# Glance (widget ecran d'accueil) : etat serialise par reflexion
-keep class com.carparking.app.widget.** { *; }
-keep class androidx.glance.** { *; }
-dontwarn androidx.glance.**

# Composants manifest (receivers/services) deja conserves par AGP,
# mais on garde explicitement les classes qu'ils referencent par nom
-keep class com.carparking.app.bluetooth.** { *; }

# Coroutines : evite les avertissements sur le code de debug interne
-dontwarn kotlinx.coroutines.**
