package extension

inline fun <T, reified K : Any> T.takeIfInstanceOf() = takeIf { it is K }?.let { it as K }