package com.meneses.budgethunter.commons.bank

object SupportedBanks {
    val BANCAMIA = BankSmsConfig(
        id = "bancamia",
        displayName = "Bancamía",
        senderKeywords = listOf("Bancamia", "BANCAMIA", "890380"),
        transactionAmountRegex = Regex("""(?:valor|monto|por|de)\s*\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""(?:compra|pago|transaccion)\s*(?:en|por|de)\s*(.+?)(?:\s*(?:por|con|el|\.|$)|$)""", RegexOption.IGNORE_CASE)
    )

    val BANCO_AGRARIO = BankSmsConfig(
        id = "banco_agrario",
        displayName = "Banco Agrario",
        senderKeywords = listOf("BancoAgrario", "BANCO AGRARIO", "890320"),
        transactionAmountRegex = Regex("""(?:compra|pago|transaccion)\s*(?:por|de)\s*\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""(?:en|del|establecimiento)\s*(.+?)(?:\s*\.|$)""", RegexOption.IGNORE_CASE)
    )
    
    val BANCO_AV_VILLAS = BankSmsConfig(
        id = "banco_av_villas",
        displayName = "Banco AV Villas",
        senderKeywords = listOf("AV Villas", "AVVILLAS", "85660"),
        transactionAmountRegex = Regex("""(?:Compra|Pago|Transaccion)\s*(?:por|de)\s*\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""(?:en|del|establecimiento)\s*(.+?)(?:\s*\.|$)""", RegexOption.IGNORE_CASE)
    )

    val BANCO_CAJA_SOCIAL = BankSmsConfig(
        id = "banco_caja_social",
        displayName = "Banco Caja Social",
        senderKeywords = listOf("Caja Social", "CAJA SOCIAL", "890303"),
        transactionAmountRegex = Regex("""(?:valor|monto|por|de)\s*\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""(?:compra|pago|transaccion)\s*(?:en|por|de)\s*(.+?)(?:\s*(?:con|\.|$)|$)""", RegexOption.IGNORE_CASE)
    )

