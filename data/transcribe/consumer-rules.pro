# Keep Vosk classes and their members
-keep class org.vosk.** { *; }
-keepclassmembers class org.vosk.** { *; }

# Keep JNA (Java Native Access) classes
-keep class com.sun.jna.** { *; }
-dontwarn com.sun.jna.**
