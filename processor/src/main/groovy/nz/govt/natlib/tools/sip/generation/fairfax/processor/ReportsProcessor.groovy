package nz.govt.natlib.tools.sip.generation.fairfax.processor

import groovy.util.logging.Slf4j
import nz.govt.natlib.m11n.tools.automation.logging.Timekeeper
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFile
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFileTitleEditionKey
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxSpreadsheet
import nz.govt.natlib.tools.sip.generation.fairfax.processor.support.TitleCodeByDateSummary
import nz.govt.natlib.tools.sip.pdf.PdfInformationExtractor

import java.time.LocalDate

@Slf4j
class ReportsProcessor {
    Timekeeper timekeeper
    Set<String> recognizedTitleCodes = []
    Set<String> unrecognizedTitleCodes = []

    ReportsProcessor(Timekeeper timekeeper) {
        this.timekeeper = timekeeper
    }

    void listFiles(File sourceFolder) {
        log.info("STARTING listFiles")

        // Clear the set of recognized and unrecognized names before processing begins
        recognizedTitleCodes = []
        unrecognizedTitleCodes = []
        Set<FairfaxFileTitleEditionKey> recognizedTitleCodeEditionCodes = []
        Set<FairfaxFileTitleEditionKey> unrecognizedTitleCodeEditionCodes = []
        Set<File> invalidFiles = []

        log.info("sourceFolder=${sourceFolder}")

        FairfaxSpreadsheet fairfaxSpreadsheet = FairfaxSpreadsheet.defaultInstance()
        Set<String> allNameKeys = fairfaxSpreadsheet.allTitleCodeKeys
        Set<FairfaxFileTitleEditionKey> allNameEditionKeys = fairfaxSpreadsheet.allTitleCodeEditionCodeKeys

        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true
        String pattern = ".*?\\.pdf"
        List<File> foundFiles = ProcessorUtils.findFiles(sourceFolder.getAbsolutePath(), isRegexNotGlob,
                matchFilenameOnly, sortFiles, pattern, timekeeper)
        List<FairfaxFile> fairfaxFiles = foundFiles.collect { File file ->
            new FairfaxFile(file)
        }

        FairfaxFile previousFile
        fairfaxFiles.each { FairfaxFile fairfaxFile ->
            if (fairfaxFile.isValidName()) {
                if (allNameKeys.contains(fairfaxFile.titleCode)) {
                    if (!recognizedTitleCodes.contains(fairfaxFile.titleCode)) {
                        recognizedTitleCodes.add(fairfaxFile.titleCode)
                        log.info("listFiles adding recognizedTitleCode=${fairfaxFile.titleCode}")
                    }
                } else {
                    if (!unrecognizedTitleCodes.contains(fairfaxFile.titleCode)) {
                        unrecognizedTitleCodes.add(fairfaxFile.titleCode)
                        log.info("listFiles adding unrecognizedTitleCode=${fairfaxFile.titleCode}")
                    }
                }
                FairfaxFileTitleEditionKey fairfaxFileTitleEditionKey = new FairfaxFileTitleEditionKey(
                        titleCode: fairfaxFile.titleCode, editionCode: fairfaxFile.editionCode)
                if (allNameEditionKeys.contains(fairfaxFileTitleEditionKey)) {
                    if (!recognizedTitleCodeEditionCodes.contains(fairfaxFileTitleEditionKey)) {
                        recognizedTitleCodeEditionCodes.add(fairfaxFileTitleEditionKey)
                        log.info("listFiles adding recognizedTitleCodeEditionCodes=${fairfaxFileTitleEditionKey}")
                    }
                } else {
                    if (!unrecognizedTitleCodeEditionCodes.contains(fairfaxFileTitleEditionKey)) {
                        unrecognizedTitleCodeEditionCodes.add(fairfaxFileTitleEditionKey)
                        log.info("listFiles adding unrecognizedTitleCodeEditionCodes=${fairfaxFileTitleEditionKey}")
                    }
                }
            } else {
                invalidFiles.add(fairfaxFile.file)
            }

            if (previousFile != null) {
                if (previousFile.titleCode != fairfaxFile.titleCode) {
                    println("* * * CHANGE OF PREFIX * * *")
                } else if (previousFile.editionCode != fairfaxFile.editionCode) {
                    println("* * * CHANGE OF EDITION * * *")
                } else if (previousFile.dateYear != fairfaxFile.dateYear &&
                        previousFile.dateMonthOfYear != fairfaxFile.dateMonthOfYear &&
                        previousFile.dateDayOfMonth != fairfaxFile.dateDayOfMonth) {
                    println("* * * CHANGE OF DATE * * *")
                }
            }
            println(fairfaxFile)

            previousFile = fairfaxFile
        }

        log.info("* * * *")
        log.info("Recognized tileCodes:")
        recognizedTitleCodes.each { String recognizedName ->
            log.info("    ${recognizedName}")
        }
        log.info("* * * *")
        log.info("Recognized titleCodes and editionCodes:")
        recognizedTitleCodeEditionCodes.each { FairfaxFileTitleEditionKey fairfaxFileNameEditionKey ->
            log.info("    ${fairfaxFileNameEditionKey}")
        }
        log.info("* * * *")
        log.info("UNRECOGNIZED titleCodes:")
        unrecognizedTitleCodes.each { String recognizedName ->
            log.info("    ${recognizedName}")
        }
        log.info("* * * *")
        log.info("UNRECOGNIZED titleCodes and editionCodes:")
        unrecognizedTitleCodeEditionCodes.each { FairfaxFileTitleEditionKey fairfaxFileNameEditionKey ->
            log.info("    ${fairfaxFileNameEditionKey}")
        }
        log.info("* * * *")
        log.info("INVALID files:")
        invalidFiles.each { File file ->
            log.info("    ${file.getCanonicalPath()}")
        }
        log.info("* * * *")

        log.info("ENDING listFiles")
        timekeeper.logElapsed()
    }

