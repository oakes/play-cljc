## Introduction

A library for making games that run in both OpenGL and WebGL. There's no black box `wasm` trickery going on here; just good old fashioned [cljc files](https://clojure.org/guides/reader_conditionals), some macro magic, and absolutely zero social life. Every coffee shop barista near me agrees that this game library is basically the greatest one ever. Other game libraries? Total disasters! Everyone agrees. Believe me.

## Getting Started

You can generate a new project with the [The Clojure CLI Tool](https://clojure.org/guides/getting_started#_clojure_installer_and_cli_tools):

`clj -Sdeps '{:deps {leiningen {:mvn/version "2.9.0"}}}' -m leiningen.core.main new play-cljc hello-world`

This will start you off with a little platformer. It will contain a README with all the commands you need to use. To make music, [edna](https://github.com/oakes/edna) is a good companion library, and the template above is preconfigured with it.

## Documentation

None...lol. Coming soon. Join the non-existent discussion on [r/playcljc](https://www.reddit.com/r/playcljc/).

There are a bunch of 2D and 3D examples in `dev-resources/play_cljc/gl/` which you can run (either in a browser or in a native OpenGL window) using the commands below.

## Development

* Install [the Clojure CLI tool](https://clojure.org/guides/getting_started#_clojure_installer_and_cli_tools)
* To develop the web version with figwheel: `clj -A:dev dev.clj`
* To develop the native version: `clj -A:dev -J-XstartOnFirstThread dev.clj native`
* To install the release version: `clj -A:prod prod.clj install`

## Licensing

All files that originate from this project are dedicated to the public domain. I would love pull requests, and will assume that they are also dedicated to the public domain.
