# stackfn-clj
A Clojure interpreter for a stack-based language.

The core of this implementation revolves around a single macro, `defstackfn`, and a handful of helper functions. Rather than creating a "calculator" function that parses the arguments and computes the result, the work performed by this interpreter deals with shifting around s-expressions to create valid Clojure code. It leaves the execution of this code up to Clojure.

## Project Structure

The work is spread across four files in the `src` folder:

- src/assertions.clj  
- src/core.clj    
- src/stack.clj
- src/utilities.clj    

The `defstackfn` definition, along with the main test case, lives in `core.clj`. The remaining functions are grouped together in files based on their functionality and the shape of data upon which they operate.

## Design Goals

A key design goal of this project is composability. Nearly all of the functions accept and return a single data type: a map with the keys `:bindings` and `:stack`. This "environment map" (`env-map`) might look like this.:
```clj
{:bindings [!a1 1 !b1 2]
 :stack    '(1 2 !a !b)}
```

A consistent data type allows functions to be broken up into small units of work. These `env-map` functions can call each other to manipulate `:bindings` and `:stack`, or can call themselves recursively. Even the central `compile>` function accepts and returns an `env-map`. While our spec for `defstackfn` calls for starting compilation with an empty `:stack`, `compile>` is free for the developer to employ in new scenarios that may benefit from passing around an existing `:stack`.

Another design goal of this implementation is transparency. An `env-map` is a simple data type, and can be inspected as its passed around functions. We delay evaluation as long as possible, and we do not resolve symbol names (e.g. `!a`) while the `env-map` is being passed around. The developer should be able to debug the `:stack` without needing to keep a mental map between `!a` and its evaluation.

This goal leads to a major implementation decision: a `defstackfn` definiton should produce valid Clojure code. A developer should be able to `macroexpand` a `defstackfn` and see familiar, readable Clojure code to aid in debugging. The execution of a `stackfn` will simply execute the expanded Clojure code. The specified testcase for `defstackfn` is below:

```clj
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
```

Assuming low gensym numbers, our implementation of `defstackfn` would `macroexpand` the above definition to:

```clj
(def f
 (fn*
   ([!a !b !c]
     (let*
       [!a:1  !a
        !b:2  !b
        !c:3  !c
        !v1:4 (+ !b:2 !a:1)
        !v2:5 (* 2 (do !c:3 !c:3))]
       (do
         :utilities/empty
         (if (= !v2:5 !v1:1)
           (let* [] (do :utilities/empty (- !v2:2 !v1:1)))
           (let* []
             (do
               (do (println "false!!") :utilities/empty)
               (* !v2:2 !v1:1)))))))))
```

The `!a:1` symbol format is derived from user-passed arguments concatenated with a gensym to ensure uniqueness. Real usage will have higher gensym numbers, and the implementation could be optimized for readibility by hand-rolling more friendly suffixes instead of relying on gensyms.

`compile>`, therefore, does not "evaluate", but "transpiles". It only concerns itself with symbols and lists of symbols, and has no knowledge of actual values. Its main considerations are shuffling around s-expressions, and ensuring that the forms passed to `defstackfn` will output valid Clojure code.

Our third design goal leverages the nature of a stack-based language to create highly-expressive error messages. The syntax of our `stackfn` language allows us to infer a good deal of information about the `:stack` at compile time. Before the developer can even evaluate `defstackfn`, we can make perform analysis that can alert them to unbound variables, incorrect syntax, and invalid stack heights.

As an example, you can modify the third form of our test case (`(invoke> + 2)`) to cause a stack error by change it to (`(invoke> + 3)`). When you try and evaluate the surrounding `defstackfn`, an error message will helpfully point out your mistake by including the follwing `ex-info`:

```clj
   Incorrect stack height
   {:received-height 2,
    :expected-height 3,
    :position 2,
    :definition
    [(defstackfn f [!a !b !c])
     !a
     !b
     [(invoke> + 3) "↞ Incorrect stack height"]
     !v1+
     !c
     !c
     ...
    }
```

## Key Functions

As we're tasked with reading and evaluating a language, many our functions are named in the tradition of Lisp parsing. We can split up the important functions into a few categories:

### Stackfn operators

These are functions used within the stack language to manipulate stack values. The developer who uses our stack language may want to define more of these, so they have a extensible specification. Each one may accept any number of arguments and should return a function that takes an `env-map` (a structure of `({ :bindings ... :stack ... })`) and returns and `env-map`.

#### invoke>
Takes the symbol of a function and an arity. It consumes "arity" number of items from the `:stack` as arguments for the function. It places the result back on the stack.

#### if>
Accepts a list of forms which represent logical branches. The "true" branch is the first set of forms before the `else>` symbol, and the "false" branch follows.

#### \<pop>
Accepts no arguments, and removes a function from the stack. `<pop>` is listed as a "special form". It does not need to be wrapped in parenthesis like the other stackfn operators. When detected by `eval>`, it will implicitly be wrapped in parentheses and called like the other operators.
 
### Evaluation operators

#### compile>
Transforms a list of "forms" (passed to `defstackfn`) into Clojure s-expressions, which are stored in the `:stack` structure.

#### eval>
Adds forms to the `:stack`. If necessary, replace the form with the appropriate suffix-ed binding.

#### apply>
Evaluates a `stackfn` operator, and then passes the `env-map` to the returned function. Places the resulting value onto the `:stack`.

#### def>
Takes the first value from `:stack` and assigns it to the `:bindings` map.

 
