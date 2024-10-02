(ns carmine-mq-issue-repro.core
  (:require [clojure.pprint :refer [pprint]]
            [taoensso.carmine :as car]
            [taoensso.carmine.connections :refer [get-conn release-conn]]
            [taoensso.carmine.message-queue :as mq])
  (:gen-class))

(defonce conn-opts
  {:pool {}, :spec {:host "localhost", :port 6379, :password "foobared"}})

(defmacro timed-exec
  "Times and evaluates expr. Returns vector of time taken (msecs) and the value of expr."
  [msg & expr]
  `(let [start# (. System (nanoTime))
         ret# ~@expr]
     (println ~msg
              (/ (double (- (. System (nanoTime)) start#)) 1000000.0)
              "msecs")
     ret#))

(defn worker
  [qname]
  (mq/worker conn-opts
             qname
             ; more threads increases difference in time between 3.4.1 and 3.2.0
             {:nthreads 1}))

(defn -main
  [& args]
  (def queues (map #(str "test-queue." %) (range 16)))
  (def workers (map worker queues))
  (reduce :start workers)
  (println "Testing")
  (timed-exec
    "Overall"
    (let [results (vec (map #(timed-exec
                               %
                               (car/wcar
                                 conn-opts ; much slower in 3.4.1
                                 ; (assoc conn-opts :pool :none) ; slightly faster in 3.4.1
                                 (car/llen (format "carmine:mq:%s:mid-circle" %))
                                 (car/scard (format "carmine:mq:%s:done" %))
                                 (car/hlen (format "carmine:mq:%s:locks" %))))
                         queues))]
      (println "Results" results)))
  (reduce :stop workers)
  (println "Done"))
