package com.meneses.budgethunter.commons.platform

/**
 * Cross-platform permissions manager interface.
 * Platform-specific implementations are provided in androidMain and iosMain.
 */
expect class PermissionsManager() : IPermissionsManager
