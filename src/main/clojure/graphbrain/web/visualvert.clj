(ns graphbrain.web.visualvert
  (:require [graphbrain.db.gbdb :as db]
            [graphbrain.db.id :as id]
            [graphbrain.db.maps :as maps]
            [graphbrain.db.entity :as entity]
            [graphbrain.db.urlnode :as url]
            [graphbrain.db.text :as text]
            [graphbrain.db.context :as context]))

(declare edge-id->text)

(defn id->label
  [id]
  (case (id/id->type id)
    :entity (entity/label id)
    :edge (edge-id->text id)
    :context (context/label id)
    :user (entity/label id)
    :edge-type (entity/text id)
    :url (url/id->url id)
    id))

(defn id->html
  [id ctxt]
  (if (= (id/id->type id) :edge-type)
    (id->label id)
    (str "<a href='/n/" (:id ctxt) "/" id "'>"
         (id->label id)
         "</a>")))

(defn edge-id->text
  [edge-id ctxt]
  (let [labels (map #(id->html (id/eid->id %) ctxt)
                    (id/id->ids edge-id))]
    (str (second labels)
         " "
         (first labels)
         " "
         (clojure.string/join " "
                              (rest
                               (rest labels))))))

(defn id->visual
  [gbdb id ctxts]
  (let [vtype (id/id->type id)
        vert (maps/id->vertex id)
        vert (maps/local->global vert)]
    (case (:type vert)
      :entity (let [sub (entity/subentities vert)
                    sub (map #(hash-map :id (id/eid->id %)
                                        :text (entity/description %)) sub)]
                (assoc vert
                  :sub sub
                  :text (entity/label id)))
      :url (let [url (url/url vert)
                 title (url/title gbdb id ctxts)
                 title (if (empty? title) url title)
                 icon (str "http://www.google.com/s2/favicons?domain=" url)]
             (assoc vert
               :text title
               :url url
               :icon icon))
      :user (let [u (db/getv gbdb id ctxts)]
              {:id id
               :type :user
               :text (:name u)
               :sub [{:id "#" :text "GraphBrain user"}]})
      :text (text/id->text gbdb id)
      :context {:id id
                :type :context
                :text (context/label id)
                :sub [{:id "#" :text "a GraphBrain"}]}
      (assoc vert
        :text id))))