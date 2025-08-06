package com.meneses.budgethunter.commons.bank

object SupportedBanks {
    val BANCAMIA = BankSmsConfig(
        id = "bancamia",
        displayName = "Bancamía",
        senderKeywords = listOf("Bancamia", "890380"), // Reemplaza con remitentes reales
        transactionAmountRegex = Regex("""\bvalor\s*\$([\d,.]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""compra\s*en\s*(.+?)\s*por""", RegexOption.IGNORE_CASE)
    )

    val BANCO_AGRARIO = BankSmsConfig(
        id = "banco_agrario",
        displayName = "Banco Agrario",
        senderKeywords = listOf("BancoAgrario", "890320"), // Reemplaza
        transactionAmountRegex = Regex("""compra\s*por\s*\$([\d,.]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""en\s*el\s*establecimiento\s*(.+?)\.""", RegexOption.IGNORE_CASE)
    )
    val BANCO_AV_VILLAS = BankSmsConfig(
        id = "banco_av_villas",
        displayName = "Banco AV Villas",
        senderKeywords = listOf("AV Villas", "85660"), // Reemplaza
        // Ejemplo simplificado: "AV Villas informa Compra por $10.000 en TIENDA XYZ."
        transactionAmountRegex = Regex("""Compra\s*por\s*\$([\d,.]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""en\s*(.+?)\.""", RegexOption.IGNORE_CASE)
    )

    val BANCO_CAJA_SOCIAL = BankSmsConfig(
        id = "banco_caja_social",
        displayName = "Banco Caja Social",
        senderKeywords = listOf("Caja Social", "890303"), // Reemplaza
        transactionAmountRegex = Regex("""valor\s*\$([\d,.]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""compra\s*en\s*(.+?)\s*con""", RegexOption.IGNORE_CASE)
    )

