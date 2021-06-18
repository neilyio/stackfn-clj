(ns backup)

(ns clean
  (:require [clojure.string :as s]
            [utilities :as u]
            [assertions :as a]))

(defn map-first [f xs]
  (map-indexed #(if (= %1 0) (f %2) %2) xs))


(defn format-compile-error [env forms e]
  (let [highlight #(vector % (str \u219e " " (ex-message e)))
        tail      (:forms (ex-data e))
        ;; err-form?  (= (count formatted) (count forms))
        ;; err-form?  (= (count formatted) (count (rest forms)))
        head      (drop-last (count tail) forms)]
    (ex-info (ex-message e)
             (cond-> (ex-data e)
               (not-empty head) (update :forms #(map-first highlight %))
               (not-empty head) (assoc  :neg-index (count tail))
               true             (update :forms #(concat head %))))))

;; (defn format-defstackfn-error [name vars forms e]
;;   (let [position    (- (count forms) (or (:neg-index (ex-data e)) 0))
;;         err-return? (= (count forms) position)
;;         definition  `[(~'defstackfn ~name [~@vars])
;;                       ~@(if-not err-return?
;;                           nil
;;                           [(str \u21a0 " " "Incorrect stack height upon return")])
;;                       ~@(:forms (ex-data e))]]
;;     (ex-info (ex-message e)
;;              (-> (ex-data e)
;;                  (dissoc :forms)
;;                  (dissoc :neg-index)
;;                  (assoc  :position (if err-return? nil position))
;;                  (assoc  :definition definition)))))



(defn def> [{:keys [bindings stack]} forms]
  (let [[expr]  forms
        new-var (u/assignable (->> expr name butlast (apply str) symbol))]
    {:bindings (conj bindings new-var (first stack))
     :stack    (cons new-var (drop 1 stack))}))

(defn apply> [env forms]
  (let [[expr] forms]
    ((apply (eval (first expr)) (rest expr)) env forms)))

(defn eval> [{:keys [bindings stack] :as env} forms]
  (let [[expr]   forms
        specials #{'<pop>}]
    (if (contains? specials expr)
      (apply> env (cons (list expr) forms))
      {:bindings bindings
       :stack    (cons (if-not (symbol? expr)
                         expr
                         (let [variable (u/latest-binding bindings expr)]
                           (a/assert-variable-syntax env forms)
                           (a/assert-variable-binding variable env forms)
                           variable))
                       stack)})))


(defn compile> [env forms]
  (if (empty? forms)
    env
    ;; (update env :stack #(cons 'do (reverse %)))
    #_(do (a/assert-empty-signal env forms)
          (a/assert-stack-height-exactly env forms)
          (update env :stack #(cons 'do (reverse %))))
    (let [[expr] forms]
      (cond
        (seq?   expr)   (compile> (apply> env forms) (rest forms))
        (u/def? expr)   (compile> (def>   env forms) (rest forms))
        :else           (compile> (eval>  env forms) (rest forms)))))
  #_(try

      (catch clojure.lang.ExceptionInfo e
        (throw (format-compile-error env forms e)))))

(defn invoke> [f arity]
  (fn [{:keys [bindings stack] :as env} forms]
    (a/assert-stack-height arity env forms)
    {:bindings bindings
     :stack    (cons (cons f (take arity stack)) (drop arity stack))}))

(defn <pop> []
  (fn [{:keys [bindings stack] :as env} forms]
    (a/assert-stack-height env forms)
    {:bindings bindings
     :stack    (cons (cons 'do (take 2 stack)) (drop 2 stack))}))

(defn branch> [env forms]
  (try
    (let [env-new (compile> {:bindings (:bindings env) :stack (u/stack-new)} forms)]
      (a/assert-stack-height-exactly env-new forms)
      (->> (update env-new :stack #(cons 'do (reverse %)))
           ;; (u/dedupe-env env)
           (u/wrap-let)))
    (catch clojure.lang.ExceptionInfo e
      (throw (format-compile-error env forms e)))))

;; (branch> {} '(1 2 3 4 4 5 6 (invoke> * 10)))
;; (branch> {} '(1 (if> 3 'else> 4)))

(defstackfn y [!a]
  1
  (if>
      3
    3
    else>
    4)
  <pop>)

(defstackfn w [!a]
  1
  2
  !a
  (invoke> + 3))

(w 1)

(defn if> [& branch-forms]
  (let [[t _ f] (partition-by #(= % 'else>) branch-forms)]
    (fn [{:keys [bindings stack] :as env} forms]
      (a/assert-if>-syntax env branch-forms)
      (a/assert-stack-height env forms)
      {:bindings bindings
       :stack (cons (list 'if (first stack) (branch> env t) (branch> env f))
                    (rest stack))}
      #_(let [branch #(let [env-new {:bindings bindings :stack (u/stack-new) }]
                        (->> % (compile> env-new) (u/dedupe-env env) (u/wrap-let)))]
          {:bindings bindings
           :stack    (cons (list 'if (first stack) (branch t) (branch f))
                           (rest stack))}))))

(defmacro defstackfn [name vars & forms]
  (let [bindings (vec (interleave (map u/assignable vars) vars))
        env
        (try (compile> {:bindings bindings
                        :stack    (u/stack-new)}
                       forms)
             (catch clojure.lang.ExceptionInfo e
               (throw (format-defstackfn-error name vars forms e))))
        ]
    `(defn ~name [~@vars]
       ;; (u/wrap-let env)
       ~(branch> {:bindings bindings} forms)
       )))


;; (defstackfn n [!a]
;;   !a
;;   (if>
;;       1
;;     1
;;     else>
;;     2))

;; (n false)



;; (defstackfn z [!a]
;;   !a
;;   2
;;   (invoke> * 2))

;; (z 10)


;; (identity (defstackfn f [!a !b !c]
;;             !a
;;             !b
;;             (invoke> + 2)
;;             !v1+
;;             !c
;;             !c
;;             <pop>
;;             2
;;             (invoke> * 2)
;;             !v2+
;;             (invoke> = 2)
;;             (if>
;;                 !v1
;;               !v2
;;               (invoke> - 3)
;;               else>
;;               "false!!"
;;               (invoke> println 1)
;;               <pop>
;;               !v1
;;               !v2
;;               (invoke> * 2))
;;             ))

;; (identity (f 1 2 4))
