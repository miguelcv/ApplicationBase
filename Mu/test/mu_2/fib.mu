fun fib(Int n) => Int:
	if n < 2
		n
    else
		fib(n - 2) + fib(n - 1)

print fib(10)