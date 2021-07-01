package com.github.daniellfalcao.parrot.database.exception

import io.grpc.Status
import io.grpc.StatusRuntimeException

// post exceptions

class PostNotFoundException : StatusRuntimeException(
    Status.NOT_FOUND.withDescription("Post not found.")
)

class EditPostForbiddenOperationException : StatusRuntimeException(
    Status.PERMISSION_DENIED.withDescription("You don't have the proper authorization to edit this post.")
)

class DeletePostForbiddenOperationException : StatusRuntimeException(
    Status.PERMISSION_DENIED.withDescription("You don't have the proper authorization to delete this post.")
)

// user exceptions

class UserAlreadyExistsException : StatusRuntimeException(
    Status.ALREADY_EXISTS.withDescription("A user with the same username already exists.")
)

class UserNotFoundException : StatusRuntimeException(
    Status.NOT_FOUND.withDescription("User not found.")
)

class InvalidUsernameOrPasswordException : StatusRuntimeException(
    Status.NOT_FOUND.withDescription("Invalid username or password.")
)
