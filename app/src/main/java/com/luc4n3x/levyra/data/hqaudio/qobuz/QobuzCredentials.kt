/*
 * For research, testing, interoperability and lawful use only.
 * Levyra does not operate a music-hosting or subscription service, issue
 * third-party accounts or subscriptions, or grant licences to music content.
 * Publicly shared credentials do not grant permission to access an account,
 * redistribute credentials or use protected content. Valid authorization and
 * compliance with applicable law and service terms are required.
 * No piracy, unauthorized access or unlawful circumvention is authorized.
 * Warranty and liability limitations are governed by the project license and
 * docs/legal/LEGAL.md, subject to applicable law and mandatory rights.
 * This notice does not replace permissions or waive legal obligations.
 */

package com.luc4n3x.levyra.data.hqaudio.qobuz

internal object QobuzCredentials {
    class Account(val userAuthToken: String, val appId: String, val appSecret: String, val userId: String)

    val accounts: List<Account> = listOf(
        Account("OFaPe-anQIhYgeeB5JqeWM3GQNwoATn654XxL5EFIBsvI9Lo_eZfkNPpNeg-eCXnr1aziYesv5AK4IuFlUFIig", "312369995", "e79f8b9be485692b0e5f9dd895826368", "15597984")
    )
}
