fun fib(Int n) => Int (
	if n < 2 (n)
	fib(n - 2) + fib(n - 1)
)

var start: clock()
print fib(30) == 832040
print clock() - start
