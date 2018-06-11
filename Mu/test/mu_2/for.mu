var m: (a: 9, b:10)

var s: {5, 7, 9}
var lst: ["a", "b", "c"]


print "Loop over map:"
for var i in m (
	print i
	print i.type
)

print "Loop over set:"
for var i in s (
	print i
	print i.type
)

var lst: ["a", "b", "c"]

for var i in lst ( 
	if (i ~= "b") (
		print i
	)
)
