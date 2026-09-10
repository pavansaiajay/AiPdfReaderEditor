package pavansaiajayx.aipdfreadereditor.core.common.result

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import pavansaiajayx.aipdfreadereditor.core.common.util.UiText

sealed interface Result<out T> {
    data class Success<out T>(val data: T) : Result<T>
    data class Error(
        val message: UiText? = null,
        val cause: Throwable? = null
    ) : Result<Nothing> {
        constructor(message: String, cause: Throwable? = null) : this(UiText.DynamicString(message), cause)
        constructor(cause: Throwable) : this(cause.message?.let { UiText.DynamicString(it) }, cause)
    }
    data object Loading : Result<Nothing>
}

inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
    is Result.Loading -> Result.Loading
}

inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onFailure(action: (message: UiText?, cause: Throwable?) -> Unit): Result<T> {
    if (this is Result.Error) action(message, cause)
    return this
}

fun <T> Result<T>.getOrNull(): T? = when (this) {
    is Result.Success -> data
    else -> null
}

fun <T> Result<T>.getOrDefault(default: T): T = when (this) {
    is Result.Success -> data
    else -> default
}

fun <T> Flow<T>.asResult(): Flow<Result<T>> = this
    .map<T, Result<T>> { Result.Success(it) }
    .onStart { emit(Result.Loading) }
    .catch { throwable ->
        emit(
            Result.Error(
                message = throwable.message?.let { UiText.DynamicString(it) },
                cause = throwable
            )
        )
    }
