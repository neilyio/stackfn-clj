(ns stack
  (:require [src.utilities :as u]
            [src.assertions :as a]))


(defn def>
  "Given an env-map of :bindings and :stack, as well as a list of forms,
  return a new env-map that contains a new binding assigning the first
  :stack value to the first item of forms. The assignment will shadow any
  existing bindings using the same symbol. "
  [{:keys [bindings stack]} forms]
  (let [[expr]  forms
        new-var (u/assignable (->> expr name butlast (apply str) symbol))]
    {:bindings (conj bindings new-var (first stack))
     :stack    (cons new-var (drop 1 stack))}))

(defn apply>
  "Given an env-map of :bindings and :stack, as well as a list of forms,
  return a new env-map that applies a function, the first value in forms,
  to the :stack. The function may consume stack values, and adds one new
  value to the :stack."
  [env forms]
  (let [[expr] forms]
    ((apply (eval (first expr)) (rest expr)) env forms)))

(defn eval>
  "Given an env-map of :bindings and :stack, as well as a list of forms,l
  return a new env-map with the first value in forms added to the :stack.

  If the first value in forms is a symbol that matches a list of special
  forms, apply> will be called with the symbol, as well as the env-map
  and argument list of forms. "
  [{:keys [bindings stack] :as env} forms]
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

(defn compile>
  "Given an env-map of :bindings and :stack, as well as a list of forms,
  recursively call compile> on each form until the end of the list. Each
  stage of recursion, including the final result, returns a new env-map
  with a new set of :bindings and a new :stack.

  At the end of the list of forms, its expected that the :stack will be
  reduced to a single item, not including the ::empty sentinel."
  [env forms]
  (if (empty? forms)
    (do (a/assert-empty-signal env forms)
        (a/assert-stack-height-exactly env forms)
        (update env :stack #(cons 'do (reverse %))))
    (let [[expr] forms]
      (cond
        (seq?   expr)   (compile> (apply> env forms) (rest forms))
        (u/def? expr)   (compile> (def>   env forms) (rest forms))
        :else           (compile> (eval>  env forms) (rest forms))))))

(defn invoke>
  "Accepts a function, f, and arity, a number. Returns a function that
  accepts an env-map of :bindings and :stack, as well as a list of forms.

  The returned function applies f to the arity number of items on top
  of the :stack. Arity number of items will be removed from the top of
  the :stack, and the return value of f will be added to the :stack."
  [f arity]
  (fn [{:keys [bindings stack] :as env} forms]
    (a/assert-stack-height arity env forms)
    {:bindings bindings
     :stack    (cons (cons f (take arity stack)) (drop arity stack))}))

(defn <pop>
  "Accepts no arguments, and returns a function that accepts an env-map r
  of :bindings and :stack, as well as a list of forms.

  The returned function will return new-env map with the item at the top
  of the :stack removed."
  []
  (fn [{:keys [bindings stack] :as env} forms]
    (a/assert-stack-height env forms)
    {:bindings bindings
     :stack    (cons (cons 'do (take 2 stack)) (drop 2 stack))}))

(defn if>
  "Accepts a list of forms seperated by the symbol else>. The symbol else>
  should occur only once, and have an equal number of forms before and after
  it within the if> arguments. Returns a function that accepts an env-map of
  :bindings and :stack, as well as a list of forms.

  The returned function will consume the first item on the :stack, and evaluate
  one of the 'branches' of forms on either side of the else> statement. Which
  branch is chosen is based on the truthiness of the first item of the stack.
  Truthiness is defined the same as with a  standard Clojure 'if' block.

  Each 'branch' in if> must conform to the same rules as the main stack in a
  stackfn. Each branch may read the :bindings in the outer 'scope', but new
  bindings will be contained to the scope of the branch, having no effect on the
  outer branches. Each branch starts with an empty :stack. "
  [& branch-forms]
  (let [[t _ f] (partition-by #(= % 'else>) branch-forms)]
    (fn [{:keys [bindings stack] :as env} forms]
      (try
        (a/assert-if>-syntax env branch-forms)
        (a/assert-stack-height env forms)
        (let [branch #(let [env-new {:bindings bindings :stack (u/stack-new) }]
                        (->> % (compile> env-new) (u/dedupe-env env) (u/wrap-let)))]
          {:bindings bindings
           :stack    (cons (list 'if (first stack) (branch t) (branch f))
                           (rest stack))})
        (catch clojure.lang.ExceptionInfo e
          (throw (ex-info (ex-message e)
                          (ex-data e))))))))
