package com.youtube.rating.android.utils

import com.youtube.rating.android.viewmodel.AppViewModel

class ContentLanguageDialogControllerImpl(
    private val appViewModel: AppViewModel
) : ContentLanguageDialogController {
    override fun openContentLanguageDialog() {
        appViewModel.setShowContentLanguageDialog(true)
    }
}
