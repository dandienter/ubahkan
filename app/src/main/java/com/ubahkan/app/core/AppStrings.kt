package com.ubahkan.app.core

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.ubahkan.app.UbahkanPrefs

/*
 * Centralized Bilingual Localization (Indonesian & English)
 * Fully reactive for Jetpack Compose via CompositionLocalProvider
 * and accessible from non-UI components (Service, Engine, QueueManager).
 */

interface Strings {
    // Navigation & Common
    val tabHome: String
    val tabTools: String
    val tabHistory: String
    val tabSettings: String
    val pressBackAgainToExit: String
    val notifPermissionRequiredToast: String
    val webUrlNotSupportedToast: String
    val noFilesFromShareToast: String
    val fileNotFoundToast: String
    fun openWithTitle(ext: String): String
    fun noAppToOpenToast(ext: String): String
    val shareViaTitle: String
    val shareFailedToast: String
    val batterySettingsGuideToast: String
    val conversionStartedBackgroundToast: String
    val languageSetToast: String

    // Welcome Screen
    val welcomeTagline: String
    val welcomeFeatureOfflineTitle: String
    val welcomeFeatureOfflineDesc: String
    val welcomeFeatureFastTitle: String
    val welcomeFeatureFastDesc: String
    val welcomeFeaturePrivacyTitle: String
    val welcomeFeaturePrivacyDesc: String
    val btnGetStarted: String

    // Home Screen
    val notifBannerTitle: String
    val notifBannerDesc: String
    val btnAllow: String
    val batteryBannerTitle: String
    val batteryBannerDesc: String
    val btnLater: String
    val btnLearnHow: String
    val selectNewFile: String
    val selectFile: String
    val selectFileSubtitle: String
    val selectYourFile: String
    val tapToSelectOrShare: String
    val homeKicker: String
    val homeHeroTitle: String
    val homeHeroSubtitle: String
    val homeShareHint: String
    val homeCategoryHeader: String
    val homePopularHeader: String
    val homeHistoryHeader: String
    val homeSavedFooter: String
    fun homeFormatsCount(count: Int): String
    val splashTagline: String
    fun noFilesInCategory(category: String): String
    val noFilesInCategoryDesc: String
    val btnShowAll: String
    val deleteFileDialogTitle: String
    fun deleteFileDialogMessage(name: String): String
    val btnDelete: String
    val btnCancel: String
    val actionShare: String
    val menuAction: String
    val batteryDialogTitle: String
    val batteryDialogIntro: String
    val batteryStep1: String
    val batteryStep2: String
    val batteryStep3: String
    val batteryNotes: String
    val btnOpenAppInfo: String
    val btnClose: String

    // File Categories
    val categoryAll: String
    val categoryImage: String
    val categoryVideo: String
    val categoryAudio: String
    val categoryPdf: String
    val categoryEbook: String
    val categoryDocument: String
    val categorySpreadsheet: String
    val categoryArchive: String
    val categoryCode: String
    val categoryOther: String

    // Tools Screen
    val toolsTitle: String
    val toolsSubtitle: String
    val toolsConversionTitle: String
    val toolsConversionSubtitle: String
    val toolVideoTitle: String
    val toolVideoDesc: String
    val toolPdfTitle: String
    val toolPdfDesc: String
    val toolAudioTitle: String
    val toolAudioDesc: String
    val toolSvgTitle: String
    val toolSvgDesc: String
    val toolEpubTitle: String
    val toolEpubDesc: String
    val toolImageTitle: String
    val toolImageDesc: String
    val toolsOtherWaysTitle: String
    val toolsOtherWaysSubtitle: String
    val toolShareTitle: String
    val toolShareDesc: String
    val toolShareDetail: String
    val toolBatchTitle: String
    val toolBatchDesc: String
    val toolBatchDetail: String
    val toolOfflineTitle: String
    val toolOfflineDesc: String
    val toolOfflineDetail: String
    val toolFormatTitle: String
    val toolFormatDesc: String
    val toolFormatDetail: String
    val toolExpand: String
    val toolCollapse: String
    val toolConvertTitle: String
    val toolConvertDesc: String
    val toolsGroupMedia: String
    val toolsViewList: String
    val toolsViewGrid: String

    // History Screen (UI 1.4.0: kartu statistik + grup hari)
    val historyStatFiles: String
    val historyStatSaved: String
    val historyStatOffline: String
    val historyGroupToday: String
    val historyGroupYesterday: String
    val historyGroupOlder: String
    fun historySavedPercent(percent: Int): String
    fun historyConversionLabel(from: String, to: String): String

    // Settings Screen
    val settingsTitle: String
    val settingsSubtitle: String
    val settingsGroupAppearance: String
    val settingsGroupConversion: String
    val settingsGroupOther: String
    val themeSectionTitle: String
    val themeDefaultNote: String
    val themeLight: String
    val themeDark: String
    val themeSystem: String
    val languageSectionTitle: String
    val langIndonesian: String
    val langEnglish: String
    val qualitySectionTitle: String
    val qualitySectionDesc: String
    val qualityHigh: String
    val qualityMedium: String
    val qualityLow: String
    val qualityInstant: String
    val qualityDescHigh: String
    val qualityDescMedium: String
    val qualityDescLow: String
    val qualityDescInstant: String
    val behaviorSectionTitle: String
    val keepNameTitle: String
    val keepNameDesc: String
    val closeShareTitle: String
    val closeShareDesc: String
    val batteryOptTitle: String
    val batteryOptStatusOk: String
    val batteryOptStatusRestricted: String
    val aboutSectionTitle: String
    fun aboutVersion(version: String): String
    val aboutAuthor: String
    val updateCheckTitle: String
    fun updateAvailableTitle(version: String): String
    val updateAvailableDesc: String
    val updateNowButton: String
    val updateStatusNever: String
    val updateStatusChecking: String
    val updateStatusUpToDate: String
    val licensesSectionTitle: String
    val licensesSectionSubtitle: String
    val licensesDialogIntro: String

    // BottomSheetConverter
    val bottomSheetTitle: String
    fun bottomSheetBatchSubtitle(fileCount: Int, formatCount: Int, sizeStr: String): String
    fun bottomSheetSingleSubtitle(fileName: String, sizeStr: String): String
    val targetFormatHeader: String
    val chooseFormat: String
    val filesCountSuffix: String
    val mergeToPdfTitle: String
    val mergeToPdfDesc: String
    val qualityTitle: String
    fun instantLosslessSupported(src: String, tgt: String): String
    val instantNotAvailable: String
    val btnConvert: String
    fun btnConvertBatch(count: Int): String
    fun btnMergeAndConvert(count: Int): String
    fun formatExplain(format: String): String

    // Conversion Banner & Progress Screen
    fun convertingTo(format: String): String
    fun convertingBatchInfo(curr: Int, total: Int): String
    val conversionInProgress: String
    val cancelConversion: String
    val runningInBackground: String
    val conversionFailed: String
    val failedToConvertDefault: String
    val btnRetry: String
    val cancelDialogTitle: String
    val cancelDialogDesc: String
    val btnConfirmCancel: String
    val btnContinue: String
    val titleFinished: String
    val titleConversionProgress: String
    val btnMinimize: String
    val convertingFileProgress: String
    val progressPhrases: List<String>
    fun progressFromSize(size: String): String
    val progressQueueHeader: String
    fun progressBatchFilesCompleted(done: Int, total: Int): String
    val conversionSuccess: String
    val singleFileSuccessDesc: String
    fun batchFilesSuccessDesc(count: Int): String
    val btnViewResult: String
    val btnBackToHome: String
    val conversionPartialSuccess: String
    fun summaryPartial(completed: Int, failed: Int): String
    val defaultErrorMsg: String

    // Queue & Service & Engine (Non-UI)
    val queueWaiting: String
    val queueCompleted: String
    val queueCancelled: String
    val queueFailed: String
    val serviceRunningTitle: String
    val servicePreparingQueue: String
    val serviceStartingConversion: String
    fun notifConvertingBatchTitle(curr: Int, total: Int, name: String): String
    fun notifConvertingSingleTitle(name: String, format: String): String
    val notifActionCancelAll: String
    val notifSuccessTitle: String
    val notifPartialTitle: String
    val notifSuccessBodySingle: String
    fun notifSuccessBodyBatch(count: Int): String
    fun notifSummaryBody(completed: Int, failed: Int): String

    // Engine Step Messages & Errors
    val enginePreparingMedia: String
    fun engineProcessedPercent(percent: Int): String
    fun engineProcessedSeconds(seconds: Long): String
    val engineFinalizingValidating: String
    val engineFinished100: String
    val engineCancelled: String
    fun engineErrSetup(msg: String): String
    val engineErrReadInput: String
    val engineErrInputEmpty: String
    fun engineErrInvalidOutput(reason: String): String
    val engineErrSaveOutput: String
    val engineErrStreamCopyIncompatible: String
    val engineErrFormatNotSupported: String
    fun engineErrFFmpeg(code: Int): String
    val engineReadingPdf: String
    val engineErrPdfRead: String
    val engineErrPdfNoPages: String
    fun engineRenderingPage(page: Int, total: Int): String
    fun enginePagesExtractedSuccess(count: Int): String
    val engineErrSavePdfImage: String
    val engineAnalyzingPdf: String
    val engineErrPdfEmpty: String
    fun engineCompressingPage(page: Int, total: Int): String
    val engineErrSavePdfCompress: String
    val engineLoadingImage: String
    val engineBuildingPdfPages: String
    val engineErrImageToPdf: String
    fun engineBuildingPdfPage(curr: Int, total: Int): String
    val engineSavingPdfDoc: String
    val engineErrSaveMergedPdf: String
    fun engineErrMergePdf(msg: String): String
    val engineProcessingImage: String
    val engineCompressingImage: String
    val engineErrSaveImage: String
    val engineLoadingSvg: String
    val engineErrReadSvg: String
    val engineCalculatingSvgDim: String
    val engineRenderingSvg: String
    val engineCompressingSvgOut: String
    val engineErrSaveSvg: String
    val engineReadingEpub: String
    val engineErrReadEpub: String
    val engineSavingEpubTxt: String
    val engineErrSaveTxt: String

