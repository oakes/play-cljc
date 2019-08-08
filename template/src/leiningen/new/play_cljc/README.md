To build this project, you'll need the Clojure CLI tool:

https://clojure.org/guides/deps_and_cli


To develop in a browser with live code reloading:

```
clj -A:dev
```


To build a release version for the web:

```
clj -A:prod
```


To develop the native version on each OS:

```
clj -A:dev:linux native

clj -A:dev:macos native

clj -A:dev:windows native
```


To build the native version as a jar file:

```
clj -A:prod uberjar
```
