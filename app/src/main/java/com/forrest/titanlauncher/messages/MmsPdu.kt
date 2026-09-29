package com.forrest.titanlauncher.messages



/*
 * MMS PDU PARSING
 *
 * MMS is a binary wire format (WSP encoding). Android implements it
 * at com.google.android.mms.pdu, but those classes are hidden and not
 * callable from an app, so this is a focused reimplementation of just
 * the parts a read-only client needs:
 *
 *   M-Notification.ind  the WAP push that says "a message is waiting",
 *                       carrying the sender and where to fetch it
 *   M-Retrieve.conf     the message itself, once downloaded
 *
 * Deliberately not implemented: sending, delivery reports, read
 * reports, DRM. Anything unrecognised is skipped rather than throwing,
 * because carriers include fields this does not need.
 */


/* PDU types */
internal const val MmsTypeNotificationInd = 0x82
internal const val MmsTypeRetrieveConf = 0x84

/* Header field codes, without the 0x80 well-known bit */
private const val FieldBcc = 0x01
private const val FieldCc = 0x02
private const val FieldContentLocation = 0x03
private const val FieldContentType = 0x04
private const val FieldDate = 0x05
private const val FieldFrom = 0x09
private const val FieldMessageSize = 0x0E
private const val FieldMessageType = 0x0C
private const val FieldSubject = 0x16
private const val FieldTo = 0x17
private const val FieldTransactionId = 0x18

/* From field tokens */
private const val AddressPresentToken = 0x80
private const val InsertAddressToken = 0x81


internal data class MmsPart(
    val contentType: String,
    val text: String?,
    val isImage: Boolean
)


internal data class MmsPdu(
    val messageType: Int,
    val transactionId: String?,
    val contentLocation: String?,
    val from: String?,
    val recipients: List<String>,
    val subject: String?,
    val dateSeconds: Long,
    val parts: List<MmsPart>
) {

    /*
     * The thread is defined by everyone on it, the sender included.
     */
    fun participants(): List<String> {

        return (
                listOfNotNull(
                    from
                ) + recipients
                )
            .map {
                it.trim()
            }
            .filter {
                it.isNotBlank()
            }
            .distinct()
    }

    fun bodyText(): String {

        val text =
            parts
                .mapNotNull {
                    it.text
                }
                .filter {
                    it.isNotBlank()
                }
                .joinToString(
                    separator = "\n"
                )
                .trim()

        val imageCount =
            parts.count {
                it.isImage
            }

        return when {

            text.isNotBlank() &&
                    imageCount > 0 ->
                text + "\n[" + imageCount + " image]"

            text.isNotBlank() ->
                text

            imageCount > 0 ->
                "[" + imageCount + " image]"

            else ->
                "[message]"
        }
    }
}


/*
 * A cursor over the PDU bytes. Every read advances the position, and
 * every reader returns null rather than throwing when the bytes do
 * not look like what it expected.
 */
