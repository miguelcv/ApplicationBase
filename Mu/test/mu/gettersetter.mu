class Point(prop Int x, prop Int y): (
	fun toString() => String : (
		"Point (#{x},#{y})"
	)
	set x, y: (
		if $new > 100 (
			$value := 100
		) else (
			$value := $new
		)
	)
	
	get x: (
		print "intercepted getter, return 1"
		return 1
	) 
)

var pt: Point
pt := Point(0, 3)
print pt.x
print pt.y
pt.x := 10
print pt.x
print pt

pt.x := 1000
pt.y := 1000
assert pt.x = 1 "Should be 1, is: #{pt.x}"
assert pt.y = 100 "Should be 100, is: #{pt.y}"
print pt.x

print "Loop over object:"
for var i in pt (
	print i
)

throw "error"
