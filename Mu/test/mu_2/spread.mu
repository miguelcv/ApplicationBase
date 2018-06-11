var arr: [1, 2, 3]
var arr2: [10, 9, *arr, 7, 6]
var arr3: [10, 9, arr, 7, 6]
print arr2
print arr3

var set1: {1, 2, 3}
var set2: {10, 9, *set1, 7, 6}
var set3: {10, 9, set1, 7, 6}
print set2
print set3

var map1: ("a" => 1, "b" => 2, "c" => 3)
var map2: ("x"=>10, "y"=>9, *map1, "z"=>7, "aa"=>6)
var map3: (bb->10, cc->9, mm=>map1, dd->7, ee->6)
print map2
print map3

// funcall...
fun f(Int a, Int b, Int c) => Int: (
	a + b * c
)

print f(*arr) // 7

