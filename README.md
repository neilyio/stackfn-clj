# stackfn-clj
A Clojure interpreter for a stack-based language.

The core of this implementation revolves around a single macro, `defstackfn`, and a handful of helper functions. Rather than creating a "calculator" function that parses the arguments and computes the result, the work performed by this interpreter deals with shifting around s-expressions to create valid Clojure code. It leaves the execution of this code up to Clojure.

## Project Structure

The work is spread across four files in the `src` folder:
`assertions.clj`
`core.clj`
`stack.clj`
`utilities.clj`.

The `defstackfn` definition, along with the main test case, lives in `core.clj`. The remaining functions are grouped together in files based on their functionality and the shape of data upon which they operate.

## Design Goals

A key design goal of this project is composability. Nearly all of the functions accept and return a single data type: a map with the keys `:bindings` and `:stack`. This "environment map" (`env-map`) might look like this.:
```clj
{:bindings [!a1 1 !b1 2]
 :stack    '(1 2 !a !b)}
```

A consistent data type allows functions to be broken up into small units of work. These `env-map` functions can call each other to manipulate `:bindings` and `:stack`, or can call themselves recursively. Even the central `compile>` function accepts and returns an `env-map`. While our spec for `defstackfn` calls for starting compilation with an empty `:stack`, `compile>` is free for the developer to employ in new scenarios that may benefit from passing around an existing `:stack`.

Another design goal of this implementation is transparency. An `env-map` is a simple data type, and can be inspected as its passed around functions. We delay evaluation as long as possible, and we do not resolve symbol names (e.g. `!a`) while the `env-map` is being passed around. The developer should be able to debug the `:stack` without needing to keep a mental map between `!a` and its evaluation.

This goal leads to a major implementation decision: a `defstackfn` definiton should produce valid Clojure code. A developer should be able to `macroexpand` a `defstackfn` and see familiar, readable Clojure code to aid in debugging. The execution of a `stackfn` will simply execute the expanded Clojure code. The specified testcase for `defstackfn` is as follows:

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

Assuming low gensym numbers, this definition would `macroexpand` to the following:

```clj

```

Our `compile>` function, therefore, does not "evaluate", but "transpiles". It only concerns itself with symbols and lists of symbols, and has no knowledge of actual values. Its main considerations are shuffling around s-expressions, and ensuring that the forms passed to `defstackfn` will output valid Clojure code.

## Key Functions

## Development Setup
