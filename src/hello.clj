(ns hello
  (:require [io.pedestal.http :as http]
            [clojure.repl :refer [doc]]
            [io.pedestal.http.route :as route]))

(comment
  (= (respond-hello {}) {:status 200, :body "Hello, world!"}))

(defn ok [resp-body] {:status 200, :body resp-body})

(defn greeting-for
  [nm]
  (if (empty? nm) ;; Both nil and 0-length string counts as empty
    "Hello, World!\n"
    (str "Hello, " nm "\n")))

(defn respond-hello
  [request]
  (let [nm (get-in request [:query-params :name])
        resp (greeting-for nm)]
    (ok resp)))

(def routes
  (route/expand-routes #{["/greet" :get respond-hello :route-name :greet]}))

(comment
  (route/try-routing-for routes :prefix-tree "/greet" :get)
  (route/try-routing-for routes :prefix-tree "/greet" :post)
  (route/try-routing-for routes :prefix-tree "/greet" :put)
  (doc route/try-routing-for))

(def service-map {::http/routes routes, ::http/type :jetty, ::http/port 8890})

;; For interactive development
(defonce server (atom nil))

(defn start-dev
  []
  (reset! server (http/start (http/create-server (assoc service-map
                                                   ::http/join? false)))))

(defn stop-dev [] (http/stop @server))

(defn restart [] (stop-dev) (start-dev))

(comment
  (start-dev)
  (stop-dev)
  (restart))

