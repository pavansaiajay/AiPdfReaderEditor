package pavansaiajayx.aipdfreadereditor.core.ai

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import pavansaiajayx.aipdfreadereditor.core.ai.client.GeminiClient
import pavansaiajayx.aipdfreadereditor.core.ai.exception.InsufficientCreditsException
import pavansaiajayx.aipdfreadereditor.core.common.economy.TokenCalculator
import pavansaiajayx.aipdfreadereditor.core.common.network.AppDispatchers
import pavansaiajayx.aipdfreadereditor.core.common.network.Dispatcher
import pavansaiajayx.aipdfreadereditor.core.common.result.Result
import pavansaiajayx.aipdfreadereditor.core.datastore.economy.CreditManager
import pavansaiajayx.aipdfreadereditor.core.model.AiMessage
import pavansaiajayx.aipdfreadereditor.core.model.AiRole
import pavansaiajayx.aipdfreadereditor.core.model.CreditReason
import pavansaiajayx.aipdfreadereditor.core.model.TokenUsage
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiEngine @Inject constructor(
    private val geminiClient: GeminiClient,
    private val creditManager: CreditManager,
    @Dispatcher(AppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun generateSummary(text: String): Result<AiMessage> {
        val balance = creditManager.creditsFlow.first()
        if (balance < 1) {
            return Result.Error(
                cause = InsufficientCreditsException(currentBalance = balance, requiredCredits = 1)
            )
        }

        return withContext(ioDispatcher) {
            try {
                val prompt = "You are an expert document analyzer. Summarize the following text clearly and concisely using markdown bullet points. Here is the text:\n\n$text"
                val response = geminiClient.generateSummary(prompt)
                val cost = TokenCalculator.calculateCreditCost(
                    promptTokens = response.promptTokens,
                    candidateTokens = response.candidateTokens
                )
                creditManager.deductCredits(cost, CreditReason.AiDocumentSummary)

                Result.Success(
                    AiMessage(
                        id = UUID.randomUUID().toString(),
                        role = AiRole.Model,
                        content = response.text,
                        tokenUsage = TokenUsage(
                            promptTokens = response.promptTokens,
                            candidateTokens = response.candidateTokens,
                            creditCost = cost
                        )
                    )
                )
            } catch (e: Exception) {
                Result.Error(cause = e)
            }
        }
    }

    fun createChatSession(documentContext: String): AiChatSession {
        return DefaultAiChatSession(
            documentContext = documentContext,
            geminiClient = geminiClient,
            creditManager = creditManager,
            ioDispatcher = ioDispatcher
        )
    }

    suspend fun sendChatMessage(systemContext: String, userMessage: String): Result<AiMessage> {
        val balance = creditManager.creditsFlow.first()
        if (balance < 1) {
            return Result.Error(
                cause = InsufficientCreditsException(currentBalance = balance, requiredCredits = 1)
            )
        }

        return withContext(ioDispatcher) {
            try {
                val response = geminiClient.sendChatMessage(systemContext, userMessage)
                val cost = TokenCalculator.calculateCreditCost(
                    promptTokens = response.promptTokens,
                    candidateTokens = response.candidateTokens
                )
                creditManager.deductCredits(cost, CreditReason.AiChatQuery)

                Result.Success(
                    AiMessage(
                        id = UUID.randomUUID().toString(),
                        role = AiRole.Model,
                        content = response.text,
                        tokenUsage = TokenUsage(
                            promptTokens = response.promptTokens,
                            candidateTokens = response.candidateTokens,
                            creditCost = cost
                        )
                    )
                )
            } catch (e: Exception) {
                Result.Error(cause = e)
            }
        }
    }
}

interface AiChatSession {
    val documentContext: String
    fun getMessages(): List<AiMessage>
    suspend fun sendMessage(userMessage: String): Result<AiMessage>
}

private class DefaultAiChatSession(
    override val documentContext: String,
    private val geminiClient: GeminiClient,
    private val creditManager: CreditManager,
    private val ioDispatcher: CoroutineDispatcher
) : AiChatSession {
    private val _messages = mutableListOf<AiMessage>()

    override fun getMessages(): List<AiMessage> = synchronized(_messages) {
        _messages.toList()
    }

    override suspend fun sendMessage(userMessage: String): Result<AiMessage> {
        val balance = creditManager.creditsFlow.first()
        if (balance < 1) {
            return Result.Error(
                cause = InsufficientCreditsException(currentBalance = balance, requiredCredits = 1)
            )
        }

        return withContext(ioDispatcher) {
            try {
                val userAiMessage = AiMessage(
                    id = UUID.randomUUID().toString(),
                    role = AiRole.User,
                    content = userMessage
                )
                val response = geminiClient.sendChatMessage(documentContext, userMessage)
                val cost = TokenCalculator.calculateCreditCost(
                    promptTokens = response.promptTokens,
                    candidateTokens = response.candidateTokens
                )
                creditManager.deductCredits(cost, CreditReason.AiChatQuery)

                val modelAiMessage = AiMessage(
                    id = UUID.randomUUID().toString(),
                    role = AiRole.Model,
                    content = response.text,
                    tokenUsage = TokenUsage(
                        promptTokens = response.promptTokens,
                        candidateTokens = response.candidateTokens,
                        creditCost = cost
                    )
                )

                synchronized(_messages) {
                    _messages.add(userAiMessage)
                    _messages.add(modelAiMessage)
                }

                Result.Success(modelAiMessage)
            } catch (e: Exception) {
                Result.Error(cause = e)
            }
        }
    }
}
