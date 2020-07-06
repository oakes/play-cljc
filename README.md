## Introduction

A library for making games that run in both OpenGL and WebGL. There's no black box `wasm` trickery going on here; just good old fashioned [cljc files](https://clojure.org/guides/reader_conditionals), some macro magic, and absolutely zero social life. Every coffee shop barista near me agrees that this game library is basically the greatest one ever. Other game libraries? Total disasters! Everyone agrees. Believe me.

## Getting Started

You can generate a new project with the [Clojure CLI Tool](https://clojure.org/guides/getting_started#_clojure_installer_and_cli_tools):

`clj -Sdeps "{:deps {leiningen {:mvn/version \""2.9.0\""}}}" -m leiningen.core.main new play-cljc hello-world`

This will start you off with a little platformer. It will contain a README with all the commands you need to use.

## Documentation

* Check out [the example games](https://github.com/oakes/play-cljc-examples)
* Read [the dynadocs](https://oakes.github.io/play-cljc/cljs/play-cljc.gl.core.html)
  * Check out the interactive [2D](https://oakes.github.io/play-cljc/cljs/play-cljc.gl.examples-2d.html) and [3D](https://oakes.github.io/play-cljc/cljs/play-cljc.gl.examples-3d.html) examples
* Join the discussion on [r/playcljc](https://www.reddit.com/r/playcljc/)
* See the screencasts:
  * Intro to play-cljc: https://www.youtube.com/watch?v=y6WpUdECwmA
  * Making play-cljc games with clara rules: https://www.youtube.com/watch?v=WnbQvdyl13k

## Companion Libraries

* [tile-soup](https://github.com/oakes/tile-soup) - Parse tiled maps
* [play-cljc.text](https://github.com/oakes/play-cljc.text) - Render text
* [iglu](https://github.com/oakes/iglu) - Write shaders as Clojure data
* [clarax](https://github.com/oakes/clarax) - Manage state with a rules engine

When you generate a new project with the command above, it'll come preconfigured with:

* [Paravim](https://github.com/oakes/Paravim) - A text editor written with play-cljc
  * Press `Esc` in dev mode to see the editor appear; see [this demo](https://youtu.be/BBw6ZwWFXwQ?t=752)
* [edna](https://github.com/oakes/edna) - Create MIDI music

## Development

* Install [the Clojure CLI tool](https://clojure.org/guides/getting_started#_clojure_installer_and_cli_tools)
* To run the examples in a native window on each OS:
  * `clj -A:dev:linux native`
  * `clj -A:dev:macos native`
  * `clj -A:dev:windows native`
* To install the release version: `clj -A:prod install`

## Licensing

All files that originate from this project are dedicated to the public domain. I would love pull requests, and will assume that they are also dedicated to the public domain.
