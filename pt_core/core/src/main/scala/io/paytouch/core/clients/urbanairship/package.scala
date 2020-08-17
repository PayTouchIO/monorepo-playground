package io.paytouch.core.clients

package object urbanairship {

  type UAResponse[T] = Either[Error, T]

}