    // Tool Operations - Engine (Batch 1: kompres target, potong, potong otomatis)
    val engineCompressingVideoTarget: String
    fun engineCompressingImageAttempt(attempt: Int): String
    val engineTrimmingVideo: String
    val engineSplittingVideo: String
    val engineErrSplitNoParts: String
    val engineErrNoDuration: String
    val btnShareToWhatsAppStatus: String
    val shareToWhatsAppFailedToast: String

    // Tool Operations - UI (Batch 1)
    val toolsActionsTitle: String
    val toolsActionsSubtitle: String
    val toolCompressVideoTitle: String
    val toolCompressVideoDesc: String
    val toolCompressImageTitle: String
    val toolCompressImageDesc: String
    val toolTrimTitle: String
    val toolTrimDesc: String
    val toolSplitTitle: String
    val toolSplitDesc: String
    val toolPickHint: String
    val toolStartButton: String
    val toolTargetSizeLabel: String
    val toolCustomSizeMbLabel: String
    val toolCustomSizeKbLabel: String
    val toolTrimStartLabel: String
    val toolTrimEndLabel: String
    val toolTrimTimeHint: String
    fun toolVideoDurationInfo(duration: String): String
    val toolSplitEveryLabel: String
    fun toolSecondsUnit(seconds: Int): String
    val toolErrCustomSize: String
    val toolErrTimeFormat: String
    val toolErrTrimRange: String
    val toolErrNotVideo: String
    val toolErrNotImage: String

    // Tool Operations - Engine (Batch 2)
    val engineMergingVideos: String
    val engineSplittingPdf: String
    fun engineSplittingPdfPage(current: Int, total: Int): String
    val engineErrPdfRangeInvalid: String
    val engineMergingPdfs: String
    fun engineMergingPdfPage(current: Int, total: Int): String
    val engineBurningSubtitles: String
    val engineErrSubtitleMissing: String
    val engineCroppingVideo: String
    val engineResizingVideo: String
    val engineMutingVideo: String
    val engineReplacingAudio: String
    val engineErrAudioMissing: String
    val engineConvertingHeic: String
    val engineErrHeicUnsupported: String
    fun engineErrHeicDecode(msg: String): String
    val engineNormalizingVolume: String

    // Tool Operations - UI (Batch 2)
    val toolsActionsBatch2Title: String
    val toolsActionsBatch2Subtitle: String
    val toolMergeVideoTitle: String
    val toolMergeVideoDesc: String
    fun toolSelectedFilesInfo(count: Int): String
    val toolErrNeedTwoFiles: String
    val toolSplitPdfTitle: String
    val toolSplitPdfDesc: String
    val toolPdfRangeLabel: String
    val toolPdfRangeHint: String
    val toolErrNotPdf: String
    val toolMergePdfTitle: String
    val toolMergePdfDesc: String
    val toolHardsubTitle: String
    val toolHardsubDesc: String
    val toolSubtitleFileLabel: String
    val toolPickCompanionButton: String
    val toolErrCompanionMissing: String
    val toolErrSubtitleExt: String
    val toolCropTitle: String
    val toolCropDesc: String
    val toolCropRatioLabel: String
    val toolResizeTitle: String
    val toolResizeDesc: String
    val toolResizeLabel: String
    val toolMuteTitle: String
    val toolMuteDesc: String
    val toolReplaceAudioTitle: String
    val toolReplaceAudioDesc: String
    val toolAudioFileLabel: String
    val toolHeicTitle: String
    val toolHeicDesc: String
    val toolErrNotHeic: String
    val toolInfoTitle: String
    val toolInfoDesc: String
    val infoFormatLabel: String
    val infoDurationLabel: String
    val infoSizeLabel: String
    val infoResolutionLabel: String
    val infoVideoCodecLabel: String
    val infoAudioCodecLabel: String
    val infoBitrateLabel: String
    val infoLoading: String
    val infoFailed: String
    val toolNormalizeTitle: String
    val toolNormalizeDesc: String
    val toolErrNotMedia: String

    // Document Scanner (1.5.0)
    val scanTitle: String
    val scanDesc: String
    val scanCaptureHint: String
    val scanImportGallery: String
    val scanCameraDeniedTitle: String
    val scanCameraDeniedDesc: String
    val scanAllowCamera: String
    val scanFlash: String
    val scanTakePhoto: String
    val scanCancel: String
    val scanOpenCvFailed: String
    val scanCaptureFailed: String
    val scanAdjustTitle: String
    val scanAdjustHint: String
    val scanResetCorners: String
    val scanNext: String
    val scanFilterTitle: String
    val scanBrightnessLabel: String
    val scanContrastLabel: String
    val scanUsePage: String
    val scanProcessing: String
    val scanFilterOriginal: String
    val scanFilterGrayscale: String
    val scanFilterBlackWhite: String
    val scanFilterWhitePaper: String
    val scanFilterShadow: String
    val scanFilterSharpen: String
    val scanFilterContrastBoost: String
    val scanFilterColorPop: String
    val scanFilterSepia: String
    val scanFilterNegative: String
    val scanFilterBlueprint: String
    val scanFilterPhotocopy: String
    val scanPagesTitle: String
    fun scanPageNumber(index: Int): String
    val scanAddPage: String
    val scanReadText: String
    val scanSignature: String
    val scanSavePdf: String
    val scanNoPages: String
    val scanDeletePage: String
    val scanMoveUp: String
    val scanMoveDown: String
    val scanSelectPageHint: String
    val scanOcrTitle: String
    val scanOcrEmpty: String
    val scanOcrRunning: String
    val scanCopyText: String
    val scanCopiedToast: String
    val scanSignatureTitle: String
    val scanSignatureHint: String
    val scanSignatureClear: String
    val scanDoneButton: String
    val scanSignatureAppliedToast: String
    val scanSavedTitle: String
    fun scanSavedLocation(name: String): String
    val scanOpenFile: String
    val scanSaveFailedToast: String

    // Core Validators & Parsers
    val validatorFileNotFound: String
    val validatorNotRegularFile: String
    val validatorOutputEmpty: String
    val validatorCorruptImage: String
    val validatorEmptyTxt: String
    fun validatorUnreadableTxt(msg: String): String
    fun epubErrNotZip(msg: String): String
    val epubStepMetadata: String
    val epubErrNoContainer: String
    fun epubErrNoOpf(path: String): String
    val epubStepSpine: String
    val epubErrEmptySpine: String
    fun epubStepExtractingChapter(curr: Int, total: Int): String
    val epubErrNoChapterText: String
    val epubBookTitlePrefix: String
    val epubBookAuthorPrefix: String
}

object IndonesianStrings : Strings {
    override val tabHome = "Beranda"
    override val tabTools = "Alat"
    override val tabHistory = "Riwayat"
    override val tabSettings = "Pengaturan"
    override val pressBackAgainToExit = "Tekan sekali lagi untuk keluar"
    override val notifPermissionRequiredToast = "Izin notifikasi diperlukan agar konversi latar belakang berjalan optimal."
    override val webUrlNotSupportedToast = "Ubahkan adalah converter file offline. Format URL atau teks web tidak didukung."
    override val noFilesFromShareToast = "Tidak ada file yang dapat diproses dari Share."
    override val fileNotFoundToast = "File tidak ditemukan"
    override fun openWithTitle(ext: String) = "Buka $ext dengan..."
    override fun noAppToOpenToast(ext: String) = "Tidak ada aplikasi untuk membuka $ext"
    override val shareViaTitle = "Bagikan via..."
    override val shareFailedToast = "Gagal membagikan file"
    override val batterySettingsGuideToast = "Buka Setelan HP > Aplikasi > Ubahkan > Baterai"
    override val conversionStartedBackgroundToast = "Konversi dimulai di latar belakang..."
    override val languageSetToast = "Bahasa diatur ke Bahasa Indonesia"

    override val welcomeTagline = "Cepat · Mudah · Fleksibel"
    override val welcomeFeatureOfflineTitle = "100% Offline"
    override val welcomeFeatureOfflineDesc = "Semua proses berlangsung di perangkat tanpa internet atau cloud."
    override val welcomeFeatureFastTitle = "Konversi Cepat"
    override val welcomeFeatureFastDesc = "Mendukung stream copy instan untuk format yang kompatibel."
    override val welcomeFeaturePrivacyTitle = "Privasi Terjaga"
    override val welcomeFeaturePrivacyDesc = "File Anda tidak pernah meninggalkan ponsel Anda."
    override val btnGetStarted = "Mulai Mengonversi"

    override val notifBannerTitle = "Aktifkan Notifikasi"
    override val notifBannerDesc = "Agar konversi latar belakang tidak dihentikan sistem."
    override val btnAllow = "Izinkan"
    override val batteryBannerTitle = "Lepas Pembatasan Baterai"
    override val batteryBannerDesc = "Agar konversi panjang tidak dihentikan paksa saat layar mati."
    override val btnLater = "Nanti"
    override val btnLearnHow = "Lihat Cara"
    override val selectNewFile = "Pilih File Baru"
    override val selectFile = "Pilih File"
    override val selectFileSubtitle = "Pilih file dari perangkat untuk mulai mengonversi."
    override val selectYourFile = "Pilih File Anda"
    override val tapToSelectOrShare = "Ketuk untuk memilih file\natau gunakan menu Share"
    override val homeKicker = "100% OFFLINE"
    override val homeHeroTitle = "Konversi file,\ntanpa internet."
    override val homeHeroSubtitle = "Video, audio, gambar, dan dokumen diubah langsung di HP kamu. Tanpa unggah, tanpa akun."
    override val homeShareHint = "atau bagikan file dari galeri dan aplikasi lain"
    override val homeCategoryHeader = "Kategori"
    override val homePopularHeader = "Alat Populer"
    override val homeHistoryHeader = "Terakhir dikonversi"
    override val homeSavedFooter = "Hasil tersimpan di Downloads/Ubahkan"
    override fun homeFormatsCount(count: Int) = "$count format"
    override val splashTagline = "Konversi file, tanpa internet"
    override fun noFilesInCategory(category: String) = "Tidak ada file $category"
    override val noFilesInCategoryDesc = "Belum ada file dalam kategori ini. Coba kategori lain atau konversi file baru."
    override val btnShowAll = "Tampilkan Semua"
    override val deleteFileDialogTitle = "Hapus File"
    override fun deleteFileDialogMessage(name: String) = "Apakah Anda yakin ingin menghapus $name secara permanen?"
    override val btnDelete = "Hapus"
    override val btnCancel = "Batal"
    override val actionShare = "Bagikan"
    override val menuAction = "Menu Aksi"
    override val batteryDialogTitle = "Cara Menonaktifkan Pembatasan Baterai"
    override val batteryDialogIntro = "Sistem Android sering menghentikan proses latar belakang untuk menghemat daya. Ikuti langkah berikut agar konversi tetap berjalan lancar:"
    override val batteryStep1 = "1. Ketuk tombol \"Buka Info Aplikasi\" di bawah."
    override val batteryStep2 = "2. Pilih menu \"Baterai\" atau \"Penggunaan baterai aplikasi\"."
    override val batteryStep3 = "3. Ubah pengaturan menjadi \"Tidak dibatasi\" (Unrestricted)."
    override val batteryNotes = "Catatan per merek HP:\n• Xiaomi / Redmi / Poco: Pilih \"Penghemat baterai\" → \"Tidak ada pembatasan\".\n• Samsung: Pilih \"Baterai\" → \"Tidak dibatasi\".\n• Oppo / Realme / Vivo: Aktifkan \"Izinkan aktivitas latar belakang\"."
    override val btnOpenAppInfo = "Buka Info Aplikasi"
    override val btnClose = "Tutup"

