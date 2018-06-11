/* python-like indent/dedent */
fun fib(Int n) => Int:
	if n < 2
		n
    else
		fib(n - 2) + fib(n - 1)

/* with brackets and semicolons */
fun fib2(Int n) => Int: (
	if n < 2 (
		n;
    ) else (
		fib2(n - 2) + fib2(n - 1);
	)
)

/* all on one line */
fun fib3(Int n) => Int: (print "fib3 #{n}"; if n < 2 (print n; n) else (fib3(n - 2) + fib3(n - 1)))

print fib(10)
print fib2(10)
print fib3(10)
