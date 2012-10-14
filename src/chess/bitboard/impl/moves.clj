(ns chess.bitboard.impl.moves
  (:use [chess.bitboard.impl.file-rank])
  (:use [chess.bitboard.impl.bitoperations])
  (:use [chess.bitboard.impl.chessboard])
  (:use [chess.bitboard.impl.piece-attacks])
  (:use [clojure.pprint]))
(comment (set! *warn-on-reflection* true))

(defn attacked-by-black? [mask-sq game-state]
  "looks at squares set in mask-sq and returns true if black attacks any of these squares"
  (some-bitmap  (fn [sq]
                  (or (bit-and? (:p game-state) (aget ^longs pawn-white-attack-array sq))
                      (bit-and? (:k game-state) (aget ^longs king-attack-array  sq))
                      (bit-and? (:n game-state) (aget ^longs knight-attack-array sq))

                      (bit-and? (bit-or (:r game-state) (:q game-state))
                                (get-attack-rank game-state sq))
                      (bit-and? (bit-or (:r game-state) (:q game-state))
                                (get-attack-file game-state sq))

                      (bit-and? (bit-or (:b game-state) (:q game-state))
                                (get-attack-diagonal-a1h8 game-state sq))
                      (bit-and? (bit-or (:b game-state) (:q game-state))
                                (get-attack-diagonal-a8h1 game-state sq)))) mask-sq))

(defn attacked-by-white? [mask-sq game-state]
  "looks at squares set in mask-sq and returns true if white attacks any of these squares"
  (some-bitmap  (fn [sq]
                  (or (bit-and? (:P game-state) (aget ^longs pawn-black-attack-array sq))
                      (bit-and? (:K game-state) (aget ^longs king-attack-array  sq))
                      (bit-and? (:N game-state) (aget ^longs knight-attack-array sq))

                      (bit-and? (bit-or (:R game-state) (:Q game-state))
                                (get-attack-rank game-state sq))
                      (bit-and? (bit-or (:R game-state) (:Q game-state))
                                (get-attack-file game-state sq))

                      (bit-and? (bit-or (:B game-state) (:Q game-state))
                                (get-attack-diagonal-a1h8 game-state sq))
                      (bit-and? (bit-or (:B game-state) (:Q game-state))
                                (get-attack-diagonal-a8h1 game-state sq)))) mask-sq))

(defmulti find-piece-moves (fn[piece _ _]
                       (case piece
                         (:N :n) :Knight
                         (:R :r) :Rook
                         (:B :b) :Bishop
                         (:Q :q) :Queen
                         :K      :WhiteKing
                         :k      :BlackKing
                         :P      :WhitePawn
                         :p      :BlackPawn)))

 (defmethod find-piece-moves :Knight [piece from-sq game-state]
   (let [piece-move-bitset    (aget ^longs knight-attack-array from-sq)
         not-occupied-squares (bit-not (pieces-by-turn game-state))
         moves                (bit-and piece-move-bitset not-occupied-squares)]
     (for-bitmap [dest-pos moves]
       [piece from-sq dest-pos])))

 (defmethod find-piece-moves :WhitePawn [piece from-sq game-state]
   (let [moves          (aget ^longs pawn-white-move-array from-sq)
         all-pieces     (:allpieces game-state)
         occupied       (bit-and all-pieces moves)
         moves          (bit-xor moves occupied)

         double-moves   (aget ^longs pawn-white-double-move-array from-sq)
         occupied-4-row (bit-and all-pieces double-moves)
         occupied-3-row (bit-and (bit-shift-left all-pieces 8) double-moves)
         double-moves   (bit-xor double-moves (bit-or occupied-3-row occupied-4-row))

         attacks  (bit-and (:blackpieces game-state)
                           (aget ^longs pawn-white-attack-array from-sq))
         moves    (bit-or moves double-moves attacks)]
     (for-bitmap [dest-pos moves]
       (if (< dest-pos 56)
         [piece from-sq dest-pos]
         [[piece from-sq dest-pos :Q] ; means last row so pawn gets promoted
          [piece from-sq dest-pos :R]
          [piece from-sq dest-pos :B]
          [piece from-sq dest-pos :N]]))))

