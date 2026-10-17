# LSPosed loads the module entry by the name in META-INF/xposed/java_init.list.
-keep class io.github.tnyfishy.all4hyper.All4HyperModule { <init>(); }
-keep class io.github.libxposed.** { *; }
-dontwarn io.github.libxposed.api.**
