// package io.paytouch.implicits

// import scala.collection.generic.CanBuildFrom
// import scala.collection.Iterable

// trait IterableOpsModule {
//   final implicit class PaytouchIterableOps[+A](private val self: Iterable[A]) {
//     def zipWithIndexOne[A1 >: A, That](implicit bf: CanBuildFrom[(A1, Int), That]): That = {
//       val b = bf(self.repr)
//       val these = self.iterator
//       var i = 1 // copy pasted from the standard library and index changed from 0 to 1
//       while (these.hasNext) {
//         b += ((these.next(), i))
//         i += 1
//       }
//       b.result()
//     }
//   }
// }
