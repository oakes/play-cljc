To build this project, you'll need the Clojure CLI tool:

https://clojure.org/guides/deps_and_cli


To develop in a browser with live code reloading:

```
clj -A:dev
```


To build a release version for the web:

```
clj -A:prod:play-cljc
```


To develop the native version:

```
clj -A:dev native

# NOTE: On Mac OS, you need to add the macos alias:

clj -A:dev:macos native
```


To build the native version as a jar file:

```
clj -A:prod uberjar
```