    val BANCOLOMBIA = BankSmsConfig(
        id = "bancolombia",
        displayName = "Bancolombia",
        senderKeywords = listOf("Bancolombia", "BANCOLOMBIA", "85784", "87400"),
        transactionAmountRegex = Regex("""(?:valor|monto|por|de)\s*\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("(?<=\\b(?:en|a|de)\\s)([A-ZÁÉÍÓÚÑa-záéíóúñ0-9 .,&-]+?)(?=\\s+(?:con|desde|en|, el|\\$|COP))", RegexOption.IGNORE_CASE)
    )

    val BANCO_DE_BOGOTA = BankSmsConfig(
        id = "banco_de_bogota",
        displayName = "Banco de Bogotá",
        senderKeywords = listOf("Bco Bogota", "BANCO BOGOTA", "890031"),
        transactionAmountRegex = Regex("""(?:valor|monto|por|de)\s*\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""(?:compra|pago|transaccion)\s*(?:en|por|de)\s*(.+?)(?:\s*(?:con|\.|$)|$)""", RegexOption.IGNORE_CASE)
    )

    val BANCO_DE_OCCIDENTE = BankSmsConfig(
        id = "banco_de_occidente",
        displayName = "Banco de Occidente",
        senderKeywords = listOf("Bco Occidente", "BANCO OCCIDENTE", "85999"),
        transactionAmountRegex = Regex("""(?:por|de)\s*(?:valor|monto)\s*(?:de)?\s*\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""(?:compra|pago|transaccion)\s*(?:en|por|de)\s*(.+?)(?:\s*\.|$)""", RegexOption.IGNORE_CASE)
    )

    val BANCO_FALABELLA = BankSmsConfig(
        id = "banco_falabella",
        displayName = "Banco Falabella",
        senderKeywords = listOf("Falabella", "BANCO FALABELLA", "890700"),
        transactionAmountRegex = Regex("""(?:monto|valor|por|de)\s*\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""(?:compra|pago|transaccion)\s*(?:en|por|de)\s*(.+?)(?:\s*\.|$)""", RegexOption.IGNORE_CASE)
    )

    val BANCO_GNB_SUDAMERIS = BankSmsConfig(
        id = "banco_gnb_sudameris",
        displayName = "Banco GNB Sudameris",
        senderKeywords = listOf("GNB Sudameris", "GNB SUDAMERIS", "890201"),
        transactionAmountRegex = Regex("""(?:valor|monto|por|de)\s*\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""(?:compra|pago|transaccion)\s*(?:en|por|de)\s*(.+?)(?:\s*(?:con|\.|$)|$)""", RegexOption.IGNORE_CASE)
    )

    val BANCO_PICHINCHA = BankSmsConfig(
        id = "banco_pichincha",
        displayName = "Banco Pichincha",
        senderKeywords = listOf("Pichincha", "BANCO PICHINCHA", "890310"),
        transactionAmountRegex = Regex("""(?:monto|valor|por|de)\s*\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""(?:compra|pago|transaccion)\s*(?:en|por|de)\s*(.+?)(?:\s*\.|$)""", RegexOption.IGNORE_CASE)
    )

    val BANCO_POPULAR = BankSmsConfig(
        id = "banco_popular",
        displayName = "Banco Popular",
        senderKeywords = listOf("Bco Popular", "BANCO POPULAR", "890102", "Popular"),
        transactionAmountRegex = Regex("""(?:por|de|valor|monto)\s*\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""(?:compra|pago|transaccion)\s*(?:en|por|de)\s*(.+?)(?:\s*(?:el|\.|$)|$)""", RegexOption.IGNORE_CASE)
    )

    val BBVA_COLOMBIA = BankSmsConfig(
        id = "bbva_colombia",
        displayName = "BBVA Colombia",
        senderKeywords = listOf("BBVA", "BBVA COLOMBIA", "85330", "890001"),
        transactionAmountRegex = Regex("""(?:compra|pago|transaccion)\s*(?:por|de)\s*\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""(?:en|del|establecimiento)\s*(.+?)(?:\s*\.|$)""", RegexOption.IGNORE_CASE)
    )

    val CITIBANK_COLOMBIA = BankSmsConfig(
        id = "citibank_colombia",
        displayName = "Citibank (ahora Scotiabank Colpatria)",
        senderKeywords = listOf("Scotiabank Colpatria", "COLPATRIA", "85777", "Citibank"),
        transactionAmountRegex = Regex("""(?:valor|monto|por|de)\s*\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""(?:compra|pago|transaccion)\s*(?:en|por|de)\s*(.+?)(?:\s*(?:con|\.|$)|$)""", RegexOption.IGNORE_CASE)
    )
    
    val DAVIVIENDA = BankSmsConfig(
        id = "davivienda",
        displayName = "Davivienda",
        senderKeywords = listOf("Davivienda", "DAVIVIENDA", "85888", "890002"),
        transactionAmountRegex = Regex("""(?:compra|pago|transaccion)\s*(?:por|de)\s*\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""(?:en|del|establecimiento)\s*(.+?)(?:\s*\.|$)""", RegexOption.IGNORE_CASE)
    )
    
    val ITAU_COLOMBIA = BankSmsConfig(
        id = "itau_colombia",
        displayName = "Itaú Colombia",
        senderKeywords = listOf("Itau", "ITAU", "890007"),
        transactionAmountRegex = Regex("""(?:compra|pago|transaccion)\s*(?:por|de)\s*\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""(?:en|del|establecimiento)\s*(.+?)(?:\s*\.|$)""", RegexOption.IGNORE_CASE)
    )
    
    val SCOTIABANK_COLPATRIA = BankSmsConfig(
        id = "scotiabank_colpatria",
        displayName = "Scotiabank Colpatria",
        senderKeywords = listOf("Scotiabank Colpatria", "COLPATRIA", "85777", "Scotiabank"),
        transactionAmountRegex = Regex("""(?:valor|monto|por|de)\s*\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
        transactionDescriptionRegex = Regex("""(?:compra|pago|transaccion)\s*(?:en|por|de)\s*(.+?)(?:\s*(?:con|\.|$)|$)""", RegexOption.IGNORE_CASE)
    )

    val ALL_BANKS = listOf(
        BANCAMIA,
        BANCO_AGRARIO,
        BANCO_AV_VILLAS,
        BANCO_CAJA_SOCIAL,
        BANCOLOMBIA,
        BANCO_DE_BOGOTA,
        BANCO_DE_OCCIDENTE,
        BANCO_FALABELLA,
        BANCO_GNB_SUDAMERIS,
        BANCO_PICHINCHA,
        BANCO_POPULAR,
        BBVA_COLOMBIA,
        CITIBANK_COLOMBIA,
        DAVIVIENDA,
        ITAU_COLOMBIA,
        SCOTIABANK_COLPATRIA
    ).sortedBy { it.displayName }

    fun getBankConfigById(id: String?): BankSmsConfig? {
        return ALL_BANKS.find { it.id == id }
    }
}
