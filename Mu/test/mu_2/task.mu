jvm class List(): "java.util.ArrayList"
var javaList: List()
javaList.add("Hi")
print javaList.`get(0)

jvm class Thread(): "java.lang.Thread"

fun async() => Int: (
	Thread.sleep(1000)
	33
)

//print async
//print async.type

var task: start async
//print task
//print task.type

var future: task()
print future

//while "unresolved" in future.str (
//	print Future.await(future, 1)
//	Thread.sleep(10)
//)

if future (
	print "done..."
	//Thread.sleep(10)
) else (
	print "waiting..."
)

print future:^
print future
print Future.isResolved(future)
