package org.nekomanga.usecases.chapters

import org.nekomanga.data.database.repository.ChapterRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Holds the use cases for Chapter handling */
class ChapterUseCases(
    chapterRepository: ChapterRepository = Injekt.get(),
    calculateChapterFilter: CalculateChapterFilterUseCase = Injekt.get(),
) {
    val markChapters = MarkChapterUseCase(chapterRepository)
    val calculateChapterFilter = calculateChapterFilter
    val markPreviousChapters = MarkPreviousChaptersUseCase()
    val validateChapterNotBlocked = ValidateChapterNotBlockedUseCase()
}
