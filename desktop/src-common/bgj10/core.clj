(ns bgj10.core
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.math :refer :all]
            [play-clj.ui :refer :all]))

(defn load-sketch []
  (let [t (texture "Design.png")]
    (println "load")
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
        burning-bright-anim (animation 0.2 tiles :set-play-mode (play-mode :loop))]
    [(assoc t
             :x x
             :y y
             :fire? true
             :scale 0.5
             :intensity 100
             :anim/burning-bright burning-bright-anim)
     (assoc (shape :line :set-color (color :green) :rect 0 0 48 48)
            :fire-bounding-box? true
            :x (- x 10) :y (- y 3)
            :w 48 :h 48)]))

(defn- create-woods []
  (let [[w h] [24 55]
        t (shape :line :set-color (color :green)
                 :rect 0 0 w h)]
    (assoc t
           :x 510 :y 78
           :w w :h h
           :woods/bounding-box? true)))

(def ^:const max-wood 5)

(defn- create-player []
  (let [[w h] [16 24]
        p (shape :filled
                 :set-color (color :blue)
                 :rect 0 0 w h)]
    (assoc p
           :player? true
           :speed 3
           :wood 0
           :x 326
           :y 78
           :w w :h h)))

(defn create-rectangle [e]
  (let [{x-e :x y-e :y w-e :w h-e :h} e]
    (rectangle x-e y-e w-e h-e)))

(defn- player-in-woods? [entities]
  (let [p (find-first :player? entities)
        w (find-first :woods/bounding-box? entities)
        r-p (create-rectangle p)
        r-w (create-rectangle w)]
    (rectangle! r-p :overlaps r-w)))

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

(defn- display-fire-status [entities]
  (let [intensity (:intensity (find-first :fire? entities))
        width (calc-indicator-width ui-indicator-max-width intensity)]
    (create-indicator-filling width)))

(defn- player-chopped-wood [entities]
  (map (fn [{:keys [player? wood] :as e}]
         (if player?
           (update e :wood #(if (>= % max-wood) max-wood (inc %)))
           e)) entities))

(defn- create-wood-indicator []
  (let [s "%d   Wood"
        l (label (format s 0) (color :brown))]
    (assoc l
           :x 120 :y 277
           :wood-label? true
           :template s)))

(defn update-ui [entities]
  (map (fn [{:keys [wood-label? template] :as e}]
         (if wood-label?
           (let [{wood :wood} (find-first :player? entities)]
             (label! e :set-text (format template wood))
             e)
           e)) entities))

(defn player-at-fire? [entities]
  (let [player (find-first :player? entities)
        fire (find-first :fire-bounding-box? entities)
        r-player (create-rectangle player)
        r-fire (create-rectangle fire)]
    (rectangle! r-fire :overlaps r-player)))

(def ^:const wood-factor 7)

(defn drop-wood-at-fire [entities]
  (when (->> entities (find-first :player?) :wood pos?)
    (map (fn [{:keys [player? fire?] :as e}]
           (cond
             player?
             (update e :wood #(if (zero? %) 0 (dec %)))
             fire?
             (update e :intensity #(let [n (+ % wood-factor)]
                                     (if (> n 100) 100 n)))
             :else e)) entities)))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage))
    (add-timer! screen :event/update-ui 1 1)
    (add-timer! screen :event/tick 1 1)
    [(create-fire)
     (create-fire-indicator)
     (create-wood-indicator)
     (create-player)
     (create-woods)])

  :on-key-down
  (fn [screen entities]
    (remove-timer! screen :event/in-woods)
    (remove-timer! screen :event/at-fire)
    (->> entities
         (map (partial #'move-player screen))))

  :on-key-up
  (fn [{:keys [key] :as screen} entities]
    (cond
      (= key (key-code :right))
      (when (player-in-woods? entities)
        (println "Player in woods, starting chopping timer")
        (add-timer! screen :event/in-woods 1)
        nil)
      
      (= key (key-code :left))
      (when (player-at-fire? entities)
        (println "Burn Baby Burn!")
        (add-timer! screen :event/at-fire 1)
        nil)
      )
    )
  
  :on-timer
  (fn [{id :id :as screen} entities]
    (case id
        :event/update-ui
        (let [fire (find-first :fire? entities)
              e1 (remove :ui/filling-indicator? entities)
              i (create-indicator-filling (calc-indicator-width ui-indicator-max-width (:intensity fire)))
              e (conj (vec e1) i)]
          e)

        :event/in-woods
        (do
          (println "I'm a lumberjack..")
          (when (< (->> entities (find-first :player?) :wood) max-wood)
            (sound "Hit.wav" :play))
          (add-timer! screen :event/in-woods 1)
          (player-chopped-wood entities))

        :event/at-fire
        (do
          (println "Adding to fire")
          (add-timer! screen :event/at-fire 1)
          (drop-wood-at-fire entities))
        
        :event/tick
        (mapv (fn [e]
                (if (:fire? e)
                  (update e :intensity (fn [i]
                                         (let [new-i (- i fire-burndown-rate)]
                                           (if (neg? new-i) 0 new-i))))
                  e)) entities)))
  
  :on-render
  (fn [screen entities]
    (let [animated (->> entities
                        (map (partial #'animate screen))
                        update-ui)]
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

(defscreen background-screen
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage))
    [(load-sketch)])

  :on-render
  (fn [screen entities]
    (clear!)
    (render! screen entities)))

(defgame bgj10-game
  :on-create
  (fn [this]
    (set-screen! this background-screen main-screen)))


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
  (on-gl (set-screen! bgj10-game background-screen main-screen))

  (require '[play-clj.repl :as repl])
  
  (repl/e main-screen)

  )
