(ns clean
  (:require
   [utilities :as u]
   [assertions :as a]
   [stack :refer [compile> invoke> if> <pop>]]))

(defmacro defstackfn
  "Given a name, a vector of vars, and a variable number of forms, return a
  function that evaluates the forms based on stackfn rules.

  A call to defstackfn may be macroexpanded so that the underlying Clojure code
  can be inspected. defstackfn creates a local scope for its variable bindings,
  and all variables must be prefixed with a !."
  [name vars & forms]
  (let [bindings (vec (interleave (map u/assignable vars) vars))
        _        (doseq [var vars] (a/assert-variable var))
        init-env {:bindings bindings
                  :stack    (u/stack-new)}
        env      (try
                   (compile> init-env forms)
                   (catch clojure.lang.ExceptionInfo e
                     (throw (u/format-defstackfn-error
                             name
                             vars
                             forms
                             (u/format-compile-error init-env forms e)))))]
    `(defn ~name [~@vars]
       ~(u/wrap-let env))))



(defstackfn f [!a !b !c]
  !a
  !b
  (invoke> + 2)
  !v1+
  !c
  !c
  <pop>
  2
  (invoke> * 2)
  !v2+
  (invoke> = 2)
  (if>
      !v1
    !v2
    (invoke> - 2)
    else>
    "false!!"
    (invoke> println 1)
    <pop>
    !v1
    !v2
    (invoke> * 2))
  )

(f 1 2 4)
