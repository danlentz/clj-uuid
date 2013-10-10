(defn long-to-bytes 
	  "convert a long into a sequence of 8 bytes. The zeroes are padded to the 
beginning to make the BigInteger contructor happy" [^long lng]
	    (let [pad (repeat 8 (byte 0))
		 bytes (map byte (.. (BigInteger/valueOf lng) toByteArray))]
		 (concat (drop (count bytes) pad) bytes)))


(defn map-indexed
  "Returns a lazy sequence consisting of the result of applying f to 0
  and the first item of coll, followed by applying f to 1 and the second
  item in coll, etc, until coll is exhausted. Thus function f should
  accept 2 arguments, index and item."
  {:added "1.2"}
  ([f coll] (map-indexed f 0 coll))
  ([f start coll] 
      (letfn [(mapi [idx coll]
	    (lazy-seq
	      (when-let [s (seq coll)]
		 (if (chunked-seq? s)
		   (let [c (chunk-first s)
			 size (int (count c))
			 b (chunk-buffer size)]
		     (dotimes [i size]
			(chunk-append b (f (+ idx i) (.nth c i))))
		     (chunk-cons (chunk b) (mapi (+ idx size) (chunk-rest s))))
		   (cons (f idx (first s)) (mapi (inc idx) (rest s)))))))]
   (mapi start coll))))

  "Returns a lazy sequence consisting of the result of applying f to 0
  and the first item of coll, followed by applying f to 1 and the second
  item in coll, etc, until coll is exhausted. Thus function f should
  accept 2 arguments, index and item."
  {:added "1.2"}
  ([f coll] (map-indexed f 0 1 coll))
  ([f start coll] (map-indexed f start 1 coll))
  ([f start step coll] 
      (letfn [(mapi [idx coll]
            (lazy-seq
             (when-let [s (seq coll)]
               (if (chunked-seq? s)
                 (let [c (chunk-first s)
                       size (int (count c))
                       b (chunk-buffer size)]
                   (dotimes [i size]
                     (chunk-append b (f (+ idx (* i step)) (.nth c i))))
                   (chunk-cons (chunk b) (mapi (+ idx (* size step)) (chunk-rest s))))
                 (cons (f idx (first s)) (mapi (+ step idx) (rest s)))))))]
    (mapi start coll))))
