package com.paradox.app.feature.capture

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.core.money.Money
import com.paradox.app.core.util.VoiceRecognitionManager
import com.paradox.app.core.util.VoiceState
import com.paradox.app.domain.model.Category
import com.paradox.app.domain.model.Expense
import com.paradox.app.domain.model.PaymentMethod
import com.paradox.app.domain.repository.CategoryRepository
import com.paradox.app.domain.repository.ExpenseRepository
import com.paradox.app.domain.repository.PaymentMethodRepository
import com.paradox.app.domain.usecase.capture.CsvImportPreview
import com.paradox.app.domain.usecase.capture.CsvImportUseCase
import com.paradox.app.domain.usecase.capture.DuplicateGuardUseCase
import com.paradox.app.domain.usecase.capture.DuplicateWarning
import com.paradox.app.domain.usecase.capture.NaturalLanguageParser
import com.paradox.app.domain.usecase.capture.ReceiptOcrParser
import com.paradox.app.feature.capture.ocr.ReceiptImageOcrHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

import android.content.Context
import com.paradox.app.core.network.GeminiApiClient
import com.paradox.app.domain.repository.AiSettingsRepository
import com.paradox.app.domain.usecase.category.FuzzyCategoryMatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first

enum class CaptureMode {
    QUICK_ADD,
    VOICE,
    OCR,
    CSV
}

data class EditableCandidate(
    val title: String = "",
    val amountStr: String = "",
    val currency: String = "INR",
    val date: LocalDate = LocalDate.now(),
    val categoryId: String = "",
    val paymentMethodId: String = "",
    val notes: String = "",
    val source: String = "QUICK_ADD",
    val duplicateWarning: DuplicateWarning = DuplicateWarning(false),
    val suggestedNewCategoryName: String? = null
)

