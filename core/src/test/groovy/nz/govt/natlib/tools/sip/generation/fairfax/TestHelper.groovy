package nz.govt.natlib.tools.sip.generation.fairfax

import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult
import nz.govt.natlib.tools.sip.extraction.SipXmlExtractor
import nz.govt.natlib.tools.sip.files.FilesFinder
import nz.govt.natlib.tools.sip.generation.parameters.Spreadsheet

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static org.hamcrest.core.Is.is
import static org.junit.Assert.assertThat

/**
 * Useful methods for use across different unit tests.
 *
 */
@Slf4j
class TestHelper {
    static final String RESOURCES_FOLDER = "nz/govt/natlib/tools/sip/generation/fairfax"

    /**
     * Returns the contents of the file from the given filename and resources folder.
     * Make an attempt to open the file as a resource.
     * If that fails, try to open the file with the path resourcesFolder/filename. This should be relative
     * to the current working directory if the the resourcesFolder is a relative path.
     *
     * @param filename
     * @param resourcesFolder
     * @return
     */
    static String getTextFromResourceOrFile(String filename, String resourcesFolder = RESOURCES_FOLDER) {
        String resourcePath = "${resourcesFolder}/${filename}"
        String localPath = "src/test/resources/${resourcePath}"

        String text
        InputStream inputStream = TestHelper.class.getResourceAsStream(filename)
        if (inputStream == null) {
            File inputFile = new File(localPath)
            if (!inputFile.exists()) {
                inputFile = new File(new File(""), localPath)
            }
            text = inputFile.text
        } else {
            text = inputStream.text
        }
        return text
    }

    /**
     * Returns the file from the given filename and resources folder.
     * Make an attempt to open the file as a resource.
     * If that fails, try to open the file with the path resourcesFolder/filename. This should be relative
     * to the current working directory if the resourcesFolder is a relative path.
     *
     * @param filename
     * @param resourcesFolder
     * @return
     */
    static File getFileFromResourceOrFile(String filename, String resourcesFolder = RESOURCES_FOLDER) {
        String resourcePath = "${resourcesFolder}/${filename}"
        String localPath = "src/test/resources/${resourcePath}"

        URL resourceURL = TestHelper.class.getResource(filename)
        File resourceFile
        if (resourceURL != null) {
            resourceFile = new File(resourceURL.getFile())
        }
        if (resourceFile != null && (resourceFile.isFile() || resourceFile.isDirectory())) {
            return resourceFile
        } else {
            File returnFile = new File(localPath)
            return returnFile
        }
    }

    /**
     * When loading files from a resource path, we assume that there aren't that many files (tens rather than thousands)
     * so we use use the traditional java.io approach to listing files.
     *
     * @param folderResourcePath
     * @return
     */
    static List<File> getResourceFiles(String folderResourcePath, boolean isRegexNotGlob, boolean matchFilenameOnly,
                                        String pattern) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader()
        URL url = loader.getResource(folderResourcePath)
        String path = url.getPath()