    void statisticalAudit(File sourceFolder) {
        log.info("STARTING statisticalAudit")

        // Clear the set of recognized and unrecognized names before processing begins
        recognizedTitleCodes = []
        unrecognizedTitleCodes = []
        Set<FairfaxFileTitleEditionKey> recognizedTitleCodeEditionCodes = []
        Set<FairfaxFileTitleEditionKey> unrecognizedTitleCodeEditionCodes = []
        Set<File> invalidFiles = []

        log.info("sourceFolder=${sourceFolder}")

        FairfaxSpreadsheet fairfaxSpreadsheet = FairfaxSpreadsheet.defaultInstance()
        Set<String> allNameKeys = fairfaxSpreadsheet.allTitleCodeKeys
        Set<FairfaxFileTitleEditionKey> allNameEditionKeys = fairfaxSpreadsheet.allTitleCodeEditionCodeKeys

        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true
        String pattern = ".*?\\.pdf"
        List<File> foundFiles = ProcessorUtils.findFiles(sourceFolder.getAbsolutePath(), isRegexNotGlob,
                matchFilenameOnly, sortFiles, pattern, timekeeper)
        Map<LocalDate, Map<String, TitleCodeByDateSummary>> dateToTitleCodeMap = [ : ]
        foundFiles.each { File file ->
            FairfaxFile fairfaxFile = new FairfaxFile(file)
            if (fairfaxFile.isValidName()) {
                if (allNameKeys.contains(fairfaxFile.titleCode)) {
                    if (!recognizedTitleCodes.contains(fairfaxFile.titleCode)) {
                        recognizedTitleCodes.add(fairfaxFile.titleCode)
                        log.info("listFiles adding recognizedTitleCode=${fairfaxFile.titleCode}")
                    }
                } else {
                    if (!unrecognizedTitleCodes.contains(fairfaxFile.titleCode)) {
                        unrecognizedTitleCodes.add(fairfaxFile.titleCode)
                        log.info("listFiles adding unrecognizedTitleCode=${fairfaxFile.titleCode}")
                    }
                }
                FairfaxFileTitleEditionKey fairfaxFileTitleEditionKey = new FairfaxFileTitleEditionKey(
                        titleCode: fairfaxFile.titleCode, editionCode: fairfaxFile.editionCode)
                if (allNameEditionKeys.contains(fairfaxFileTitleEditionKey)) {
                    if (!recognizedTitleCodeEditionCodes.contains(fairfaxFileTitleEditionKey)) {
                        recognizedTitleCodeEditionCodes.add(fairfaxFileTitleEditionKey)
                        //log.info("listFiles adding recognizedTitleCodeEditionCodes=${fairfaxFileTitleEditionKey}")
                    }
                } else {
                    if (!unrecognizedTitleCodeEditionCodes.contains(fairfaxFileTitleEditionKey)) {
                        unrecognizedTitleCodeEditionCodes.add(fairfaxFileTitleEditionKey)
                        //log.info("listFiles adding unrecognizedTitleCodeEditionCodes=${fairfaxFileTitleEditionKey}")
                    }
                }

                LocalDate localDate = fairfaxFile.dateAsLocalDate()
                Map<String, TitleCodeByDateSummary> titleCodeToSummaryMap
                if (dateToTitleCodeMap.containsKey(localDate)) {
                    titleCodeToSummaryMap = dateToTitleCodeMap.get(localDate)
                } else {
                    titleCodeToSummaryMap = [:]
                    dateToTitleCodeMap.put(localDate, titleCodeToSummaryMap)
                }
                TitleCodeByDateSummary titleCodeByDateSummary
                if (titleCodeToSummaryMap.containsKey(fairfaxFile.titleCode)) {
                    titleCodeByDateSummary = titleCodeToSummaryMap.get(fairfaxFile.titleCode)
                } else {
                    titleCodeByDateSummary = new TitleCodeByDateSummary(localDate: localDate,
                            titleCode: fairfaxFile.titleCode)
                    titleCodeToSummaryMap.put(fairfaxFile.titleCode, titleCodeByDateSummary)
                }
                titleCodeByDateSummary.addFile(fairfaxFile)
            } else {
                invalidFiles.add(file)
            }
        }

        log.info("* * * *")
        log.info("Recognized tileCodes:")
        recognizedTitleCodes.each { String recognizedName ->
            log.info("    ${recognizedName}")
        }
        log.info("* * * *")
        log.info("Recognized titleCodes and editionCodes:")
        recognizedTitleCodeEditionCodes.each { FairfaxFileTitleEditionKey fairfaxFileNameEditionKey ->
            log.info("    ${fairfaxFileNameEditionKey}")
        }
        log.info("* * * *")
        log.info("UNRECOGNIZED titleCodes:")
        unrecognizedTitleCodes.each { String recognizedName ->
            log.info("    ${recognizedName}")
        }
        log.info("* * * *")
        log.info("UNRECOGNIZED titleCodes and editionCodes:")
        unrecognizedTitleCodeEditionCodes.each { FairfaxFileTitleEditionKey fairfaxFileNameEditionKey ->
            log.info("    ${fairfaxFileNameEditionKey}")
        }
        log.info("* * * *")
        log.info("INVALID files:")
        invalidFiles.each { File file ->
            log.info("    ${file.getCanonicalPath()}")
        }
        log.info("* * * *")

        println("date|total_files|title_code|out-of-sequence-files|duplicate-files")
        String spreadsheetSeparator = "|"
        List<LocalDate> sortedDates = dateToTitleCodeMap.keySet().sort()
        sortedDates.each { LocalDate dateKey ->
            Map<String, TitleCodeByDateSummary> titleCodeToSummaryMap = dateToTitleCodeMap.get(dateKey)
            List<String> sortedTitleCodes = titleCodeToSummaryMap.keySet().sort()
            boolean firstForDate = true
            long totalFilesForDate = 0L
            sortedTitleCodes.each { String titleCode ->
                TitleCodeByDateSummary titleCodeByDateSummary = titleCodeToSummaryMap.get(titleCode)
                if (firstForDate) {
                    print("${dateKey}")
                    firstForDate = false
                }
                println("${spreadsheetSeparator}${titleCodeByDateSummary.forSpreadsheet(spreadsheetSeparator)}")
                totalFilesForDate += titleCodeByDateSummary.files.size()
            }
            println("${spreadsheetSeparator}${totalFilesForDate}")
        }

        log.info("ENDING statisticalAudit")
        timekeeper.logElapsed()
    }

    void extractMetadata(File sourceFolder) {
        log.info("STARTING extractMetadata doLast")
        FileNameFinder fileNameFinder = new FileNameFinder()
        List<String> filenames = fileNameFinder.getFileNames(sourceFolder.getAbsolutePath(), "**/*.pdf")
        List<File> pdfFiles = filenames.collect { String filename ->
            new File(filename)
        }

        pdfFiles.each { File pdfFile ->
            log.info("* * * * *")
            log.info("${pdfFile.getCanonicalPath()} METADATA:")
            Map<String, String> pdfMetadata = PdfInformationExtractor.extractMetadata(pdfFile)
            pdfMetadata.each { String key, String value ->
                log.info("    key=${key}, value=${value}")
            }
            log.info("* * * * *")
            log.info("* * * * *")
            log.info("${pdfFile.getCanonicalPath()} TEXT:")
            String text = PdfInformationExtractor.extractText(pdfFile)
            log.info("${text}")
            log.info("* * * * *")
            log.info("* * * * *")
            log.info("")
        }

        timekeeper.logElapsed()
    }

}