    override val categoryAll = "Semua"
    override val categoryImage = "Gambar"
    override val categoryVideo = "Video"
    override val categoryAudio = "Audio"
    override val categoryPdf = "PDF"
    override val categoryEbook = "E-book"
    override val categoryDocument = "Dokumen"
    override val categorySpreadsheet = "Spreadsheet"
    override val categoryArchive = "Arsip"
    override val categoryCode = "Kode"
    override val categoryOther = "Lainnya"

    override val toolsTitle = "Alat"
    override val toolsSubtitle = "Kenali fitur dan kemampuan Ubahkan."
    override val toolsConversionTitle = "Alat Konversi"
    override val toolsConversionSubtitle = "Konversi file bawaan Ubahkan."
    override val toolVideoTitle = "Video Conversion"
    override val toolVideoDesc = "Mengubah container video dengan proses cepat tanpa encoding ulang. Ukuran hasil dapat sedikit berbeda."
    override val toolPdfTitle = "PDF Toolkit"
    override val toolPdfDesc = "Kompres dokumen PDF atau ubah setiap halaman menjadi gambar yang dapat dibuka dan dibagikan."
    override val toolAudioTitle = "Audio Extraction"
    override val toolAudioDesc = "Ekstrak jalur audio dari video dan simpan dalam berbagai format audio."
    override val toolSvgTitle = "SVG Conversion"
    override val toolSvgDesc = "Ubah grafik vektor SVG menjadi gambar raster yang dapat dibuka di galeri atau dibagikan."
    override val toolEpubTitle = "EPUB Text Extraction"
    override val toolEpubDesc = "Ekstrak isi dan bab buku digital menjadi satu file teks yang lebih ringan dan mudah dibaca."
    override val toolImageTitle = "Image Conversion"
    override val toolImageDesc = "Ubah format gambar dengan penanganan transparansi dan pilihan output yang beragam."
    override val toolsOtherWaysTitle = "Cara Lain Menggunakan Ubahkan"
    override val toolsOtherWaysSubtitle = "Gunakan Ubahkan langsung dari aplikasi lain."
    override val toolShareTitle = "Share to Ubahkan"
    override val toolShareDesc = "Kirim file langsung dari aplikasi lain ke Ubahkan melalui menu Share Android."
    override val toolShareDetail = "Pilih satu atau beberapa file dari Gallery, File Manager, atau aplikasi lain yang mendukung berbagi file. Ubahkan menerima file tersebut dan menyiapkan pilihan konversi tanpa perlu mencarinya kembali secara manual."
    override val toolBatchTitle = "Batch Conversion"
    override val toolBatchDesc = "Pilih beberapa file sekaligus dan proses semuanya dalam satu sesi konversi."
    override val toolBatchDetail = "Ubahkan dapat mengelompokkan file berdasarkan format sumber sehingga setiap kelompok dapat memiliki format tujuan dan pengaturan yang sesuai."
    override val toolOfflineTitle = "Offline Conversion"
    override val toolOfflineDesc = "Konversi file secara lokal di perangkat."
    override val toolOfflineDetail = "Untuk format yang didukung, proses konversi dilakukan tanpa perlu mengunggah file ke layanan cloud."
    override val toolFormatTitle = "Format Support"
    override val toolFormatDesc = "Lihat format sumber dan tujuan yang tersedia."
    override val toolFormatDetail = "Video: MP4, MKV, MOV, WEBM, AVI\nAudio: MP3, WAV, FLAC, AAC, M4A, OGG, OPUS\nGambar: PNG, JPG, WEBP, BMP, SVG\nDokumen: PDF, TXT, EPUB"
    override val toolExpand = "Buka"
    override val toolCollapse = "Tutup"
    override val toolConvertTitle = "Konversi Berkas"
    override val toolConvertDesc = "Ubah format semua jenis berkas."
    override val toolsGroupMedia = "Gambar dan Audio"
    override val toolsViewList = "Tampilan daftar"
    override val toolsViewGrid = "Tampilan kotak"

    override val historyStatFiles = "Berkas"
    override val historyStatSaved = "Dihemat"
    override val historyStatOffline = "Offline"
    override val historyGroupToday = "Hari Ini"
    override val historyGroupYesterday = "Kemarin"
    override val historyGroupOlder = "Lebih Lama"
    override fun historySavedPercent(percent: Int) = "hemat $percent%"
    override fun historyConversionLabel(from: String, to: String) = "$from ke $to"

    override val settingsTitle = "Pengaturan"
    override val settingsSubtitle = "Konfigurasi tampilan dan perilaku Ubahkan."
    override val settingsGroupAppearance = "Tampilan"
    override val settingsGroupConversion = "Konversi"
    override val settingsGroupOther = "Lainnya"
    override val themeSectionTitle = "Tema Aplikasi"
    override val themeDefaultNote = "Terang adalah bawaan Ubahkan"
    override val themeLight = "Terang"
    override val themeDark = "Gelap"
    override val themeSystem = "Sistem"
    override val languageSectionTitle = "Bahasa / Language"
    override val langIndonesian = "Bahasa Indonesia"
    override val langEnglish = "English"
    override val qualitySectionTitle = "Kualitas Konversi Default"
    override val qualitySectionDesc = "Dipakai saat panel konversi dibuka."
    override val qualityHigh = "Tinggi"
    override val qualityMedium = "Sedang"
    override val qualityLow = "Rendah"
    override val qualityInstant = "Instan"
    override val qualityDescHigh = "Tinggi: bitrate 100% dipertahankan tanpa kompresi tambahan."
    override val qualityDescMedium = "Sedang: kompresi seimbang, ukuran berkurang ~50%."
    override val qualityDescLow = "Rendah: kompresi agresif untuk hemat ruang."
    override val qualityDescInstant = "Instan: stream copy cepat tanpa re-encode (kualitas asli, hanya untuk format yang kompatibel)."
    override val behaviorSectionTitle = "Perilaku Aplikasi"
    override val keepNameTitle = "Pertahankan Nama Asli"
    override val keepNameDesc = "Tanpa imbuhan nama baru."
    override val closeShareTitle = "Tutup Otomatis (Share)"
    override val closeShareDesc = "Tutup panel usai memilih format."
    override val batteryOptTitle = "Baterai Latar Belakang"
    override val batteryOptStatusOk = "Status: Tidak dibatasi (Optimal)"
    override val batteryOptStatusRestricted = "Status: Dibatasi · Ketuk untuk melihat cara mengatur"
    override val aboutSectionTitle = "Tentang Ubahkan"
    override fun aboutVersion(version: String) = "Versi $version · Offline File Converter"
    override val aboutAuthor = "Oleh @dandisubhani_"
    override val updateCheckTitle = "Periksa Pembaruan"
    override fun updateAvailableTitle(version: String) = "Versi $version tersedia"
    override val updateAvailableDesc = "Unduh versi terbaru untuk fitur dan perbaikan paling baru."
    override val updateNowButton = "Perbarui"
    override val updateStatusNever = "Belum pernah diperiksa"
    override val updateStatusChecking = "Sedang memeriksa"
    override val updateStatusUpToDate = "Sudah versi terbaru"
    override val licensesSectionTitle = "Lisensi Open Source"
    override val licensesSectionSubtitle = "GPL-3.0, Apache 2.0 & pustaka pihak ketiga"
    override val licensesDialogIntro = "Ubahkan didistribusikan di bawah GNU General Public License v3.0 (GPL-3.0) dan menggunakan pustaka open-source berikut:"

    override val bottomSheetTitle = "Konversi File"
    override fun bottomSheetBatchSubtitle(fileCount: Int, formatCount: Int, sizeStr: String) =
        "$fileCount file terpilih • $formatCount format$sizeStr"
    override fun bottomSheetSingleSubtitle(fileName: String, sizeStr: String) = "$fileName$sizeStr"
    override val targetFormatHeader = "Format Tujuan"
    override val chooseFormat = "PILIH"
    override val filesCountSuffix = " terpilih"
    override val mergeToPdfTitle = "Gabung menjadi satu PDF"
    override val mergeToPdfDesc = "Jika ada banyak file gambar."
    override val qualityTitle = "Kualitas"
    override fun instantLosslessSupported(src: String, tgt: String) =
        "Lossless (Instan) didukung untuk ${src.uppercase()} → ${tgt.uppercase()}"
    override val instantNotAvailable = "Instant tidak tersedia untuk format ini. Ubahkan menggunakan kualitas tertinggi."
    override val btnConvert = "Konversi"
    override fun btnConvertBatch(count: Int) = "Konversi ($count File)"
    override fun btnMergeAndConvert(count: Int) = "Gabung & Konversi ($count File)"
    override fun formatExplain(format: String): String = when (format.lowercase()) {
        "mp3" -> "Audio saja dari berkas sumber, ukuran kecil dan bisa diputar di semua perangkat."
        "m4a" -> "Audio ringkas berkualitas baik, umum dipakai di perangkat Apple."
        "wav" -> "Audio tanpa kompres, kualitas penuh tetapi ukuran berkas besar."
        "flac" -> "Audio lossless, kualitas sama persis seperti sumbernya."
        "ogg", "opus" -> "Audio ringkas berformat terbuka, hemat ukuran."
        "mp4" -> "Format video paling umum, cocok dibagikan ke mana saja."
        "mkv" -> "Wadah video fleksibel, cocok untuk kualitas tinggi dan subtitle."
        "mov" -> "Format video khas Apple, umum dari iPhone dan Mac."
        "webm" -> "Video ringkas untuk web, ukuran kecil."
        "gif" -> "Gambar bergerak tanpa suara, pas untuk cuplikan singkat."
        "jpg", "jpeg" -> "Format foto paling umum, ukuran kecil dan didukung semua aplikasi."
        "png" -> "Gambar tajam dengan dukungan latar transparan."
        "webp" -> "Format gambar modern, lebih kecil dari JPG pada kualitas setara."
        "bmp" -> "Gambar tanpa kompres, kualitas penuh tetapi ukuran besar."
        "pdf" -> "Dokumen yang tampilannya tetap sama di semua perangkat."
        "txt" -> "Teks polos tanpa format, bisa dibuka di aplikasi apa saja."
        else -> ""
    }

