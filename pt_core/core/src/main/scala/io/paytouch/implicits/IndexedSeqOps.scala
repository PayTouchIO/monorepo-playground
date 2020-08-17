package io.paytouch.implicits

trait IndexedSeqOpsModule {
  final implicit class PaytouchIndexedSeqOps[+A](private val self: IndexedSeq[A]) {
    def random: A =
      self(scala.util.Random.nextInt(self.size))

    def secureRandom: A =
      self(SecureRandom.nextInt(self.size))
  }

  /**
    * https://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/CryptoSpec.html#AppC
    * https://docs.oracle.com/javase/8/docs/technotes/guides/security/SunProviders.html
    */
  private lazy val SecureRandom: java.security.SecureRandom =
    java.security.SecureRandom.getInstance("NativePRNGNonBlocking")

  // package io.paytouch.implicits

// import scala.collection.generic.CanBuildFrom
// import scala.collection.IndexedSeq

// trait IndexedSeqOpsModule {
//   // final implicit class PaytouchIndexedSeqOps[+A](private val self: IndexedSeq[A]) {
//   //   def zipWithIndexOne[A1 >: A, That](implicit bf: CanBuildFrom[(A1, Int), That]): That = // {
//   //     val b = bf(self.repr)
//   //     val len = self.length
//   //     b.sizeHint(len)
//   //     var i = 1 // copy pasted from the standard library and index changed from 0 to 1
//   //     while (i < len) {
//   //       b += ((self(i), i))
//   //       i += 1
//   //     }
//   //     b.result()
//   //   }
//   // }
// }

}
