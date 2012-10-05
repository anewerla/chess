(ns chess.test.bitboard.api
  (:use [chess.bitboard.api])
  (:use [clojure.test]))


(deftest test-possible-moves
  (is (= '((2 2) (0 2)) (possible-moves initial-board [1 0]))))


(comment
  (deftest test-generate-moves
    (is (= '(((0 1) (0 2)) ((0 1) (0 3)) ((1 0) (2 2)) ((1 0) (0 2)) ((1 1) (1 2)) ((1 1) (1 3))
             ((2 1) (2 2)) ((2 1) (2 3)) ((3 1) (3 2)) ((3 1) (3 3)) ((4 1) (4 2)) ((4 1) (4 3))
             ((5 1) (5 2)) ((5 1) (5 3)) ((6 0) (7 2)) ((6 0) (5 2)) ((6 1) (6 2)) ((6 1) (6 3))
             ((7 1) (7 2)) ((7 1) (7 3)))) (generate-moves initial-board) )))
(comment (run-tests))