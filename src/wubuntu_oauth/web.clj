(ns wubuntu-oauth.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :refer [redirect response]]
            [environ.core :refer [env]]
            [clj-http.client :as http]
            [cheshire.core :refer [generate-string parse-string]]))

;; (def WL_DOMAIN "http://localhost:5000")
(def WL_DOMAIN "http://wubuntu-oauth.herokuapp.com")

(def alphanumeric "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890")
(defn get-random-id [length]
  (apply str (repeatedly length #(rand-nth alphanumeric))))

(defn splash []
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body (pr-str ["Hello" :from 'Heroku])})

(defn- oauth-start []
  (let [random-key (get-random-id 40)]
    (-> (redirect (str "https://www.wunderlist.com/oauth/authorize"
                       "?client_id=" (env :api-key)
                       "&redirect_uri=" WL_DOMAIN "/oauth/accept"
                       "&state=" random-key))
        (assoc :session {:random random-key}))))

(defn- oauth-accept [{:keys [code state]} {:keys [random]}]
  (when-not (= state random) (throw (Exception. "Random key does not mach! Authentication cancelled")))
  (let [post-result (http/post 
                      "https://www.wunderlist.com/oauth/access_token"
                      {:body    (generate-string {:client_id     (env :api-key) 
                                                  :client_secret (env :api-secret)
                                                  :code          code})
                       :headers {"X-Api-Version" "2"}
                       :content-type :json
                       :accept :json})
        body (parse-string (:body post-result) true)]
    (redirect (str "/oauth/finished?token=" (:access_token body)))))

(defn- oauth-finish [{:keys [token]}]
  (response "Done."))

(defroutes app
  (GET "/" []
       (splash))
  (GET "/oauth/start" []
    (oauth-start))
  (GET "/oauth/accept" {params :params session :session}
    (oauth-accept params session))
  (GET "/oauth/finished" {params :params}
    (oauth-finish params))
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site #'app) {:port port :join? false})))

;; For interactive development:
;; (.stop server)
;; (def server (-main))
