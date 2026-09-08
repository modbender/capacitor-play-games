# Consumer ProGuard/R8 rules applied to apps that depend on this plugin.
# The Capacitor bridge instantiates the plugin reflectively by class name, so
# keep the plugin and its native modules even under full minification.
-keep class com.idleflowgames.playgames.** { *; }
