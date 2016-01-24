## Introduction

You are living in the wilderness of Arizona together with your uncle.
If living with the nagging chap would not be enough, you're also surrounded by hungry wolves.
In order to keep them away you have to make sure your fire is burning.

But thankfully you have your trusty Pipe Tomahawk!
It is more than just a weapon.
It allows you to chop wood for the fire and it can be used as a pipe.
By sitting down and having a smoke with your uncle, you can keep him entertained.
And you better do unless you want the old man to fumble with your fire!
Sure he only wants to help, but he was never good with keeping a fire burning...

## How to play

The goal of the game is to have the fire burn as long as possible.
If it goes out it's Game Over! How well the fire is burning is show in the top left corner.

You control the player symbolized by the blue rectangle (imagine a charming animation of
a native American) with the left and right key on your keyboard.

To gather wood you have to walk to the trees on the right.
Upon arrival release the key and the player will start chopping wood (image a nice animation of said native American).
You can gather up to 5 logs.
When you get back to the fire place the player will begin dropping the logs into the fire (again imagine a funny animation). Make sure not to move while at the fire, the logs will only drop if you stand still.
Dropping the logs will cause the fire'o'meter in the top left corner to get filled up again.

Your uncle's (the yellow rectangle) mood will change when time passes.
If you don't sit down with him and have a smoke (which sadly is not yet implemented) he will get up and start making use of the fire.
As he's not good with fires this will increase the rate of the fire burning out.
If you get back to the fireplace while he's there, you'll send him back to his place.
(Imagine speech bubbles of them having an argument at this point)

## What's missing

- Sitting down having a smoke with your uncle 
- Other uses of the Tomahawk (Fighting, ...)
- Animations for the player and the uncle are missing
- Environment animations 
- Sound


## Contents

* `android/src` Android-specific code
* `desktop/resources` Images, audio, and other files
* `desktop/src` Desktop-specific code
* `desktop/src-common` Cross-platform game code

## Building

All projects can be built using [Nightcode](https://sekao.net/nightcode/), or on the command line using [Leiningen](https://github.com/technomancy/leiningen) with the [lein-droid](https://github.com/clojure-android/lein-droid) plugin.
