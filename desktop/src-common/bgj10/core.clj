(ns bgj10.core
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.ui :refer :all]))

(defn load-sketch []
  (let [t (texture "Design.png")]
    t))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage))
    [(load-sketch)])
  
  :on-render
  (fn [screen entities]
    (clear!)
    (render! screen entities)))

(defscreen error-screen
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage))
    (label "ERROR!" (color :white)))
  
  :on-render
  (fn [screen entities]
    (clear!)
    (render! screen entities)))

(defgame bgj10-game
  :on-create
  (fn [this]
    (set-screen! this main-screen)))


(comment
  (set-screen-wrapper! (fn [screen screen-fn]
                         (try (screen-fn)
                              (catch Exception e
                                (.printStackTrace e)
                                (set-screen! bgj10-game blank-screen)))))

  (do
    (require '[bgj10.core.desktop-launcher :as launcher])
    (launcher/-main))
  
  (require '[play-clj.repl :as repl])
  
  (on-gl (set-screen! bgj10-game main-screen))
  
  (repl/e main-screen))
