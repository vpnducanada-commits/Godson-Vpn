package com.godson.vpn.vpn

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wireguard.crypto.Key
import com.wireguard.crypto.KeyPair
import kotlinx.coroutines.flow.first
import java.util.Base64

private val Context.keyStore by preferencesDataStore(name = "godson_keys")

/**
 * Gestion des clés WireGuard du client : UNE paire par appareil, générée localement.
 *
 * Règles de sécurité appliquées :
 * - la clé privée ne quitte JAMAIS l'appareil (seule la clé publique part vers le backend) ;
 * - la clé privée n'apparaît dans aucun journal ni rapport d'erreur ;
 * - aucune clé partagée n'est intégrée au code source.
 *
 * Amélioration planifiée (Phase 6) : chiffrement de la persistance via Android Keystore.
 * À ce jour, la clé est stockée dans le stockage privé de l'application (isolation par
 * UID Linux) — compromis documenté et assumé, sans exposition réseau possible.
 */
class VpnKeyManager(private val context: Context) {

    private object Keys {
        val PRIVATE = stringPreferencesKey("wg_private_key_b64")
    }

    /** @return la paire de l'appareil, créée au premier appel si nécessaire. */
    suspend fun getOrCreateKeyPair(): KeyPair {
        val stored = context.keyStore.data.first()[Keys.PRIVATE]
        if (stored != null) {
            return runCatching { KeyPair(Key.fromBase64(stored)) }.getOrElse { regenerate() }
        }
        return regenerate()
    }

    suspend fun regenerate(): KeyPair {
        val pair = KeyPair()
        context.keyStore.edit {
            it[Keys.PRIVATE] = Base64.getEncoder().encodeToString(pair.privateKey.getBytes())
        }
        return pair
    }

    /** Clé publique base64, à envoyer au backend lors de la création de session. */
    suspend fun publicKeyBase64(): String =
        Base64.getEncoder().encodeToString(getOrCreateKeyPair().publicKey.getBytes())
}