    val BANCOLOMBIA = BankSmsConfig(
        id = "bancolombia",
        displayName = "Bancolombia",
        senderKeywords = listOf("Bancolombia", "85784", "87400"),
        transactionAmountRegex = Regex("""(\$|COP)?\s?([\d.,]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("(?<=\\ba\\b)(.*?)(?=\\bdesde\\b)|(?<=\\ben\\b)(.*?)(?=\\bcon tu\\b)", RegexOption.IGNORE_CASE)
    )

    val BANCO_DE_BOGOTA = BankSmsConfig(
        id = "banco_de_bogota",
        displayName = "Banco de Bogotá",
        senderKeywords = listOf("Bco Bogota", "890031"), // Reemplaza
        transactionAmountRegex = Regex("""valor\s*\$([\d,.]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""compra\s*en\s*(.+?)\s*con""", RegexOption.IGNORE_CASE)
    )

    val BANCO_DE_OCCIDENTE = BankSmsConfig(
        id = "banco_de_occidente",
        displayName = "Banco de Occidente",
        senderKeywords = listOf("Bco Occidente", "85999"), // Reemplaza
        transactionAmountRegex = Regex("""por\s*valor\s*de\s*\$([\d,.]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""compra\s*en\s*(.+?)\.""", RegexOption.IGNORE_CASE)
    )

    val BANCO_FALABELLA = BankSmsConfig(
        id = "banco_falabella",
        displayName = "Banco Falabella",
        senderKeywords = listOf("Falabella", "890700"), // Reemplaza
        transactionAmountRegex = Regex("""monto\s*\$([\d,.]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""compra\s*en\s*(.+?)\.""", RegexOption.IGNORE_CASE)
    )

    val BANCO_GNB_SUDAMERIS = BankSmsConfig(
        id = "banco_gnb_sudameris",
        displayName = "Banco GNB Sudameris",
        senderKeywords = listOf("GNB Sudameris", "890201"), // Reemplaza
        transactionAmountRegex = Regex("""valor\s*\$([\d,.]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""compra\s*en\s*(.+?)\s*con""", RegexOption.IGNORE_CASE)
    )

    val BANCO_PICHINCHA = BankSmsConfig(
        id = "banco_pichincha",
        displayName = "Banco Pichincha",
        senderKeywords = listOf("Pichincha", "890310"), // Reemplaza
        transactionAmountRegex = Regex("""monto\s*\$([\d,.]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""compra\s*en\s*(.+?)\.""", RegexOption.IGNORE_CASE)
    )

    val BANCO_POPULAR = BankSmsConfig(
        id = "banco_popular",
        displayName = "Banco Popular",
        senderKeywords = listOf("Bco Popular", "890102"), // Reemplaza
        transactionAmountRegex = Regex("""por\s*\$([\d,.]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""compra\s*en\s*(.+?)\s*el""", RegexOption.IGNORE_CASE)
    )

    val BBVA_COLOMBIA = BankSmsConfig(
        id = "bbva_colombia",
        displayName = "BBVA Colombia",
        senderKeywords = listOf("BBVA", "85330", "890001"), // Reemplaza
        transactionAmountRegex = Regex("""compra\s*por\s*\$?([\d,.]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""en\s*(.+?)\.""", RegexOption.IGNORE_CASE)
    )

    val CITIBANK_COLOMBIA = BankSmsConfig( // Scotiabank Colpatria adquirió la banca de consumo de Citibank
        id = "citibank_colombia", // Podrías mantenerlo por compatibilidad o renombrar
        displayName = "Citibank (ahora Scotiabank Colpatria)",
        senderKeywords = listOf("Scotiabank Colpatria", "Colpatria", "85777"), // Reemplaza
        transactionAmountRegex = Regex("""valor\s*\$([\d,.]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""compra\s*en\s*(.+?)\s*con""", RegexOption.IGNORE_CASE)
    )
     val DAVIVIENDA = BankSmsConfig(
        id = "davivienda",
        displayName = "Davivienda",
        senderKeywords = listOf("Davivienda", "85888", "890002"), // Reemplaza
        // Ejemplo: "Davivienda informa compra por $50.000 en SUPERMERCADO."
        transactionAmountRegex = Regex("""compra\s*por\s*\$?([\d,.]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""en\s*(.+?)\.""", RegexOption.IGNORE_CASE)
    )
    val ITAU_COLOMBIA = BankSmsConfig(
        id = "itau_colombia",
        displayName = "Itaú Colombia",
        senderKeywords = listOf("Itau", "890007"), // Reemplaza
        transactionAmountRegex = Regex("""compra\s*por\s*\$?([\d,.]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""en\s*(.+?)\.""", RegexOption.IGNORE_CASE)
    )
    val SCOTIABANK_COLPATRIA = BankSmsConfig(
        id = "scotiabank_colpatria",
        displayName = "Scotiabank Colpatria",
        senderKeywords = listOf("Scotiabank Colpatria", "Colpatria", "85777"), // Reemplaza (similar a Citibank)
        transactionAmountRegex = Regex("""valor\s*\$([\d,.]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""compra\s*en\s*(.+?)\s*con""", RegexOption.IGNORE_CASE)
    )

    val ALL_BANKS = listOf(
        //BANCAMIA,
        //BANCO_AGRARIO,
        //BANCO_AV_VILLAS,
        //BANCO_CAJA_SOCIAL,
        BANCOLOMBIA,
        //BANCO_DE_BOGOTA,
        //BANCO_DE_OCCIDENTE,
        //BANCO_FALABELLA,
        //BANCO_GNB_SUDAMERIS,
        //BANCO_PICHINCHA,
        //BANCO_POPULAR,
        BBVA_COLOMBIA,
        //CITIBANK_COLOMBIA, // o Scotiabank Colpatria directamente
        DAVIVIENDA,
        //ITAU_COLOMBIA,
        //SCOTIABANK_COLPATRIA
    ).sortedBy { it.displayName }

    fun getBankConfigById(id: String?): BankSmsConfig? {
        return ALL_BANKS.find { it.id == id }
    }
}
