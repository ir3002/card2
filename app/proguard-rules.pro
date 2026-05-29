## CardBudget ProGuard Rules

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Models
-keep class com.cardbudget.data.entity.** { *; }
-keep class com.cardbudget.data.dao.** { *; }

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker

# Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