(defmethod find-piece-moves :BlackPawn [piece from-sq game-state]
  (let [moves          (aget ^longs pawn-black-move-array from-sq)
        all-pieces     (:allpieces game-state)
        occupied       (bit-and all-pieces moves)
        moves          (bit-xor moves occupied)

        double-moves   (aget ^longs pawn-black-double-move-array from-sq)
        occupied-5-row (bit-and all-pieces double-moves)
        occupied-6-row (bit-and (bit-shift-right  all-pieces 8) double-moves)
        double-moves   (bit-xor double-moves (bit-or occupied-5-row occupied-6-row))

        attacks        (bit-and (:whitepieces game-state)
                               (aget ^longs pawn-black-attack-array from-sq))
        moves          (bit-or moves double-moves attacks)]
     (for-bitmap [dest-pos moves]
       (if (> dest-pos 7)
         [piece from-sq dest-pos]
         [[piece from-sq dest-pos :q] ; means last row so pawn gets promoted
          [piece from-sq dest-pos :r]
          [piece from-sq dest-pos :b]
          [piece from-sq dest-pos :n]]))))

(defn  ^Long get-rochade-moves  [game-state kind]
  (let [rochade  (:rochade game-state )
        occupied (pieces-by-turn game-state)]
    (condp = kind
     :K (if (and (:K rochade)
                 (not (bit-and? mask-rochade-white-king occupied))
                 (not (attacked-by-black? mask-rochade-white-king game-state)))
            move-rochade-white-king 0)
     :Q (if (and (:Q rochade)
                  (not (bit-and? mask-rochade-white-queen occupied))
                 (not (attacked-by-black? mask-rochade-white-queen game-state)))
           move-rochade-white-queen 0)
     :k (if (and (:k rochade)
                 (not (bit-and? mask-rochade-black-king occupied))
                 (not (attacked-by-white? mask-rochade-black-king game-state)))
           move-rochade-black-king 0)
     :q (if (and (:q rochade)
                 (not (bit-and? mask-rochade-black-queen occupied))
                 (not (attacked-by-white? mask-rochade-black-queen game-state)))
           move-rochade-black-queen 0))))

 (defmethod find-piece-moves :WhiteKing [piece from-sq game-state]
   (let [moves             (aget ^longs king-attack-array from-sq)
         occupied           (pieces-by-turn game-state)
         not-occupied       (bit-not occupied)
         moves              (bit-and moves not-occupied)
         moves              (bit-or moves
                                    (get-rochade-moves game-state :K)
                                    (get-rochade-moves game-state :Q))]
     (for-bitmap [dest-pos moves]
       [piece from-sq dest-pos])))

(defmethod find-piece-moves :BlackKing [piece from-sq game-state]
   (let [moves              (aget ^longs king-attack-array from-sq)
         ^long occupied     (pieces-by-turn game-state)
         not-occupied       (bit-not occupied)
         moves              (bit-and moves not-occupied)
         moves              (bit-or moves
                                    (get-rochade-moves game-state :k)
                                    (get-rochade-moves game-state :q))]
     (for-bitmap [dest-pos moves]
       [piece from-sq dest-pos])))

(defmethod find-piece-moves :Rook [piece from-sq game-state]
  (let [not-occupied       (bit-not (pieces-by-turn game-state))
        slide-moves-rank   (bit-and (get-attack-rank game-state from-sq) not-occupied)
        slide-moves-file   (bit-and (get-attack-file  game-state from-sq) not-occupied)
        slide-moves        (bit-or slide-moves-rank slide-moves-file)]
    (for-bitmap [dest-pos slide-moves]
      [piece from-sq dest-pos])))

(defmethod find-piece-moves :Bishop [piece from-sq game-state]
  (let [not-occupied       (bit-not (pieces-by-turn game-state))
        slide-diagonal-a1  (bit-and (get-attack-diagonal-a1h8 game-state from-sq) not-occupied)
        slide-diagonal-a8  (bit-and (get-attack-diagonal-a8h1 game-state from-sq) not-occupied)
        slide-moves        (bit-or slide-diagonal-a1 slide-diagonal-a8)]
         (for-bitmap [dest-pos slide-moves]
           [piece from-sq dest-pos])))

(defmethod find-piece-moves :Queen  [piece from-sq game-state]
  (concat (find-piece-moves  :r from-sq game-state)
           (find-piece-moves :b from-sq game-state)))

(defn generate-moves [game-state]
  (let [squares  (:board game-state)
        pieces   (pieces-by-turn game-state)]
    (for-bitmap [from-sq pieces]
                     (find-piece-moves (squares from-sq) from-sq game-state))))

(defn print-generate-moves [game-state]
  (print-board
    (create-board-fn
        (map (fn [[p x y]] [p y])
             (apply concat (filter #(not (empty? %)) (generate-moves game-state))) ))))
