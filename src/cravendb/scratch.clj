(ns cravendb.scratch
  "The sole purpose of this file is to act as a place to play with stuff in repl"
  (:require [cravendb.indexing :as indexing]
            [cravendb.documents :as docs]
            [cravendb.client :as client]
            [cravendb.query :as query]
            [cravendb.indexstore :as indexes]
            [cravendb.indexengine :as indexengine]
            [cravendb.storage :as storage]
            [me.raynes.fs :as fs]
            [ring.adapter.jetty :refer [run-jetty]]
            [cravendb.http :as http]  
            [cravendb.lucene :as lucene])
  (use [cravendb.testing]))

#_ (do
   (with-open [db (indexengine/start 
                   (storage/create-storage "testdb"))]
    (try
      (with-open [tx (.ensure-transaction db)]
        (-> tx
          (docs/store-document "1" (pr-str { :foo "bar" }) ) 
          (docs/store-document "2" (pr-str { :foo "bas" }) ) 
          (docs/store-document "3" (pr-str { :foo "baz" }) )
          (.commit!)))

    (with-open [tx (.ensure-transaction db)]
      (-> tx
          (indexes/put-index { :id "by_bar" :map "(fn [doc] {\"foo\" (:foo doc)})"})   
          (.commit!))) 
      
    (println "waiting for indexing")
    (indexing/wait-for-index-catch-up db)
     (println "waited for indexing, putting index")
  
    (with-open [tx (.ensure-transaction db)]
      (-> tx
          (indexes/put-index { :id "by_foo" :map "(fn [doc] {\"foo\" (:foo doc)})"})   
          (.commit!))) 

    (println "put index, querying against it")
    (println (query/execute db (indexengine/get-engine db) { :index "by_foo" :query "foo:bar" :wait true} )) 

    (finally
      (indexengine/teardown db))))
    (fs/delete-dir "testdb"))


#_ (client/put-index 
    "http://localhost:8080" 
    "by_username" 
    "(fn [doc] {\"username\" (doc :username)})")

#_ (client/put-document 
          "http://localhost:8080" 
          "2" { :username "bob"})

#_ (client/query 
          "http://localhost:8080" 
          { :query "username:bob" :index "by_username" :wait true})

#_ (client/delete-document "http://localhost:8080" "2" )

#_ (doall '(nil nil nil))


#_ (def index (lucene/create-memory-index))
#_ (def wirter (.open-writer index))
#_ (map #(println %1) (.getMethods (type (:writer wirter))))


#_ (.close wirter)
#_ (.close index)
#_ (.delete-all-entries-for wirter "1")