private class PduReader(
    private val data: ByteArray
) {

    var position = 0

    fun hasMore(): Boolean =
        position < data.size

    fun peek(): Int =
        data[position].toInt() and 0xFF

    fun readByte(): Int {

        val value =
            data[position].toInt() and 0xFF

        position += 1

        return value
    }

    /*
     * Variable-length unsigned integer: seven bits per byte, high bit
     * set on every byte except the last.
     */
    fun readUintvar(): Int {

        var result = 0

        while (
            hasMore()
        ) {

            val byte =
                readByte()

            result =
                (result shl 7) or
                        (byte and 0x7F)

            if (
                byte and 0x80 == 0
            ) {
                break
            }
        }

        return result
    }

    fun readTextString(): String {

        /*
         * A leading 0x7F quotes a string whose first character would
         * otherwise look like a header code.
         */
        if (
            hasMore() &&
            peek() == 0x7F
        ) {
            position += 1
        }

        val start =
            position

        while (
            hasMore() &&
            peek() != 0x00
        ) {
            position += 1
        }

        val text =
            String(
                data,
                start,
                position - start,
                Charsets.UTF_8
            )

        if (
            hasMore()
        ) {
            position += 1
        }

        return text
    }

    /*
     * Short length (1..30), or 31 followed by a uintvar.
     */
    fun readValueLength(): Int {

        val first =
            readByte()

        return if (
            first == 31
        ) {
            readUintvar()
        } else {
            first
        }
    }

    fun readLongInteger(): Long {

        val length =
            readByte()

        var value = 0L

        repeat(
            length.coerceIn(
                0,
                8
            )
        ) {
            value =
                (value shl 8) or
                        readByte().toLong()
        }

        return value
    }

    fun readIntegerValue(): Long {

        return if (
            peek() >= 0x80
        ) {
            (readByte() and 0x7F).toLong()
        } else {
            readLongInteger()
        }
    }

    /*
     * Either a bare text-string, or a length-prefixed string carrying
     * a charset code this parser ignores beyond decoding as UTF-8,
     * which is what carriers send in practice.
     */
    fun readEncodedString(): String {

        if (
            !hasMore()
        ) {
            return ""
        }

        if (
            peek() >= 0x80 ||
            peek() > 31
        ) {
            return readTextString()
        }

        val length =
            readValueLength()

        val end =
            (position + length)
                .coerceAtMost(
                    data.size
                )

        /*
         * Skip the charset, which is an integer value.
         */
        if (
            position < end
        ) {
            readIntegerValue()
        }

        val text =
            readTextString()

        position =
            end

        return text
    }

    fun slice(
        from: Int,
        length: Int
    ): ByteArray {

        val safeFrom =
            from.coerceIn(
                0,
                data.size
            )

        val safeEnd =
            (safeFrom + length)
                .coerceIn(
                    safeFrom,
                    data.size
                )

        return data.copyOfRange(
            safeFrom,
            safeEnd
        )
    }
}


/*
 * Addresses arrive as "+15551234567/TYPE=PLMN". Only the number is
 * useful here.
 */
private fun cleanAddress(
    raw: String
): String {

    return raw
        .substringBefore(
            "/TYPE="
        )
        .trim()
}


internal object MmsPduParser {


    fun parse(
        data: ByteArray
    ): MmsPdu? {

        return runCatching {
            parseInternal(
                data
            )
        }
            .onFailure { error ->
            }
            .getOrNull()
    }

    fun toHex(
        data: ByteArray
    ): String {

        return data
            .take(
                512
            )
            .joinToString(
                separator = ""
            ) {
                "%02x".format(
                    it
                )
            }
    }

    private fun parseInternal(
        data: ByteArray
    ): MmsPdu? {

        val reader =
            PduReader(
                data
            )

        var messageType = -1
        var transactionId: String? = null
        var contentLocation: String? = null
        var from: String? = null
        var subject: String? = null
        var dateSeconds = 0L
        var contentTypeSeen = false

        val recipients =
            mutableListOf<String>()

        while (
            reader.hasMore()
        ) {

            val header =
                reader.peek()

            /*
             * Header codes are well-known values with the high bit
             * set. Anything else means the header block is over and
             * the body begins.
             */
            if (
                header < 0x80
            ) {
                break
            }

            reader.readByte()

            when (
                header and 0x7F
            ) {

                FieldMessageType ->
                    messageType =
                        reader.readByte()

                FieldTransactionId ->
                    transactionId =
                        reader.readTextString()

                FieldContentLocation ->
                    contentLocation =
                        reader.readTextString()

                FieldFrom -> {

                    val length =
                        reader.readValueLength()

                    val end =
                        reader.position + length

                    val token =
                        reader.readByte()

                    from =
                        if (
                            token == AddressPresentToken
                        ) {
                            cleanAddress(
                                reader.readEncodedString()
                            )
                        } else if (
                            token == InsertAddressToken
                        ) {
                            null
                        } else {
                            null
                        }

                    reader.position =
                        end.coerceAtMost(
                            data.size
                        )
                }

                FieldTo,
                FieldCc,
                FieldBcc -> {

                    val address =
                        cleanAddress(
                            reader.readEncodedString()
                        )

                    if (
                        address.isNotBlank()
                    ) {
                        recipients.add(
                            address
                        )
                    }
                }

                FieldSubject ->
                    subject =
                        reader.readEncodedString()

                FieldDate ->
                    dateSeconds =
                        reader.readLongInteger()

                FieldMessageSize ->
                    reader.readLongInteger()

                FieldContentType -> {

                    contentTypeSeen =
                        true

                    /*
                     * Content-Type is the last header; the body
                     * follows immediately.
                     */
                    skipContentType(
                        reader
                    )
                }

                else -> {

                    /*
                     * Unknown field. Skip its value by shape so the
                     * cursor stays aligned.
                     */
                    skipUnknownValue(
                        reader
                    )
                }
            }

            if (
                contentTypeSeen
            ) {
                break
            }
        }

        if (
            messageType == -1
        ) {
            return null
        }

        val parts =
            if (
                contentTypeSeen &&
                messageType == MmsTypeRetrieveConf
            ) {
                parseParts(
                    reader
                )
            } else {
                emptyList()
            }

        return MmsPdu(
            messageType =
                messageType,
            transactionId =
                transactionId,
            contentLocation =
                contentLocation,
            from =
                from,
            recipients =
                recipients,
            subject =
                subject,
            dateSeconds =
                dateSeconds,
            parts =
                parts
        )
    }

