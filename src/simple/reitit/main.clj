(ns simple.reitit.main
  (:require
   [muuntaja.core :as m]
   [reitit.coercion.malli :as rcm]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.spec :as rs]
   [reitit.swagger :as swagger]
   [ring.adapter.jetty :as jetty]
   [ring.middleware.cors :refer [wrap-cors]])
  (:import
   [org.eclipse.jetty.server Server]))

(defn ^:private router
  []
  (ring/router
   ["/organisations/:id/users/:uid"
    {:put {:parameters {:role string?}
           :handler (fn [request]
                      (let [{{:keys [id uid]} :path-params} request
                            {{:strs [role]} :query-params} request]
                        {:status 200 :body (format "Hello World '%s', '%s', '%s'. " id uid role)}))}}]

   {:validate rs/validate
    :data {:coercion rcm/coercion
           :muuntaja m/instance
           :middleware [swagger/swagger-feature
                        muuntaja/format-middleware
                        [wrap-cors :access-control-allow-origin [#".*"]
                         :access-control-allow-methods [:get :put :post :patch :delete]]
                        parameters/parameters-middleware
                        coercion/coerce-exceptions-middleware
                        coercion/coerce-request-middleware
                        coercion/coerce-response-middleware]}}))

(defn start
  [opts]
  (jetty/run-jetty
   (ring/ring-handler (router)
                      (ring/routes
                       (ring/create-default-handler)))
   (assoc opts
          :send-server-version? false
          :send-date-header? false
          :join? false))) ;; false so that we can stop it at the repl!

(defn stop
  [^Server server]
  (.stop server) ; stop is async
  (.join server)) ; so let's make sure it's really stopped!

(comment

 (def server (start {:port 8080}))

 (stop server)

 ;❯ http PUT :8080/organisations/123/users/abcd?role=david)
 ;HTTP/1.1 200 OK)
 ;Transfer-Encoding: chunked)

 ;Hello World '123', 'abcd', 'david'.

 ,)
