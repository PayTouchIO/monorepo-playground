package io.paytouch

import spells.Spells

package object implicits extends AnyOpsModule with BooleanOpsModule with Spells with ZonedDateTimeModule
