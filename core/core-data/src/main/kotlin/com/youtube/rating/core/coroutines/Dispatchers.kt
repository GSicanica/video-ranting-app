package com.youtube.rating.core.coroutines

import kotlinx.coroutines.Dispatchers

// Centralize dispatchers so call-sites don't hardcode Dispatchers.* everywhere.
// If you ever add tests, you can swap these via DI (don’t do that implicitly here).
val mainDispatcher = Dispatchers.Main.immediate
val ioDispatcher = Dispatchers.IO
