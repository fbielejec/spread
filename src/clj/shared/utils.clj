(ns shared.utils
  (:require [clj-http.client :as http-cli]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.walk :as walk]
            [cognitect.transit :as transit])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
           [java.util Base64]))

(def tree-object-extension ".tree")
(def trees-object-extension ".trees")
(def locations-object-extension ".txt")
(def log-object-extension ".log")
(def output-object-extension ".json")

(defn get-env-variable
  [var-name & [required?]]
  (let [var-value (System/getenv var-name)]
    (if (and (empty? var-value)
             required?)
      (throw (Exception. (str "MISSING ENV VARIABLE: " var-name " not defined in environment")))
      var-value)))

(defn decode-transit
  "Decodes the string s into a clojure datastructure.
  s is expected to be a transit json encoded string."
  [s]
  (let [in (ByteArrayInputStream. (.getBytes s))
        reader (transit/reader in :json)]
    (transit/read reader)))

(defn decode-json
  "Decodes JSON string into a clojure datastructure"
  [s]
  (json/read-str s :key-fn keyword))

(defn encode-transit
  "Encodes x into a transit json string."
  [x]
  ;; TODO: Figure out this buffer
  ;; this will probably generate a lot of garbage for the GC
  ;; Creating a global stream isn't going to work for multiple threads
  (let [out (ByteArrayOutputStream. 10000)
        writer (transit/writer out :json)]
    (transit/write writer x)
    (.toString out)))

(defn encode-base64 [to-encode]
  (.encodeToString (Base64/getEncoder) (.getBytes to-encode)))

(defn decode-base64 [to-decode]
  (String. (.decode (Base64/getDecoder) to-decode)))

(defn new-uuid [] (str (java.util.UUID/randomUUID)))

(defn transform-keys
  "Recursively transforms all map keys in coll with t."
  [t coll]
  (let [f (fn [[k v]] [(t k) v])]
    (walk/postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) coll)))

(defn ->camelCase [^String s]
  (string/replace s #"-(\w)"
                  #(string/upper-case (second %1))))

(defn clj->gql
  [m]
  (transform-keys (comp keyword ->camelCase name) m))

(defn file-exists?
  [path]
  (.exists (io/file path)))

(defn http-file-exists?
  "Checks that a resource under `url` exists without retrieving content."
  [url]
  (boolean
   (try
     (http-cli/head url)
     (catch Exception _ nil))))

(defn round
  "Round a double to the given precision"
  [precision d]
  (let [factor (Math/pow 10 precision)]
    (/ (Math/round (* d factor)) factor)))

(defn longest-common-substring
  "Finds the Longest Common Substring"
  [str1 str2]
  (loop [s1 (seq str1), s2 (seq str2), len 0, maxlen 0]
    (cond
      (>= maxlen (count s1))         maxlen
      (>= maxlen (+ (count s2) len)) (recur (rest s1) (seq str2) 0 maxlen)
      :else                          (let [a (nth s1 len ""), [b & s2] s2, len (inc len)]
              (if (= a b)
                (recur s1 s2 len (if (> len maxlen) len maxlen))
                (recur s1 s2 0 maxlen))))))

(defn file-extension [s]
  (second (re-find #"(\.[a-zA-Z0-9]+)$" s)))

(comment
  (decode-transit (encode-transit {:a 1}))
  (clj->gql {:tree-id "fubar"}))
