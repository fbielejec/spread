(ns api.auth
  (:require [buddy.core.keys :as buddy.keys]
            [buddy.sign.jws :as buddy.sign]
            [buddy.sign.jwt :as jwt]
            [clj-http.client :as http]
            [shared.time :as time]
            [shared.utils :refer [decode-json]])
  (:import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey))

(defn token-decode-header
  "Decodes a JWT tokens header"
  [token]
  (buddy.sign/decode-header token))

(defn verify-token
  "Verifies a JWT token, automatically verifyes :exp claim
  Parameters:
  :token, the token string
  :public-key, the public key to validate the token
  :claims a map of claims to verify (:iss :aud)"
  [{:keys [token public-key claims]}]
  (jwt/unsign token
              (if-not (instance? BCRSAPublicKey public-key)
                (buddy.keys/str->public-key public-key)
                public-key)
              (merge {:alg :rs256}
                     claims)))

(defn get-google-public-key
  "Retrieves the google public key used to validate google authentication tokens."
  [kid]
  (-> (http/get "https://www.googleapis.com/oauth2/v1/certs")
      :body
      (decode-json)
      (get (keyword kid))
      buddy.keys/str->public-key))

(defn verify-google-token
  "Verify token with google public key. If the token is valid and not expired
  returns the google token data"
  [token google-client-id]
  (let [header     (token-decode-header token)
        ;; TODO: memoize it for kid (key id)
        public-key (get-google-public-key (:kid header))
        token-data (verify-token {:token      token
                                  :public-key public-key
                                  :claims     {:iss ["https://accounts.google.com" "accounts.google.com"]
                                               :aud google-client-id}})]
    token-data))

(defn token-encode
  "Generates a signed JWT token.
  Parameters:
  :private-key to sign the token
  :claims a map of claims to add
  :expiration, timestamp of token expiration"
  [{:keys [private-key claims]}]
  (jwt/sign claims
            (buddy.keys/str->private-key private-key)
            {:alg :rs256}))

(defn generate-spread-access-token
  "Generates a long-lived spread token signed with our private key.
   Clients are supposed to store that token safely and include in every request
   that requires authorization"
  [user-id private-key]
  (let [now     (int (/ (time/millis (time/now)) 1000)) ;; in seconds
        expires (+ now 2.628e6)                         ;; now + 1 month
        token   (token-encode {:private-key private-key
                               :claims      {:iss "spread"
                                             :iat now
                                             :exp expires
                                             :aud "spread-client"
                                             :sub user-id}})]
    {:access-token token}))
