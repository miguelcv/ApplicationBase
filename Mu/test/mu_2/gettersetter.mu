class Point(prop Int x, prop Int y): (
	fun toString() => String : (
		"Point (#{x},#{y})"
	)
)

var pt: Point
pt := Point(0, 3)
print pt.x
print pt.y
pt.x := 10
print pt.x
print pt

print "Loop over object:"
for var i in pt (
	print i
)
