package nz.govt.natlib.tools.sip.generation.fairfax.processor

import groovy.util.logging.Slf4j
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFile
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFilesProcessor
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxSpreadsheet
import nz.govt.natlib.tools.sip.processing.ProcessLogger
import nz.govt.natlib.tools.sip.state.SipProcessingState

import java.time.LocalDate

/**
 * For calling from gradle build scripts.
 */
@Slf4j
class ReadyForIngestionProcessor {
    FairfaxSpreadsheet fairfaxSpreadsheet
    ProcessorConfiguration processorConfiguration

    ReadyForIngestionProcessor(ProcessorConfiguration processorConfiguration) {
        this.processorConfiguration = processorConfiguration
    }

    SipProcessingState processTitleCodeFolder(File titleCodeFolder, File destinationFolder, File forReviewFolder,
                                              String dateString, String titleCode) {
        // Process the files in the titleCode folder
        ProcessLogger processLogger = new ProcessLogger()
        processLogger.startSplit()

        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true
        // Only process PDF files
        String pattern = '\\w{5,7}-\\d{8}-\\w{3,4}.*?\\.[pP]{1}[dD]{1}[fF]{1}'

        log.info("START processTitleCodeFolder for pattern=${pattern}, titleCodeFolder=${titleCodeFolder.getCanonicalPath()}")
        processorConfiguration.timekeeper.logElapsed()

        List<File> allFiles = ProcessorUtils.findFiles(titleCodeFolder.getAbsolutePath(), isRegexNotGlob,
                matchFilenameOnly, sortFiles, pattern, processorConfiguration.timekeeper)

        SipProcessingState sipProcessingState = new SipProcessingState()
        // Process the folder as a single collection of files
        // TODO Note that there may be multiple destinations (as there could be different editions of the same titleCode for a given day).
        String sipAsXml = FairfaxFilesProcessor.processCollectedFiles(sipProcessingState, fairfaxSpreadsheet, allFiles)

        File sipAndFilesFolder
        if (sipProcessingState.complete && sipProcessingState.successful) {
            sipAndFilesFolder = new File(destinationFolder,
                    "${sipProcessingState.ieEntityType.getDisplayName()}/${dateString}_${titleCode}${sipProcessingState.identifier}")
        } else {
            sipAndFilesFolder = new File(forReviewFolder,
                    "${sipProcessingState.ieEntityType.getDisplayName()}/${dateString}_${titleCode}${sipProcessingState.identifier}")
        }
        // TODO may need to adjust logic for creation of content/streams folder
        File contentStreamsFolder = new File(sipAndFilesFolder, "content/streams")
        File unrecognizedFilesFolder = new File(forReviewFolder, "UNRECOGNIZED/${dateString}/${titleCode}")

        boolean hasSipAndFilesFolder
        boolean hasUnrecognizedFilesFolder
        // Move or copy the processed files to the destination folder
        if ((sipProcessingState.validFiles.size() > 0 || sipProcessingState.invalidFiles.size() > 0)) {
            hasSipAndFilesFolder = true
            if (!sipAndFilesFolder.exists() && processorConfiguration.createDestination) {
                sipAndFilesFolder.mkdirs()
                contentStreamsFolder.mkdirs()
            }
        }
        ProcessorUtils.copyOrMoveFiles(sipProcessingState.validFiles, contentStreamsFolder, processorConfiguration.moveFiles)
        ProcessorUtils.copyOrMoveFiles(sipProcessingState.invalidFiles, contentStreamsFolder, processorConfiguration.moveFiles)

        // If the files aren't recognized, then dump the files in an exception folder
        if (sipProcessingState.unrecognizedFiles.size() > 0) {
            hasUnrecognizedFilesFolder = true
            if (!unrecognizedFilesFolder.exists() && processorConfiguration.createDestination) {
                unrecognizedFilesFolder.mkdirs()
            }
        }
        ProcessorUtils.copyOrMoveFiles(sipProcessingState.unrecognizedFiles, unrecognizedFilesFolder, processorConfiguration.moveFiles)

        // Write out the SipProcessingState
        Date now = new Date()
        File sipProcessingStateFile = new File(sipAndFilesFolder,
                "sipProcessingState_${ProcessorUtils.FILE_TIMESTAMP_FORMATTER.format(now)}.txt")
        sipProcessingStateFile.write(sipProcessingState.toString())

        // Write out the SIP file
        File sipFile = new File(sipAndFilesFolder, "content/mets.xml")
        sipFile.write(sipAsXml)

        log.info("END processTitleCodeFolder for pattern=${pattern}, titleCodeFolder=${titleCodeFolder.getCanonicalPath()}")
        processorConfiguration.timekeeper.logElapsed()

        if (hasSipAndFilesFolder) {
            processLogger.copySplit(sipAndFilesFolder, "Process-Name-Folder", false)
        }
        if (hasUnrecognizedFilesFolder) {
            processLogger.copySplit(unrecognizedFilesFolder, "Process-Name-Folder", true)
        }
        return sipProcessingState
    }

    // See README.md for folder descriptions and structures.
    void process() {
        if (processorConfiguration.createDestination) {
            processorConfiguration.targetForIngestionFolder.mkdirs()
        }

        this.fairfaxSpreadsheet = FairfaxSpreadsheet.defaultInstance()

        // Loop through the dates in sequence, finding and processing files
        LocalDate currentDate = processorConfiguration.startingDate
        while (currentDate <= processorConfiguration.endingDate) {
            String currentDateString = FairfaxFile.LOCAL_DATE_TIME_FORMATTER.format(currentDate)
            File dateFolder = new File(processorConfiguration.sourceFolder, currentDateString)
            if (dateFolder.exists() && dateFolder.isDirectory()) {
                dateFolder.listFiles().each { File subFile ->
                    if (subFile.isDirectory()) {
                        // we want to process this directory, which should be a <titleCode>
                        processTitleCodeFolder(subFile, processorConfiguration.targetForIngestionFolder,
                                processorConfiguration.forReviewFolder, currentDateString, subFile.getName())
                    } else {
                        log.info("Skipping ${subFile.getCanonicalPath()} as not directory=${subFile.isDirectory()}")
                    }
                }
            } else {
                log.info("Skipping ${dateFolder.getCanonicalPath()} as exists=${dateFolder.exists()}, directory=${dateFolder.isDirectory()}")
            }
            currentDate = currentDate.plusDays(1L)
        }
    }

}
