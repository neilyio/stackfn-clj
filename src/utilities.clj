(ns utilities
  (:require [clojure.string :as s]))


(defn wrap-let
  "Given an env-map of :bindings and :stack, return a list of symbols
  that form a Clojure 'let' block, using the :bindings as assignments
  and the :stack as the body."
  [{:keys [bindings stack]}]
  (list 'let (vec bindings) stack))

(defn dedupe-env
  "Given two env-maps each of :bindings and :stack, return a new
  env-map with the :stack of the second env-map, and de-duplicated
  :bindings. The returned :bindings represents the symmetric
  difference of the two input :bindings."
  [env1 env2]
  {:bindings (vec (drop (count (:bindings env1)) (:bindings env2)))
   :stack    (:stack env2)})

(defn stack-new
  "Return a new stack data structure, containing only the ::empty
  stack sentinel."
  []
  (list ::empty))

(defn stack-top
  "Return the top item on the stack. The input stack is unchanged."
  [stack]
  (first stack))

(defn stack-empty?
  "Returns true if is a stack data structure is empty, false otherwise."
  [stack]
  (= (first stack) ::empty))

(defn def?
  "Given a symbol, return true if that symbol represents an assignment
  directive, and return false otherwise."
  [sym]
  (and (symbol? sym) (s/ends-with? (name sym) "+")))

(defn assignable
  "Given a symbol, return a new symbol with a unique numerical suffix."
  [sym]
  (gensym (str (name sym) ":")))


(defn latest-binding
  "Given a list of ':'-suffixed bindings, returns the symbol that represents
  the latest binding. It's assumed that suffixes are numeric and assign higher
  numbers to later bindings. Symbols are compared using (name my-symbol).
  A non-':'-suffixed binding is sorted lower than a zero suffix."
  [bindings sym]
  (let [sym-str      (name sym)
        no-suffix    #(subs % 0 (or (s/last-index-of % ":") (count %)))
        equal-sym    #(= % sym-str)
        same-binding (comp equal-sym no-suffix name)]
    (first (sort (comp - compare)
                 (->> bindings (take-nth 2) (filter same-binding))))))

(defn map-first
  "Given a collection, return a lazy sequence with f applied to the first
  element of the collection, and the rest of the collection unchanged."
  [f coll]
  (map-indexed #(if (= %1 0) (f %2) %2) coll))


(defn format-compile-error
  "Given an env-map of :bindings and :stack, along with a list of forms and
  an error object, return a new error message that adds helpful context
  describing the cause of a stack error."
  [env forms e]
  (let [highlight #(vector % (str \u219e " " (ex-message e)))
        default   [(str \u21a0 " " (ex-message e))]
        post-err  (:forms (ex-data e))
        ;; err-form?  (= (count formatted) (count forms))
        pre-err   (drop-last (count post-err) forms)]
    (ex-info (ex-message e)
             (cond-> (ex-data e)
               (not-empty pre-err) (update :forms #(map-first highlight %))
               (not-empty pre-err) (assoc  :neg-index (count post-err))
               (empty? post-err)    (update :forms #(cons default %))
               true                (update :forms #(concat pre-err %))))))

(defn format-defstackfn-error
  "Given the name of a stackfn, a list of its parameter symbols, a list of forms,
  and an error object, return a new error message that adds helpful context
  describing the cause of a stack error."
  [name vars forms e]
  (let [position    (- (count forms) (:neg-index (ex-data e)))
        err-return? (= (count forms) position)
        definition  `[(~'defstackfn ~name [~@vars])
                      ~@(if-not err-return?
                          nil
                          [(str \u21a0 " " "Incorrect stack height upon return")])
                      ~@(:forms (ex-data e))]]
    (ex-info (ex-message e)
             (-> (ex-data e)
                 (dissoc :forms)
                 (dissoc :neg-index)
                 (assoc  :position (if err-return? nil position))
                 (assoc  :definition definition)))))
