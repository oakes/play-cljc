## Introduction

A Clojure and ClojureScript library for making games that run in both OpenGL and WebGL.

## Development

* Install [the Clojure CLI tool](https://clojure.org/guides/getting_started#_clojure_installer_and_cli_tools)
* To develop the web version with figwheel: `clj -A:dev dev.clj`
* To develop the native version: `clj -A:dev -J-XstartOnFirstThread dev.clj native`
* To install the release version: `clj -A:prod prod.clj install`

## Licensing

All files that originate from this project are dedicated to the public domain. I would love pull requests, and will assume that they are also dedicated to the public domain.
