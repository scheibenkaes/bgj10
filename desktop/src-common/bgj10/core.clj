(ns bgj10.core
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.ui :refer :all]))

(defn load-sketch []
  (let [t (texture "Design.png")]
    t))

(defn- create-player []
  (let [p (shape :filled
                 :set-color (color :blue)
                 :rect 0 0 16 24)]
    (assoc p
           :player? true
           :speed 3
           :x 326
           :y 78)))

(def ^:const player-limit-left 100)
(def ^:const player-limit-right 520)

(defn- move-player [screen {:keys [player? speed x y] :as entity}]
  (if player?
    (condp = (:key screen)
      (key-code :right)
      (let [new-x (+ x speed)]
        (if (< new-x player-limit-right)
          (assoc entity :x new-x)
          entity))

      (key-code :left)
      (let [new-x (- x speed)]
        (if (> new-x player-limit-left)
          (assoc entity :x new-x)
          entity))

      entity)
    entity))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage))
    [(load-sketch)
     (create-player)])

  :on-key-down
  (fn [screen entities]
    (->> entities
         (map (partial #'move-player screen))))
  
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
                                (set-screen! bgj10-game error-screen)))))

  #_(fset 'reset-to-main-screen
   [?\C-s ?R ?E ?S ?E ?T ?\S-  ?T ?O return ?\C-n ?\C-e ?\C-x ?\C-e ?\C-u ?\C- ])

  
  (do
    (require '[bgj10.core.desktop-launcher :as launcher])
    (launcher/-main))
  
  (require '[play-clj.repl :as repl])

  ;; RESET TO MAIN SCREEN  
  (on-gl (set-screen! bgj10-game main-screen))
  
  (repl/e main-screen))
