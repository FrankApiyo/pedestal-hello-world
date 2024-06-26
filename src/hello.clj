(ns hello
  (:require [clojure.data.json :as json]
            [io.pedestal.log :as l]
            [io.pedestal.http :as http]
            [clojure.repl :refer [doc]]
            [io.pedestal.http.content-negotiation :as content-negotiation]
            [io.pedestal.http.route :as route]))

(comment
  (= (respond-hello {}) {:status 200, :body "Hello, world!"}))

(comment
  (clojure.repl/doc contains?)
  (contains? {:one 1} :one))

(def supported-types
  ["text/html" "application/edn" "application/json" "text/plain"])


(def content-negotiation-interceptor
  (content-negotiation/negotiate-content supported-types))

(defn accepted-types
  [context]
  (l/info :accept-field (get-in context [:request :accept :field]))
  (get-in context [:request :accept :field] "text/plain"))

(defn transform-content
  [body content-type]
  (case content-type
    "text/html" body
    "text/plain" body
    "application/edn" (pr-str body)
    "application/json" (json/write-str body)))

(defn coerce-to
  [response content-type]
  (-> response
      (update :body transform-content content-type)
      (assoc-in [:headers "Content-Type"] content-type)))

(comment
  (clojure.repl/doc l/debug)
  (l/info :test "one"))

(def coerce-body-interceptor
  {:name ::coerce-body,
   :leave (fn [context]
            (l/info :message ">>>>content-type: "
                    :content-type (get-in context
                                          [:response :headers "Content-Type"]))
            (cond-> context
              (nil? (get-in context [:response :headers "Content-Type"]))
                (update-in [:response] coerce-to (accepted-types context))))})

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
  (route/expand-routes
    #{["/greet" :get
       [coerce-body-interceptor content-negotiation-interceptor respond-hello]
       :route-name :greet] ["/echo" :get echo]}))

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