    override fun convertingTo(format: String) = "Mengonversi ke ${format.uppercase()}"
    override fun convertingBatchInfo(curr: Int, total: Int) = "Mengonversi ($curr/$total)"
    override val conversionInProgress = "Proses konversi sedang berjalan"
    override val cancelConversion = "Batalkan Konversi"
    override val runningInBackground = "Berjalan di latar belakang"
    override val conversionFailed = "Konversi Gagal"
    override val failedToConvertDefault = "Gagal dikonversi"
    override val btnRetry = "Coba Lagi"
    override val cancelDialogTitle = "Batalkan Konversi?"
    override val cancelDialogDesc = "Proses konversi yang sedang berjalan akan dihentikan."
    override val btnConfirmCancel = "Batalkan"
    override val btnContinue = "Lanjutkan"
    override val titleFinished = "Selesai"
    override val titleConversionProgress = "Progres Konversi"
    override val btnMinimize = "Minimalkan"
    override val convertingFileProgress = "Mengonversi File..."
    override val progressPhrases = listOf(
        "Lagi dimasak, sabar ya...",
        "Sedikit lagi, jangan ditinggal tidur...",
        "File-mu lagi dirapikan...",
        "Sabar, hasilnya sebentar lagi...",
        "Lagi dipoles biar makin kinclong...",
        "Tenang, semua dikerjakan di HP kamu...",
        "Nyeduh kopi dulu juga boleh...",
        "Mesin lagi ngebut, tunggu sebentar..."
    )
    override fun progressFromSize(size: String) = "dari $size"
    override val progressQueueHeader = "Antrean"
    override fun progressBatchFilesCompleted(done: Int, total: Int) = "$done dari $total file selesai"
    override val conversionSuccess = "Konversi Berhasil!"
    override val singleFileSuccessDesc = "File Anda telah berhasil dikonversi."
    override fun batchFilesSuccessDesc(count: Int) = "$count file berhasil dikonversi."
    override val btnViewResult = "Lihat Hasil"
    override val btnBackToHome = "Kembali ke Beranda"
    override val conversionPartialSuccess = "Konversi Selesai Sebagian"
    override fun summaryPartial(completed: Int, failed: Int) = "$completed berhasil, $failed gagal diproses."
    override val defaultErrorMsg = "Terjadi kesalahan saat memproses file."

    override val queueWaiting = "Menunggu antrean..."
    override val queueCompleted = "Konversi Selesai"
    override val queueCancelled = "Dibatalkan"
    override val queueFailed = "Konversi Gagal"
    override val serviceRunningTitle = "Ubahkan Berjalan"
    override val servicePreparingQueue = "Menyiapkan antrean konversi..."
    override val serviceStartingConversion = "Memulai konversi..."
    override fun notifConvertingBatchTitle(curr: Int, total: Int, name: String) =
        "Mengonversi ($curr/$total): $name"
    override fun notifConvertingSingleTitle(name: String, format: String) =
        "Mengonversi $name → ${format.uppercase()}"
    override val notifActionCancelAll = "Batal Semua"
    override val notifSuccessTitle = "✅ Konversi Selesai"
    override val notifPartialTitle = "⚠️ Konversi Selesai Sebagian"
    override val notifSuccessBodySingle = "1 berkas berhasil dikonversi ke Downloads/Ubahkan"
    override fun notifSuccessBodyBatch(count: Int) = "$count berkas berhasil dikonversi ke Downloads/Ubahkan"
    override fun notifSummaryBody(completed: Int, failed: Int) = "$completed berhasil, $failed gagal diproses."

    override val enginePreparingMedia = "Menyiapkan file multimedia..."
    override fun engineProcessedPercent(percent: Int) = "Telah diproses: $percent%"
    override fun engineProcessedSeconds(seconds: Long) = "Telah diproses: ${seconds}s"
    override val engineFinalizingValidating = "Menyelesaikan dan memvalidasi berkas..."
    override val engineFinished100 = "Selesai 100%"
    override val engineCancelled = "Konversi dibatalkan."
    override fun engineErrSetup(msg: String) = "Gagal setup konversi: $msg"
    override val engineErrReadInput = "Tidak dapat membaca berkas input."
    override val engineErrInputEmpty = "Berkas input kosong atau tidak valid."
    override fun engineErrInvalidOutput(reason: String) = "Hasil tidak valid: $reason"
    override val engineErrSaveOutput = "Gagal menyimpan hasil ke direktori Downloads/Ubahkan"
    override val engineErrStreamCopyIncompatible =
        "Stream Copy tidak kompatibel untuk codec stream berkas ini. Gunakan kualitas Tinggi atau Sedang."
    override val engineErrFormatNotSupported =
        "Format tujuan tidak mendukung salah satu stream media dari berkas sumber."
    override fun engineErrFFmpeg(code: Int) = "Gagal Konversi FFmpeg (Kode: $code)"
    override val engineReadingPdf = "Membaca dokumen PDF..."
    override val engineErrPdfRead = "Tidak dapat membaca dokumen PDF."
    override val engineErrPdfNoPages = "Dokumen PDF tidak memiliki halaman."
    override fun engineRenderingPage(page: Int, total: Int) = "Merender halaman $page dari $total..."
    override fun enginePagesExtractedSuccess(count: Int) = "$count halaman berhasil diekstrak ke Downloads/Ubahkan"
    override val engineErrSavePdfImage = "Gagal menyimpan hasil gambar PDF"
    override val engineAnalyzingPdf = "Menganalisis ukuran PDF..."
    override val engineErrPdfEmpty = "Dokumen PDF kosong."
    override fun engineCompressingPage(page: Int, total: Int) = "Mengompres halaman $page dari $total..."
    override val engineErrSavePdfCompress = "Gagal menyimpan hasil kompresi PDF"
    override val engineLoadingImage = "Memuat gambar..."
    override val engineBuildingPdfPages = "Menyusun lembar PDF..."
    override val engineErrImageToPdf = "Gagal membuat PDF dari gambar"
    override fun engineBuildingPdfPage(curr: Int, total: Int) = "Menyusun lembar $curr dari $total..."
    override val engineSavingPdfDoc = "Menyimpan dokumen PDF..."
    override val engineErrSaveMergedPdf = "Gagal menyimpan dokumen PDF gabungan"
    override fun engineErrMergePdf(msg: String) = "Gagal menggabungkan gambar ke PDF: $msg"
    override val engineProcessingImage = "Memproses gambar..."
    override val engineCompressingImage = "Mengompresi gambar..."
    override val engineErrSaveImage = "Gagal menyimpan gambar hasil"
    override val engineLoadingSvg = "Memuat file SVG..."
    override val engineErrReadSvg = "Tidak dapat membaca data dari berkas SVG."
    override val engineCalculatingSvgDim = "Menghitung dimensi vektor SVG..."
    override val engineRenderingSvg = "Merender vektor SVG ke kanvas..."
    override val engineCompressingSvgOut = "Mengompresi gambar output..."
    override val engineErrSaveSvg = "Gagal menyimpan gambar SVG"
    override val engineReadingEpub = "Membaca berkas EPUB..."
    override val engineErrReadEpub = "Tidak dapat membaca berkas EPUB."
    override val engineSavingEpubTxt = "Menyimpan teks hasil ekstraksi..."
    override val engineErrSaveTxt = "Gagal menyimpan file TXT"

    override val engineCompressingVideoTarget = "Mengompres video ke ukuran target..."
    override fun engineCompressingImageAttempt(attempt: Int) = "Mencoba kualitas gambar (percobaan $attempt)..."
    override val engineTrimmingVideo = "Memotong video..."
    override val engineSplittingVideo = "Membagi video menjadi potongan..."
    override val engineErrSplitNoParts = "Tidak ada potongan yang dihasilkan."
    override val engineErrNoDuration = "Tidak dapat membaca durasi media."
    override val btnShareToWhatsAppStatus = "Bagikan ke Status WA"
    override val shareToWhatsAppFailedToast = "Tidak dapat membuka berbagi ke WhatsApp."

