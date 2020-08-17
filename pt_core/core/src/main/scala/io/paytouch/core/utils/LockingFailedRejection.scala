package io.paytouch.core.utils

import akka.http.javadsl.server.CustomRejection

class LockingFailedRejection(val msg: String) extends CustomRejection
