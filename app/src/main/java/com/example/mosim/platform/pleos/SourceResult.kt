package com.example.mosim.platform.pleos

/**
 * 차량·내비·외부 API 호출 결과다.
 *
 * SDK가 실패를 예외 콜백(onFailed)으로 주기 때문에, 화면 쪽에서 예외를 잡지 않아도 되도록 결과 값으로 바꿔 전달한다.
 * 실패 사유는 Vehicle SDK ErrorCode를 앱이 다르게 처리해야 하는 단위로만 묶었다.
 */
sealed interface SourceResult<out T> {
    data class Success<T>(val value: T) : SourceResult<T>
    data class Failure(val reason: FailureReason, val message: String? = null) : SourceResult<Nothing>
}

enum class FailureReason {
    /** ERROR_SECURITY_PERMISSION, TTS/STT PERMISSION_DENIED */
    PERMISSION_DENIED,

    /** ERROR_NOT_SUPPORTED_PROPERTY / AREA. 해당 기능 UI를 끈다 */
    NOT_SUPPORTED,

    /** 허용 범위 밖 값, 경유지 초과처럼 요청 자체가 잘못됨 */
    INVALID_REQUEST,

    /** ERROR_DRIVING_RESTRICTED */
    DRIVING_RESTRICTED,

    /** ERROR_SERVICE_CONNECTION_FAILURE, NaviHelper initialize 실패 */
    CONNECTION_FAILED,

    /** 외부 API 네트워크 실패, 타임아웃 */
    NETWORK,

    /** 그 밖의 오류 */
    UNKNOWN,
}