    override val toolsActionsTitle = "Alat Ukuran dan Potong"
    override val toolsActionsSubtitle = "Kompres ke target ukuran, potong, dan bagi video."
    override val toolCompressVideoTitle = "Kompres Video ke Target"
    override val toolCompressVideoDesc = "Kompres ke ukuran target."
    override val toolCompressImageTitle = "Kompres Gambar ke Target"
    override val toolCompressImageDesc = "Kompres JPG ke ukuran kecil."
    override val toolTrimTitle = "Potong Video"
    override val toolTrimDesc = "Potong dari waktu ke waktu."
    override val toolSplitTitle = "Potong Otomatis"
    override val toolSplitDesc = "Bagi rata buat Status WA."
    override val toolPickHint = "Ketuk untuk memilih file"
    override val toolStartButton = "Mulai"
    override val toolTargetSizeLabel = "Ukuran target"
    override val toolCustomSizeMbLabel = "Ukuran lain (MB)"
    override val toolCustomSizeKbLabel = "Ukuran lain (KB)"
    override val toolTrimStartLabel = "Mulai"
    override val toolTrimEndLabel = "Selesai"
    override val toolTrimTimeHint = "Format mm:ss atau detik, contoh 1:30"
    override fun toolVideoDurationInfo(duration: String) = "Durasi video: $duration"
    override val toolSplitEveryLabel = "Potong setiap"
    override fun toolSecondsUnit(seconds: Int) = "$seconds detik"
    override val toolErrCustomSize = "Isi ukuran target yang valid, lebih besar dari 0."
    override val toolErrTimeFormat = "Format waktu tidak valid. Pakai mm:ss atau detik."
    override val toolErrTrimRange = "Waktu selesai harus sesudah waktu mulai dan tidak melewati durasi video."
    override val toolErrNotVideo = "File terpilih bukan video."
    override val toolErrNotImage = "File terpilih bukan gambar."

    override val engineMergingVideos = "Menggabungkan video..."
    override val toolsActionsBatch2Title = "Alat Video, PDF, dan Audio"
    override val toolsActionsBatch2Subtitle = "Gabung, pisah, subtitle, crop, dan lainnya."
    override val toolMergeVideoTitle = "Gabung Video"
    override val toolMergeVideoDesc = "Sambung klip jadi satu berkas."
    override fun toolSelectedFilesInfo(count: Int) = "$count berkas terpilih, urutan sesuai pilihan."
    override val toolErrNeedTwoFiles = "Pilih minimal dua berkas untuk digabung."
    override val engineSplittingPdf = "Memisahkan halaman PDF..."
    override fun engineSplittingPdfPage(current: Int, total: Int) = "Memisahkan halaman $current dari $total..."
    override val engineErrPdfRangeInvalid = "Rentang halaman tidak valid."
    override val toolSplitPdfTitle = "Pisah PDF"
    override val toolSplitPdfDesc = "Satu halaman, satu berkas."
    override val toolPdfRangeLabel = "Rentang halaman"
    override val toolPdfRangeHint = "Kosongkan untuk semua halaman, atau isi misalnya 1-3 atau 1,3,5."
    override val toolErrNotPdf = "File terpilih bukan PDF."
    override val engineMergingPdfs = "Menggabungkan PDF..."
    override fun engineMergingPdfPage(current: Int, total: Int) = "Menggabungkan halaman $current dari $total..."
    override val toolMergePdfTitle = "Gabung PDF"
    override val toolMergePdfDesc = "Sambung PDF jadi satu berkas."
    override val engineBurningSubtitles = "Membakar subtitle ke video..."
    override val engineErrSubtitleMissing = "Berkas subtitle tidak ditemukan."
    override val toolHardsubTitle = "Bakar Subtitle (Hardsub)"
    override val toolHardsubDesc = "Tempel .srt permanen ke video."
    override val toolSubtitleFileLabel = "Berkas subtitle (.srt)"
    override val toolPickCompanionButton = "Pilih berkas"
    override val toolErrCompanionMissing = "Pilih berkas pendamping dulu."
    override val toolErrSubtitleExt = "Berkas subtitle harus berekstensi .srt."
    override val engineCroppingVideo = "Memotong bingkai video..."
    override val toolCropTitle = "Crop Rasio Video"
    override val toolCropDesc = "Rasio 9:16, 1:1, atau 16:9."
    override val toolCropRatioLabel = "Rasio bingkai"
    override val engineResizingVideo = "Mengubah resolusi video..."
    override val toolResizeTitle = "Ubah Resolusi Video"
    override val toolResizeDesc = "Turunkan ke 720p, 480p, 360p."
    override val toolResizeLabel = "Resolusi target"
    override val engineMutingVideo = "Membisukan video..."
    override val toolMuteTitle = "Bisukan Video"
    override val toolMuteDesc = "Hapus jalur audio dari video."
    override val engineReplacingAudio = "Mengganti audio video..."
    override val engineErrAudioMissing = "Berkas audio pengganti tidak ditemukan."
    override val toolReplaceAudioTitle = "Ganti Audio Video"
    override val toolReplaceAudioDesc = "Ganti audio pada video."
    override val toolAudioFileLabel = "Berkas audio pengganti"
    override val engineConvertingHeic = "Mengubah HEIC menjadi JPG..."
    override val engineErrHeicUnsupported = "Konversi HEIC membutuhkan Android 9 atau lebih baru."
    override fun engineErrHeicDecode(msg: String) = "Gagal membaca berkas HEIC: $msg"
    override val toolHeicTitle = "HEIC ke JPG"
    override val toolHeicDesc = "Foto HEIC jadi JPG."
    override val toolErrNotHeic = "File terpilih bukan HEIC atau HEIF."
    override val toolInfoTitle = "Info Berkas"
    override val toolInfoDesc = "Lihat detail berkas."
    override val infoFormatLabel = "Format"
    override val infoDurationLabel = "Durasi"
    override val infoSizeLabel = "Ukuran"
    override val infoResolutionLabel = "Resolusi"
    override val infoVideoCodecLabel = "Codec video"
    override val infoAudioCodecLabel = "Codec audio"
    override val infoBitrateLabel = "Bitrate"
    override val infoLoading = "Membaca info berkas..."
    override val infoFailed = "Tidak dapat membaca info berkas ini."
    override val engineNormalizingVolume = "Menormalkan volume..."
    override val toolNormalizeTitle = "Normalisasi Volume"
    override val toolNormalizeDesc = "Samakan keras suara."
    override val toolErrNotMedia = "File terpilih bukan video atau audio."

    override val validatorFileNotFound = "Berkas output tidak ditemukan di sistem penyimpanan."
    override val validatorNotRegularFile = "Path output bukan merupakan berkas reguler."
    override val validatorOutputEmpty = "Berkas output kosong (ukuran 0 byte)."
    override val validatorCorruptImage = "Format gambar output rusak atau tidak dapat diuraikan."
    override val validatorEmptyTxt = "Berkas teks hasil konversi kosong."
    override fun validatorUnreadableTxt(msg: String) = "Berkas teks tidak dapat dibaca: $msg"
    override fun epubErrNotZip(msg: String) = "Berkas bukan kontainer ZIP/EPUB yang valid: $msg"
    override val epubStepMetadata = "Mencari berkas metadata EPUB..."
    override val epubErrNoContainer = "META-INF/container.xml atau rootfile OPF tidak ditemukan di dalam berkas EPUB."
    override fun epubErrNoOpf(path: String) = "Berkas paket OPF ($path) tidak ditemukan di dalam kontainer EPUB."
    override val epubStepSpine = "Membaca struktur dan urutan membaca (Spine)..."
    override val epubErrEmptySpine = "OPF Spine tidak memiliki itemref urutan membaca yang valid."
    override fun epubStepExtractingChapter(curr: Int, total: Int) = "Mengekstrak bab $curr dari $total..."
    override val epubErrNoChapterText = "Tidak ditemukan teks bab yang dapat dibaca dalam urutan Spine EPUB."
    override val epubBookTitlePrefix = "JUDUL: "
    override val epubBookAuthorPrefix = "PENULIS: "

    // Document Scanner (1.5.0)
    override val scanTitle = "Pindai Dokumen"
    override val scanDesc = "Foto dokumen jadi PDF rapi."
    override val scanCaptureHint = "Posisikan dokumen di dalam bingkai."
    override val scanImportGallery = "Impor dari Galeri"
    override val scanCameraDeniedTitle = "Izin kamera ditolak"
    override val scanCameraDeniedDesc = "Tanpa izin kamera, kamu tetap bisa mengimpor foto dari galeri."
    override val scanAllowCamera = "Izinkan Kamera"
    override val scanFlash = "Lampu Kilat"
    override val scanTakePhoto = "Jepret Foto"
    override val scanCancel = "Batalkan pindaian"
    override val scanOpenCvFailed = "Mesin pemindai gagal dimulai. Coba impor dari galeri."
    override val scanCaptureFailed = "Gagal mengambil foto. Coba lagi."
    override val scanAdjustTitle = "Sesuaikan Sudut"
    override val scanAdjustHint = "Geser titik ke sudut dokumen."
    override val scanResetCorners = "Atur Ulang"
    override val scanNext = "Lanjut"
    override val scanFilterTitle = "Filter Dokumen"
    override val scanBrightnessLabel = "Kecerahan"
    override val scanContrastLabel = "Kontras"
    override val scanUsePage = "Pakai Halaman Ini"
    override val scanProcessing = "Memproses..."
    override val scanFilterOriginal = "Asli"
    override val scanFilterGrayscale = "Abu-abu"
    override val scanFilterBlackWhite = "Hitam Putih"
    override val scanFilterWhitePaper = "Kertas Putih"
    override val scanFilterShadow = "Hilang Bayangan"
    override val scanFilterSharpen = "Pertajam Teks"
    override val scanFilterContrastBoost = "Kontras Kuat"
    override val scanFilterColorPop = "Warna Pop"
    override val scanFilterSepia = "Sepia"
    override val scanFilterNegative = "Negatif"
    override val scanFilterBlueprint = "Blueprint"
    override val scanFilterPhotocopy = "Fotokopi"
    override val scanPagesTitle = "Halaman Pindai"
    override fun scanPageNumber(index: Int) = "Halaman $index"
    override val scanAddPage = "Tambah Halaman"
    override val scanReadText = "Baca Teks"
    override val scanSignature = "Tanda Tangan"
    override val scanSavePdf = "Simpan PDF"
    override val scanNoPages = "Belum ada halaman. Tambahkan halaman pertama."
    override val scanDeletePage = "Hapus halaman"
    override val scanMoveUp = "Geser ke atas"
    override val scanMoveDown = "Geser ke bawah"
    override val scanSelectPageHint = "Ketuk halaman untuk memilihnya sebelum Baca Teks atau Tanda Tangan."
    override val scanOcrTitle = "Hasil Baca Teks"
    override val scanOcrEmpty = "Tidak ada teks yang terbaca."
    override val scanOcrRunning = "Membaca teks..."
    override val scanCopyText = "Salin Teks"
    override val scanCopiedToast = "Teks disalin."
    override val scanSignatureTitle = "Gambar Tanda Tangan"
    override val scanSignatureHint = "Gambar tanda tanganmu di area ini."
    override val scanSignatureClear = "Hapus Gambar"
    override val scanDoneButton = "Selesai"
    override val scanSignatureAppliedToast = "Tanda tangan ditempel."
    override val scanSavedTitle = "PDF Tersimpan"
    override fun scanSavedLocation(name: String) = "Tersimpan sebagai $name di Downloads/Ubahkan."
    override val scanOpenFile = "Buka"
    override val scanSaveFailedToast = "Gagal menyimpan PDF. Coba lagi."
}

