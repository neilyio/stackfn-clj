(ns assertions
  (:require [clojure.string :as s]
            [utilities :as u]))


(defn assert-variable
  "Given a symbol, assert that the symbol conforms to the
  variable naming rules of a stackfn."
  [sym]
  (when-not (s/starts-with? (name sym) "!")
    (throw (ex-info "Variable names must start with '!'"
                    {:found sym}))))

(defn assert-variable-syntax
  "Given an env-map of :bindings and :stack, as well as a list
  of forms, assert that the first item in the list of forms
  conforms to the variable naming rules of a stackfn."
  [env forms]
  (let [[expr] forms]
    (when-not (s/starts-with? (name expr) "!")
      (throw (ex-info "Variable names must start with '!'"
                      {:forms forms
                       :found expr})))))

(defn assert-variable-binding
  "Given an env-map of :bindings and :stack, as well as a list
  of forms, assert that the first item in the list of forms
  has a matching variable in the :bindings of the environment."
  [variable env forms]
  (let [[expr] forms]
    (when-not variable
      (throw (ex-info (str "Unbound variable " expr)
                      {:forms forms
                       :found expr
                       :bound (take-nth 2 (:bindings env))})))))

(defn assert-if>-syntax
  "Given an env-map of :bindings and :stack, as well as a list
  of forms, assert that the branches of an if> statment adhere
  to the syntax rules of the stack langauge."
  [env forms]
  (let [[t elses f & err] (partition-by #(= % 'else>) forms)]
    (when (> (count elses) 1)
      (throw (ex-info "Too many else> statements"
                      {:forms forms})))
    (when err
      (throw (ex-info "Too many else> statements"
                      {:forms forms})))
    (when-not f (throw (ex-info "No else> statement"
                                {:forms forms})))))

(defn assert-empty-signal
  "Given an env-map of :bindings and :stack, as well as a list
  of forms, assert that the final item of a stack data structure
  is the ::empty sentinel.

  Because this function must realize the entire stack, it should
  only be called at the end of stack compilation, when the stack
  is expected to have a length of one."
  [env forms]
  (let [final (take-last 1 (:stack env))]
    (when-not (or (u/stack-empty? final)
                  (and (seq? (first final)) (-> final first (nth 2)))
                  (u/stack-empty? final))
      (throw (ex-info "Corrupt stack, missing ::empty signal as final element"
                      {:found (first final) :forms forms})))))

(defn assert-stack-height-exactly
  "Given an env-map of :bindings and :stack, as well as a list of
  forms, assert that the height of the stack, not including the
  ::empty sentinel, is exactly n. If not passed, n defaults to 1."
  ([env forms]
   (assert-stack-height-exactly 1 env forms))
  ([n env forms]
   (let [height (dec (count (:stack env)))]
     (when-not (= height n)
       (throw (ex-info "Incorrect stack height"
                       {:forms           forms
                        :received-height height
                        :expected-height n}))))))

(defn assert-stack-height
  "Given an env-map of :bindings and :stack, as well as a list of
  forms, assert that the height of the stack, not including the
  ::empty sentinel, is greater than or equal to n. If not passed,
  n defaults to 1."
  ([env forms]
   (assert-stack-height 1 env forms))
  ([n env forms]
   (let [height (dec (count (:stack env)))]
     (when-not (<= n height)
       (throw (ex-info "Incorrect stack height"
                       {:forms           forms
                        :received-height height
                        :expected-height n}))))))
