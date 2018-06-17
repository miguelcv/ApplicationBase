fun xyz(): (
	print "included function"
)

local fun abc(): (
	print "not included"
) 

//print "this will not print (immediately)"
return xyz
