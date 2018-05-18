var f: Func() => Void
var g: Func() => Void

(
  var local = "local"
  fun f_() => Void: (
    print local
    local := "after f"
    print local
  )
  f := f_

  fun g_() => Void: (
    print local
    local = "after g"
    print local
  }
  g = g_
}

f()
// expect: local
// expect: after f

g()
// expect: after f
// expect: after g
