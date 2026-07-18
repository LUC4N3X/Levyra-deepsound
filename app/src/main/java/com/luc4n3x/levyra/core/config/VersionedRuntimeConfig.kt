<html>
 <head></head>
 <body>
  package com.luc4n3x.levyra.core.config import java.util.concurrent.atomic.AtomicReference data class RuntimeConfigSnapshot
  <t>
   ( val value: T, val epoch: Long ) class VersionedRuntimeConfig
   <t>
    (initialValue: T) { private val state = AtomicReference(RuntimeConfigSnapshot(initialValue, 0L)) fun snapshot(): RuntimeConfigSnapshot
    <t>
     = state.get() fun update(newValue: T): RuntimeConfigSnapshot<t> { while (true) { val current = state.get() if (current.value == newValue) return current val updated = RuntimeConfigSnapshot(newValue, current.epoch + 1L) if (state.compareAndSet(current, updated)) return updated } } } </t>
    </t>
   </t>
  </t>
 </body>
</html>