        List<File> files = Arrays.asList(new File(path).listFiles())
        println("All files:")
        files.each { file ->
            println("folderResourcePath=${folderResourcePath} found file=${file.getCanonicalPath()}")
        }
        if (!isRegexNotGlob) {
            throw new RuntimeException("Globbing not supported for finding resource files, use a regex pattern instead")
        }
        List<File> filteredFiles
        if (matchFilenameOnly) {
            filteredFiles = files.findAll { File  file ->
                file.getName() ==~ /${pattern}/
            }
        } else {
            filteredFiles = files.findAll { File file ->
                file.getCanonicalPath() ==~ /${pattern}/
            }
        }
        return filteredFiles
    }

    static List<File> getMatchingFiles(Collection<File> files, String pattern) {
        return files.findAll { file ->
            file.getCanonicalPath() ==~ /${pattern}/
        }
    }

    static FairfaxSpreadsheet loadSpreadsheet(String resourcePath, String localPath, String importParametersFilename, String idColumnName) {
        Spreadsheet spreadsheet
        InputStream defaultSpreadsheetInputStream = FairfaxSpreadsheet.getResourceAsStream(resourcePath)
        if (defaultSpreadsheetInputStream == null) {
            File spreadsheetFile = new File("${localPath}/${importParametersFilename}")
            spreadsheet = Spreadsheet.fromJson(idColumnName, spreadsheetFile.text, true, true)
        } else {
            spreadsheet = Spreadsheet.fromJson(idColumnName, defaultSpreadsheetInputStream.text, true, true)
        }
        FairfaxSpreadsheet fairfaxSpreadsheet = new FairfaxSpreadsheet(spreadsheet)

        return fairfaxSpreadsheet
    }

    // TODO Could handle more than one pattern (see https://www.javacodegeeks.com/2012/11/java-7-file-filtering-using-nio-2-part-2.html)
    static List<File> findFiles(String resourcePath, String localPath, boolean isRegexNotGlob, boolean matchFilenameOnly, String pattern) {
        List<File> filesList = [ ]
        // We check if we're using a resource stream to load the files, otherwise we are loading from the file system
        InputStream doWeChooseAResourceStream = TestHelper.getResourceAsStream(resourcePath)
        if (doWeChooseAResourceStream == null) {
            Path filesPath = Paths.get(localPath)
            if (!Files.exists(filesPath) || !Files.isDirectory(filesPath)) {
                log.warn("Path '${filesPath}' does not exist is not a directory. Returning empty file list.")
                return filesList
            }

            filesList = FilesFinder.getMatchingFiles(filesPath, isRegexNotGlob, matchFilenameOnly, pattern)
            return filesList
        } else {
            List<File> files = TestHelper.getResourceFiles(resourcePath)
            filesList = TestHelper.getMatchingFiles(files, pattern)

            return filesList
        }
    }

    static void assertExpectedSipMetadataValues(SipXmlExtractor sipForValidation, String title, int year, int month,
    int dayOfMonth, String ieEntityType, String objectIdentifierType, String objectcIdentifierValue, String policyId,
    String preservationType, String usageType, boolean isDigitalOriginal, int revisionNumber) {
        assertThat("title", sipForValidation.getTitle(), is(title))
        assertThat("year", sipForValidation.getYear(), is(year))
        assertThat("month", sipForValidation.getMonth(), is(month))
        assertThat("dayOfMonth", sipForValidation.getDayOfMonth(), is(dayOfMonth))
        assertThat("ieEntityType", sipForValidation.getIEEntityType(), is(ieEntityType))
        assertThat("objectIdentifierType", sipForValidation.getObjectIdentifierType(), is(objectIdentifierType))
        assertThat("objectIdentifierValue", sipForValidation.getObjectIdentifierValue(), is(objectcIdentifierValue))
        assertThat("policyId", sipForValidation.getPolicyId(), is(policyId))
        assertThat("preservationType", sipForValidation.getPreservationType(), is(preservationType))
        assertThat("usageType", sipForValidation.getUsageType(), is(usageType))
        assertThat("digitalOriginal", sipForValidation.getDigitalOriginal(), is(isDigitalOriginal))
        assertThat("revisionNumber", sipForValidation.getRevisionNumber(), is(revisionNumber))
    }

    static void assertExpectedSipFileValues(SipXmlExtractor sipForValidation, int idIndex, String originalName,
                                            String originalPath, long sizeBytes, String fixityType, String fixityValue,
                                            String fileLabel, String mimeType) {
        GPathResult fileGPath = sipForValidation.getFileIdRecord(idIndex)
        // NOTE Any unit test errors in this section (such as:
        // java.lang.NoSuchMethodError: org.hamcrest.Matcher.describeMismatch(Ljava/lang/Object;Lorg/hamcrest/Description;)V
        // could indicate that a null value is coming into the test, which could mean that the value is not in the SIP's
        // XML.
        assertThat("fileWrapper${idIndex}.fileOriginalName", sipForValidation.getFileOriginalName(fileGPath), is(originalName))
        assertThat("fileWrapper${idIndex}.fileOriginalPath", sipForValidation.getFileOriginalPath(fileGPath), is(originalPath))
        assertThat("fileWrapper${idIndex}.fileSizeBytes", sipForValidation.getFileSizeBytes(fileGPath), is(sizeBytes))
        assertThat("fileWrapper${idIndex}.fixityType", sipForValidation.getFileFixityType(fileGPath), is(fixityType))
        assertThat("fileWrapper${idIndex}.fixityValue", sipForValidation.getFileFixityValue(fileGPath), is(fixityValue))
        assertThat("fileWrapper${idIndex}.label", sipForValidation.getFileLabel(fileGPath), is(fileLabel))
        assertThat("fileWrapper${idIndex}.mimeType", sipForValidation.getFileMimeType(fileGPath), is(mimeType))
        // This is dependent on the filesystem, so we can't really test this
        //assertThat("fileWrapper${idIndex}.modificationDate", sipForValidation.getFileModificationDate(fileGPath), is(LocalDateTime.of(
        //        LocalDate.of(2015, 7, 29),
        //        LocalTime.of(0, 0, 0, 0))))
        //assertThat("fileWrapper${idIndex}.creationDate", sipForValidation.getFileCreationDate(fileGPath), is(LocalDateTime.of(
        //        LocalDate.of(2015, 7, 29),
        //        LocalTime.of(0, 0, 0, 0))))

    }
}
