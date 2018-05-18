var f

fun foo(String param) {
  fun f_() {
    print param;
  }
  f = f_;
}
foo("param");

f(); // expect: param