    private fun skipUnknownValue(
        reader: PduReader
    ) {

        if (
            !reader.hasMore()
        ) {
            return
        }

        val next =
            reader.peek()

        when {

            next >= 0x80 ->
                reader.readByte()

            next <= 31 -> {

                val length =
                    reader.readValueLength()

                reader.position =
                    (reader.position + length)
            }

            else ->
                reader.readTextString()
        }
    }

    private fun skipContentType(
        reader: PduReader
    ) {

        if (
            !reader.hasMore()
        ) {
            return
        }

        val next =
            reader.peek()

        when {

            next >= 0x80 ->
                reader.readByte()

            next <= 31 -> {

                val length =
                    reader.readValueLength()

                reader.position =
                    reader.position + length
            }

            else ->
                reader.readTextString()
        }
    }

    /*
     * Multipart body: a count, then for each part a header length, a
     * data length, the content type, the headers, and the data.
     */
    private fun parseParts(
        reader: PduReader
    ): List<MmsPart> {

        if (
            !reader.hasMore()
        ) {
            return emptyList()
        }

        val count =
            reader.readUintvar()

        val parts =
            mutableListOf<MmsPart>()

        repeat(
            count.coerceIn(
                0,
                64
            )
        ) {

            if (
                !reader.hasMore()
            ) {
                return parts
            }

            val headersLength =
                reader.readUintvar()

            val dataLength =
                reader.readUintvar()

            val headerStart =
                reader.position

            val contentType =
                readPartContentType(
                    reader
                )

            reader.position =
                headerStart + headersLength

            val dataStart =
                reader.position

            val data =
                reader.slice(
                    dataStart,
                    dataLength
                )

            reader.position =
                dataStart + dataLength

            val lower =
                contentType.lowercase()

            parts.add(
                MmsPart(
                    contentType =
                        contentType,
                    text =
                        if (
                            lower.startsWith(
                                "text/"
                            )
                        ) {
                            String(
                                data,
                                Charsets.UTF_8
                            )
                                .trim()
                                .trimEnd(
                                    '\u0000'
                                )
                        } else {
                            null
                        },
                    isImage =
                        lower.startsWith(
                            "image/"
                        ) ||
                                lower.startsWith(
                                    "video/"
                                )
                )
            )
        }

        return parts
    }

    /*
     * Well-known media types are a single byte; anything else is a
     * plain string. Only the handful a text client cares about are
     * mapped by code.
     */
    private fun readPartContentType(
        reader: PduReader
    ): String {

        if (
            !reader.hasMore()
        ) {
            return "application/octet-stream"
        }

        val next =
            reader.peek()

        if (
            next >= 0x80
        ) {

            return wellKnownContentType(
                reader.readByte() and 0x7F
            )
        }

        if (
            next <= 31
        ) {

            val length =
                reader.readValueLength()

            val end =
                reader.position + length

            val type =
                if (
                    reader.hasMore() &&
                    reader.peek() >= 0x80
                ) {
                    wellKnownContentType(
                        reader.readByte() and 0x7F
                    )
                } else {
                    reader.readTextString()
                }

            reader.position =
                end

            return type
        }

        return reader.readTextString()
    }

    private fun wellKnownContentType(
        code: Int
    ): String {

        return when (
            code
        ) {
            0x03 -> "text/plain"
            0x1D -> "image/gif"
            0x1E -> "image/jpeg"
            0x20 -> "image/png"
            0x21 -> "application/vnd.wap.multipart.related"
            0x23 -> "application/smil"
            0x33 -> "application/vnd.wap.multipart.mixed"
            else -> "application/octet-stream"
        }
    }
}