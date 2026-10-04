# Glance instantiates action callbacks by class name
-keep class * implements androidx.glance.appwidget.action.ActionCallback { <init>(); }
# WorkManager creates workers by class name
-keep class * extends androidx.work.ListenableWorker { <init>(android.content.Context, androidx.work.WorkerParameters); }
# Activity started by name from feature modules
-keep class com.example.lifeorganizer.MainActivity
# Keep line numbers so shared crash reports stay readable
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
