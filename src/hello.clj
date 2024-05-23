(ns hello
  (:require [io.pedestal.http :as http]
            [clojure.repl :refer [doc]]
            [io.pedestal.http.route :as route]))

(comment
  (= (respond-hello {}) {:status 200, :body "Hello, world!"}))

(comment
  (clojure.repl/doc contains?)
  (contains? {:one 1} :one))

(defn ok [resp-body] {:status 200, :body resp-body})

(defn resp-400 [] {:status 400, :body "Name can not be empty\n"})

(defn not-found [] {:status 404, :body "Not found\n"})

(def unmentionables #{"YHWH" "Voldemort" "Mxyzptlk" "Rumplestiltskin" "曹操"})

(defn greeting-for
  [nm]
  (cond (unmentionables nm) nil
        (empty? nm) ;; Both nil and 0-length string counts as empty
          "Hello, World!\n"
        :else (str "Hello, " nm "\n")))

(defn respond-hello
  [request]
  (let [nm (get-in request [:query-params :name])
        resp (greeting-for nm)]
    (cond (and (contains? (:query-params request) :name) (empty? nm)) (resp-400)
          resp (ok resp)
          :else (not-found))))

(def echo
  {:name ::echo,
   :enter (fn [context]
            (let [request (:request context)
                  response (ok request)]
              (assoc context :response response)))})

(def routes
  (route/expand-routes #{["/greet" :get respond-hello :route-name :greet]
                         ["/echo" :get echo]}))

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

