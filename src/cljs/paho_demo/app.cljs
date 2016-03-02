(ns paho-demo.app
  (:require [reagent.core :as r]
            [cljsjs.paho]
            [json-html.core :refer [edn->hiccup]]
            [cljs.reader :as reader]
            [reagent-forms.core :refer [bind-fields init-field value-of]]))

(enable-console-print!)

(declare client)
(def broker "test.mosquitto.org")
(def topic "/paho/cljsjs/chat/")
(def state (r/atom {:my-name ""
                    :text-area ""
                    :message ""}))

(defn send-message [payload]
  (println "Sending: " payload)
  (let [msg (Paho.MQTT.Message. payload)]
    (set! (.-destinationName msg) (str topic (:my-name @state)))
    (set! (.-qos msg) 2)
    (.send client msg)))

(defn row [label input]
  [:div.row
   [:div.col-md-2 [:label label]]
   [:div.col-md-5 input]])

(defn input [label type id]
  (row label
    [:input.form-control
     {:field type
      :id id
      :on-key-press (fn [e]
                     (when (and (= 13 (.-charCode e)) (= id :message))
                      (do
                        (send-message (pr-str {:message (:message @state) :name (:my-name @state)}))
                        (swap! state assoc-in [:message] ""))))}]))


(def form-template
  [:div
   (input "My name" :text :my-name)
   [:div.row
    [:div.col-md-2]
    [:div.col-md-5
     [:div.alert.alert-danger
      {:field :alert :id :errors.my-name}]]]
   (input "Message:" :text :message)])

(def form2
  [:div
   [:textarea.form-control
    {:field :textarea :id :text-area  :readOnly "true" :rows 10 :cols 10}]])

(defn page []
  (let [doc @state]
    (fn []
      [:div
       [:div.padding]
       [:div.page-header [:h1 "Simple Chat app"]]
       [:div.col-md-5
        [bind-fields
         form-template
         state
         (fn [[id] value extra]
          ;(println id value extra)
          (swap! state assoc-in [id] value))]]



       [:div.col-md-7
        [:h3 "Chats!"]
        [bind-fields
         form2
         state]]])))

(defn init []
  (r/render-component [page]
    (.getElementById js/document "container")))

(defn on-connect []
  (.subscribe client "/paho/cljsjs/chat/#" #js {:qos 2}))

(defn update-text [old {name :name message :message}]
  (let [old-text (:text-area old)]
    (println old-text)
    (assoc-in old [:text-area] (str old-text name ": " message "\n"))))

(defn msg-arrived [msg]
  (let [data (reader/read-string (.-payloadString msg))]
   (println "Recieved:" data)
   ;(swap! state update-in [:text-area] conj (str (:name data) ": " (:message data) ""))))
   (swap! state update-text data)))

(defn connect []
  (let [mqtt (Paho.MQTT.Client. broker 8080 "")
        connectOptions (js/Object.)]
      (set! (.-onConnectionLost mqtt) (fn [reasonCode reasonMessage]
                                          (println reasonCode reasonMessage)))
      (set! (.-onMessageArrived mqtt) (fn [msg] (msg-arrived msg)))
      (set! (.-onSuccess connectOptions) (fn [] (on-connect)))
      (set! (.-onFailure connectOptions ) (fn [_ _ msg] (println "Failure Connect: " msg)))
      (.connect mqtt connectOptions)
   mqtt))

(defonce client (connect))
