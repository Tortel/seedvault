package com.stevesoltys.backup.settings

import android.app.Application
import android.util.ByteStringUtils.toHexString
import androidx.lifecycle.AndroidViewModel
import com.stevesoltys.backup.LiveEvent
import com.stevesoltys.backup.MutableLiveEvent
import com.stevesoltys.backup.security.KeyManager
import io.github.novacrypto.bip39.*
import io.github.novacrypto.bip39.Validation.InvalidChecksumException
import io.github.novacrypto.bip39.Validation.InvalidWordCountException
import io.github.novacrypto.bip39.Validation.UnexpectedWhiteSpaceException
import io.github.novacrypto.bip39.Validation.WordNotFoundException
import io.github.novacrypto.bip39.wordlists.English
import java.security.SecureRandom
import java.util.*

internal const val WORD_NUM = 12

class RecoveryCodeViewModel(application: Application) : AndroidViewModel(application) {

    internal val wordList: List<CharSequence> by lazy {
        val items: ArrayList<CharSequence> = ArrayList(WORD_NUM)
        // TODO factor out entropy generation
        val entropy = ByteArray(Words.TWELVE.byteLength())
        SecureRandom().nextBytes(entropy)
        MnemonicGenerator(English.INSTANCE).createMnemonic(entropy) {
            if (it != " ") items.add(it)
        }
        items
    }

    private val mConfirmButtonClicked = MutableLiveEvent<Boolean>()
    internal val confirmButtonClicked: LiveEvent<Boolean> = mConfirmButtonClicked
    internal fun onConfirmButtonClicked() = mConfirmButtonClicked.setEvent(true)

    private val mRecoveryCodeSaved = MutableLiveEvent<Boolean>()
    internal val recoveryCodeSaved: LiveEvent<Boolean> = mRecoveryCodeSaved

    @Throws(WordNotFoundException::class, InvalidChecksumException::class)
    fun validateAndContinue(input: List<CharSequence>) {
        try {
            MnemonicValidator.ofWordList(English.INSTANCE).validate(input)
        } catch (e: UnexpectedWhiteSpaceException) {
            throw AssertionError(e)
        } catch (e: InvalidWordCountException) {
            throw AssertionError(e)
        }
        val mnemonic = input.joinToString(" ")
        val seed = SeedCalculator(JavaxPBKDF2WithHmacSHA512.INSTANCE).calculateSeed(mnemonic, "")
        KeyManager.storeBackupKey(seed)

        // TODO remove once encryption/decryption uses key from KeyStore
        setBackupPassword(getApplication(), toHexString(seed))

        mRecoveryCodeSaved.setEvent(true)
    }

}