object EnglishStrings : Strings {
    override val tabHome = "Home"
    override val tabTools = "Tools"
    override val tabHistory = "History"
    override val tabSettings = "Settings"
    override val pressBackAgainToExit = "Press back again to exit"
    override val notifPermissionRequiredToast = "Notification permission is required for optimal background conversions."
    override val webUrlNotSupportedToast = "Ubahkan is an offline file converter. Web URLs or plain text are not supported."
    override val noFilesFromShareToast = "No files found to process from Share."
    override val fileNotFoundToast = "File not found"
    override fun openWithTitle(ext: String) = "Open $ext with..."
    override fun noAppToOpenToast(ext: String) = "No app found to open $ext"
    override val shareViaTitle = "Share via..."
    override val shareFailedToast = "Failed to share file"
    override val batterySettingsGuideToast = "Open Settings > Apps > Ubahkan > Battery"
    override val conversionStartedBackgroundToast = "Conversion started in background..."
    override val languageSetToast = "Language set to English"

    override val welcomeTagline = "Fast · Simple · Flexible"
    override val welcomeFeatureOfflineTitle = "100% Offline"
    override val welcomeFeatureOfflineDesc = "All processing happens on device without internet or cloud."
    override val welcomeFeatureFastTitle = "Fast Conversion"
    override val welcomeFeatureFastDesc = "Supports instant stream copy for compatible formats."
    override val welcomeFeaturePrivacyTitle = "Privacy Protected"
    override val welcomeFeaturePrivacyDesc = "Your files never leave your device."
    override val btnGetStarted = "Start Converting"

    override val notifBannerTitle = "Enable Notifications"
    override val notifBannerDesc = "Keeps background conversions running reliably."
    override val btnAllow = "Allow"
    override val batteryBannerTitle = "Disable Battery Restrictions"
    override val batteryBannerDesc = "So long conversions are not stopped when the screen turns off."
    override val btnLater = "Later"
    override val btnLearnHow = "Learn How"
    override val selectNewFile = "Select New File"
    override val selectFile = "Select File"
    override val selectFileSubtitle = "Choose a file from your device to start converting."
    override val selectYourFile = "Select Your File"
    override val tapToSelectOrShare = "Tap to select a file\nor use the Share menu"
    override val homeKicker = "100% OFFLINE"
    override val homeHeroTitle = "Convert files,\nno internet needed."
    override val homeHeroSubtitle = "Video, audio, images, and documents are converted right on your phone. No uploads, no account."
    override val homeShareHint = "or share files from your gallery and other apps"
    override val homeCategoryHeader = "Categories"
    override val homePopularHeader = "Popular Tools"
    override val homeHistoryHeader = "Recently converted"
    override val homeSavedFooter = "Results are saved to Downloads/Ubahkan"
    override fun homeFormatsCount(count: Int) = if (count == 1) "1 format" else "$count formats"
    override val splashTagline = "File conversion, no internet"
    override fun noFilesInCategory(category: String) = "No $category files"
    override val noFilesInCategoryDesc = "No files found in this category. Try another category or convert a new file."
    override val btnShowAll = "Show All"
    override val deleteFileDialogTitle = "Delete File"
    override fun deleteFileDialogMessage(name: String) = "Are you sure you want to permanently delete $name?"
    override val btnDelete = "Delete"
    override val btnCancel = "Cancel"
    override val actionShare = "Share"
    override val menuAction = "Action Menu"
    override val batteryDialogTitle = "How to Disable Battery Restrictions"
    override val batteryDialogIntro = "Android often terminates background tasks to save power. Follow these steps so conversions run uninterrupted:"
    override val batteryStep1 = "1. Tap the \"Open App Info\" button below."
    override val batteryStep2 = "2. Select \"Battery\" or \"App battery usage\"."
    override val batteryStep3 = "3. Set the option to \"Unrestricted\"."
    override val batteryNotes = "Device-specific notes:\n• Xiaomi / Redmi / Poco: Select \"Battery saver\" → \"No restrictions\".\n• Samsung: Select \"Battery\" → \"Unrestricted\".\n• Oppo / Realme / Vivo: Enable \"Allow background activity\"."
    override val btnOpenAppInfo = "Open App Info"
    override val btnClose = "Close"

    override val categoryAll = "All"
    override val categoryImage = "Images"
    override val categoryVideo = "Video"
    override val categoryAudio = "Audio"
    override val categoryPdf = "PDF"
    override val categoryEbook = "E-book"
    override val categoryDocument = "Documents"
    override val categorySpreadsheet = "Spreadsheets"
    override val categoryArchive = "Archives"
    override val categoryCode = "Code"
    override val categoryOther = "Others"

    override val toolsTitle = "Tools"
    override val toolsSubtitle = "Discover Ubahkan features and capabilities."
    override val toolsConversionTitle = "Conversion Tools"
    override val toolsConversionSubtitle = "Ubahkan built-in conversion tools."
    override val toolVideoTitle = "Video Conversion"
    override val toolVideoDesc = "Quickly re-container videos without re-encoding. Output file size may vary slightly."
    override val toolPdfTitle = "PDF Toolkit"
    override val toolPdfDesc = "Compress PDF documents or extract each page into shareable images."
    override val toolAudioTitle = "Audio Extraction"
    override val toolAudioDesc = "Extract audio tracks from video files and save them in various audio formats."
    override val toolSvgTitle = "SVG Conversion"
    override val toolSvgDesc = "Convert SVG vector graphics into raster images for gallery viewing and sharing."
    override val toolEpubTitle = "EPUB Text Extraction"
    override val toolEpubDesc = "Extract digital book chapters into a clean, lightweight text file."
    override val toolImageTitle = "Image Conversion"
    override val toolImageDesc = "Convert image formats with transparency handling and multiple output options."
    override val toolsOtherWaysTitle = "More Ways to Use Ubahkan"
    override val toolsOtherWaysSubtitle = "Use Ubahkan directly from other apps."
    override val toolShareTitle = "Share to Ubahkan"
    override val toolShareDesc = "Send files directly from other apps to Ubahkan via Android's Share menu."
    override val toolShareDetail = "Select one or multiple files from Gallery, File Manager, or other apps supporting file sharing. Ubahkan receives them and prepares conversion options automatically."
    override val toolBatchTitle = "Batch Conversion"
    override val toolBatchDesc = "Select multiple files at once and convert them all in a single session."
    override val toolBatchDetail = "Ubahkan groups files by source format so each group can have its own target format and quality settings."
    override val toolOfflineTitle = "Offline Conversion"
    override val toolOfflineDesc = "Convert files locally on your device."
    override val toolOfflineDetail = "All supported conversions are processed on-device without uploading files to cloud services."
    override val toolFormatTitle = "Format Support"
    override val toolFormatDesc = "View available source and target formats."
    override val toolFormatDetail = "Video: MP4, MKV, MOV, WEBM, AVI\nAudio: MP3, WAV, FLAC, AAC, M4A, OGG, OPUS\nImages: PNG, JPG, WEBP, BMP, SVG\nDocuments: PDF, TXT, EPUB"
    override val toolExpand = "Expand"
    override val toolCollapse = "Collapse"
    override val toolConvertTitle = "Convert File"
    override val toolConvertDesc = "Change any file format."
    override val toolsGroupMedia = "Image and Audio"
    override val toolsViewList = "List view"
    override val toolsViewGrid = "Grid view"

    override val historyStatFiles = "Files"
    override val historyStatSaved = "Saved"
    override val historyStatOffline = "Offline"
    override val historyGroupToday = "Today"
    override val historyGroupYesterday = "Yesterday"
    override val historyGroupOlder = "Older"
    override fun historySavedPercent(percent: Int) = "saved $percent%"
    override fun historyConversionLabel(from: String, to: String) = "$from to $to"

    override val settingsTitle = "Settings"
    override val settingsSubtitle = "Configure Ubahkan display and behavior."
    override val settingsGroupAppearance = "Appearance"
    override val settingsGroupConversion = "Conversion"
    override val settingsGroupOther = "Other"
    override val themeSectionTitle = "App Theme"
    override val themeDefaultNote = "Light is the Ubahkan default"
    override val themeLight = "Light"
    override val themeDark = "Dark"
    override val themeSystem = "System"
    override val languageSectionTitle = "Language / Bahasa"
    override val langIndonesian = "Bahasa Indonesia"
    override val langEnglish = "English"
    override val qualitySectionTitle = "Default Conversion Quality"
    override val qualitySectionDesc = "Used when the conversion sheet opens."
    override val qualityHigh = "High"
    override val qualityMedium = "Medium"
    override val qualityLow = "Low"
    override val qualityInstant = "Instant"
    override val qualityDescHigh = "High: 100% bitrate preserved without additional compression."
    override val qualityDescMedium = "Medium: balanced compression, ~50% size reduction."
    override val qualityDescLow = "Low: aggressive compression to save space."
    override val qualityDescInstant = "Instant: fast stream copy without re-encoding (original quality, compatible formats only)."
    override val behaviorSectionTitle = "App Behavior"
    override val keepNameTitle = "Keep Original Filename"
    override val keepNameDesc = "No new name suffix added."
    override val closeShareTitle = "Auto-Close (Share)"
    override val closeShareDesc = "Close the sheet after picking a format."
    override val batteryOptTitle = "Background Battery"
    override val batteryOptStatusOk = "Status: Unrestricted (Optimal)"
    override val batteryOptStatusRestricted = "Status: Restricted · Tap to see instructions"
    override val aboutSectionTitle = "About Ubahkan"
    override fun aboutVersion(version: String) = "Version $version · Offline File Converter"
    override val aboutAuthor = "By @dandisubhani_"
    override val updateCheckTitle = "Check for Updates"
    override fun updateAvailableTitle(version: String) = "Version $version available"
    override val updateAvailableDesc = "Download the latest version for the newest features and fixes."
    override val updateNowButton = "Update"
    override val updateStatusNever = "Never checked"
    override val updateStatusChecking = "Checking"
    override val updateStatusUpToDate = "Already up to date"
    override val licensesSectionTitle = "Open Source Licenses"
    override val licensesSectionSubtitle = "GPL-3.0, Apache 2.0 & third-party libraries"
    override val licensesDialogIntro = "Ubahkan is distributed under the GNU General Public License v3.0 (GPL-3.0) and uses the following open-source libraries:"

