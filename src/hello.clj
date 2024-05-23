(ns hello
  (:require [io.pedestal.http :as http]
            [clojure.repl :refer [doc]]
            [io.pedestal.http.route :as route]))

(comment
  (= (respond-hello {}) {:status 200, :body "Hello, world!"}))

(defn respond-hello [_request] {:status 200, :body "Hello, world!"})

(def routes
  (route/expand-routes #{["/greet" :get respond-hello :route-name :greet]}))

(comment
  (route/try-routing-for routes :prefix-tree "/greet" :get)
  (route/try-routing-for routes :prefix-tree "/greet" :post)
  (route/try-routing-for routes :prefix-tree "/greet" :put)
  (doc route/try-routing-for))

(defn create-server
  []
  (http/create-server
    {::http/routes routes, ::http/type :jetty, ::http/port 8890}))

(defn start [] (http/start (create-server)))

(comment
  (start))

