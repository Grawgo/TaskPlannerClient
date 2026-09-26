package com.taskplanner.app.domain.model

data class AuthUser(val uid: String, val email: String?)

class AuthOperationException(message: String) : Exception(message)
class AuthValidationException(message: String) : Exception(message)