    override val bottomSheetTitle = "Convert Files"
    override fun bottomSheetBatchSubtitle(fileCount: Int, formatCount: Int, sizeStr: String) =
        "$fileCount files selected • $formatCount formats$sizeStr"
    override fun bottomSheetSingleSubtitle(fileName: String, sizeStr: String) = "$fileName$sizeStr"
    override val targetFormatHeader = "Target Format"
    override val chooseFormat = "SELECT"
    override val filesCountSuffix = " selected"
    override val mergeToPdfTitle = "Merge into a single PDF"
    override val mergeToPdfDesc = "When multiple image files are present."
    override val qualityTitle = "Quality"
    override fun instantLosslessSupported(src: String, tgt: String) =
        "Lossless (Instant) supported for ${src.uppercase()} → ${tgt.uppercase()}"
    override val instantNotAvailable = "Instant is not available for this format. Ubahkan will use High quality."
    override val btnConvert = "Convert"
    override fun btnConvertBatch(count: Int) = "Convert ($count Files)"
    override fun btnMergeAndConvert(count: Int) = "Merge & Convert ($count Files)"
    override fun formatExplain(format: String): String = when (format.lowercase()) {
        "mp3" -> "Audio only from the source file, small in size and playable everywhere."
        "m4a" -> "Compact audio with good quality, common on Apple devices."
        "wav" -> "Uncompressed audio, full quality but large in size."
        "flac" -> "Lossless audio, quality identical to the source."
        "ogg", "opus" -> "Compact open audio format, small in size."
        "mp4" -> "The most common video format, easy to share anywhere."
        "mkv" -> "Flexible video container, great for high quality and subtitles."
        "mov" -> "Apple video format, common from iPhones and Macs."
        "webm" -> "Compact video for the web, small in size."
        "gif" -> "Moving image without sound, great for short clips."
        "jpg", "jpeg" -> "The most common photo format, small and supported everywhere."
        "png" -> "Sharp images with transparency support."
        "webp" -> "Modern image format, smaller than JPG at the same quality."
        "bmp" -> "Uncompressed image, full quality but large in size."
        "pdf" -> "A document that looks the same on every device."
        "txt" -> "Plain text without formatting, opens in any app."
        else -> ""
    }

    override fun convertingTo(format: String) = "Converting to ${format.uppercase()}"
    override fun convertingBatchInfo(curr: Int, total: Int) = "Converting ($curr/$total)"
    override val conversionInProgress = "Conversion in progress"
    override val cancelConversion = "Cancel Conversion"
    override val runningInBackground = "Running in background"
    override val conversionFailed = "Conversion Failed"
    override val failedToConvertDefault = "Failed to convert"
    override val btnRetry = "Retry"
    override val cancelDialogTitle = "Cancel Conversion?"
    override val cancelDialogDesc = "The current conversion process will be stopped."
    override val btnConfirmCancel = "Cancel"
    override val btnContinue = "Continue"
    override val titleFinished = "Finished"
    override val titleConversionProgress = "Conversion Progress"
    override val btnMinimize = "Minimize"
    override val convertingFileProgress = "Converting Files..."
    override val progressPhrases = listOf(
        "Cooking it up, hang tight...",
        "Almost there, no napping yet...",
        "Tidying up your file...",
        "Patience, it is nearly done...",
        "Polishing it to a shine...",
        "Relax, it all happens on your phone...",
        "Plenty of time for a coffee...",
        "The engine is speeding, one moment..."
    )
    override fun progressFromSize(size: String) = "of $size"
    override val progressQueueHeader = "Queue"
    override fun progressBatchFilesCompleted(done: Int, total: Int) = "$done of $total files completed"
    override val conversionSuccess = "Conversion Successful!"
    override val singleFileSuccessDesc = "Your file has been converted successfully."
    override fun batchFilesSuccessDesc(count: Int) = "$count files converted successfully."
    override val btnViewResult = "View Result"
    override val btnBackToHome = "Back to Home"
    override val conversionPartialSuccess = "Conversion Partially Completed"
    override fun summaryPartial(completed: Int, failed: Int) = "$completed succeeded, $failed failed."
    override val defaultErrorMsg = "An error occurred while processing the file."

    override val queueWaiting = "Waiting in queue..."
    override val queueCompleted = "Conversion Completed"
    override val queueCancelled = "Cancelled"
    override val queueFailed = "Conversion Failed"
    override val serviceRunningTitle = "Ubahkan Running"
    override val servicePreparingQueue = "Preparing conversion queue..."
    override val serviceStartingConversion = "Starting conversion..."
    override fun notifConvertingBatchTitle(curr: Int, total: Int, name: String) =
        "Converting ($curr/$total): $name"
    override fun notifConvertingSingleTitle(name: String, format: String) =
        "Converting $name → ${format.uppercase()}"
    override val notifActionCancelAll = "Cancel All"
    override val notifSuccessTitle = "✅ Conversion Completed"
    override val notifPartialTitle = "⚠️ Conversion Partially Completed"
    override val notifSuccessBodySingle = "1 file successfully converted to Downloads/Ubahkan"
    override fun notifSuccessBodyBatch(count: Int) = "$count files successfully converted to Downloads/Ubahkan"
    override fun notifSummaryBody(completed: Int, failed: Int) = "$completed succeeded, $failed failed."

    override val enginePreparingMedia = "Preparing multimedia file..."
    override fun engineProcessedPercent(percent: Int) = "Processed: $percent%"
    override fun engineProcessedSeconds(seconds: Long) = "Processed: ${seconds}s"
    override val engineFinalizingValidating = "Finalizing and validating file..."
    override val engineFinished100 = "Completed 100%"
    override val engineCancelled = "Conversion cancelled."
    override fun engineErrSetup(msg: String) = "Failed to setup conversion: $msg"
    override val engineErrReadInput = "Cannot read input file."
    override val engineErrInputEmpty = "Input file is empty or invalid."
    override fun engineErrInvalidOutput(reason: String) = "Invalid output: $reason"
    override val engineErrSaveOutput = "Failed to save output to Downloads/Ubahkan"
    override val engineErrStreamCopyIncompatible =
        "Stream Copy is incompatible with this file's stream codec. Please use High or Medium quality."
    override val engineErrFormatNotSupported =
        "Target format does not support one of the source media streams."
    override fun engineErrFFmpeg(code: Int) = "FFmpeg conversion failed (Code: $code)"
    override val engineReadingPdf = "Reading PDF document..."
    override val engineErrPdfRead = "Cannot read PDF document."
    override val engineErrPdfNoPages = "PDF document has no pages."
    override fun engineRenderingPage(page: Int, total: Int) = "Rendering page $page of $total..."
    override fun enginePagesExtractedSuccess(count: Int) = "$count pages successfully extracted to Downloads/Ubahkan"
    override val engineErrSavePdfImage = "Failed to save PDF images"
    override val engineAnalyzingPdf = "Analyzing PDF size..."
    override val engineErrPdfEmpty = "PDF document is empty."
    override fun engineCompressingPage(page: Int, total: Int) = "Compressing page $page of $total..."
    override val engineErrSavePdfCompress = "Failed to save compressed PDF"
    override val engineLoadingImage = "Loading image..."
    override val engineBuildingPdfPages = "Building PDF pages..."
    override val engineErrImageToPdf = "Failed to create PDF from image"
    override fun engineBuildingPdfPage(curr: Int, total: Int) = "Building page $curr of $total..."
    override val engineSavingPdfDoc = "Saving PDF document..."
    override val engineErrSaveMergedPdf = "Failed to save merged PDF document"
    override fun engineErrMergePdf(msg: String) = "Failed to merge images to PDF: $msg"
    override val engineProcessingImage = "Processing image..."
    override val engineCompressingImage = "Compressing image..."
    override val engineErrSaveImage = "Failed to save output image"
    override val engineLoadingSvg = "Loading SVG file..."
    override val engineErrReadSvg = "Cannot read SVG file data."
    override val engineCalculatingSvgDim = "Calculating SVG vector dimensions..."
    override val engineRenderingSvg = "Rendering SVG vector to canvas..."
    override val engineCompressingSvgOut = "Compressing output image..."
    override val engineErrSaveSvg = "Failed to save SVG image"
    override val engineReadingEpub = "Reading EPUB file..."
    override val engineErrReadEpub = "Cannot read EPUB file."
    override val engineSavingEpubTxt = "Saving extracted text..."
    override val engineErrSaveTxt = "Failed to save TXT file"

    override val engineCompressingVideoTarget = "Compressing video to target size..."
    override fun engineCompressingImageAttempt(attempt: Int) = "Trying image quality (attempt $attempt)..."
    override val engineTrimmingVideo = "Trimming video..."
    override val engineSplittingVideo = "Splitting video into parts..."
    override val engineErrSplitNoParts = "No parts were produced."
    override val engineErrNoDuration = "Unable to read media duration."
    override val btnShareToWhatsAppStatus = "Share to WA Status"
    override val shareToWhatsAppFailedToast = "Could not open WhatsApp sharing."