data class CaptureUiState(
    val selectedMode: CaptureMode = CaptureMode.QUICK_ADD,
    val inputText: String = "",
    val candidate: EditableCandidate? = null,
    val categories: List<Category> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val voiceState: VoiceState = VoiceState.Idle,
    val csvPreview: CsvImportPreview? = null,
    val isProcessing: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val activeProfileId: String? = null,
    val isAiEnabled: Boolean = false
)

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val sessionDataStore: SessionDataStore,
    private val categoryRepository: CategoryRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val expenseRepository: ExpenseRepository,
    private val nlParser: NaturalLanguageParser,
    private val ocrParser: ReceiptOcrParser,
    private val ocrHelper: ReceiptImageOcrHelper,
    private val voiceManager: VoiceRecognitionManager,
    private val duplicateGuard: DuplicateGuardUseCase,
    private val csvImportUseCase: CsvImportUseCase,
    private val geminiApiClient: GeminiApiClient,
    private val aiSettingsRepository: AiSettingsRepository,
    private val fuzzyCategoryMatcher: FuzzyCategoryMatcher,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    private var voiceCollectionJob: Job? = null

    init {
        loadData()
        observeVoiceState()
    }

    private fun loadData() {
        viewModelScope.launch {
            aiSettingsRepository.isAiEnabled.collectLatest { enabled ->
                _uiState.update { it.copy(isAiEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            sessionDataStore.activeProfileId.collectLatest { profileId ->
                _uiState.update { it.copy(activeProfileId = profileId) }
                if (profileId != null) {
                    val cats = categoryRepository.getCategories(profileId).firstOrNull() ?: emptyList()
                    val pms = paymentMethodRepository.getPaymentMethods(profileId).firstOrNull() ?: emptyList()
                    _uiState.update { it.copy(categories = cats, paymentMethods = pms) }
                }
            }
        }
    }

    private fun observeVoiceState() {
        voiceCollectionJob?.cancel()
        voiceCollectionJob = viewModelScope.launch {
            voiceManager.voiceState.collectLatest { state ->
                _uiState.update { it.copy(voiceState = state) }
                if (state is VoiceState.Recognized) {
                    processNlInput(state.text, source = "VOICE")
                }
            }
        }
    }

    fun setMode(mode: CaptureMode) {
        _uiState.update { it.copy(selectedMode = mode, errorMessage = null) }
        if (mode != CaptureMode.VOICE) {
            voiceManager.reset()
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
        if (text.isNotBlank()) {
            processNlInput(text, source = "QUICK_ADD")
        } else {
            _uiState.update { it.copy(candidate = null) }
        }
    }

    fun startVoiceListening() {
        voiceManager.startListening()
    }

    fun stopVoiceListening() {
        voiceManager.stopListening()
    }

    fun processImageUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }

            // 1. Try Gemini Multimodal Vision if AI is enabled + API key is present
            val isAiOn = aiSettingsRepository.isAiEnabled.first()
            val apiKey = if (isAiOn) aiSettingsRepository.getApiKey() else null

            if (isAiOn && !apiKey.isNullOrBlank()) {
                val imageBytes = try {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } catch (_: Exception) { null }

                if (imageBytes != null && imageBytes.isNotEmpty()) {
                    val catNames = _uiState.value.categories.map { it.name }
                    val visionResult = geminiApiClient.analyzeReceiptMultimodal(
                        imageBytes = imageBytes,
                        mimeType = "image/jpeg",
                        availableCategories = catNames,
                        apiKey = apiKey
                    )

                    if (visionResult.isSuccess) {
                        val extraction = visionResult.getOrThrow()
                        applyParsedCandidate(
                            title = extraction.merchant ?: "Receipt Expense",
                            amount = extraction.totalAmount?.let { BigDecimal.valueOf(it).toPlainString() } ?: "",
                            date = extraction.date?.let { try { LocalDate.parse(it) } catch (_: Exception) { LocalDate.now() } } ?: LocalDate.now(),
                            suggestedCatName = extraction.categoryName,
                            suggestedPayType = extraction.paymentMode,
                            notes = extraction.notes ?: "Scanned via Gemini Vision AI",
                            source = "AI_VISION"
                        )
                        _uiState.update { it.copy(isProcessing = false) }
                        return@launch
                    }
                }
            }

            // 2. Deterministic ML Kit OCR fallback
            val result = ocrHelper.processUri(uri)
            result.onSuccess { lines ->
                val parsed = ocrParser.parseReceiptText(lines)
                applyParsedCandidate(
                    title = parsed.title,
                    amount = parsed.amount?.amount?.toPlainString() ?: "",
                    date = parsed.date,
                    suggestedCatName = parsed.suggestedCategoryName,
                    suggestedPayType = parsed.suggestedPaymentType?.name,
                    notes = parsed.notes ?: "Scanned from receipt",
                    source = "OCR"
                )
            }.onFailure { e ->
                _uiState.update { it.copy(errorMessage = "Failed to process image: ${e.message}") }
            }
            _uiState.update { it.copy(isProcessing = false) }
        }
    }

    fun processCapturedLines(lines: List<String>) {
        val parsed = ocrParser.parseReceiptText(lines)
        applyParsedCandidate(
            title = parsed.title,
            amount = parsed.amount?.amount?.toPlainString() ?: "",
            date = parsed.date,
            suggestedCatName = parsed.suggestedCategoryName,
            suggestedPayType = parsed.suggestedPaymentType?.name,
            notes = parsed.notes ?: "Live OCR Camera scan",
            source = "OCR"
        )
    }

    fun onCsvContentSelected(csvContent: String) {
        val profileId = _uiState.value.activeProfileId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            val preview = csvImportUseCase.parseAndPreview(csvContent, profileId)
            _uiState.update { it.copy(csvPreview = preview, isProcessing = false) }
        }
    }

    fun commitCsvImport() {
        val profileId = _uiState.value.activeProfileId ?: return
        val preview = _uiState.value.csvPreview ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val count = csvImportUseCase.commitImport(profileId, preview.validCandidates)
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    saveSuccess = true,
                    csvPreview = null,
                    errorMessage = "Successfully imported $count transactions"
                )
            }
        }
    }

    private fun processNlInput(input: String, source: String) {
        val parsed = nlParser.parse(input)
        applyParsedCandidate(
            title = parsed.title,
            amount = parsed.amount?.amount?.toPlainString() ?: "",
            date = parsed.date,
            suggestedCatName = parsed.suggestedCategoryName,
            suggestedPayType = parsed.suggestedPaymentType?.name,
            notes = parsed.notes ?: "",
            source = source
        )
    }

    private fun applyParsedCandidate(
        title: String,
        amount: String,
        date: LocalDate,
        suggestedCatName: String?,
        suggestedPayType: String?,
        notes: String,
        source: String
    ) {
        val state = _uiState.value
        val matchResult = fuzzyCategoryMatcher.matchCategory(suggestedCatName, state.categories)
        val defaultCat = matchResult.matchedCategory
            ?: state.categories.firstOrNull { it.isDefault }
            ?: state.categories.firstOrNull()

        val defaultPay = state.paymentMethods.firstOrNull { it.type.name.equals(suggestedPayType, ignoreCase = true) }
            ?: state.paymentMethods.firstOrNull()

        val candidate = EditableCandidate(
            title = title,
            amountStr = amount,
            currency = "INR",
            date = date,
            categoryId = defaultCat?.id ?: "",
            paymentMethodId = defaultPay?.id ?: "",
            notes = notes,
            source = source,
            suggestedNewCategoryName = matchResult.suggestedNewCategoryName
        )

        _uiState.update { it.copy(candidate = candidate) }
        checkCandidateDuplicate(candidate)
    }

    fun updateCandidateTitle(title: String) {
        _uiState.update { it.copy(candidate = it.candidate?.copy(title = title)) }
        _uiState.value.candidate?.let { checkCandidateDuplicate(it) }
    }

    fun updateCandidateAmount(amount: String) {
        _uiState.update { it.copy(candidate = it.candidate?.copy(amountStr = amount)) }
        _uiState.value.candidate?.let { checkCandidateDuplicate(it) }
    }

    fun updateCandidateCategory(categoryId: String) {
        _uiState.update { it.copy(candidate = it.candidate?.copy(categoryId = categoryId)) }
    }

    fun updateCandidatePaymentMethod(paymentMethodId: String) {
        _uiState.update { it.copy(candidate = it.candidate?.copy(paymentMethodId = paymentMethodId)) }
    }

    fun updateCandidateDate(date: LocalDate) {
        _uiState.update { it.copy(candidate = it.candidate?.copy(date = date)) }
        _uiState.value.candidate?.let { checkCandidateDuplicate(it) }
    }

    fun updateCandidateNotes(notes: String) {
        _uiState.update { it.copy(candidate = it.candidate?.copy(notes = notes)) }
    }

    fun createAndSelectCategory(categoryName: String) {
        val profileId = _uiState.value.activeProfileId ?: return
        viewModelScope.launch {
            val newCategory = Category(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                name = categoryName.trim(),
                iconName = "category",
                colorHex = "#5B7C99",
                isCustom = true,
                isDefault = false
            )
            categoryRepository.addCategory(newCategory)
            val updatedCategories = categoryRepository.getCategories(profileId).firstOrNull() ?: emptyList()
            _uiState.update {
                it.copy(
                    categories = updatedCategories,
                    candidate = it.candidate?.copy(
                        categoryId = newCategory.id,
                        suggestedNewCategoryName = null
                    )
                )
            }
        }
    }

    private fun checkCandidateDuplicate(candidate: EditableCandidate) {
        val profileId = _uiState.value.activeProfileId ?: return
        val bd = try { BigDecimal(candidate.amountStr) } catch (_: Exception) { null } ?: return
        if (bd <= BigDecimal.ZERO || candidate.title.isBlank()) return

        viewModelScope.launch {
            val warning = duplicateGuard.checkDuplicate(profileId, Money.of(bd, candidate.currency), candidate.title, candidate.date)
            _uiState.update {
                it.copy(candidate = it.candidate?.copy(duplicateWarning = warning))
            }
        }
    }

    fun saveCandidateExpense() {
        val state = _uiState.value
        val profileId = state.activeProfileId ?: return
        val candidate = state.candidate ?: return

        val amountBd = try { BigDecimal(candidate.amountStr) } catch (_: Exception) { null }
        if (amountBd == null || amountBd <= BigDecimal.ZERO) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid amount greater than 0") }
            return
        }

        if (candidate.title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a title") }
            return
        }

        if (candidate.categoryId.isBlank() || candidate.paymentMethodId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Category and Payment Method are required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            val now = Instant.now()
            val expense = Expense(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                title = candidate.title.trim(),
                money = Money.of(amountBd, candidate.currency),
                categoryId = candidate.categoryId,
                paymentMethodId = candidate.paymentMethodId,
                date = candidate.date,
                notes = candidate.notes.ifBlank { null },
                recurringFlag = false,
                source = candidate.source,
                createdAt = now,
                updatedAt = now
            )
            expenseRepository.addExpense(expense)
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    saveSuccess = true,
                    candidate = null,
                    inputText = ""
                )
            }
        }
    }

    fun resetSuccess() {
        _uiState.update { it.copy(saveSuccess = false, errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.reset()
    }
}
