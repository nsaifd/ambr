# Room, Compose, WorkManager, ML Kit, CameraX and Play Billing ship their own consumer rules.

# Backups store enum names as text ("Kitchen", "Sold"); keep the names stable across releases.
-keepclassmembers enum app.ambr.** { *; }

# Readable crash reports in Play Console (mapping file is uploaded with the bundle).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
