(ns wubuntu-oauth.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :refer [redirect response]]
            [environ.core :refer [env]]))

(defn splash []
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body (pr-str ["Hello" :from 'Heroku])})

(defn- oauth-start []
  (let [random-key "some-random-value"]
    (-> (redirect (str "https://www.wunderlist.com/oauth/authorize"
                       "?client_id=" (env :api-key)
                       "&redirect_uri=http://wubuntu-oauth.herokuapp.com/oauth/accept"
                       "&random="))
        (assoc :session {:random random-key}))))

(defn- oauth-accept [{:keys [code]} {:keys [random]}]
  (response [code random]))

(defroutes app
  (GET "/" []
       (splash))
  (GET "/oauth/start" []
    (oauth-start))
  (GET "/oauth/accept" {params :params session :session}
    (oauth-accept params session))
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site #'app) {:port port :join? false})))

;; For interactive development:
;; (.stop server)
;; (def server (-main))
