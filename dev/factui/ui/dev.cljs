(ns factui.ui.dev
  (:require [factui.api :as f :include-macros true]
            [factui.rum :as fr :refer [*results*] :refer-macros [q]]
            [rum.core :as rum :include-macros true]

            [cljs.pprint :as pprint]
            [factui.impl.store :as store]
            [cljs.core.async :as a])
  (:require-macros [clojure.core.async]))

(fr/set-rebuild-on-refresh true)

(enable-console-print!)

(def schema
  [{:db/ident :task/title
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :task/completed
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one}])

(defn rand-string
  []
  (apply str (repeatedly (+ 5 (rand-int 10))
               #(rand-nth (seq "abcdefghijklmnopqrstuvwxyz")))))

(defn new-tasks
  "Generate txdata for N new tasks"
  [n]
  (repeatedly n (fn []
                  {:task/title (str "Task " (rand-string))
                   :task/completed false})))

(rum/defc Task < (q [:find [?title ?completed]
                     :in ?task
                     :where
                     [?task :task/title ?title]
                     [?task :task/completed ?completed]])
                 {:key-fn (fn [_ id] id)}
                 rum/static

  [app-state ?task]
  (let [[title completed] *results*]
    [:li
     [:span {:style {:cursor "pointer"
                     :font-weight "bold"}
             :on-click (fn []
                         (fr/transact! app-state
                           [{:db/id ?task
                             :task/completed (not completed)}]))}
      (if completed "DONE:" "TODO:")]
     " "
     title]))

(rum/defc TaskList < (q [:find ?t ?title
                         :where
                         [?t :task/title ?title]])
                     rum/static
  [app-state]
  [:div
   [:h1 "Task List"]
   [:button {:on-click (fn []
                         (fr/transact! app-state (new-tasks 1)))}
    "Add Task"]
   [:button {:on-click (fn []
                         (fr/transact! app-state (new-tasks 10)))}
    "Add 10 Tasks"]
   [:button {:on-click (fn []
                         (fr/transact! app-state (new-tasks 500)))}
    "Add 500 Tasks"]
   [:button {:on-click (fn []
                         (pprint/pprint (store/datoms (:store @app-state))))}
    "Dump Datoms"]
   [:br]
   [:br]
   [:div "Results:" (count *results*)]
   [:ul (for [[t _] *results*]
          (Task app-state t))]
   ])

(f/rulebase rulebase factui.ui.dev)

(def initial-data
  [{:task/title "Task A"
    :task/completed false}
   {:task/title "Task B"
    :task/completed false}
   {:task/title "Task C"
    :task/completed false}])

(defn ^:export main
  []
  (let [app-state (fr/initialize
                    #'rulebase
                    schema
                    #'TaskList
                    (.getElementById js/document "root"))]
    (fr/transact! app-state initial-data)))