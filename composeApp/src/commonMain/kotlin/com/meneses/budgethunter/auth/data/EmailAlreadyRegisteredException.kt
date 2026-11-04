package com.meneses.budgethunter.auth.data

/** Sign up was refused because an account with that email already exists. */
class EmailAlreadyRegisteredException : Exception("An account with this email already exists")
