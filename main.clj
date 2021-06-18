(ns main
  (:require [clojure.string :as s]))

;; (declare eval-ast)

;; (def repl-env
;;   {"+" +
;;    "-" -
;;    "*" *
;;    "/" /})

;; (defn m-read [x] x)

;; (defn m-eval [ast env]
;;   (if-not (coll? ast)
;;     (eval-ast ast env)
;;     (if (empty? ast)
;;       ast
;;       (let [e (eval-ast ast env)]
;;         (apply (first e) (rest e))))))

;; (defn m-print [x] x)

;; (defn m-rep [x]
;;   (-> (m-read x)
;;       (m-eval repl-env)
;;       (m-print)))


;; (defn main []
;;   (loop [inpt ""]
;;     (if (= "x" inpt)
;;       inpt
;;       (do
;;         (print "user> ")
;;         (recur (read-line))))))


;; (defn eval-error [value]
;;   (ex-info "Eval error." {:cause value}))


;; (defn eval-ast [ast env]
;;   (cond
;;     (coll? ast) (map #(m-eval % env) ast)
;;     (symbol?)   (or (get env ast) (throw (eval-error ast)))
;;     :else ast))

;; (defstackfn
;;   [:!a :!b :!c] ; example uses input: 1 2 4. Stack starts empty.
;;   :!a ; 1
;;   :!b ; 1 2
;;   (:invoke> + 2) ; 3
;;   :!v1+ ; 3
;;   :!c ; 3 4
;;   :!c ; 3 4 4
;;   :<pop> ; 3 4
;;   2 ; 3 4 2
;;   (:invoke> 8 2) ; 3 8
;;   :!v2+ ; 3 8
;;   (:invoke> = 2) ; false
;;   (:if> ; stack empty
;;    :!v1
;;    :!v2
;;    (:invoke> - 2)
;;    :else>
;;    "false!!" ; "false!!"
;;    (:invoke> println 1) ; nil
;;    :<pop> ; stack empty
;;    :!v1 ; 3
;;    :!v2 ; 3 8
;;    (:invoke> * 2) ; 24
;;    )
;;   )



;; def must(predicate, msg=None):
;;     def from_value(value):
;;         result = predicate(value)
;;         tstr = type(value).__name__
;;         if msg:
;;             assert result, f"{msg}... {value}: {tstr}."
;;         else:
;;             assert (
;;                 result
;;             ), f"{predicate.__name__} returns {result}... {value}: {tstr}"
;;         assert result, msg

;;         return True

;;     return from_value


;; def validate_safe(i, *validators):
;;     errors = []
;;     for valid_fn in validators:
;;         try:
;;             valid_fn(i)
;;         except AssertionError as err:
;;             errors.append(err)
;;     return errors


;; def validate(i, *validators):
;;     errors = validate_safe(i, *validators)
;;     if errors:
;;         message = f"'{i}' failed validation. Errors:"
;;         for index, error in enumerate(errors):
;;             message += f"\n    {index + 1}. "
;;             message += error.args[0]
;;         raise AssertionError(message)
;;     return True




;; (defn fn-name
;;   [f]
;;   (first (re-find #"(?<=\$)([^@]+)(?=@)" (str f))))

;; (defn must
;;   ([pred]
;;    (must pred nil))
;;   ([pred message]
;;    (fn [value]
;;      (let [result    (pred value)
;;            pred-name (-> pred meta)
;;            type-str  (str "... " value ":" (type value))
;;            default   (str "(" pred-name " value " ")" "returns " result type-str)
;;            msg       (str "must " (if message (str message type-str) default))]
;;        (when-not result {:value value :cause msg})))))

;; (defn validate [i & validators]
;;   (doseq [f validators]
;;     (throw (ex-info "validation failed" err-map)))
;;   i)



;; (defmacro validate [value & validators]
;;   (loop [remaining validators errors []]
;;     (if-not remaining
;;       errors
;;       (recur (rest remaining)
;;              (conj errors {:func (second (first validators))}))))
;;   `(for [error (remove nil? (apply (juxt validators) value))
;;          :let ]

;;      value)
;;   )

;; (def env {:!a 1
;;           :!b 2
;;           :!c 4
;;           :invoke> (fn [stack fn arity]
;;                      )})

;; (def p [:!a :!b :!c :!c :!b :!a])


;; ;; (def s-eval [sym env stack]
;; ;;   )
;; ;;

;; (defn invoke> [stack fn arity]
;;   (cons (transduce (take arity) fn stack) (drop arity stack)))

;; (defn pop> [stack]
;;   (drop 1 stack))

;; (defmacro assert-assigned {:style/indent 1} [[env sym] & forms]
;;   `(do (when-not (contains? (:table ~env) ~sym)
;;          (throw (ex-info (str "Referenced unassigned var>: ") {:symbol ~sym})))
;;        ~@forms))

(defn assignable [sym]
  (if-let [str-sym (and (symbol? sym) (name sym))]
    (if-not (s/ends-with? str-sym "+")
      nil
      (symbol (subs str-sym 0 (dec (count str-sym)))))))

;; (let [n "neilhansen"]
;;   (subs n 0 (dec (count n))))

;; (defn <pop> [stack]
;;   (drop 1 stack))

;; (def *specials*
;;   {'<pop> <pop>})

;; (defn eval> [{:keys [table stack] :as env} & forms]
;;   (if-not (empty? forms)
;;     (let [expr (first forms)]
;;       (recur
;;        (cond
;;          (list? expr)   (merge env
;;                                {:stack (apply (get-in env [:table (first expr)])
;;                                               stack
;;                                               (map eval (rest expr)))})
;;          (symbol? expr) (if-let [special (get *specials* expr)]
;;                           (merge env {:stack (special stack)})
;;                           (if (assignable? expr)
;;                             (merge env {:table (assoc table expr (first stack))})
;;                             (assert-assigned [env expr]
;;                               (merge-with cons {:stack (get table expr)} env))))
;;          :else (merge-with cons {:stack expr} env))
;;        (rest forms)))
;;     env))

;; (contains? {:a nil} :a) ; => true
;; (tap> (contains? {:a nil} :b)) ; => false

;; (defn push> [stack & forms]
;;   (concat (reverse forms) stack))


;; (defn switch-first-push-rest [[x & xs] ts fs]
;;   (apply push> xs (if x ts fs)))

;; (defmacro if> {:style/indent 0} [& forms]
;;   (let [[t _ f & err] (partition-by #(= % 'else>) forms)]
;;     (when err   (throw (ex-info "Too many else> statements" {})))
;;     (when-not f (throw (ex-info "No else> statement" {})))
;;     `'(switch-first-push-rest '~(map str t) '~(map str f))
;;     ;; `(if (first ~stack)
;;     ;;    (push> (drop 1 ~stack) ~@(map str t))
;;     ;;    (push> (drop 1 ~stack) ~@(map str f)))
;;     ))


;; (defmacro defstackfn [name variables & forms]
;;   ;; all variables must start with !
;;   ;; `(eval> ~@(map #(quote %) forms))
;;   (let [table-keys     variables
;;         table-defaults {'invoke> invoke> 'if> if>}]
;;     `(defn ~name [~@variables]
;;        ;; '~forms
;;        (apply eval>
;;               {:stack (list)
;;                :table (merge table-defaults
;;                              (apply hash-map (interleave '(~@table-keys)
;;                                                          ~variables)))} '~forms))))
;; (defmacro compile> [stack forms]
;;   (if (empty? forms)
;;     `(quote ~stack)
;;     (let [[expr & remaining] forms]
;;       (cond
;;         (list? expr)   `(compile> ~`(~(first expr) ~stack ~@(rest expr)) ~remaining)
;;         (symbol? expr) (if-let [new-var (assignable expr)]
;;                          `(let [~new-var ~(first stack)] (compile> ~stack ~remaining))
;;                          `(compile> ~(cons expr stack) ~remaining))
;;         :else          `(compile> ~(cons expr stack) ~remaining)))))
;;

(defmacro wrap-let [binding expression]
  (if (empty? binding) expression `(let ~binding ~expression)))

(defmacro wrap-let [bindings expression]
  (if (empty? bindings) expression `(let ~bindings ~expression)))

(defmacro let> [cc bindings stack forms]
  `(~cc [~(assignable (first forms)) ~(second stack)] (let ~bindings ~stack) ~(rest forms)))

(defmacro cons> [cc bindings stack forms]
  `(~cc ~bindings (cons ~(first forms) ~stack) ~(rest forms)))

(defmacro apply> [cc bindings stack forms]
  (let [[f & args] (first forms)]
    `(~cc ~bindings (~f ~stack ~@args) ~(rest forms))))

(defmacro parse [bindings stack forms]
  (if (empty? forms)
    `(wrap-let ~bindings ~stack)
    (if (seq? (first forms))
      `(apply> parse ~bindings ~stack ~forms)
      (if-let [new-var (assignable (first forms))]
        `(let> parse ~bindings ~stack ~forms)
        `(cons> parse ~bindings ~stack ~forms)))))


(defn invoke> [reducer arity]
  (fn [{:keys [env stack]}]
    {:env env :stack (cons (transduce (take arity) reducer stack)
                           (drop arity stack))}))

(defmacro invoke> [stack reducer arity]
  `'((= ~@(take arity stack)) ~@(drop arity stack))
  )

(defmacro <pop> [stack]
  `(~@(drop 1 stack)))



(defn reducer [stack form]
  ((first form) stack (rest form))
  #_(if (seq? form)
      ((first form) stack ())
      (cons stack form)))


(defmacro defstackfn [name variables & forms]
  (let [stack-forms nil]
    `(for [e (->  '() ~@(for [f forms]
                          (if (seq? f)
                            f
                            #(cons f %))))]
       (eval e))))

(defstackfn neilfunc [!a !b !c]
  1
  2
  3
  4
  5
  (invoke> = 2)
  7
  8)

;; (defmacro compile> [stack forms]
;;   (if (empty? forms)
;;     `(quote ~stack)
;;     (let [[expr & remaining] forms]
;;       (cond
;;         (list? expr)   `(compile> ~`(~(first expr) ~stack ~@(rest expr)) ~remaining)
;;         (symbol? expr) (if-let [new-var (assignable expr)]
;;                          `(let [~new-var ~(first stack)] (compile> ~stack ~remaining))
;;                          `(cons ~expr (compile> ~stack ~remaining)))
;;         :else          `(cons ~expr (compile> ~stack ~remaining))))))


(defn map> [arity func stack]
  (cons (transduce (take arity) func stack) (drop arity stack)))

(defmacro invoke> [stack func arity]
  (when (< arity 1) (throw (ex-info "Invoke> must be passed an arity of 1 or more" {})))
  `(map> ~arity ~func ~stack)
  ;; `(cons (~func ~@(take arity stack)) (drop ~arity '~stack))
  ;; `(cons (transduce (take ~arity) ~fn ~stack) (drop ~arity ~stack))
  )

;; at first I thought I could simplify by making invoke> a function, because
;; it seemed from the example
;; if symbol, get symbol from env
;;     if symbol has +, assign symbol in env
;; if list, eval list
;; else put value on stack
