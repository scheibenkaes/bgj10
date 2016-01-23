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

(def ^:const ui-indicator-max-width 120)

(defn calc-indicator-width [max-width percentage]
  (-> max-width (/ 100) (* percentage) int))

(defn- create-indicator-filling [width]
  (let [filling (shape :filled :set-color (color :red) :rect 0 0 width 12)]
    (assoc filling :x 10 :y 300 :ui/filling-indicator? true :width width)))

(defn- create-fire-indicator []
  (let [outline (shape :line :set-color (color :red) :rect 0 0 ui-indicator-max-width 12)
        filling (create-indicator-filling ui-indicator-max-width)]
    [(assoc outline :x 10 :y 300)
     (assoc filling :x 10 :y 300 :ui/filling-indicator? true)
     (assoc (label "Fire" (color :red)) :x 140 :y 297)]))

(def ^:const fire-burndown-rate 2)

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
           :intensity 100
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

(defn update-ui [entities]
  (let [intensity (:intensity (find-first :fire? entities))
        width (calc-indicator-width ui-indicator-max-width intensity)]
    (conj (remove :ui/filling-indicator? entities)
          (create-indicator-filling 220)
          (label "FFFFF" (color :white)))
    entities))

(defn- display-fire-status [entities]
  (let [intensity (:intensity (find-first :fire? entities))
        width (calc-indicator-width ui-indicator-max-width intensity)]
    (create-indicator-filling width)))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage))
    (add-timer! screen :event/update-ui 1 1)
    (add-timer! screen :event/tick 1 1)
    [(load-sketch) (create-fire) (create-fire-indicator) (create-player)])

  :on-key-down
  (fn [screen entities]
    (->> entities
         (map (partial #'move-player screen))))

  :on-timer
  (fn [{id :id} entities]
    (case id
        :event/update-ui
        (let [fire (find-first :fire? entities)
              e1 (remove :ui/filling-indicator? entities)
              i (create-indicator-filling (calc-indicator-width ui-indicator-max-width (:intensity fire)))
              e (conj (vec e1) i)]
          e)
        :event/tick
        (mapv (fn [e]
                (if (:fire? e)
                  (update e :intensity (fn [i]
                                         (let [new-i (- i fire-burndown-rate)]
                                           (if (neg? new-i) 0 new-i))))
                  e)) entities)
      ))
  
  :on-render
  (fn [screen entities]
    (clear!)
    (let [animated (map (partial #'animate screen) entities)]
      (render! screen animated))))

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

  ;; (fset 'reset-to-main-screen [?\C-s ?R ?E ?S ?E ?T ?\S-  ?T ?O return ?\C-n ?\C-e ?\C-x ?\C-e ?\C-u ?\C- ])

  
  (do
    (require '[bgj10.core.desktop-launcher :as launcher])
    (launcher/-main))
  

  ;; RESET TO MAIN SCREEN  
  (on-gl (set-screen! bgj10-game main-screen))

  (require '[play-clj.repl :as repl])
  
  (repl/e main-screen)

  )
