(ns bgj10.core
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.ui :refer :all]))

(defn load-sketch []
  (let [t (texture "Design.png")]
    t))

(defn- split-texture [t width]
  (-> t
      (texture! :split width (texture! t :get-region-height))
      (aget 0)
      (->> (map texture*))))

(defn- create-fire-indicator []
  (let [outline (shape :line :set-color (color :red) :rect 0 0 120 12)
        filling (shape :filled :set-color (color :red) :rect 0 0 111 12)]
    [(assoc outline :x 10 :y 300)
     (assoc filling :x 10 :y 300 :ui/filling-indicator? true)
     (assoc (label "Fire" (color :red)) :x 140 :y 297)]))

(defn- create-fire []
  (let [x 305 y 80
        t (texture "Fire.png")
        tiles (split-texture t 24)
        burning-bright-anim (animation 0.8 tiles :set-play-mode (play-mode :loop))]
    (assoc t
           :x x
           :y y
           :fire? true
           :scale 0.5
           :intensity 1.0
           :anim/burning-bright burning-bright-anim)))

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

(defn animate [screen entity]
  (cond
    (:fire? entity)
    (let [frame (animation->texture screen (:anim/burning-bright entity))]
      (merge entity frame))
    :else entity))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage))
    [(load-sketch) (create-fire) (create-fire-indicator) (create-player)])

  :on-key-down
  (fn [screen entities]
    (->> entities
         (map (partial #'move-player screen))))
  
  :on-render
  (fn [screen entities]
    (clear!)
    (render! screen (->> entities
                         (map (partial #'animate screen))))))

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

  ;; (fset 'reset-to-main-screen
  ;;    [?\C-s ?R ?E ?S ?E ?T ?\S-  ?T ?O return ?\C-n ?\C-e ?\C-x ?\C-e ?\C-u ?\C- ])

  
  (do
    (require '[bgj10.core.desktop-launcher :as launcher])
    (launcher/-main))
  
  (require '[play-clj.repl :as repl])

  ;; RESET TO MAIN SCREEN  
  (on-gl (set-screen! bgj10-game main-screen))
  
  (repl/e main-screen))
