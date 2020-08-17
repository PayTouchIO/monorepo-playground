package io.paytouch

import spells.Spells

package object implicits
    extends AnyOpsModule
       with BooleanOpsModule
       with IndexedSeqOpsModule
       //  with IterableOpsModule
       with SeqOpsModule
       with Spells
       with StringOpsModule
