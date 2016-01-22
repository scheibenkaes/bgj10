(ns bgj10.core.desktop-launcher
  (:require [bgj10.core :refer :all])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn -main
  []
  (LwjglApplication. bgj10-game "bgj10" 800 600)
  (Keyboard/enableRepeatEvents true))
