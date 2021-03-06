(ns bgj10.core
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.math :refer :all]
            [play-clj.ui :refer :all]
            [clj-time.core :as t]))

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

(def uncle-start 125)

(defn create-uncle []
  (let [[x y] [uncle-start 78]
        [w h] [16 24]
        s (shape :filled
                 :set-color (color :yellow)
                 :rect 0 0 w h)
        t (texture "UncleMood.png")
        [happy meh sad] (split-texture t 24)]
    [(assoc happy :mood-indicator? true
            :happy happy :needs-a-smoke meh :pissed sad
            :x 110 :y 250)
     (assoc (label "Uncle mood" (color :yellow))
            :x 140 :y 250)
     (assoc s :x x :y y :w w :h h :uncle? true :mood :satisfied
            :last-smoke (t/now))]))

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

(defn entity-at-fire? [entity entities]
  (let [player entity
        fire (find-first :fire-bounding-box? entities)
        r-player (create-rectangle player)
        r-fire (create-rectangle fire)]
    (rectangle! r-fire :overlaps r-player)))

(def ^:const wood-factor 7)

(defn drop-wood-at-fire [entities]
  (when (->> entities (find-first :player?) :wood pos?)
    (println "Whoosh!")
    (map (fn [{:keys [player? fire?] :as e}]
           (cond
             player?
             (update e :wood #(if (zero? %) 0 (dec %)))
             fire?
             (update e :intensity #(let [n (+ % wood-factor)]
                                     (if (> n 100) 100 n)))
             :else e)) entities)))

(declare bgj10-game main-screen background-screen game-over-screen)

(defn restart-game []
  (on-gl (set-screen! bgj10-game background-screen main-screen)))

(defscreen game-over-screen
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage))
    [(assoc (label (str "GAME OVER!!1!\nThe wolves got you!"
                        "\nPress R to play again") (color :red)
                   :set-alignment (align :center))
            :x (- (/ (width screen) 2) 60)
            :y (/ (height screen) 2))])

  :on-key-down
  (fn [{:keys [key] :as screen} entities]
    (cond
      (= key (key-code :r))
      (restart-game)
      :else nil))
  
  :on-render
  (fn [screen entities]
    (clear!)
    (render! screen entities)))

(defn- game-over! []
  (on-gl (set-screen! bgj10-game game-over-screen)))

(defn check-for-game-over [entities]
  (let [{:keys [intensity]} (find-first :fire? entities)]
    (if (zero? intensity)
      (game-over!)
      entities)))

(def ^:const uncle-interval 6)

(defn- last-smoke-too-far-in-past? [{:keys [last-smoke] :as e} seconds]
  (let [next-smoke (t/plus last-smoke (t/seconds seconds))
        now (t/now)]
    (t/after? now next-smoke)))

(defn update-uncle-mood
  "Update uncles state"
  [entities]
  (map (fn [{:keys [uncle? mood last-smoke] :as e}]
         (if uncle?
           (case mood
             :satisfied
             (if (last-smoke-too-far-in-past? e uncle-interval)
               (do
                 (println "UNCLE NEEDS A SMOKE!! He's not amused")
                 (assoc e :mood :needs-a-smoke))
               e)
             :needs-a-smoke
             (if (last-smoke-too-far-in-past? e (* 2 uncle-interval))
               (do
                 (println "UNCLE NOW IS PISSED!!")
                 (assoc e :mood :pissed))
               e)
             e)
           e)) entities))

(defn move-uncle [entities]
  (map (fn [{:keys [uncle? mood x] :as e}]
         (if uncle?
           (case mood
             :pissed
             (if-not (entity-at-fire? e entities)
               (update e :x inc)
               e)
             :needs-a-smoke
             (if-not (= x uncle-start)
               (update e :x dec)
               e)
             e)
           e)) entities))

(defn show-mood [entities]
  (map (fn [{:keys [mood-indicator?] :as e}]
         (if mood-indicator?
           (let [{mood :mood} (find-first :uncle? entities)]
             (merge e (get e mood)))
           e)) entities))

(defn interact-with-uncle [screen entities]
  (map (fn [{:keys [uncle?] :as e}]
         (if uncle?
           (let [player (find-first :player? entities)
                 both-at-fire? (and (entity-at-fire? e entities)
                                    (entity-at-fire? player entities))]
             (if both-at-fire?
               (do
                 (remove-timer! screen :event/uncle-check-smoke)
                 (add-timer! screen :event/uncle-check-smoke uncle-interval uncle-interval)

                 (assoc e :mood :needs-a-smoke))
               e))
           e)
         ) entities))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage))
    (add-timer! screen :event/update-ui 1 1)
    (add-timer! screen :event/tick 1 1)
    (add-timer! screen :event/uncle-check-smoke uncle-interval uncle-interval)
    [(create-fire)
     (create-fire-indicator)
     (create-wood-indicator)
     (create-uncle)
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
    (when (or (= key (key-code :left))
              (= key (key-code :right)))
      (when (player-in-woods? entities)
          (println "Player in woods, starting chopping timer")
          (add-timer! screen :event/in-woods 1))
      (when (entity-at-fire? (find-first :player? entities) entities)
        (println "Burn Baby Burn!")
        (add-timer! screen :event/at-fire 1))
      nil))
  
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
          (when (< (->> entities (find-first :player?) :wood) max-wood)
            (println "I'm a lumberjack..")
            (sound "Hit.wav" :play))
          (add-timer! screen :event/in-woods 1)
          (player-chopped-wood entities))

        :event/at-fire
        (do
          (add-timer! screen :event/at-fire 1)
          (drop-wood-at-fire entities))

        :event/uncle-check-smoke
        (do
          (update-uncle-mood entities))
        
        :event/tick
        (mapv (fn [e]
                (cond
                  (:fire? e)
                  (let [uncle (find-first :uncle? entities)
                        at-fire? (entity-at-fire? uncle entities)
                        rate (if at-fire? 2 1)]
                    (update e :intensity (fn [i]
                                           (let [new-i (- i (* fire-burndown-rate rate))]
                                             (if (neg? new-i) 0 new-i)))))                  
                  :else e)) entities)))
  
  :on-render
  (fn [screen entities]
    (check-for-game-over entities)
    (let [anim-fn (fn [xs]
                    (animate screen xs))
          interact-fn (partial interact-with-uncle screen)
          animated (->> entities
                        move-uncle
                        show-mood
                        interact-fn
                        (map anim-fn)
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
  (on-gl (set-screen! bgj10-game game-over-screen))

  (require '[play-clj.repl :as repl])
  
  (repl/e main-screen)

  )
