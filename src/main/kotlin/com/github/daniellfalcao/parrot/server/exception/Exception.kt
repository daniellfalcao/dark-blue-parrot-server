package com.github.daniellfalcao.parrot.server.exception

import io.grpc.Status
import io.grpc.StatusRuntimeException

// session exception

class InvalidTokenException : StatusRuntimeException(
    Status.UNAUTHENTICATED.withDescription("Invalid session.")
)

// unknown exceptions

class UnexpectedErrorException(override val cause: Throwable?) : StatusRuntimeException(
    Status.UNKNOWN.withCause(cause).withDescription("A unexpected error happened.")
)