    override val toolsActionsTitle = "Size and Cut Tools"
    override val toolsActionsSubtitle = "Compress to a target size, trim, and split videos."
    override val toolCompressVideoTitle = "Compress Video to Size"
    override val toolCompressVideoDesc = "Compress to a target size."
    override val toolCompressImageTitle = "Compress Image to Size"
    override val toolCompressImageDesc = "Compress JPG to a small size."
    override val toolTrimTitle = "Trim Video"
    override val toolTrimDesc = "Trim from time to time."
    override val toolSplitTitle = "Auto Split"
    override val toolSplitDesc = "Even parts for WA Status."
    override val toolPickHint = "Tap to pick a file"
    override val toolStartButton = "Start"
    override val toolTargetSizeLabel = "Target size"
    override val toolCustomSizeMbLabel = "Custom size (MB)"
    override val toolCustomSizeKbLabel = "Custom size (KB)"
    override val toolTrimStartLabel = "Start"
    override val toolTrimEndLabel = "End"
    override val toolTrimTimeHint = "Format mm:ss or seconds, e.g. 1:30"
    override fun toolVideoDurationInfo(duration: String) = "Video duration: $duration"
    override val toolSplitEveryLabel = "Split every"
    override fun toolSecondsUnit(seconds: Int) = "$seconds seconds"
    override val toolErrCustomSize = "Enter a valid target size greater than 0."
    override val toolErrTimeFormat = "Invalid time format. Use mm:ss or seconds."
    override val toolErrTrimRange = "End time must be after start time and within the video duration."
    override val toolErrNotVideo = "The selected file is not a video."
    override val toolErrNotImage = "The selected file is not an image."

    override val engineMergingVideos = "Merging videos..."
    override val toolsActionsBatch2Title = "Video, PDF, and Audio Tools"
    override val toolsActionsBatch2Subtitle = "Merge, split, subtitles, crop, and more."
    override val toolMergeVideoTitle = "Merge Videos"
    override val toolMergeVideoDesc = "Join clips into one file."
    override fun toolSelectedFilesInfo(count: Int) = "$count files selected, in picking order."
    override val toolErrNeedTwoFiles = "Pick at least two files to merge."
    override val engineSplittingPdf = "Splitting PDF pages..."
    override fun engineSplittingPdfPage(current: Int, total: Int) = "Splitting page $current of $total..."
    override val engineErrPdfRangeInvalid = "Invalid page range."
    override val toolSplitPdfTitle = "Split PDF"
    override val toolSplitPdfDesc = "One page, its own PDF file."
    override val toolPdfRangeLabel = "Page range"
    override val toolPdfRangeHint = "Leave empty for all pages, or enter e.g. 1-3 or 1,3,5."
    override val toolErrNotPdf = "The selected file is not a PDF."
    override val engineMergingPdfs = "Merging PDFs..."
    override fun engineMergingPdfPage(current: Int, total: Int) = "Merging page $current of $total..."
    override val toolMergePdfTitle = "Merge PDFs"
    override val toolMergePdfDesc = "Join PDFs into one file."
    override val engineBurningSubtitles = "Burning subtitles into video..."
    override val engineErrSubtitleMissing = "Subtitle file not found."
    override val toolHardsubTitle = "Burn Subtitles (Hardsub)"
    override val toolHardsubDesc = "Burn .srt into the video."
    override val toolSubtitleFileLabel = "Subtitle file (.srt)"
    override val toolPickCompanionButton = "Pick file"
    override val toolErrCompanionMissing = "Pick the companion file first."
    override val toolErrSubtitleExt = "The subtitle file must have the .srt extension."
    override val engineCroppingVideo = "Cropping video frame..."
    override val toolCropTitle = "Crop Video Ratio"
    override val toolCropDesc = "Ratios 9:16, 1:1, or 16:9."
    override val toolCropRatioLabel = "Frame ratio"
    override val engineResizingVideo = "Resizing video..."
    override val toolResizeTitle = "Resize Video"
    override val toolResizeDesc = "Lower to 720p, 480p, 360p."
    override val toolResizeLabel = "Target resolution"
    override val engineMutingVideo = "Muting video..."
    override val toolMuteTitle = "Mute Video"
    override val toolMuteDesc = "Remove the audio track."
    override val engineReplacingAudio = "Replacing video audio..."
    override val engineErrAudioMissing = "Replacement audio file not found."
    override val toolReplaceAudioTitle = "Replace Video Audio"
    override val toolReplaceAudioDesc = "Replace the video soundtrack."
    override val toolAudioFileLabel = "Replacement audio file"
    override val engineConvertingHeic = "Converting HEIC to JPG..."
    override val engineErrHeicUnsupported = "HEIC conversion requires Android 9 or newer."
    override fun engineErrHeicDecode(msg: String) = "Failed to read the HEIC file: $msg"
    override val toolHeicTitle = "HEIC to JPG"
    override val toolHeicDesc = "HEIC photos become JPG."
    override val toolErrNotHeic = "The selected file is not HEIC or HEIF."
    override val toolInfoTitle = "File Info"
    override val toolInfoDesc = "See file details."
    override val infoFormatLabel = "Format"
    override val infoDurationLabel = "Duration"
    override val infoSizeLabel = "Size"
    override val infoResolutionLabel = "Resolution"
    override val infoVideoCodecLabel = "Video codec"
    override val infoAudioCodecLabel = "Audio codec"
    override val infoBitrateLabel = "Bitrate"
    override val infoLoading = "Reading file info..."
    override val infoFailed = "Could not read info for this file."
    override val engineNormalizingVolume = "Normalizing volume..."
    override val toolNormalizeTitle = "Normalize Volume"
    override val toolNormalizeDesc = "Even out loudness."
    override val toolErrNotMedia = "The selected file is not a video or audio file."

    override val validatorFileNotFound = "Output file not found in storage."
    override val validatorNotRegularFile = "Output path is not a regular file."
    override val validatorOutputEmpty = "Output file is empty (0 bytes)."
    override val validatorCorruptImage = "Output image format is corrupted or cannot be parsed."
    override val validatorEmptyTxt = "Converted text file is empty."
    override fun validatorUnreadableTxt(msg: String) = "Text file cannot be read: $msg"
    override fun epubErrNotZip(msg: String) = "File is not a valid ZIP/EPUB container: $msg"
    override val epubStepMetadata = "Finding EPUB metadata file..."
    override val epubErrNoContainer = "META-INF/container.xml or OPF rootfile not found in EPUB file."
    override fun epubErrNoOpf(path: String) = "OPF package file ($path) not found in EPUB container."
    override val epubStepSpine = "Reading structure and reading order (Spine)..."
    override val epubErrEmptySpine = "OPF Spine has no valid reading order itemref."
    override fun epubStepExtractingChapter(curr: Int, total: Int) = "Extracting chapter $curr of $total..."
    override val epubErrNoChapterText = "No readable chapter text found in EPUB Spine."
    override val epubBookTitlePrefix = "TITLE: "
    override val epubBookAuthorPrefix = "AUTHOR: "

    // Document Scanner (1.5.0)
    override val scanTitle = "Scan Document"
    override val scanDesc = "Turn paper into a tidy PDF."
    override val scanCaptureHint = "Position the document in the frame."
    override val scanImportGallery = "Import from Gallery"
    override val scanCameraDeniedTitle = "Camera permission denied"
    override val scanCameraDeniedDesc = "Without camera permission, you can still import a photo from the gallery."
    override val scanAllowCamera = "Allow Camera"
    override val scanFlash = "Flash"
    override val scanTakePhoto = "Take Photo"
    override val scanCancel = "Cancel scan"
    override val scanOpenCvFailed = "The scanner engine failed to start. Try importing from the gallery."
    override val scanCaptureFailed = "Failed to take the photo. Try again."
    override val scanAdjustTitle = "Adjust Corners"
    override val scanAdjustHint = "Drag the points to the document corners."
    override val scanResetCorners = "Reset"
    override val scanNext = "Continue"
    override val scanFilterTitle = "Document Filter"
    override val scanBrightnessLabel = "Brightness"
    override val scanContrastLabel = "Contrast"
    override val scanUsePage = "Use This Page"
    override val scanProcessing = "Processing..."
    override val scanFilterOriginal = "Original"
    override val scanFilterGrayscale = "Grayscale"
    override val scanFilterBlackWhite = "Black and White"
    override val scanFilterWhitePaper = "White Paper"
    override val scanFilterShadow = "Shadow Removal"
    override val scanFilterSharpen = "Sharpen Text"
    override val scanFilterContrastBoost = "Strong Contrast"
    override val scanFilterColorPop = "Color Pop"
    override val scanFilterSepia = "Sepia"
    override val scanFilterNegative = "Negative"
    override val scanFilterBlueprint = "Blueprint"
    override val scanFilterPhotocopy = "Photocopy"
    override val scanPagesTitle = "Scanned Pages"
    override fun scanPageNumber(index: Int) = "Page $index"
    override val scanAddPage = "Add Page"
    override val scanReadText = "Read Text"
    override val scanSignature = "Signature"
    override val scanSavePdf = "Save as PDF"
    override val scanNoPages = "No pages yet. Add your first page."
    override val scanDeletePage = "Delete page"
    override val scanMoveUp = "Move up"
    override val scanMoveDown = "Move down"
    override val scanSelectPageHint = "Tap a page to select it before Read Text or Signature."
    override val scanOcrTitle = "Recognized Text"
    override val scanOcrEmpty = "No text could be recognized."
    override val scanOcrRunning = "Reading text..."
    override val scanCopyText = "Copy Text"
    override val scanCopiedToast = "Text copied."
    override val scanSignatureTitle = "Draw Signature"
    override val scanSignatureHint = "Draw your signature in this area."
    override val scanSignatureClear = "Clear Drawing"
    override val scanDoneButton = "Done"
    override val scanSignatureAppliedToast = "Signature applied."
    override val scanSavedTitle = "PDF Saved"
    override fun scanSavedLocation(name: String) = "Saved as $name in Downloads/Ubahkan."
    override val scanOpenFile = "Open"
    override val scanSaveFailedToast = "Failed to save the PDF. Try again."
}

val LocalAppStrings = staticCompositionLocalOf<Strings> { IndonesianStrings }

object AppStrings {
    private var _currentLanguage: String = "id"
    val currentLanguage: String get() = _currentLanguage

    val current: Strings
        @Composable
        @ReadOnlyComposable
        get() = LocalAppStrings.current

    fun init(context: Context) {
        _currentLanguage = UbahkanPrefs(context).appLanguage
    }

    fun setLanguage(lang: String) {
        _currentLanguage = if (lang.equals("en", ignoreCase = true)) "en" else "id"
    }

    fun get(lang: String = _currentLanguage): Strings {
        return if (lang.equals("en", ignoreCase = true)) EnglishStrings else IndonesianStrings
    }
}
