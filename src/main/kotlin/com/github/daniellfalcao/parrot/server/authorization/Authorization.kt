package com.github.daniellfalcao.parrot.server.authorization

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.github.daniellfalcao.parrot.server.ParrotServer
import com.github.daniellfalcao.parrot.server.exception.InvalidTokenException
import java.util.Date
import java.util.concurrent.TimeUnit

object Authorization {

    private const val USER_KEY = "dark-blue-parrot-user|"

    private val algorithm = Algorithm.HMAC256(ParrotServer.Constants.JWT_SECRET)
    private val verifier = JWT.require(algorithm).build()

    fun encode(userId: String): String {
        return JWT.create()
            .withSubject(USER_KEY + userId)
            .withExpiresAt(Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)))
            .sign(algorithm)
    }

    @Throws(InvalidTokenException::class)
    fun decode(token: String): String {
        val subject = try {
            verifier.verify(token).subject
        } catch (e: Exception) {
            throw InvalidTokenException()
        }
        if (subject.startsWith(USER_KEY)) {
            return subject.substringAfter(USER_KEY)
        } else {
            throw InvalidTokenException()
        }
    }

}