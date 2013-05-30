(ns map-experiments.smart-maps.set-map
  (:require [map-experiments.smart-maps.protocol :refer :all])
  (:import [clojure.lang
            IPersistentMap IPersistentSet IPersistentCollection IEditableCollection ITransientMap ITransientSet ILookup IFn IObj IMeta Associative MapEquivalence Seqable MapEntry SeqIterator]))

; A SetMap is like a regular map, but forces keys to be sets, and overrides assoc so that it augments the set at that key rather than replacing the value. It's used as a building block for the later constructs.
(deftype SetMap [metadata contents]
  IPersistentMap
  (assoc [this k v]
         (SetMap. metadata (assoc contents k ((fnil conj #{}) (get contents k) v))))
  (without [this k]
           (SetMap. metadata (dissoc contents k)))
  (iterator [this]
    (SeqIterator. (seq this)))
  IPersistentCollection
  (cons [this x]
        (if (and (sequential? x) (= 2 (count x)))
            (let [[k v] x]
                 (assoc this k v))
            (throw (IllegalArgumentException.
                     "Vector arg to map conj must be a pair"))))
  (equiv [this o]
         (or (and (isa? (class o) SetMap)
                  (= contents (.contents ^SetMap o)))
             (= contents o)))
  (empty [this] (SetMap. metadata (empty contents)))
  (count [this] (count contents))
  IPersistentSet
  (disjoin [this [k v]]
           (if-let [old-v-set (get contents k)]
                   (SetMap. metadata
                            (if (< 1 (count old-v-set))
                                (assoc contents k (disj old-v-set v))
                                (dissoc contents k)))
                   this))
  IObj (withMeta [this new-meta] (SetMap. new-meta contents))
  ; Boilerplate map-like object implementation code. Common to all the mirrored maps, and also to SetMap (although SetMap uses differing field names).
  Associative
  (containsKey [this k] (contains? contents k))
  (entryAt     [this k] (find contents k))
  ILookup
  (valAt [this k]           (get contents k))
  (valAt [this k not-found] (get contents k not-found))
  Seqable (seq      [this]   (seq contents))
  IFn     (invoke   [this k] (get contents k))
  IMeta   (meta     [this]   metadata)
  Object  (toString [this]   (str contents))
  MapEquivalence)

(defn set-map
  "Creates a SetMap, which is a smart map that overrides assoc so that every value is a set of all values which have been associated with it; that is, assoc is non-overwriting."
  ([] (SetMap. nil (hash-map)))
  ([& keyvals]
   (apply assoc (set-map) keyvals)))