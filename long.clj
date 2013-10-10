(defn long-to-bytes 
	  "convert a long into a sequence of 8 bytes. The zeroes are padded to the 
beginning to make the BigInteger contructor happy" [^long lng]
	    (let [pad (repeat 8 (byte 0))
		 bytes (map byte (.. (BigInteger/valueOf lng) toByteArray))]
		 (concat (drop (count bytes) pad) bytes)))
