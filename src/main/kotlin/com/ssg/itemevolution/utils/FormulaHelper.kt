import net.objecthunter.exp4j.ExpressionBuilder

object FormulaHelper {
    /**
     * Avalia uma fórmula matemática para gerar o Counter de um nível específico.
     * @param formula A string da fórmula (ex: "({L} - 1)^1.4 * {DUR} * 1.0")
     * @param level O nível atual (L)
     * @param durability A durabilidade máxima do item (DUR)
     * @return O Counter (uso) necessário.
     */
    fun evaluateCounter(formula: String, level: Int, durability: Double): Double {
        if (level <= 1) return 0.0

        // 1. Substituir variáveis amigáveis por nomes válidos para Exp4J
        val exp4jFormula = formula
            .replace("{L}", "L")
            .replace("{DUR}", "DUR")
            .replace(" ", "") // Remover espaços

        try {
            // 2. Construir e avaliar a expressão
            val expression = ExpressionBuilder(exp4jFormula)
                .variables("L", "DUR")
                .build()
                .setVariable("L", level.toDouble())
                .setVariable("DUR", durability)

            return expression.evaluate()

        } catch (e: Exception) {
            // Logar o erro se a fórmula for inválida (ex: "2^2^2")
            // Retornar um valor padrão seguro para evitar crash
            println("ERRO ao avaliar a fórmula '$formula': ${e.message}")
            return (level - 1).toDouble() * 100.0 // Fallback simples
        }
    }
}